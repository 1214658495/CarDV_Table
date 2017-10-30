package com.bydauto.car.i_key.cardv_table;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import com.bydauto.car.i_key.cardv_table.connect.CmdChannel;
import com.bydauto.car.i_key.cardv_table.connect.CmdChannelBLE;
import com.bydauto.car.i_key.cardv_table.connect.CmdChannelWIFI;
import com.bydauto.car.i_key.cardv_table.connect.DataChannel;
import com.bydauto.car.i_key.cardv_table.connect.DataChannelWIFI;
import com.bydauto.car.i_key.cardv_table.connect.IChannelListener;
import com.bydauto.car.i_key.cardv_table.util.ServerConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by jli on 9/8/14.
 */
public class RemoteCam implements IChannelListener {
    private final static String TAG = "RemoteCam";

    public static final int CAM_CONNECTIVITY_INVALID = 0;
    public static final int CAM_CONNECTIVITY_BT_BT = 1;
    public static final int CAM_CONNECTIVITY_BLE_WIFI = 2;
    public static final int CAM_CONNECTIVITY_WIFI_WIFI = 3;
    public static final int CAM_CONNECTIVITY_BT_WIFI = 4;

    private int mConnectivityType;
    private String mBlueAddrRequested;
    private String mWifiSSIDRequested;
    private String mBlueAddrConnected;
    private String mWifiSSIDConnected;
    private String mGetFileName;
    private String mPutFileName;
    private String mZoomInfoType;

    private Boolean mfDataChannelConnected = false;
    private String mWifiIpAddr;
    private final Context mContext;
    private CmdChannel mCmdChannel;
    private DataChannel mDataChannel;
    private IChannelListener mListener; // �ӿ�

    private String mWifiHostURL;
    // private String mSDCardDirectory;
    private String mVideoFolder;
    private String mEventFolder;
    private String mPhotoFolder;
    private String sw_ver;
    private String hw_ver;
    private String uuid;

    private int mMediaInfoStep;
    private String mMediaInfoReply;

    static private CmdChannelBLE mCmdChannelBLE;
    static private CmdChannelWIFI mCmdChannelWIFI;
    static private DataChannelWIFI mDataChannelWIFI;

    private static final ExecutorService worker = Executors.newSingleThreadExecutor();

    public RemoteCam(Context context) {
        mContext = context;
        mConnectivityType = CAM_CONNECTIVITY_WIFI_WIFI;
        if (mCmdChannelWIFI == null) {
            mCmdChannelWIFI = new CmdChannelWIFI(this);
            mDataChannelWIFI = new DataChannelWIFI(this);
        }
        setWifiIP(ServerConfig.HOST, 7878, 8787);
//        setWifiIP("192.168.8.6", 7878, 8787);

        // if
        // (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        // mCmdChannelBLE = new CmdChannelBLE(this);
    }
//我自己加的
    public DataChannel getDataChannel() {
        return mDataChannel;
    }

    public void reset() {
        Log.e(TAG, "resset");
        mBlueAddrConnected = null;
        mWifiSSIDConnected = null;
        // mSDCardDirectory = null;
        mVideoFolder = null;
        mEventFolder = null;
        mPhotoFolder = null;
        mfDataChannelConnected = false;
        if (mCmdChannel != null) {
            mCmdChannel.reset();
        }
    }

    public RemoteCam setWifiIP(String host, int cmdPort, int dataPort) {
        mWifiHostURL = host;
        mCmdChannelWIFI.setIP(host, cmdPort);
        mDataChannelWIFI.setIP(host, dataPort);
        return this;
    }

    public RemoteCam setConnectivity(int type) {
        if (mConnectivityType != type) {
            reset();
            mConnectivityType = type;
        }
        return this;
    }

    public RemoteCam setBtDeviceAddr(String addr) {
        mBlueAddrRequested = addr;
        return this;
    }

    public RemoteCam setWifiInfo(String ssid, String ipAddr) {
        mWifiSSIDRequested = ssid;
        mWifiIpAddr = ipAddr;
        return this;
    }

    public RemoteCam setChannelListener(IChannelListener listener) {
        mListener = listener;
        return this;
    }

