package com.bydauto.car.i_key.cardv_table.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bydauto.car.i_key.cardv_table.R;

/**
 * author：hm2359767 on 17/5/3 09:32
 * mail：huang.min12@byd.com
 * tele: 18666287409
 */
public class HMToast {
    private Toast mToast;

    private HMToast(Context context, CharSequence text, int duration) {
        View v = LayoutInflater.from(context).inflate(R.layout.eplay_toast, null);
        TextView textView = (TextView) v.findViewById(R.id.textView1);
        textView.setText(text);
        mToast = new Toast(context);
        mToast.setDuration(duration);
        mToast.setView(v);
    }

    public static HMToast makeText(Context context, CharSequence text, int duration) {
        return new HMToast(context, text, duration);
    }

    public void show() {
        if (mToast != null) {
            mToast.show();
        }
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        if (mToast != null) {
            mToast.setGravity(gravity, xOffset, yOffset);
        }
    }

    public void cancel(){
        if (mToast!=null){
            mToast.cancel();
        }
    }
}
