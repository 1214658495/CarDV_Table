package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class RecordFragment extends Fragment implements OnClickListener, IMediaPlayer
        .OnPreparedListener, IMediaPlayer.OnCompletionListener, SurfaceHolder.Callback,
        IMediaPlayer.OnBufferingUpdateListener {
    private final static String TAG = "---->RecordFragment";

    private RemoteCam mRemoteCam;
    private IFragmentListener mListener;

    public IjkMediaPlayer mediaPlayer;
    public SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private LinearLayout isRecIcon;
    private TextView mViewTime;
    private TextView takePhotoBtn, fullScreenBtn;
    private ImageView recordSwitch;
    private ImageView micSwitch;
    private ImageView mLoading;
    private RelativeLayout record_bg;
    private RelativeLayout recordTitle;
//    private View backupView;

    public static int mRecordingState;
    public static int mMicphoneState;

    public final static int STATE_OFF = 0b00;
    public final static int STATE_ON = 0b10;

    @BindView(R.id.iv_photo_focus)
    ImageView photoFocus;


    static private final ScheduledExecutorService worker = Executors
            .newSingleThreadScheduledExecutor();
    static private ScheduledFuture<?> mScheduledTask;

    public void setRemoteCam(RemoteCam cam) {
        mRemoteCam = cam;
    }

    public void reset() {
        mRecordingState = STATE_OFF;
        hideTimer();
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, null, false);
        ButterKnife.bind(this, view);
        initView(view);
        initVideoView(view);
//		if (mPlayingState == STATE_ON)
//			mVideoView.start();
//        backupView = view;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        hideTimer();// ����
    }

    @Override
    public void onDetach() {
        Log.e(TAG, "onDetach");
        super.onDetach();
        if (mScheduledTask != null) {
            mScheduledTask.cancel(true);
            mScheduledTask = null;
        }
        mListener = null;


        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Log.e(TAG, "onAttach");
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IFragmentListener");
        }
    }

    private void initVideoView(View view) {
        surfaceView = (SurfaceView) view.findViewById(R.id.videoViewFinder);
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay().getWidth
                (), getActivity().getWindowManager().getDefaultDisplay().getWidth() / 16 * 9);

        surfaceHolder.addCallback(this);
        mediaPlayer = new IjkMediaPlayer();
        Log.e(TAG,""+ System.currentTimeMillis());
        options(mediaPlayer);
        try {
            mediaPlayer.setDataSource("rtsp://192.168.42.1/live");
//            mediaPlayer.setDataSource("/storage/emulated/0/比亚迪行车记录仪/000002AA.MP4");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //mediaPlayer准备工作
        mediaPlayer.setOnPreparedListener(this);
        //MediaPlayer完成
        mediaPlayer.setOnCompletionListener(this);

        mediaPlayer.setOnBufferingUpdateListener(this);//当前加载进度的监听

//        mediaPlayer.prepareAsync();
    }

    private void initView(View view) {

        recordTitle = (RelativeLayout) view.findViewById(R.id.title_record);

        record_bg = (RelativeLayout) view.findViewById(R.id.record_bg);
        mLoading = (ImageView) view.findViewById(R.id.record_loading);
        startLoadingAnimation();

        mViewTime = (TextView) view.findViewById(R.id.textViewRecordTime);

        takePhotoBtn = (TextView) view.findViewById(R.id.btn_take_photo);
        takePhotoBtn.setOnClickListener(this);

        fullScreenBtn = (TextView) view.findViewById(R.id.btn_full_screen);
        fullScreenBtn.setOnClickListener(this);

        recordSwitch = (ImageView) view.findViewById(R.id.btn_record_switch);
        recordSwitch.setOnClickListener(this);

        micSwitch = (ImageView) view.findViewById(R.id.btn_mic_switch);
        micSwitch.setOnClickListener(this);

        isRecIcon = (LinearLayout) view.findViewById(R.id.isRec);


    }

    private void options(IjkMediaPlayer mp) {
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 16);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 50000);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
//
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_all_videos", 1);//硬解码
//
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);

        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 60);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 0);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fps", 30);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", -16);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_idct", -16);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer
                .SDL_FCC_RV32);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 1024);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 5);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probsize", 1024);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 500000);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "rtsp_transport", 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_photo:
                mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_START, null);
                break;
            case R.id.btn_full_screen:

//                if (getActivity().getRequestedOrientation() != ActivityInfo
//                        .SCREEN_ORIENTATION_LANDSCAPE) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//                }
                break;
            case R.id.btn_record_switch:
                if (mRecordingState != STATE_OFF) {
                    //若开则关
                    mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_STOP, null);
