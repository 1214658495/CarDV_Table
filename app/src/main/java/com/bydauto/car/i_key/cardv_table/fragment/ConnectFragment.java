package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.R;

/**
 * author：hm2359767 on 17/4/24 11:46
 * mail：huang.min12@byd.com
 * tele: 18666287409
 */
public class ConnectFragment extends Fragment {
    private RelativeLayout btn_connect;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect, null, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        btn_connect = (RelativeLayout) view.findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).showLoadingFrag();
            }
        });
    }
}
