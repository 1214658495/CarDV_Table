package com.bydauto.car.i_key.cardv_table.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.Model;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;
import com.bydauto.car.i_key.cardv_table.util.ServerConfig;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class VideoDetailFragment extends Fragment implements OnClickListener, SurfaceHolder
        .Callback, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "VideoDetailFragment";

    private static final int HANDLER_BUFFER_START = 1;
    private static final int HANDLER_BUFFER_END = 2;
    private static final int HANDLER_SURFACE_SIZE = 3;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;

    private final int mCurrentSize = SURFACE_BEST_FIT;

    private static final int SHOW_PROGRESS = 0;
    private static final int SHOW_CONTROLLER = 1;

//    private static final String ServerConfig.HOST = "192.168.8.6";
    private String filePath;
    private IFragmentListener mListener;

    private int currentIndex;
    private int totalIndex;
    private ArrayList<Model> mPlaylist;
    private String mPWD;
    private String mListPath;

    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    private ImageView mLoading;
    private ImageView btn_back;
    private ImageView img_pause, img_play;
    private TextView btn_delete, btn_export, btn_fullScreen;
    private RelativeLayout title_video;
    @BindView(R.id.video_title)
    TextView video_title;
    private boolean showControl = true;
    @BindView(R.id.player_control)
    View player_control;

    @OnClick(R.id.playbackSurfaceView)
    void clickVideoView() {
        if (showControl) {
            player_control.setVisibility(View.GONE);
            handler.removeMessages(SHOW_CONTROLLER);
        } else {
            player_control.setVisibility(View.VISIBLE);
            handler.sendEmptyMessageDelayed(SHOW_CONTROLLER, 3000);
        }
        showControl = !showControl;
    }

    private int isOnly = 0;

    private IjkMediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private SeekBar progressSeekBar;
    private TextView tv_currentTime, tv_totalTime;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS:
//                    setOvProgress();
//                    handler.sendEmptyMessageDelayed(SHOW_PROGRESS, 500);
                    long pos = setOverlayProgress();
//                    if (!isDragging && isShowControlPanl) {
//                        msg = obtainMessage(MESSAGE_SHOW_PROGRESS);
                    sendEmptyMessageDelayed(SHOW_PROGRESS, 1000 - (pos % 1000));
//                        updatePausePlay();
//                    }
                    break;
                case SHOW_CONTROLLER:
                    clickVideoView();
                    break;
            }
        }
    };

    public VideoDetailFragment() {

    }

    @SuppressLint("ValidFragment")
    public VideoDetailFragment(ArrayList<Model> playlist, Model currentItem, String pwd) {
        currentIndex = playlist.indexOf(currentItem);
        totalIndex = playlist.size();
        mPlaylist = playlist;
//        mPWD = pwd + "/M_video/";
        mPWD = pwd + "/";
        mListPath = pwd;
        filePath = mPWD + currentItem.getName();
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_detail, null, false);
        ButterKnife.bind(this, view);//bangding frag
        initView(view);
        initVideoView(view);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IFragmentListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e(TAG, "onDetach");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            handler.removeMessages(SHOW_PROGRESS);
            handler.removeMessages(SHOW_CONTROLLER);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
    }

    private void initVideoView(View view) {

        surfaceView = (SurfaceView) view.findViewById(R.id.playbackSurfaceView);
        surfaceView.getHolder().setFormat(PixelFormat.RGBX_8888);

        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay().getWidth
                (), getActivity().getWindowManager().getDefaultDisplay().getWidth() / 16 * 9);

        surfaceHolder.addCallback(this);

        mediaPlayer = new IjkMediaPlayer();
        try {
            mediaPlayer.setDataSource("rtsp://" + ServerConfig.HOST + getStreamURL());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //mediaPlayer准备工作
        mediaPlayer.setOnPreparedListener(this);
        //MediaPlayer完成
        mediaPlayer.setOnCompletionListener(this);

        mediaPlayer.setOnBufferingUpdateListener(this);

        options(mediaPlayer);
    }

    private void options(IjkMediaPlayer mp) {
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 16);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 50000);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
//        if (mIsLive) {
        // Param for living
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
//        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
//        } else {
        // Param for playback
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
        mp.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
