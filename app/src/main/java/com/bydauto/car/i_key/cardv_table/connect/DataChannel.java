package com.bydauto.car.i_key.cardv_table.connect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jli on 9/19/14.
 */
public class DataChannel {
    private final static String TAG = "DataChannel";
    private final static int PROGRESS_MIN_STEP = 1;

    protected IChannelListener mListener;
    protected InputStream mInputStream;
    protected OutputStream mOutputStream;
    protected boolean mContinueRx;

    protected boolean mContinueTx;
    protected int mTxBytes;
    protected final Object mTxLock = new Object();

    private static final ExecutorService worker = Executors.newSingleThreadExecutor();

    public DataChannel(IChannelListener listener) {
        mListener = listener;
    }

    public DataChannel setStream(InputStream input, OutputStream output) {
        mInputStream = input;
        mOutputStream = output;
        return this;
    }

//    添加自己的方法

    public InputStream getmInputStream() {
        return mInputStream;
    }

    public void getFile(final String dstPath, final int size) {
        mContinueRx = true;
        worker.execute(new Runnable() {
            @Override
            public void run() {
                rxStream(dstPath, size);
            }
        });
    }

//    public void getYuvFile(final String dstPath, final int size, final String md5) {
////		final Bitmap[] bitmap = {null};
//        mContinueRx = true;
//        worker.execute(new Runnable() {
//            @Override
//            public void run() {
////                rxYUY2Stream(dstPath, size, md5);
////                rxYuvStream2(size, md5);
//            }
//        });
//    }

    public void getYuvFile(final String dstPath, final int size, final String md5) {
        rxYuvStream2();
    }

    public void cancelGetFile() {
        mContinueRx = false;
    }

    public void putFile(final String srcPath) {
        mContinueTx = true;
        worker.execute(new Runnable() {
            @Override
            public void run() {
                txStream(srcPath);
            }
        });
    }

