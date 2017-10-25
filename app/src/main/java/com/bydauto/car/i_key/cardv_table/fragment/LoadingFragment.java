package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;

/**
 * author：hm2359767 on 17/4/24 11:46
 * mail：huang.min12@byd.com
 * tele: 18666287409
 */
public class LoadingFragment extends Fragment{
    private static final String TAG = "LoadingFragment";
    private RemoteCam mRemoteCam;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        Log.e(TAG, "onCreateView: 111");
        View view = inflater.inflate(R.layout.fragment_loading,null,false);
        return view;
    }
    public void setRemoteCam(RemoteCam cam) {
        mRemoteCam = cam;
    }
    private void connect(){
        mRemoteCam.startSession();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: 111");
       connect();
    }
}