//        }
    }

    private void initView(View view) {
        String nameFile = filePath.replace(mPWD, "");//mingc
        Log.e(TAG, "initView: 11 nameFile 2017-08-29-14-32-20A.MP4"+ nameFile);
        // TODO: 2017/8/29 仿照下面修改文件名
//        nameFile = nameFile.substring(nameFile.indexOf('_') + 1, nameFile.indexOf('A'));
//        //yymmdd hhmmss
//        StringBuilder sb = new StringBuilder(nameFile);
//        sb.insert(6, '-').insert(4, '-');//yy-mm-dd
//        sb.insert(sb.indexOf("_") + 3, ':').insert(sb.indexOf("_") + 6, ':').replace(sb.indexOf
//                ("_"), sb.indexOf("_") + 1, " ");

//        video_title.setText(sb);
        video_title.setText(nameFile);

        img_pause = (ImageView) view.findViewById(R.id.img_pause);
        img_pause.setOnClickListener(this);

        img_play = (ImageView) view.findViewById(R.id.img_play);
        img_play.setOnClickListener(this);

        tv_currentTime = (TextView) view.findViewById(R.id.current_time);
        tv_totalTime = (TextView) view.findViewById(R.id.total_time);

        progressSeekBar = (SeekBar) view.findViewById(R.id.progress);
        progressSeekBar.setOnSeekBarChangeListener(this);
        progressSeekBar.setMax(1000);

        handler.sendEmptyMessage(SHOW_PROGRESS);
        handler.sendEmptyMessageDelayed(SHOW_CONTROLLER, 3000);

        title_video = (RelativeLayout) view.findViewById(R.id.title_video);

        btn_fullScreen = (TextView) view.findViewById(R.id.btn_full_screen);
        btn_fullScreen.setOnClickListener(this);

        mLoading = (ImageView) view.findViewById(R.id.player_overlay_loading);
        startLoadingAnimation();

        btn_back = (ImageView) view.findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);

        btn_delete = (TextView) view.findViewById(R.id.btn_delete);
        btn_delete.setOnClickListener(this);

        btn_export = (TextView) view.findViewById(R.id.btn_export);
        btn_export.setOnClickListener(this);
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
        mLoading.setVisibility(View.INVISIBLE);
        mLoading.clearAnimation();
    }

    private String getStreamURL() {
        // TODO: 2017/8/29 代替什么
        Log.e(TAG, "getStreamURL: 11111filePath前"+ filePath);
//        return filePath.replace("M_video", "S_video").replace("A.MP4", "B.MP4").trim();
        return filePath.trim();
    }

    private String millisToString(long millis, boolean text) {
        boolean negative = millis < 0;
        millis = Math.abs(millis);
        int mini_sec = (int) millis % 1000;
        millis /= 1000;
        int sec = (int) (millis % 60);
        millis /= 60;
        int min = (int) (millis % 60);
        millis /= 60;
        int hours = (int) millis;

        String time;
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        format.applyPattern("00");

        DecimalFormat format2 = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        format2.applyPattern("000");
        if (text) {
            if (millis > 0) time = (negative ? "-" : "") + hours + "h" + format.format(min) + "min";
            else if (min > 0) time = (negative ? "-" : "") + min + "min";
            else time = (negative ? "-" : "") + sec + "s";
        } else {
            if (millis > 0)
                time = (negative ? "-" : "") + hours + ":" + format.format(min) + ":" + format
                        .format(sec);
                // + ":" + format2.format(mini_sec);
            else time = (negative ? "-" : "") + min + ":" + format.format(sec);
            // + ":"+ format2.format(mini_sec);
        }
        return time;
    }

    private boolean isTheFirstVideo() {
        return (currentIndex == 0);
    }

    private boolean isTheLastVideo() {
        return (currentIndex == totalIndex - 1);
    }

    private void showPlayHub() {
    }

    private void hidePlayHub() {
    }

    private final void doPlayPause() {
        handler.removeMessages(SHOW_CONTROLLER);
        handler.sendEmptyMessageDelayed(SHOW_CONTROLLER,3000);
        if (mediaPlayer.isPlaying()) {
            pause();
//            stop();
        } else {
            play();
        }
    }

    private void stop() {
        handler.removeMessages(SHOW_PROGRESS);
//        progressSeekBar.setProgress(0);
        mediaPlayer.stop();
//        tv_currentTime.setText("0:00");
        img_pause.setVisibility(View.INVISIBLE);
        img_play.setVisibility(View.VISIBLE);
//        mSurfaceView.setKeepScreenOn(false);
    }

    private void pause() {
        handler.removeMessages(SHOW_PROGRESS);
        mediaPlayer.pause();
        img_pause.setVisibility(View.INVISIBLE);
        img_play.setVisibility(View.VISIBLE);
//        img_pause.setImageResource(R.drawable.bt_icon_play);
//        mSurfaceView.setKeepScreenOn(false);
    }

    private void play() {
        handler.sendEmptyMessage(SHOW_PROGRESS);
        mediaPlayer.start();
        img_play.setVisibility(View.INVISIBLE);
        img_pause.setVisibility(View.VISIBLE);
//        img_pause.setImageResource(R.drawable.bt_icon_stop);
//        mSurfaceView.setKeepScreenOn(true);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.img_play:
                doPlayPause();
                break;
            case R.id.img_pause:
                doPlayPause();
                break;
            case R.id.btn_back:
//                ((MainActivity) getActivity()).removeVideoDetailFrag();
                ((MainActivity) getActivity()).onBackPressed();
                break;
            case R.id.btn_delete:
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle("提示")
                        .setTitle("确定删除该文件？").setPositiveButton("确定", new DialogInterface
                                .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE, filePath);
                        mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE_STHWRONG,
                                null);
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();

                break;
            case R.id.btn_export:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_DOWNLOAD, filePath);
                break;
            case R.id.btn_full_screen:
