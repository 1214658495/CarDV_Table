package com.bydauto.car.i_key.cardv_table;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bydauto.car.i_key.cardv_table.connect.IChannelListener;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;
import com.bydauto.car.i_key.cardv_table.custom.HMAlertDialog;
import com.bydauto.car.i_key.cardv_table.custom.HMDownloadProgressDialog;
import com.bydauto.car.i_key.cardv_table.custom.HMToast;
import com.bydauto.car.i_key.cardv_table.fragment.AboutFragment;
import com.bydauto.car.i_key.cardv_table.fragment.ConnectFragment;
import com.bydauto.car.i_key.cardv_table.fragment.DeviceInfoFragment;
import com.bydauto.car.i_key.cardv_table.fragment.LoadingFragment;
import com.bydauto.car.i_key.cardv_table.fragment.PhotoDetailFragment;
import com.bydauto.car.i_key.cardv_table.fragment.PhotoDetailFullFragment;
import com.bydauto.car.i_key.cardv_table.fragment.PhotoFragment;
import com.bydauto.car.i_key.cardv_table.fragment.RecordFragment;
import com.bydauto.car.i_key.cardv_table.fragment.VideoDetailFragment;
import com.bydauto.car.i_key.cardv_table.fragment.VideoFragment;
import com.bydauto.car.i_key.cardv_table.util.LogcatHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity implements OnClickListener, IFragmentListener,
        IChannelListener {
    private static final String TAG = "MainActivity";

    private final static String KEY_CONNECTIVITY_TYPE = "connectivity_type";
    private final static String KEY_NEVER_SHOW = "key_never_show";

    private long exitTime = 0;
    private int mConnectivityType;
    private boolean neverShow = false;

    private TextView btn_record;
    private TextView btn_video;
    private TextView btn_photo;
    private TextView btn_about;
    private Drawable drawable_record_pressed;
    private Drawable drawable_record_normal;

    private Drawable drawable_video_pressed;
    private Drawable drawable_video_normal;

    private Drawable drawable_photo_pressed;
    private Drawable drawable_photo_normal;

    private Drawable drawable_about_pressed;
    private Drawable drawable_about_normal;

    public SharedPreferences mPref;

    public RemoteCam mRemoteCam;

    private AlertDialog mAlertDialog;
    private HMAlertDialog hmAlertDialog;
    private ProgressDialog mProgressDialog;
    private HMDownloadProgressDialog hmDownloadDig;
    private Toast mToast;
    private HMToast hmToast;

    private String mGetFileName;
    private String mPutFileName;
    private boolean mIsPreview;
    private boolean isConnected;

    private LinearLayout controlMenu;


    @BindView(R.id.iv_takephoto)
    ImageView ivTakePhoto;

    @OnClick(R.id.iv_takephoto)
    public void takePhoto() {
        mRecordFrag.setPhotoFocus(true);
        mRemoteCam.takePhoto();
    }

    @BindView(R.id.iv_record)
    ImageView ivRecord;

    @OnClick(R.id.iv_record)
    public void doInRecord() {
        ivRecord.setClickable(false);
        if (RecordFragment.mRecordingState != RecordFragment.STATE_OFF) {
            //若开则关
//            showCustomToastText("正在关闭录像",Toast.LENGTH_LONG);
            mRemoteCam.stopRecord();
            mRecordFrag.stopRecord();
        } else {
            //若关则开
//            showCustomToastText("正在打开录像",Toast.LENGTH_LONG);
            mRemoteCam.startRecord();
            mRecordFrag.startRecord();
        }
    }

    @BindView(R.id.iv_mic)
    ImageView ivMic;

    @OnClick(R.id.iv_mic)
    public void doInMic() {
//        ivMic.setClickable(false);
        if (RecordFragment.mMicphoneState != RecordFragment.STATE_OFF) {
            //若开则关
//            mListener.onFragmentAction(IFragmentListener.ACTION_MIC_OFF, null);
            showCustomToastText("关闭录音", Toast.LENGTH_SHORT);
            mRemoteCam.stopMic();
            mRecordFrag.stopMic();
        } else {
            //若关则开
            showCustomToastText("打开录音", Toast.LENGTH_SHORT);
//            mListener.onFragmentAction(IFragmentListener.ACTION_MIC_ON, null);
            mRemoteCam.startMic();
            mRecordFrag.startMic();
        }
    }

    @BindView(R.id.iv_setting)
    ImageView ivSetting;

    @OnClick(R.id.iv_setting)
    public void clickSetting() {
        showAboutFrag();
    }

    public Fragment mCurrentFrag;
    private ConnectFragment mConnectFrag = new ConnectFragment();
    private LoadingFragment mLoadingFrag = new LoadingFragment();
    private RecordFragment mRecordFrag = new RecordFragment();
    private VideoFragment mVideoFrag = new VideoFragment();
    private PhotoFragment mPhotoFrag = new PhotoFragment();
    private AboutFragment mAboutFrag = new AboutFragment();

    public VideoDetailFragment mVideoDetailFrag = null;
    private PhotoDetailFragment mPhotoDetailFrag = null;
    private PhotoDetailFullFragment mPhotoDetailFullFrag = null;
    private DeviceInfoFragment mDeviceInfoFrag = new DeviceInfoFragment();

    public ArrayList<Model> mPlaylist; //播放列表

    private final static int DDD = 0;
    private final static int DOWNLOAD_FILES = 1;

    private List<Model> selectedFiles;//已选择文件列表
    private int selectedFilesCount; //已选择文件数目
    private static int hadDeleted = 0;//已删除文件数目
    private static int hadDownloadedFlag = 0;//下载文件标志
    private static int hadDownloadCount = 0;//已下载数目

    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DDD:
                    dismissDialog();
                    if (mCurrentFrag == mVideoDetailFrag) {
                        removeVideoDetailFrag();
                        if (mVideoFrag.currentSegment == 0) {
                            mVideoFrag.showNormalVideo();
                        } else {
                            mVideoFrag.showCollisionVideo();
                        }
                    } else if (mCurrentFrag == mPhotoDetailFrag) {
                        removePhotoDetailFrag();
                        mPhotoFrag.showPhoto();
                    }
                    handler.removeMessages(DDD);
                    break;
                case DOWNLOAD_FILES:
                    downLoadFiles();
                    break;
                default:
                    break;
            }
        }

    };

    private boolean isfull = false;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mCurrentFrag == mRecordFrag) {
            if (!isfull) {
                mRecordFrag.fullScreen(true);
                isfull = true;
            } else {
                mRecordFrag.fullScreen(false);
                isfull = false;
            }

        } else if (mCurrentFrag == mVideoDetailFrag) {
            if (!isfull) {
                mVideoDetailFrag.fullScreen(true);
                isfull = true;
            } else {
                mVideoDetailFrag.fullScreen(false);
                isfull = false;
            }

        }
    }

    public void hideNavigation(boolean hide) {
        LinearLayout menu = (LinearLayout) findViewById(R.id.menu);
        if (hide) {
            menu.setVisibility(View.GONE);
        } else {
            menu.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogcatHelper.getInstance(this).start();
        Log.e(TAG, "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
//                .LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

//        isSupportMediaCodecHardDecoder();
        //Butterknife绑定Activity
        ButterKnife.bind(this);

        initView();

        mPref = getPreferences(Context.MODE_PRIVATE);
        getPrefs(mPref);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mRemoteCam = new RemoteCam(this);
        mRemoteCam.setChannelListener(this).setConnectivity(mConnectivityType).setWifiInfo
                (wifiManager.getConnectionInfo().getSSID().replace("\"", ""), getWifiIpAddr());
        // mRemoteCam.startSession();
        mLoadingFrag.setRemoteCam(mRemoteCam);
        mRecordFrag.setRemoteCam(mRemoteCam);
        mVideoFrag.setRemoteCam(mRemoteCam);
        mPhotoFrag.setRemoteCam(mRemoteCam);
        mAboutFrag.setRemoteCam(mRemoteCam);
        mDeviceInfoFrag.setRemoteCam(mRemoteCam);

    }


    @Override
    protected void onResume() {
        super.onResume();
//        mRemoteCam.startSession();
        ActivityManager activityManager = (ActivityManager)      getSystemService(ACTIVITY_SERVICE);

        int largeMemoryClass = activityManager.getLargeMemoryClass();
        int memoryClass = activityManager.getMemoryClass();

        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
//        activityManager.getMemoryInfo(info);
        Log.e(TAG, "onResume");
        Log.e(TAG, "^^^^^largeMemoryClass = " + largeMemoryClass);
        Log.e(TAG, "^^^^^memoryClass = " + memoryClass);
//        Log.e(TAG, "^^^^^getMemoryInfo(info) = " + info.toString());
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        Log.e("TAG", "Max memory is " + maxMemory + "KB");
    }

    @Override
    protected void onPause() {
        putPrefs(mPref);
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        LogcatHelper.getInstance(this).stop();
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFrag == mVideoDetailFrag) {
            if (!isfull) {
                showRecordFrag();
                removeVideoDetailFrag();
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else if (mCurrentFrag == mPhotoDetailFrag) {
            removePhotoDetailFrag();
        } else if (mCurrentFrag == mPhotoDetailFullFrag) {
            getFragmentManager().popBackStack();
//            mCurrentFrag = mPhotoDetailFrag;
            mCurrentFrag = mVideoFrag;
        } else if (mCurrentFrag == mDeviceInfoFrag) {
            removeDeviceInfo();
        } else if (mCurrentFrag == mRecordFrag && isfull) {
//            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            }
        } else if (mCurrentFrag == mAboutFrag) {
            getFragmentManager().popBackStack();
//            mCurrentFrag = mPhotoDetailFrag;
            mCurrentFrag = mVideoFrag;
            if (mVideoFrag.currentSegment == 0) {
                mVideoFrag.showNormalVideo();
            } else if (mVideoFrag.currentSegment == 1) {
                mVideoFrag.showCollisionVideo();
            } else if (mVideoFrag.currentSegment == 2) {
                mVideoFrag.showPhoto();
            }
        } else {
            if (mVideoFrag.isMultiChoose) {
                mVideoFrag.cancelMultiChoose();
                handler.removeMessages(DOWNLOAD_FILES);
            } else {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    showCustomToastText("再按一次退出程序", Toast.LENGTH_SHORT);
                    exitTime = System.currentTimeMillis();
                } else {
                    mRemoteCam.stopSession();
                    finish();
                    Log.e(TAG, "kill the process to force fresh launch next time");
                    android.os.Process.killProcess(android.os.Process.myPid());

                }
            }
        }
    }

    public void resetRemoteCamera() {
        mRemoteCam.reset();
    }

    private void getPrefs(SharedPreferences preferences) {
        mConnectivityType = mPref.getInt(KEY_CONNECTIVITY_TYPE, RemoteCam
                .CAM_CONNECTIVITY_WIFI_WIFI);
        neverShow = mPref.getBoolean(KEY_NEVER_SHOW, false);
    }

    public void putPrefs(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_CONNECTIVITY_TYPE, mConnectivityType);
        editor.putBoolean(KEY_NEVER_SHOW, neverShow);
        editor.commit();
    }

    private String getWifiIpAddr() {
        WifiManager mgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = mgr.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ip & 0xFF), (ip >> 8 & 0xFF), (ip >> 16 & 0xFF), ip
                >> 24);
    }

    public void isSupportMediaCodecHardDecoder() {
        //读取系统配置文件/system/etc/media_codecc.xml
        File file = new File("/system/etc/media_codecs.xml");
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (in == null) {
            android.util.Log.i("xp", "in == null");
        } else {
            android.util.Log.i("xp", "in != null");
        }

        String msg = null;
        boolean isHardcode = false;
        XmlPullParserFactory pullFactory;
        try {
            pullFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = pullFactory.newPullParser();
            xmlPullParser.setInput(in, "UTF-8");
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("MediaCodec".equals(tagName)) {
                            String componentName = xmlPullParser.getAttributeValue(0);
                            android.util.Log.i("xp", componentName);
                            msg += componentName + "\n";
                            if (componentName.startsWith("OMX.")) {
                                if (!componentName.startsWith("OMX.google.")) {
                                    isHardcode = true;
                                }
                            }
                        }
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        android.util.Log.i("xp", "" + isHardcode);
//        msg += "硬解码" + isHardcode;
        final String finalMsg = msg;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showAlertDialog("解码方式", finalMsg);
            }
        }, 3000);
    }

    private void initView() {

        btn_record = (TextView) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
        btn_video = (TextView) findViewById(R.id.btn_video);
        btn_video.setOnClickListener(this);
        btn_photo = (TextView) findViewById(R.id.btn_photo);
        btn_photo.setOnClickListener(this);
        btn_about = (TextView) findViewById(R.id.btn_about);
        btn_about.setOnClickListener(this);

        drawable_record_pressed = getResources().getDrawable(R.mipmap.record_pressed);
        drawable_record_pressed.setBounds(0, 0, drawable_record_pressed.getMinimumWidth(),
                drawable_record_pressed.getMinimumHeight());
        drawable_record_normal = getResources().getDrawable(R.mipmap.record_normal);
        drawable_record_normal.setBounds(0, 0, drawable_record_normal.getMinimumWidth(),
                drawable_record_normal.getMinimumHeight());
        drawable_video_pressed = getResources().getDrawable(R.mipmap.video_pressed);
        drawable_video_pressed.setBounds(0, 0, drawable_video_pressed.getMinimumWidth(),
                drawable_video_pressed.getMinimumHeight());
        drawable_video_normal = getResources().getDrawable(R.mipmap.video_normal);
        drawable_video_normal.setBounds(0, 0, drawable_video_normal.getMinimumWidth(),
                drawable_video_normal.getMinimumHeight());
        drawable_photo_pressed = getResources().getDrawable(R.mipmap.photo_pressed);
        drawable_photo_pressed.setBounds(0, 0, drawable_photo_pressed.getMinimumWidth(),
                drawable_photo_pressed.getMinimumHeight());
        drawable_photo_normal = getResources().getDrawable(R.mipmap.photo_normal);
        drawable_photo_normal.setBounds(0, 0, drawable_photo_normal.getMinimumWidth(),
                drawable_photo_normal.getMinimumHeight());
        drawable_about_pressed = getResources().getDrawable(R.mipmap.about_pressed);
        drawable_about_pressed.setBounds(0, 0, drawable_about_pressed.getMinimumWidth(),
                drawable_about_pressed.getMinimumHeight());
        drawable_about_normal = getResources().getDrawable(R.mipmap.about_normal);
        drawable_about_normal.setBounds(0, 0, drawable_about_normal.getMinimumWidth(),
                drawable_about_normal.getMinimumHeight());

        controlMenu = (LinearLayout) findViewById(R.id.control_menu);

//        ImageView ivSetting = (ImageView)findViewById(R.id.iv_setting);
//        ivSetting.setOnClickListener(this);

//        showRecordFrag();
        showLoadingFrag();
    }

    public void updateRecButton(boolean isRec) {
        if (isRec) {
            RecordFragment.mRecordingState = RecordFragment.STATE_ON;
            //图片替换
//            recordSwitch.setImageResource(R.drawable.record_on_selector);
            ivRecord.setImageResource(R.mipmap.icon_rec_on);
        } else {
            RecordFragment.mRecordingState = RecordFragment.STATE_OFF;
            //图片替换
//            recordSwitch.setImageResource(R.drawable.record_off_selector);
            ivRecord.setImageResource(R.mipmap.icon_rec_off);
        }
    }

    public void updateMicButton(boolean isOn) {
        if (isOn) {
            RecordFragment.mMicphoneState = RecordFragment.STATE_ON;
            //图片替换
//            micSwitch.setImageResource(R.drawable.mic_on_selector);
            ivMic.setImageResource(R.mipmap.icon_mic_on);
        } else {
            RecordFragment.mMicphoneState = RecordFragment.STATE_OFF;
            //图片替换
//            micSwitch.setImageResource(R.drawable.mic_off_selector);
            ivMic.setImageResource(R.mipmap.icon_mic_off);
        }
    }

    public void setControlMenu(boolean show) {
        if (show) {
            controlMenu.setVisibility(View.VISIBLE);
        } else {
            controlMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                if (mVideoDetailFrag != null && mVideoDetailFrag.isAdded()) {
                    removeVideoDetailFrag();
                }
                showRecordFrag();
                break;
            case R.id.btn_video:
                if (mVideoDetailFrag != null && mVideoDetailFrag.isAdded()) {
                    removeVideoDetailFrag();
                }
                showVideoFrag();
                break;
            case R.id.btn_photo:
                if (mVideoDetailFrag != null && mVideoDetailFrag.isAdded()) {
                    removeVideoDetailFrag();
                }
                showPhotoFrag();
                break;
            case R.id.btn_about:
                if (mVideoDetailFrag != null && mVideoDetailFrag.isAdded()) {
                    removeVideoDetailFrag();
                }
                showAboutFrag();
                break;
//            case R.id.iv_setting:
//                showAboutFrag();
//                break;
            default:
                break;
        }
    }

    private void showConnectFrag() {
        dismissDialog();
        if (null == mConnectFrag) {
            mConnectFrag = new ConnectFragment();
        }
        getFragmentManager().beginTransaction()
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

                .replace(R.id.all, mConnectFrag).commitAllowingStateLoss();

        mCurrentFrag = mConnectFrag;
    }

    public void showLoadingFrag() {
        dismissDialog();

        if (null == mLoadingFrag) {
            mLoadingFrag = new LoadingFragment();
        }
        getFragmentManager().beginTransaction()
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.all, mLoadingFrag).commitAllowingStateLoss();

        mCurrentFrag = mLoadingFrag;
    }

    private void removeLoadingFrag() {
        getFragmentManager().beginTransaction().remove(mLoadingFrag).commit();
    }

    private void showRecordFrag() {


        dismissDialog();

        btn_record.setTextColor(Color.rgb(0xff, 0xff, 0xff));
        btn_record.setCompoundDrawables(null, drawable_record_pressed, null, null);
        btn_record.setClickable(false);
        btn_video.setTextColor(Color.rgb(204, 204, 204));
        btn_video.setCompoundDrawables(null, drawable_video_normal, null, null);
        btn_video.setClickable(true);
        btn_photo.setTextColor(Color.rgb(204, 204, 204));
        btn_photo.setCompoundDrawables(null, drawable_photo_normal, null, null);
        btn_photo.setClickable(true);
        btn_about.setTextColor(Color.rgb(204, 204, 204));
        btn_about.setCompoundDrawables(null, drawable_about_normal, null, null);
        btn_about.setClickable(true);

        if (null == mRecordFrag) {
            mRecordFrag = new RecordFragment();
        }
        getFragmentManager().beginTransaction()

                .replace(R.id.main_content, mRecordFrag).commitAllowingStateLoss();

        if (isConnected) {
            mRemoteCam.checkState();
            mRemoteCam.checkMicState();
        }

        mCurrentFrag = mRecordFrag;
    }

    private void showVideoFrag() {


        dismissDialog();

        btn_video.setTextColor(Color.rgb(0xff, 0xff, 0xff));
        btn_video.setCompoundDrawables(null, drawable_video_pressed, null, null);
        btn_video.setClickable(false);
        btn_record.setTextColor(Color.rgb(204, 204, 204));
        btn_record.setCompoundDrawables(null, drawable_record_normal, null, null);
        btn_record.setClickable(true);
        btn_photo.setTextColor(Color.rgb(204, 204, 204));
        btn_photo.setCompoundDrawables(null, drawable_photo_normal, null, null);
        btn_photo.setClickable(true);
        btn_about.setTextColor(Color.rgb(204, 204, 204));
        btn_about.setCompoundDrawables(null, drawable_about_normal, null, null);
        btn_about.setClickable(true);

        if (null == mVideoFrag) {
            mVideoFrag = new VideoFragment();
        }
        getFragmentManager().beginTransaction()

                .replace(R.id.main, mVideoFrag).commitAllowingStateLoss();

        mCurrentFrag = mVideoFrag;
    }

    //    应该未使用如下函数 即未用到PhotoFragment
    private void showPhotoFrag() {


        dismissDialog();

        btn_photo.setTextColor(Color.rgb(0xff, 0xff, 0xff));
        btn_photo.setCompoundDrawables(null, drawable_photo_pressed, null, null);
        btn_photo.setClickable(false);
        btn_video.setTextColor(Color.rgb(204, 204, 204));
        btn_video.setCompoundDrawables(null, drawable_video_normal, null, null);
        btn_video.setClickable(true);
        btn_record.setTextColor(Color.rgb(204, 204, 204));
        btn_record.setCompoundDrawables(null, drawable_record_normal, null, null);
        btn_record.setClickable(true);
        btn_about.setTextColor(Color.rgb(204, 204, 204));
        btn_about.setCompoundDrawables(null, drawable_about_normal, null, null);
        btn_about.setClickable(true);

        if (null == mPhotoFrag) {
            mPhotoFrag = new PhotoFragment();
        }
        getFragmentManager().beginTransaction()

                .replace(R.id.main_content, mPhotoFrag).commitAllowingStateLoss();

        mCurrentFrag = mPhotoFrag;
    }

    private void showAboutFrag() {

        dismissDialog();

        btn_about.setTextColor(Color.rgb(0xff, 0xff, 0xff));
        btn_about.setCompoundDrawables(null, drawable_about_pressed, null, null);
        btn_about.setClickable(false);
        btn_video.setTextColor(Color.rgb(204, 204, 204));
        btn_video.setCompoundDrawables(null, drawable_video_normal, null, null);
        btn_video.setClickable(true);
        btn_photo.setTextColor(Color.rgb(204, 204, 204));
        btn_photo.setCompoundDrawables(null, drawable_photo_normal, null, null);
        btn_photo.setClickable(true);
        btn_record.setTextColor(Color.rgb(204, 204, 204));
        btn_record.setCompoundDrawables(null, drawable_record_normal, null, null);
        btn_record.setClickable(true);

        if (null == mAboutFrag) {
            mAboutFrag = new AboutFragment();
        }
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction
                .TRANSIT_FRAGMENT_OPEN).add(R.id.all, mAboutFrag).addToBackStack(null)
                .commitAllowingStateLoss();

        mCurrentFrag = mAboutFrag;
    }

    public void showVideoDetailFrag(Model model) {

        mVideoDetailFrag = new VideoDetailFragment(mPlaylist, model, mRemoteCam.videoFolder());
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator
                        .slide_fragment_horizontal_left_in,

                R.animator.slide_fragment_horizontal_left_out,

                R.animator.slide_fragment_horizontal_right_in,

                R.animator.slide_fragment_horizontal_right_out).replace(R.id.all,
                mVideoDetailFrag).addToBackStack(null)
