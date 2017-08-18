package com.bydauto.car.i_key.cardv_table.custom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.R;

import org.xmlpull.v1.XmlPullParser;


public class SegmentView extends LinearLayout {
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private onSegmentViewClickListener listener;

    public SegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SegmentView(Context context) {
        super(context);
        init();
    }

    private void init() {
//		 this.setLayoutParams(new
//		 LinearLayout.LayoutParams(dp2Px(getContext(),
//		 60), LinearLayout.LayoutParams.WRAP_CONTENT));
        textView1 = new TextView(getContext());
        textView2 = new TextView(getContext());
        textView3 = new TextView(getContext());
        textView1.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams
                .WRAP_CONTENT));
        textView2.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams
                .WRAP_CONTENT));
        textView3.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams
                .WRAP_CONTENT));
        textView1.setText("行车视频");
        textView2.setText("锁定视频");
        textView3.setText("照片");
        XmlPullParser xrp = getResources().getXml(R.drawable.seg_text_color_selector);
        try {
            ColorStateList csl = ColorStateList.createFromXml(getResources(), xrp);
            textView1.setTextColor(csl);
            textView2.setTextColor(csl);
            textView3.setTextColor(csl);
        } catch (Exception e) {

        }
        textView1.setGravity(Gravity.CENTER);
        textView2.setGravity(Gravity.CENTER);
        textView3.setGravity(Gravity.CENTER);
        textView1.setPadding(0, 6, 19, 6);
        textView2.setPadding(19, 6, 0, 6);
        textView3.setPadding(19, 6, 19, 6);


        setSegmentTextSize();
//        textView1.setBackgroundResource(R.drawable.seg_left);
//        textView2.setBackgroundResource(R.drawable.seg_right);
//        textView3.setBackgroundResource(R.drawable.seg_right);
        textView1.setSelected(true);
        this.removeAllViews();
        this.addView(textView1);
        this.addView(textView3);
        this.addView(textView2);
        this.invalidate();
//        textView3.setVisibility(View.GONE);

        textView1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (textView1.isSelected()) {
                    return;
                }
                textView1.setSelected(true);
                textView2.setSelected(false);
                textView3.setSelected(false);
                if (listener != null) {
                    listener.onSegmentViewClick(textView1, 0);
                }
            }
        });
        textView2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (textView2.isSelected()) {
                    return;
                }
                textView2.setSelected(true);
                textView1.setSelected(false);
                textView3.setSelected(false);
                if (listener != null) {
                    listener.onSegmentViewClick(textView2, 1);
                }
            }
        });

        textView3.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (textView3.isSelected()) {
                    return;
                }
                textView3.setSelected(true);
                textView1.setSelected(false);
                textView2.setSelected(false);
                if (listener != null) {
                    listener.onSegmentViewClick(textView3, 2);
                }
            }
        });
    }


    public void setSegmentTextSize (){
        textView1.setTextSize(TypedValue.COMPLEX_UNIT_PX,getContext().getResources().getDimension
                (R.dimen.x33));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_PX,getContext().getResources().getDimension
                (R.dimen.x33));
        textView3.setTextSize(TypedValue.COMPLEX_UNIT_PX,getContext().getResources().getDimension
                (R.dimen.x33));
    }

    private static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int px2dp(Context context, float pxVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxVal / scale);
    }

    public void setOnSegmentViewClickListener(onSegmentViewClickListener listener) {
        this.listener = listener;
    }


    public void setSegmentText(CharSequence text, int position) {
        if (position == 0) {
            textView1.setText(text);
        }
        if (position == 1) {
            textView2.setText(text);
        }
        if (position == 2) {
            textView3.setText(text);
        }
    }

    public static interface onSegmentViewClickListener {

        public void onSegmentViewClick(View v, int position);
    }
}