    public int cancelPutFile() {
        mContinueTx = false;
        synchronized (mTxLock) {
            try {
                mTxLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mTxBytes;
    }

    private void txStream(String srcPath) {
        int total = 0;
        int prev = 0;

        try {
            byte[] buffer = new byte[1024];
            File file = new File(srcPath);
            FileInputStream in = new FileInputStream(file);
            final int size = (int) file.length();

            mTxBytes = 0;
            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_PUT_START, srcPath);
            while (mContinueTx) {
                int read = in.read(buffer);
                if (read <= 0) {
                    break;
                }
                mOutputStream.write(buffer, 0, read);
                mTxBytes += read;

                total += read;
                int curr = (int) (((long) total * 100) / size);
                if (curr - prev >= PROGRESS_MIN_STEP) {
                    mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_PUT_PROGRESS, curr);
                    prev = curr;
                }
            }
            in.close();

            if (mContinueTx) {
                mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_PUT_FINISH, srcPath);
            } else {
                synchronized (mTxLock) {
                    mTxLock.notify();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rxStream(String dstPath, int size) {
        int total = 0;
        int prev = 0;
        try {
            byte[] buffer = new byte[1024];
            FileOutputStream out = new FileOutputStream(dstPath);
            int bytes;

            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_START, dstPath);
            while (total < size) {
                try {
                    bytes = mInputStream.read(buffer);
                    out.write(buffer, 0, bytes);
                } catch (SocketTimeoutException e) {
                    if (!mContinueRx) {
                        Log.e(TAG, "RX canceled");
                        File file = new File(dstPath);
                        Log.e(TAG, "取消下载删除：" + dstPath);
                        file.delete();
                        mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_CANCLE_XFER, null);
                        out.close();
                        return;
                    }
                    continue;
                }

                total += bytes;
                int curr = (int) (((long) total * 100) / size);
                if (curr - prev >= PROGRESS_MIN_STEP) {
                    mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_PROGRESS, curr);
                    prev = curr;
                }
            }
            out.close();
            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_FINISH, dstPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rxYUY2Stream(String dstPath, int size, String md5) {
        int total = 0;
        int prev = 0;
//        MessageDigest digest;
        try {
            byte[] buffer = new byte[size];
            Log.e(TAG, "rxYUY2Stream: size:-----" + size);
            FileOutputStream out = new FileOutputStream(dstPath);

            int bytes;
//            digest = MessageDigest.getInstance("MD5");
//            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_START, dstPath);
            while (total < size) {
                try {
                    bytes = mInputStream.read(buffer, total, size - total);
                    if (bytes > 0) {
                        total += bytes;
                    } else {
                        break;
                    }
                    Log.e(TAG, "rxYUY2Stream: bytes:-----" + bytes);
                    if (bytes == size) {
//                        digest.update(buffer, 0, bytes);
//                        BigInteger bigInt = new BigInteger(1, digest.digest());
//                        String bitIntString = "00" + bigInt.toString(16);
//                        String bitIntString1 = bitIntString.substring(bitIntString.length() - 32);
//                        Log.e(TAG, "rxYUY2Stream: bigInt.toString(16) = " + bigInt.toString(16));
//                        Log.e(TAG, "rxYUY2Stream: bitIntString1       = " + bitIntString1);
//                        Log.e(TAG, "rxYUY2Stream: 原md5 =               " + md5);
//                    if (bitIntString1.equals(md5)) {
                        out.write(buffer, 0, bytes);
//                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECK, true);
//                        Log.e(TAG, "rxYUY2Stream: 校验成功");
//                    } else {
//                        Log.e(TAG, "rxYUY2Stream: 校验失败");
//                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECK, false);
//                    }
                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECKSIZE, true);
                    }
//                    else {
//                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_THUMB_CHECKSIZE, false);
//                    }
                } catch (SocketTimeoutException e) {

                }
            }
            out.close();
            mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_TEST, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap rxYuvStream2() {
        int total = 0;
        int width = 160;
        int height = 90;
        int size = 34560;
        Bitmap bitmap = null;
        MessageDigest digest = null;
        try {
            byte[] yuvArray = new byte[size];
            byte[] yuvArray1 = new byte[160 * 90 * 2];
            byte[] yuvArray2 = new byte[160 * 90 * 2];
//			FileOutputStream out = new FileOutputStream(dstPath);
            int bytes;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


//			mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_START, dstPath);
            while (total < size) {
                try {
                    bytes = mInputStream.read(yuvArray, total, size - total);
                    Log.e(TAG, "rxYuvStream2: bytes = " + bytes);
                    if (bytes > 0) {
                        total += bytes;
                    } else {
                        Log.e(TAG, "rxYuvStream2: bytes <= 0");
                        break;
                    }
                    Log.e(TAG, "rxYuvStream2: mInputStream.read");
                    if (total == size) {
                        digest.update(yuvArray, 0, bytes);
                        BigInteger bigInt = new BigInteger(1, digest.digest());
                        String bitIntString = "00" + bigInt.toString(16);
                        String bitIntString1 = bitIntString.substring(bitIntString.length() - 32);
                        Log.e(TAG, "rxYUY2Stream: bigInt.toString(16) = " + bigInt.toString(16));
                        Log.e(TAG, "rxYUY2Stream: bitIntString1       = " + bitIntString1);
//                        Log.e(TAG, "rxYUY2Stream: 原md5 =               " + md5);
                        for (int i = 0; i < 90 * 2; i++) {
                            System.arraycopy(yuvArray, 192 * i, yuvArray1, 160 * i, 160);
                        }

                        for (int j = 0; j < 160 * 90; j++) {
                            yuvArray2[2 * j] = yuvArray1[j];
                            yuvArray2[2 * j + 1] = yuvArray1[160 * 90 + j];
                        }

                        YuvImage yuvImage = new YuvImage(yuvArray2, ImageFormat.YUY2, width, height, null);

                        if (yuvArray2 != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            Log.e(TAG, "rxYuvStream2: BitmapFactory.decodeByteArray stream.size()" + stream.size() + bitmap.getByteCount());
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                }
            }
//			out.close();
//            mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_TEST, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public Bitmap rxYuvStream() {
        int width = 160;
        int height = 90;
        Bitmap bitmap = null;
        try {
            byte[] yuvArray = new byte[34560];
            byte[] yuvArray1 = new byte[160 * 90 * 2];
            byte[] yuvArray2 = new byte[160 * 90 * 2];
//			FileOutputStream out = new FileOutputStream(dstPath);
            int bytes;

//			mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_START, dstPath);
//			while (total < size) {
            try {
//                if (mInputStream.read(yuvArray) != -1) {
                mInputStream.read(yuvArray);
                Log.e(TAG, "rxYuvStream: mInputStream.read");
                for (int i = 0; i < 90 * 2; i++) {
                    System.arraycopy(yuvArray, 192 * i, yuvArray1, 160 * i, 160);
                }

                for (int j = 0; j < 160 * 90; j++) {
                    yuvArray2[2 * j] = yuvArray1[j];
                    yuvArray2[2 * j + 1] = yuvArray1[160 * 90 + j];
                }

                YuvImage yuvImage = new YuvImage(yuvArray2, ImageFormat.YUY2, width, height, null);

                if (yuvArray2 != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                    bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                    Log.e(TAG, "rxYuvStream: BitmapFactory.decodeByteArray stream.size()" + stream.size() + bitmap.getByteCount());
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
//                } else {
//                    Log.e(TAG, "rxYuvStream: mInputStream.read(yuvArray)) = null");
//                }
//					out.write(buffer, 0, bytes);
            } catch (SocketTimeoutException e) {
                if (!mContinueRx) {
                    Log.e(TAG, "RX canceled");
//						File file = new File(dstPath);
//						Log.e(TAG, "取消下载删除：" + dstPath);
//						file.delete();
//						mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_CANCLE_XFER, null);
//						out.close();
//						return;
                }
//					continue;
            }
//			}
//			out.close();
//			mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_FINISH, dstPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