//                   mediaPlayer.release();
                } else {
                    //若关则开
                    mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_START, null);
                }
                break;

            case R.id.btn_mic_switch:
                if (mMicphoneState != STATE_OFF) {
                    //若开则关
                    mListener.onFragmentAction(IFragmentListener.ACTION_MIC_OFF, null);
                } else {
                    //若关则开
                    mListener.onFragmentAction(IFragmentListener.ACTION_MIC_ON, null);
                }

                break;
            default:
                break;
        }
    }

    public void startStreamView() {

        isRecIcon.setVisibility(View.VISIBLE);
//        showTimer();
    }

    public void stopStreamView() {

        isRecIcon.setVisibility(View.INVISIBLE);
//        hideTimer();
    }

    public void resetStreamView() {
//        Log.e(TAG,"resetstream");
//        initVideoView(backupView);
    }

    public void showTimer() {
        mViewTime.setVisibility(View.VISIBLE);
        mScheduledTask = worker.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_TIME, null);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void hideTimer() {
        if (mViewTime != null) {
            mViewTime.setVisibility(View.INVISIBLE);
        }
        if (mScheduledTask != null) {
            mScheduledTask.cancel(false);
        }
    }

    public void upDateRecordTime(String time) {
        int seconds = Integer.parseInt(time);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds -= minutes * 60;
        minutes -= hours * 60;
        final String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        mViewTime.setText(timeText);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mp.start();
        Log.e(TAG,""+ System.currentTimeMillis());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopLoadingAnimation();
                startStreamView();
            }
        }, 1500);
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer.setDisplay(holder);
        //开启异步准备
        mediaPlayer.prepareAsync();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
    }

    private void startLoadingAnimation() {
        AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);
        mLoading.startAnimation(anim);
    }

    private void stopLoadingAnimation() {
        record_bg.setVisibility(View.INVISIBLE);
        mLoading.setVisibility(View.INVISIBLE);
        mLoading.clearAnimation();
    }

    public void updateRecordView(boolean isRecord) {
//        Log.e(TAG,"updateRecordView");
        if (isRecord) {
            mRecordingState = STATE_ON;
            //图片替换
            recordSwitch.setImageResource(R.drawable.record_on_selector);
        } else {
            mRecordingState = STATE_OFF;
            //图片替换
            recordSwitch.setImageResource(R.drawable.record_off_selector);
        }
    }

    public void updateMicView(boolean isOn) {
//        Log.e(TAG,"updateMicView");
        if (isOn) {
            mMicphoneState = STATE_ON;
            //图片替换
            micSwitch.setImageResource(R.drawable.mic_on_selector);
        } else {
            mMicphoneState = STATE_OFF;
            //图片替换
            micSwitch.setImageResource(R.drawable.mic_off_selector);

        }
    }

    public void startRecord() {
        mRecordingState = STATE_ON;
//        updateRecordView(true);
        ((MainActivity) getActivity()).updateRecButton(true);
    }

    public void stopRecord() {
        mRecordingState = STATE_OFF;
//        updateRecordView(false);
        ((MainActivity) getActivity()).updateRecButton(false);
    }

    public void startMic() {
        mMicphoneState = STATE_ON;
//        updateMicView(true);
        ((MainActivity) getActivity()).updateMicButton(true);
    }

    public void stopMic() {
        mMicphoneState = STATE_OFF;
//        updateMicView(false);
        ((MainActivity) getActivity()).updateMicButton(false);
    }

    public void fullScreen(boolean yes) {

        if (yes) {
            surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay()
                    .getWidth(), getActivity().getWindowManager().getDefaultDisplay().getHeight());

            ((MainActivity) getActivity()).hideNavigation(true);

            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getActivity().getWindow().setAttributes(lp);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            recordTitle.setVisibility(View.GONE);
        } else {
            surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay()
                    .getWidth(), getActivity().getWindowManager().getDefaultDisplay().getWidth()
                    / 16 * 9);

            ((MainActivity) getActivity()).hideNavigation(false);

            WindowManager.LayoutParams attr = getActivity().getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getActivity().getWindow().setAttributes(attr);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            recordTitle.setVisibility(View.VISIBLE);
        }
    }

    public void setPhotoFocus(boolean show) {
        if (show) {
            photoFocus.setVisibility(View.VISIBLE);
        } else {
            photoFocus.setVisibility(View.INVISIBLE);
        }
    }
}