//				.remove(mRecordFrag)
                .commit();
        removeFragment(mRecordFrag);//yichu zhibo jiemian
        mCurrentFrag = mVideoDetailFrag;
    }

    public void showCollisionDetailFrag(Model model) {

        mVideoDetailFrag = new VideoDetailFragment(mPlaylist, model, mRemoteCam.eventFolder());
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator
                        .slide_fragment_horizontal_left_in,

                R.animator.slide_fragment_horizontal_left_out,

                R.animator.slide_fragment_horizontal_right_in,

                R.animator.slide_fragment_horizontal_right_out).add(R.id.all, mVideoDetailFrag)
                .addToBackStack(null)
//				.hide(mVideoFrag)
                .commit();
        removeFragment(mRecordFrag);//yichu zhibo jiemianxw
        mCurrentFrag = mVideoDetailFrag;
    }

    public void showPhotoDetailFrag(Model model) {
        Log.e(TAG, "showPhotoDetailFullFrag: 1111  file  " + model);

        mPhotoDetailFrag = new PhotoDetailFragment(mPlaylist, model, mRemoteCam.photoFolder());
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator
                        .slide_fragment_horizontal_left_in,

                R.animator.slide_fragment_horizontal_left_out,

                R.animator.slide_fragment_horizontal_right_in,

                R.animator.slide_fragment_horizontal_right_out).add(R.id.main, mPhotoDetailFrag)
                .addToBackStack(null)