    public void syncTime() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.syncTime();
            }
        });
    }

    public void checkState() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.checkState();
            }
        });
    }

    public void checkMicState() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.checkMicState();
            }
        });
    }

    public void wakeUp() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                String cmd = "amba discovery";
                switch (mConnectivityType) {
                    case CAM_CONNECTIVITY_WIFI_WIFI:
                        WifiManager mgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                        CmdChannelWIFI.wakeup(mgr, cmd, 7877, 7877);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void standBy() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.standBy();
            }
        });
    }

    public void startSession() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.startSession();
                if (mVideoFolder == null || mEventFolder == null || mPhotoFolder == null) {
                    mCmdChannel.getDevInfo();
                }
            }
        });
    }

    public void stopSession() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.stopSession();
            }
        });
    }

    public void getAllSettings() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getAllSettings();
            }
        });
    }
    public void getSingleSetting(final String type) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getSingleSetting(type);
            }
        });
    }
    public void getSettingOptions(final String setting) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getSettingOptions(setting);
            }
        });
    }

    public void setSetting(final String setting) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.setSetting(setting);
            }
        });
    }

    public void listDir(final String path) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.listDir(path);
            }
        });
    }

    public void deleteFile(final String path) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.deleteFile(path);
            }
        });
    }

    public void burnFW(final String path) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.burnFW(path);
            }
        });
    }

    public void setZoom(final String type, final int level) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.setZoom(type, level);
            }
        });
    }

    public void getZoomInfo(final String type) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mZoomInfoType = type;
                mCmdChannel.getZoomInfo(type);
            }
        });
    }

    public void setBitRate(final int bitRate) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.setBitRate(bitRate);
            }
        });
    }

    public void getThumb(final String path) {
        int pos = path.lastIndexOf('/');
        mGetFileName = path.substring(pos + 1, path.length()) + ".thumb";
//        mGetFileName = path.substring(pos + 1, path.length());
//        Log.e(TAG, "getThumb path=" + path + "\ngetThumb mGetFileName=" + mGetFileName);
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel() || !connectToDataChannel()) {
                    return;
                }
