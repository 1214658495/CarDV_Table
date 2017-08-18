package com.bydauto.car.i_key.cardv_table.custom;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.R;

import java.text.NumberFormat;


public class HMDownloadProgressDialog extends Dialog {

    private ProgressBar mProgress;
    private TextView mProgressPercent;
    private TextView mProgressMessage;
    private ImageView mImgLoading;

    private Handler mViewUpdateHandler;
    private int mMax;
    private CharSequence mMessage;
    private String title;
    private boolean mHasStarted;
    private int mProgressVal;

    private final String TAG = "CommonProgressDialog";
    private NumberFormat mProgressPercentFormat;

    private final Context context;
    private String positiveButtonText;
    private String nagetiveButtonText;
    private OnClickListener positiveButtonClickListener;

    public HMDownloadProgressDialog(Context context) {
        super(context);
        this.context = context;
        initFormats();
    }

    public HMDownloadProgressDialog(Context context, int theme) {
        super(context, theme);
        this.context = context;
        initFormats();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final HMDownloadProgressDialog dialog = new HMDownloadProgressDialog(context, R.style
				.Dialog);
        dialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
        View layout = inflater.inflate(R.layout.customdownloadprogressdialog, null, false);
        setContentView(layout);

        mProgress = (ProgressBar) findViewById(R.id.progress);
        mProgressPercent = (TextView) findViewById(R.id.progress_percent);
        mProgressMessage = (TextView) findViewById(R.id.progress_message);

        mImgLoading = (ImageView) findViewById(R.id.img_loading);
        Animation testAnim = AnimationUtils.loadAnimation(getContext(), R.drawable
				.anim_loading_rotate);
        mImgLoading.startAnimation(testAnim);
        if (this.title != null) {
            ((TextView) layout.findViewById(R.id.progress_title)).setText(title);
        }
        if (positiveButtonText != null || nagetiveButtonText != null) {
            if (positiveButtonText != null) {
                ((Button) layout.findViewById(R.id.positiveButton)).setText(positiveButtonText);
                if (positiveButtonClickListener != null) {
                    ((Button) layout.findViewById(R.id.positiveButton)).setOnClickListener(new
																								   View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // method stub
                            positiveButtonClickListener.onClick(dialog, DialogInterface
									.BUTTON_POSITIVE);
                        }
                    });
                }
            } else {
                layout.findViewById(R.id.positiveButton).setVisibility(View.GONE);
            }
        }

        mViewUpdateHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int progress = mProgress.getProgress();
                int max = mProgress.getMax();
                // double dProgress = (double) progress /
                // (double) (1024 * 1024);
                double dProgress = progress;
                // double dMax = (double) max / (double)
                // (1024 * 1024);
                double dMax = max;
                if (mProgressPercentFormat != null) {
                    double percent = (double) progress / (double) max;
                    SpannableString tmp = new SpannableString(mProgressPercentFormat.format
							(percent));
                    tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tmp.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mProgressPercent.setText(tmp);
                } else {
                    mProgressPercent.setText("");
                }
            }

        };

        onProgressChanged();
        if (mMessage != null) {
            setMessage(mMessage);
        }
        if (mMax > 0) {
            setMax(mMax);
        }
        if (mProgressVal > 0) {
            setProgress(mProgressVal);
        }
    }

    private void initFormats() {
        mProgressPercentFormat = NumberFormat.getPercentInstance();
        mProgressPercentFormat.setMaximumFractionDigits(0);
    }

    private void onProgressChanged() {
        mViewUpdateHandler.sendEmptyMessage(0);

    }

    public void setProgressStyle(int style) {
        // mProgressStyle = style;
    }

    public int getMax() {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }

    public void setMax(int max) {
        if (mProgress != null) {
            mProgress.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        }
    }

    public void setProgress(int value) {
        if (mHasStarted) {
            mProgress.setProgress(value);
            onProgressChanged();
        } else {
            mProgressVal = value;
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public void setMessage(CharSequence message) {
        // super.setMessage(message);
        if (mProgressMessage != null) {
            mProgressMessage.setText(message);
        } else {
            mMessage = message;
        }
    }

    public void setPositiveButton(String positiveButtonText, OnClickListener listener) {
        this.positiveButtonText = positiveButtonText;
        this.positiveButtonClickListener = listener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHasStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHasStarted = false;
    }

}