//                Log.e("ddd","cccc");
//                surfaceHolder.setFixedSize(900,900);
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default:
                break;
        }

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

    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mp.start();
        stopLoadingAnimation();
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        Log.e("ddd", "播完了");
//        mp.seekTo(1);
//       stop();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
    }

    public void fullScreen(boolean yes) {

        if (yes) {
            surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay()
                    .getWidth(), getActivity().getWindowManager().getDefaultDisplay().getHeight());

//            ((MainActivity) getActivity()).hideNavigation(true);

//            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
//            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//            getActivity().getWindow().setAttributes(lp);
//            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            title_video.setVisibility(View.GONE);
        } else {
            surfaceHolder.setFixedSize(getActivity().getWindowManager().getDefaultDisplay()
                    .getWidth(), getActivity().getWindowManager().getDefaultDisplay().getWidth()
                    / 16 * 9);

//            ((MainActivity) getActivity()).hideNavigation(false);

//            WindowManager.LayoutParams attr = getActivity().getWindow().getAttributes();
//            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            getActivity().getWindow().setAttributes(attr);
//            getActivity().getWindow().clearFlags(WindowManager.LayoutParams
// .FLAG_LAYOUT_NO_LIMITS);

            title_video.setVisibility(View.VISIBLE);
        }
    }

    private long setOverlayProgress() {
        long position = mediaPlayer.getCurrentPosition();
        long duration = mediaPlayer.getDuration();
        if (position > duration) {

            return 0;
        }
        if (progressSeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                progressSeekBar.setProgress((int) pos);
            }

        }
//        Log.e("ddd", position + " " + duration);
        tv_currentTime.setText(millisToString(position, false));
        tv_totalTime.setText(millisToString(duration, false));

        return position;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        } else {
            long duration = mediaPlayer.getDuration();
            int position = (int) ((duration * progress * 1.0) / 1000);
            String time = millisToString(position, false);
            tv_currentTime.setText(time);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeMessages(SHOW_CONTROLLER);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        long duration = mediaPlayer.getDuration();
        mediaPlayer.seekTo((int) ((duration * seekBar.getProgress() * 1.0) / 1000));
        handler.sendEmptyMessageDelayed(SHOW_CONTROLLER,3000);
    }
}