//                int len = path.length();
//                String surfix = path.substring(len - 3, len).toLowerCase();
//                String type = surfix.equals("jpg") ? "thumb" : "IDR";
//                mCmdChannel.getThumb(path, "thumb");
                mCmdChannel.getThumb(path, "thumb");
            }
        });
    }

    public void getFile(final String path) {
        int pos = path.lastIndexOf('/');
        mGetFileName = path.substring(pos + 1, path.length());
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel() || !connectToDataChannel()) {
                    return;
                }
                mCmdChannel.getFile(path);
            }
        });
    }

    public void putFile(final String srcFile, final String dstFile) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel() || !connectToDataChannel()) {
                    return;
                }

                mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_PUT_MD5, null);
                File file = new File(srcFile);
                String md5;
                try {
                    FileInputStream in = new FileInputStream(file);
                    byte[] buf = new byte[4096];
                    int bytes;
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    while ((bytes = in.read(buf)) > 0) {
                        md.update(buf, 0, bytes);
                    }
                    byte[] hash = md.digest();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hash) {
                        sb.append(String.format("%02x", b & 0xff));
                    }
                    md5 = sb.toString();
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                mPutFileName = srcFile;
                mCmdChannel.putFile(dstFile, md5, file.length());
            }
        });
    }

    public void getInfo(final String path) {
        int pos = path.lastIndexOf('/');
        mGetFileName = path.substring(pos + 1, path.length());
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getInfo(path);
            }
        });
    }

    public void setMediaAttribute(final String path, final int flag) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.setMediaAttribute(path, flag);
            }
        });
    }

    public void cancelGetFile(final String path) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.cancelGetFile(path);
                mDataChannel.cancelGetFile();
            }
        });
    }

    public void cancelPutFile(final String path) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                int xfer_size = mDataChannel.cancelPutFile();
                mCmdChannel.cancelPutFile(path, xfer_size);
            }
        });
    }

    public void startVF() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.resetViewfinder();
            }
        });
    }

    public void stopVF() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.stopViewfinder();
            }
        });
    }

    public void getRecordTime() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getRecordTime();
            }
        });
    }

    public void getBatteryLevel() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.getBatteryLevel();
            }
        });
    }

    public void takePhoto() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.takePhoto();
            }
        });
    }

    public void stopPhoto() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.stopPhoto();
            }
        });
    }

    public void startRecord() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.startRecord();
            }
        });
    }

    public void stopRecord() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.stopRecord();
            }
        });
    }

    public void startMic() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.startMic();
            }
        });
    }

    public void stopMic() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.stopMic();
            }
        });
    }


    public void forceSplit() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.forceSplit();
            }
        });
    }

    public void formatSD(final String slot) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.formatSD(slot);
            }
        });
    }

    public void defaultSetting() {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.defaultSetting();
            }
        });
    }
    public void getMediaInfo() {
        mMediaInfoStep = 0;
        mMediaInfoReply = "";
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                if (!mCmdChannel.getNumFiles("photo")) {
                    return;
                }
                mCmdChannel.getNumFiles("video");
                mCmdChannel.getNumFiles("total");
                mCmdChannel.getSpace("free");
                mCmdChannel.getSpace("total");
                mCmdChannel.getDevInfo();
            }
        });
    }

    public void sendCommand(final String command) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                if (!connectToCmdChannel()) {
                    return;
                }
                mCmdChannel.sendRequest(command);
            }
        });
    }

    public String streamFile(String path) {
        return "rtsp://" + mWifiHostURL + path;
    }

    // public String sdCardDirectory() {
    // return mSDCardDirectory;
    // }

    public String videoFolder() {
        return mVideoFolder;
    }

    public String eventFolder() {
        return mEventFolder;
    }

    public String photoFolder() {
        return mPhotoFolder;
    }

    public String getuuid() {
        return uuid;
    }

    public String gethw_ver() {
        return hw_ver;
    }

    public String getsw_ver() {
        return sw_ver;
    }


    @Override
    public void onChannelEvent(int type, Object param, String... array) {
        JSONObject parser;
        int size;
        String path;
        String md5;

        switch (type) {
            case IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB:
                parser = (JSONObject) param;
                try {
                    if (parser.getInt("rval") != 0) {
                        /*mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT,
                                "GET_THUMB failed");*/
                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_FAIL,true);

                        break;
                    }
                    else {
                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_TEST,true);
                    }

//                    File cacheDir = getDiskCacheDir(mContext, "yuvThumb");
//                    if (!cacheDir.exists()) {
//                        cacheDir.mkdirs();
//                    }
//                    // TODO: 2017/9/4 在对得到的缩略图数据进行处理
//                    size = parser.getInt("size");
//                    md5 = parser.getString("md5sum");
////                    path = Environment.getExternalStoragePublicDirectory(Environment
////							.DIRECTORY_DOWNLOADS) + "/" + mGetFileName;
//                    path = cacheDir + "/" + mGetFileName;
//                    Log.e(TAG, "onChannelEvent path=" + path + "\nonChannelEvent size=" + size);
//
//                    File file = new File(mGetFileName);
//                    if (!file.exists()) {
//                        mDataChannel.getYuvFile(path, size, md5);
//                    }
//                    mDataChannel.getFile(path, size);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                Log.e(TAG, "onChannelEvent:CMD_CHANNEL_EVENT_GET_THUMB:but do nothing");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_FILE:
                size = Integer.parseInt((String) param);
                // path =
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                // + "/"
                // + mGetFileName;
                String dirName = Environment.getExternalStorageDirectory() + "/" + "行车记录仪";
                File dir = new File(dirName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                path = dirName + "/" + mGetFileName;
                mDataChannel.getFile(path, size);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_PUT_FILE:
                mDataChannel.putFile(mPutFileName);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_SPACE:
                mMediaInfoStep++;
                mMediaInfoReply += "\n" + (mMediaInfoStep == 4 ? "free space: " : "total space: ");
                mMediaInfoReply += (String) param;
                mMediaInfoReply += "KB";
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_NUM_FILES:
                mMediaInfoStep++;
                if (mMediaInfoStep == 1) {
                    mMediaInfoReply += "\nPhoto Files: ";
                } else if (mMediaInfoStep == 2) {
                    mMediaInfoReply += "\nVideo Files: ";
                } else {
                    mMediaInfoReply += "\nTotal Files: ";
                }
                mMediaInfoReply += (String) param;
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_DEVINFO:
                try {
                    parser = (JSONObject) param;
                    // if (mSDCardDirectory == null) {
                    // if (parser.has("media_folder")) {
                    // String val =
                    // parser.getString("media_folder");
                    // val = val.substring(0, val.length() - 1);
                    // mSDCardDirectory = val.substring(0,
                    // val.lastIndexOf('/') + 1);
                    // } else {
                    // mSDCardDirectory = "/tmp/fuse_d/";
                    // }
                    // // break;
                    // }
                    // �г���Ƶ·��
                    if (mVideoFolder == null) {
//                        if (parser.has("video_folder")) {
                        if (parser.has("media_folder")) {
                            String val = parser.getString("media_folder");
                            mVideoFolder = val;
                        }
                        // break;
                    }
                    // ������Ƶ·��
                    if (mEventFolder == null) {
                        if (parser.has("event_folder")) {
                            String val = parser.getString("event_folder");
                            mEventFolder = val;
                        }
                        // break;
                    }
                    // ���·��
                    if (mPhotoFolder == null) {
                        if (parser.has("photo_folder")) {
                            String val = parser.getString("photo_folder");
                            mPhotoFolder = val;
//                            mPhotoFolder = "/tmp/SD0/PHOTO";
                        }
                        // break;
                    }

                    // Ӳ��Ψһʶ����
                    if (uuid == null) {
                        if (parser.has("uuid")) {
                            String val = parser.getString("uuid");
                            uuid = val;
                        } else {
                            uuid = "��ȡʧ��";
                        }
                        // break;
                    }
                    // Ӳ���汾��
                    if (hw_ver == null) {
                        if (parser.has("hw_ver")) {
                            String val = parser.getString("hw_ver");
                            hw_ver = val;

                        } else {
                            hw_ver = "��ȡʧ��";
                        }
                        // break;
                    }
                    // ����汾��
                    if (sw_ver == null) {
                        if (parser.has("api_ver")) {
                            String val = parser.getString("api_ver");
                            sw_ver = val;
                        } else {
                            sw_ver = "��ȡʧ��";
                        }
                        break;
                    }

                    Iterator<?> keys = parser.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        if (key.equals("rval") || key.equals("msg_id")) {
                            continue;
                        }
                        mMediaInfoReply += "\n" + key + ": " + parser.getString(key);
                    }
                    mListener.onChannelEvent(type, mMediaInfoReply);
                    mMediaInfoReply = null;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_ZOOM_INFO:
                mListener.onChannelEvent(type, mZoomInfoType, (String) param);
                break;
            default:
                if (mListener != null) {
                    mListener.onChannelEvent(type, param);
                }
        }
    }

    private boolean connectToCmdBLE() {
        // check if we are connected already
        if (mBlueAddrRequested.equals(mBlueAddrConnected)) {
            return true;
        }

        // try to connect
        if (mCmdChannelBLE.connectTo(mBlueAddrRequested)) {
            mBlueAddrConnected = mBlueAddrRequested;
            mCmdChannel = mCmdChannelBLE;
            return true;
        }

        mBlueAddrConnected = null;
        return false;
    }

    private boolean connectToCmdWIFI() {
        // check if we are connected already
        if (mWifiSSIDRequested.equals(mWifiSSIDConnected)) {
            return true;
        }
        mWifiSSIDConnected = null;

        // check if we can connect to cmd channel
        if (mCmdChannelWIFI.connect()) {
            mCmdChannel = mCmdChannelWIFI;
            mWifiSSIDConnected = mWifiSSIDRequested;
            return true;
        }
        return false;
    }

    private boolean connectToDataWIFI() {
        if (mfDataChannelConnected) {
            return true;
        }

        // check if we can connect to data channel
        mCmdChannel.setClntInfo("TCP", mWifiIpAddr);
        if (mDataChannelWIFI.connect()) {
            mDataChannel = mDataChannelWIFI;
            mfDataChannelConnected = true;
            return true;
        }

        return false;
    }

    private boolean connectToDataChannel() {
        switch (mConnectivityType) {
            case CAM_CONNECTIVITY_BLE_WIFI:
            case CAM_CONNECTIVITY_WIFI_WIFI:
                return connectToDataWIFI();

            default:
                if (mListener != null) {
                    String msg = mContext.getString(R.string.invalid_connect_error);
                    mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT, msg);
                }
        }
        return false;
    }

    private boolean connectToCmdChannel() {
        switch (mConnectivityType) {
            case CAM_CONNECTIVITY_BLE_WIFI:
                return connectToCmdBLE();

            case CAM_CONNECTIVITY_WIFI_WIFI:
                return connectToCmdWIFI();

            default:
                if (mListener != null) {
                    String msg = mContext.getString(R.string.invalid_connect_error);
                    mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT, msg);
                }
        }
        return false;
    }

    public String getConnectIp() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
        String line;
        int i = 0;
        String ip = null;
        String getConnectedIp = null;
        while ((line = br.readLine()) != null) {
            i++;
            String[] splitted = line.split(" +");
            if (splitted != null && splitted.length >= 4) {
                ip = splitted[0];
            }
            if (i == 2 && null != ip) {
                getConnectedIp = ip;
            }
        }
        return getConnectedIp;
    }

    /**
     * 根据传入的uniqueName获取硬盘缓存的路径地址。
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

}