//				.hide(mPhotoFrag)
                .commit();

        mCurrentFrag = mPhotoDetailFrag;
    }

    public void removeVideoDetailFrag() {
        getFragmentManager().popBackStack();
        mCurrentFrag = mVideoFrag;
    }

    public void removePhotoDetailFrag() {
        getFragmentManager().popBackStack();

        mCurrentFrag = mPhotoFrag;
    }

    public void showPhotoDetailFullFrag(Model file) {
        Log.e(TAG, "showPhotoDetailFullFrag: 1111  file  " + file);
        mPhotoDetailFullFrag = new PhotoDetailFullFragment(mPlaylist, file, mRemoteCam
                .photoFolder());
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction
                .TRANSIT_FRAGMENT_OPEN).addToBackStack(null).replace(R.id.all,
                mPhotoDetailFullFrag).commitAllowingStateLoss();
        mCurrentFrag = mPhotoDetailFullFrag;
    }

    public void showDeviceInfo() {
        if (mDeviceInfoFrag == null) {
            mDeviceInfoFrag = new DeviceInfoFragment();
        }
        getFragmentManager().beginTransaction().setCustomAnimations(R.animator
                        .slide_fragment_horizontal_left_in,

                R.animator.slide_fragment_horizontal_left_out,

                R.animator.slide_fragment_horizontal_right_in,

                R.animator.slide_fragment_horizontal_right_out).add(R.id.main_content,
                mDeviceInfoFrag).addToBackStack(null)
//				.hide(mAboutFrag)
                .commit();
        mCurrentFrag = mDeviceInfoFrag;
    }

    public void removeDeviceInfo() {
//		getFragmentManager().beginTransaction().show(mAboutFrag).remove(mDeviceInfoFrag).commit();
        getFragmentManager().popBackStack();
        mCurrentFrag = mAboutFrag;
    }

    public void removeFragment(Fragment frag) {
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
    }

    public void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
        if (mToast != null) {
            mToast.cancel();
        }
        if (hmToast != null) {
            hmToast.cancel();
        }
        if (hmDownloadDig != null) {
            hmDownloadDig.dismiss();
        }

    }

    private void showAlertDialog(String title, String msg) {
        dismissDialog();
        mAlertDialog = new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton("OK", null).show();
    }

    public void showAlertDialogWithButton(String title, String msg, String posText,
                                          DialogInterface.OnClickListener posListener, String
                                                  negText, DialogInterface.OnClickListener
                                                  negListener) {
        mAlertDialog = new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton(posText, posListener).setNegativeButton(negText, negListener)
                .show();
    }

    public void showCustomAlertDialog(String title, String msg, String positiveButtonText,
                                      DialogInterface.OnClickListener positivelistener, String
                                              negativeButtonText, DialogInterface.OnClickListener
                                              negativelistener) {
        dismissDialog();
        hmAlertDialog = new HMAlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton(positiveButtonText, positivelistener).setNegativeButton
                        (negativeButtonText, negativelistener).create();
        hmAlertDialog.show();
    }

    public void showWaitDialog(String msg) {
        dismissDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("PLEASE WAIT ...");
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    public void showToastText(String msg, int time) {
        dismissDialog();
        mToast = Toast.makeText(getApplication(), msg, time);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void showCustomToastText(String msg, int duration) {
        dismissDialog();
        hmToast = HMToast.makeText(getApplication(), msg, duration);
        hmToast.setGravity(Gravity.CENTER, 0, 0);
        hmToast.show();
    }

    private void showProgressDialog(String title, DialogInterface.OnClickListener listener) {
        dismissDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", listener);
        mProgressDialog.show();
    }

    public void showCustomDownloadProgressDialog(String title, String msg, String positiveText,
                                                 DialogInterface.OnClickListener
                                                         positiveListener, DialogInterface
                                                         .OnCancelListener cancelListener) {
        dismissDialog();
        hmDownloadDig = new HMDownloadProgressDialog(this);
        hmDownloadDig.setTitle(title);
        hmDownloadDig.setMessage(msg);
        hmDownloadDig.setPositiveButton(positiveText, positiveListener);
        hmDownloadDig.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        hmDownloadDig.setCanceledOnTouchOutside(false);
        hmDownloadDig.setOnCancelListener(cancelListener);
        hmDownloadDig.show();
    }


    @Override
    public void onFragmentAction(int type, final Object param, Integer... array) {
        Intent intent;
        switch (type) {
            case IFragmentListener.ACTION_FS_DELETE_MULTI:
                selectedFiles = (ArrayList<Model>) param;
                selectedFilesCount = selectedFiles.size();
                break;
            case IFragmentListener.ACTION_PHOTO_DETAIL:
//                showPhotoDetailFrag((Model) param);
                showPhotoDetailFullFrag((Model) param);
                break;
            case IFragmentListener.ACTION_COLLISION_DETAIL:
                showCollisionDetailFrag((Model) param);
                break;
            case IFragmentListener.ACTION_VIDEO_DETAIL:
                // showVideoDetailFrag((String) param);
                showVideoDetailFrag((Model) param);
                break;
            case IFragmentListener.ACTION_RECORD_START:
                mRemoteCam.startRecord();
                mRecordFrag.startRecord();
                break;
            case IFragmentListener.ACTION_RECORD_STOP:
                mRemoteCam.stopRecord();
                mRecordFrag.stopRecord();
                break;
            case IFragmentListener.ACTION_MIC_ON:
                mRemoteCam.startMic();
                mRecordFrag.startMic();
                break;
            case IFragmentListener.ACTION_MIC_OFF:
                mRemoteCam.stopMic();
                mRecordFrag.stopMic();
                break;
            case IFragmentListener.ACTION_CONNECTIVITY_SELECTED:
                mConnectivityType = (Integer) param;
                resetRemoteCamera();
                mRemoteCam.setConnectivity(mConnectivityType);
                break;

            case IFragmentListener.ACTION_BC_WAKEUP:
                mRemoteCam.wakeUp();
                break;
            case IFragmentListener.ACTION_BC_STANDBY:
                mRemoteCam.standBy();
                break;
            case IFragmentListener.ACTION_BC_START_SESSION:
                mRemoteCam.startSession();
                break;
            case IFragmentListener.ACTION_BC_STOP_SESSION:
                mRemoteCam.stopSession();
                break;
            case IFragmentListener.ACTION_BC_SEND_COMMAND:
                mRemoteCam.sendCommand((String) param);
                break;
            case IFragmentListener.ACTION_BC_GET_ALL_SETTINGS:
                mRemoteCam.getAllSettings();
                break;
            case IFragmentListener.ACTION_BC_GET_SINGLE_SETTING:
                mRemoteCam.getSingleSetting((String) param);
                break;
            case IFragmentListener.ACTION_BC_GET_ALL_SETTINGS_DONE:
                dismissDialog();
                break;
            case IFragmentListener.ACTION_BC_GET_SETTING_OPTIONS:
                mRemoteCam.getSettingOptions((String) param);
                break;
            case IFragmentListener.ACTION_BC_SET_SETTING:
                mRemoteCam.setSetting((String) param);
                break;
            case IFragmentListener.ACTION_BC_SET_BITRATE:
                mRemoteCam.setBitRate((Integer) param);
                break;

            case IFragmentListener.ACTION_FS_BURN_FW:
                mRemoteCam.burnFW((String) param);
                break;
            case IFragmentListener.ACTION_FS_GET_FILE_INFO:
                mRemoteCam.getMediaInfo();
                break;
            case IFragmentListener.ACTION_FS_FORMAT_SD:
//                new AlertDialog.Builder(this).setTitle("Warning").setMessage("Are you sure to " +
//                        "format SD card?").setPositiveButton("OK", new DialogInterface
//                        .OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
                mRemoteCam.stopRecord();
//                mRecordFrag.stopRecord();
                mRemoteCam.formatSD((String) param);
//                    }
//                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                }).show();
                break;
            case IFragmentListener.ACTION_FS_LS:
                mRemoteCam.listDir((String) param);
                break;
            case IFragmentListener.ACTION_FS_DELETE:
                showWaitDialog("Deleting...Please Wait");
                mRemoteCam.deleteFile((String) param);
                break;
            case IFragmentListener.ACTION_FS_DELETE_STHWRONG:
                handler.sendEmptyMessageDelayed(DDD, 3000);
                break;
            case IFragmentListener.ACTION_FS_DOWNLOAD:
                //单文件下载
                if ((String) param != null) {
                    mGetFileName = (String) param;
                    mRemoteCam.getFile(mGetFileName);
                    //多文件下载
                } else {

                    downLoadFiles();

                }
                break;
            case IFragmentListener.ACTION_FS_INFO:
                mRemoteCam.getInfo((String) param);
                break;
            case IFragmentListener.ACTION_FS_SET_RO:
                mRemoteCam.setMediaAttribute((String) param, 0);
                break;
            case IFragmentListener.ACTION_FS_SET_WR:
                mRemoteCam.setMediaAttribute((String) param, 1);
                break;
            case IFragmentListener.ACTION_FS_GET_THUMB:
                Log.e(TAG, "onFragmentAction: 主函数收到监听处理ACTION_FS_GET_THUMB");
                mRemoteCam.getThumb((String) param);
                break;
            case IFragmentListener.ACTION_FS_VIEW:
                String path = (String) param;
                if (path.endsWith(".jpg")) {
                    mIsPreview = true;
                    mRemoteCam.getFile(path);
                } else {
                    String uri = mRemoteCam.streamFile(path);
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(uri), "video/mp4");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        new AlertDialog.Builder(this).setTitle("Warning").setMessage("You don't " +
                                "have any compatible video player installed in your device. " +
                                "Please install one (such as RTSP player) first.")
                                .setPositiveButton("OK", null).show();
                    }
                }
                break;

            case IFragmentListener.ACTION_VF_START:
                mRemoteCam.startVF();
                break;
            case IFragmentListener.ACTION_VF_STOP:
                mRemoteCam.stopVF();
                break;
            case IFragmentListener.ACTION_PLAYER_START:
//			mRemoteCam.startLiveStream();
                break;
            case IFragmentListener.ACTION_PLAYER_STOP:
//			mRemoteCam.stopLiveStream();
                mRecordFrag.stopStreamView();
                break;
            case IFragmentListener.ACTION_RECORD_TIME:
                mRemoteCam.getRecordTime();
                break;
            case IFragmentListener.ACTION_PHOTO_START:
//                mRecordFrag.setPhotoFocus(true);
                mRemoteCam.takePhoto();
                break;
            case IFragmentListener.ACTION_PHOTO_STOP:
                mRemoteCam.stopPhoto();
                break;
            case IFragmentListener.ACTION_FORCE_SPLIT:
                mRemoteCam.forceSplit();
                break;
            case IFragmentListener.ACTION_GET_ZOOM_INFO:
                mRemoteCam.getZoomInfo((String) param);
                break;
            case IFragmentListener.ACTION_SET_ZOOM:
                mRemoteCam.setZoom((String) param, array[0]);
                break;
            case IFragmentListener.ACTION_UPDATE_PLAYLIST:
                mPlaylist = (ArrayList<Model>) param;
                break;
            case IFragmentListener.ACTION_PHOTO_FULL:
                showPhotoDetailFullFrag((Model) param);
                break;
            case IFragmentListener.ACTION_SHOW_DEVICE_INFO:
                showDeviceInfo();
                break;
            case IFragmentListener.ACTION_DEFAULT_SETTING:
                mRemoteCam.defaultSetting();
                break;
            default:
                break;
        }
    }


    private void downLoadFiles() {
        if (hadDownloadedFlag < selectedFilesCount) {
            if (mVideoFrag.currentSegment == 0) {
                mGetFileName = mRemoteCam.videoFolder() +
                        "/" + selectedFiles.get(hadDownloadedFlag).getName();
//                        "/M_video/" + selectedFiles.get(hadDownloadedFlag).getName();
            } else if (mVideoFrag.currentSegment == 1) {
                mGetFileName = mRemoteCam.eventFolder() +
                        "/" + selectedFiles.get(hadDownloadedFlag).getName();
//                        "/M_video/" + selectedFiles.get(hadDownloadedFlag).getName();
            } else if (mVideoFrag.currentSegment == 2) {
//                mGetFileName = mRemoteCam.photoFolder() +
//                        "/M_photo/" + selectedFiles.get(hadDownloadedFlag).getName();
                mGetFileName = mRemoteCam.photoFolder() + "/" +
                        selectedFiles.get(hadDownloadedFlag).getName();//删除二级文件
            }
            String filename = Environment.getExternalStorageDirectory() + "/行车记录仪" + mGetFileName
                    .substring(mGetFileName.lastIndexOf('/'));
            File file = new File(filename);
            if (!file.exists()) {
                mRemoteCam.getFile(mGetFileName);
            } else {
                showCustomToastText("此文件已下载，跳过下载此文件", Toast.LENGTH_SHORT);
                hadDownloadedFlag++;
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        downLoadFiles();
//                    }
//                }, 1500);
                handler.sendEmptyMessageDelayed(DOWNLOAD_FILES, 1500);
            }
        } else {
//            showCustomToastText("下载结束，共下载" + hadDownloadCount + "个文件", Toast.LENGTH_SHORT);
            showCustomToastText("下载结束，共下载", Toast.LENGTH_SHORT);

            hadDownloadedFlag = 0;
            hadDownloadCount = 0;
        }
    }

    /**
     * IChannelListener
     */
    @Override
    public void onChannelEvent(final int type, final Object param, final String... array) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (type & IChannelListener.MSG_MASK) {
                    case IChannelListener.CMD_CHANNEL_MSG:
                        handleCmdChannelEvent(type, param, array);
                        return;
                    case IChannelListener.DATA_CHANNEL_MSG:
                        handleDataChannelEvent(type, param);
                        return;
                    case IChannelListener.STREAM_CHANNEL_MSG:
                        handleStreamChannelEvent(type, param);
                        return;
                    default:
                        break;
                }
            }
        });
    }

    private void handleCmdChannelEvent(int type, Object param, String... array) {
        if (type >= 80) {
            handleCmdChannelError(type, param);
            return;
        }

        switch (type) {
//第一个判断我自己添加的
            // TODO: 2017/10/17  
            case IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_TEST:
                if ((boolean) param) {
                    mVideoFrag.isYuvDownload = true;
                } else {
                    mVideoFrag.isYuvDownload = false;
                }
                Log.e(TAG, "handleCmdChannelEvent: main EVENT_GET_THUMB");

                break;
            case IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECKSIZE:
                if ((boolean) param) {
                    mVideoFrag.isThumbCheckSize = true;
                } else {
                    mVideoFrag.isThumbCheckSize = false;
                }
                Log.e(TAG, "handleCmdChannelEvent: main EVENT_THUMB_CHECK");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECK:
                if ((boolean) param) {
                    mVideoFrag.isThumbChecked = true;
                } else {
                    mVideoFrag.isThumbChecked = false;
                }
                Log.e(TAG, "handleCmdChannelEvent: main EVENT_THUMB_CHECK");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_SINGLE_SETTING:
                String setting = null;
                String option = null;
                try {
                    setting = ((JSONObject) param).getString("type");
                    option = ((JSONObject) param).getString("param");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mAboutFrag.updateSettings(setting, option);

                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CHECK_STATE:
//                if (mCurrentFrag == mRecordFrag && mRecordFrag != null) {
//                    mRecordFrag.updateRecordView((boolean) param);
//                }
                updateRecButton((boolean) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CHECK_MIC_STATE:
//                if (mCurrentFrag == mRecordFrag && mCurrentFrag != null) {
//                    mRecordFrag.updateMicView((boolean) param);
//                }
                updateMicButton((boolean) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CONNECT_STATE:
                isConnected = (boolean) param;
                if (isConnected) {

                } else {
//                    showAlertDialog("提示", "请将行车记录仪连接车机后再次尝试");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showConnectFrag();
                        }
                    }, 1000);

                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SYNC_TIME:
                mRemoteCam.syncTime();//同步时间
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_SESSION:
                isConnected = true;
//                showToastText("连接成功", Toast.LENGTH_SHORT);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        removeLoadingFrag();
                        showRecordFrag();
                        showVideoFrag();
                    }
                }, 1000);

                mRemoteCam.checkState();
                mRemoteCam.checkMicState();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT:
                showAlertDialog("Warning", (String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_LOG:
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_LS:
                dismissDialog();
                if (mCurrentFrag == mVideoFrag) {
                    mVideoFrag.updateDirContents((JSONObject) param);
                } else if (mCurrentFrag == mPhotoFrag) {
                    mPhotoFrag.updatePicDirContents((JSONObject) param);
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_ATTRIBUTE:
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_BATTERY_LEVEL:
            case IChannelListener.CMD_CHANNEL_EVENT_GET_INFO:
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_DEVINFO:
                // showAlertDialog("Info", (String) param);
//                showVideoFrag();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RECORD_TIME:
                mRecordFrag.upDateRecordTime((String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_DEL:

                if (mCurrentFrag == mVideoDetailFrag) {
                    dismissDialog();
                    onBackPressed();
//                    removeVideoDetailFrag();
                    if (mVideoFrag.currentSegment == 0) {
                        mVideoFrag.showNormalVideo();
                    } else if (mVideoFrag.currentSegment == 1) {
                        mVideoFrag.showCollisionVideo();
                    } else if (mVideoFrag.currentSegment == 2) {
                        mVideoFrag.showPhoto();
                    }
                } else if (mCurrentFrag == mPhotoDetailFrag) {
                    dismissDialog();
                    removePhotoDetailFrag();
                    mPhotoFrag.showPhoto();
                } else if (mCurrentFrag == mVideoFrag) {
                    hadDeleted++;
                    //已删除hadDeleted
                    if (selectedFilesCount == hadDeleted) {
                        //都删完了
                        dismissDialog();
                        if (mVideoFrag.currentSegment == 0) {
                            mVideoFrag.showNormalVideo();
                        } else if (mVideoFrag.currentSegment == 1) {
                            mVideoFrag.showCollisionVideo();
                        } else if (mVideoFrag.currentSegment == 2) {
                            mVideoFrag.showPhoto();
                        }
                        hadDeleted = 0;
                    }
                }
//                else if (mCurrentFrag == mPhotoFrag) {
//                    hadDeleted++;
//                    //已删除hadDeleted
//                    if (deleteFileCount == hadDeleted) {
//                        //都删完了
//                        dismissDialog();
//                        mPhotoFrag.showPhoto();
//                        hadDeleted = 0;
//                    }
//                }

                handler.removeMessages(DDD);
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_DEL_FAIL:
//                dismissDialog();
                showToastText("删除失败，可能是正在录制中的文件，请稍后重试", Toast.LENGTH_SHORT);

                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_ALL_SETTINGS:

                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_CONNECT:
                showWaitDialog("Connecting to Remote Camera");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_LS:
//			showWaitDialog("Fetching Directory Info");
                if (mCurrentFrag == mVideoFrag) {
                    mVideoFrag.refreshView.setRefreshing(true);
                } else if (mCurrentFrag == mPhotoFrag) {
                    mPhotoFrag.refreshView.setRefreshing(true);
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_START:
                showWaitDialog("Waking up the Remote Camera");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_RECORD:
//                mRemoteCam.checkState();
                showCustomToastText("开始录像", Toast.LENGTH_SHORT);
                ivRecord.setClickable(true);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_STOP_RECORD:
//                mRemoteCam.checkState();
                showCustomToastText("停止录像", Toast.LENGTH_SHORT);
                ivRecord.setClickable(true);
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_TAKE_PHOTO:
                mRecordFrag.setPhotoFocus(false);
//                showToastText("拍照成功",Toast.LENGTH_SHORT);
                showCustomToastText("拍照成功", Toast.LENGTH_SHORT);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CONNECTED:
            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_OK:
            case IChannelListener.CMD_CHANNEL_EVENT_SET_SETTING:
//                dismissDialog();
                break;
            default:
                break;
        }
    }

    private void handleCmdChannelError(int type, Object param) {
        switch (type) {
            case IChannelListener.CMD_CHANNEL_ERROR_INVALID_TOKEN:
                isConnected = false;
                if (mCurrentFrag == mRecordFrag) {
                    mRecordFrag.hideTimer();
                }
                showAlertDialog("Error", "Invalid Session! Please start session first!");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_TIMEOUT:
                isConnected = false;
                showAlertDialog("Error", "Timeout! No response from Remote Camera!");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BLE_INVALID_ADDR:
                showAlertDialog("Error", "Invalid bluetooth device");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BLE_DISABLED:
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BROKEN_CHANNEL:
                isConnected = false;
                showAlertDialog("Error", "Lost connection with Remote Camera!");
                resetRemoteCamera();
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_CONNECT:
                isConnected = false;
                showAlertDialog("Error", "Cannot connect to the Camera. \n" + "Please make sure " +
                        "the selected camera is on. \n" + "If problem persists, please reboot " +
                        "both camera and this device.");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_WAKEUP:
                showAlertDialog("Error", "Cannot wakeup the Remote Camera");
                break;
            default:
                break;
        }
    }

    private void handleDataChannelEvent(int type, Object param) {
        switch (type) {
            case IChannelListener.DATA_CHANNEL_EVENT_GET_START:
//                String str = mIsPreview ? "请稍等" : "玩命下载中";
//                showProgressDialog(str, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface Dialog, int which) {
//                        mRemoteCam.cancelGetFile(mGetFileName);
//                    }
//                });
                showCustomDownloadProgressDialog("请稍后", "正在下载", "取消", new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDialog();
                        mRemoteCam.cancelGetFile(mGetFileName);
                        showCustomToastText("下载取消,已下载" + hadDownloadCount + "个文件", Toast
                                .LENGTH_SHORT);
                        hadDownloadedFlag = 0;
                        hadDownloadCount = 0;
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mRemoteCam.cancelGetFile(mGetFileName);
                        showCustomToastText("下载取消,已下载" + hadDownloadCount + "个文件", Toast
                                .LENGTH_SHORT);
                        hadDownloadedFlag = 0;
                        hadDownloadCount = 0;
                    }
                });

                break;
            case IChannelListener.DATA_CHANNEL_EVENT_GET_PROGRESS:
                // mCustomDownloadDig.setProgress((Integer)
                // param);
                hmDownloadDig.setProgress((Integer) param);
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_GET_FINISH:

                showToastText("下载成功", Toast.LENGTH_SHORT);

                hadDownloadedFlag++;
                hadDownloadCount++;
                downLoadFiles();
                break;

            case IChannelListener.DATA_CHANNEL_EVENT_PUT_MD5:
                showWaitDialog("Calculating mMD5");
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_START:

                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_PROGRESS:
                // mCustomDownloadDig.setProgress((Integer)
                // param);
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_FINISH:
                showToastText("���³ɹ���������¼������ɸ���", Toast.LENGTH_SHORT);
                mPutFileName = null;
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_CANCLE_XFER:
                dismissDialog();

                break;
            default:
                break;
        }
    }

    private void handleStreamChannelEvent(int type, Object param) {
        switch (type) {
            case IChannelListener.STREAM_CHANNEL_EVENT_BUFFERING:
                showWaitDialog("Buffering...");
                break;
            case IChannelListener.STREAM_CHANNEL_EVENT_PLAYING:
                dismissDialog();
                mRecordFrag.startStreamView();
                break;

            case IChannelListener.STREAM_CHANNEL_ERROR_PLAYING:
                mRecordFrag.resetStreamView();
                showAlertDialog("Error", "Cannot connect to LiveView!");
                break;
            default:
                break;

        }
    }
}
