package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;


public class DeviceInfoFragment extends Fragment implements OnClickListener {
	private final static String TAG = "---->DeviceInfoFragment";

	private RemoteCam mRemoteCam;
	private IFragmentListener mListener;

	private TextView tv_uuid, hw_ver, sw_ver;

	public void setRemoteCam(RemoteCam cam) {
		mRemoteCam = cam;
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_device_info, null, false);
		initView(view);
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
	}

	@Override
	public void onDetach() {
		Log.e(TAG, "onDetach");
		super.onDetach();
		mListener = null;
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

	private void initView(View view) {
		ImageView btn_back = (ImageView) view.findViewById(R.id.btn_back);
		btn_back.setOnClickListener(this);
		tv_uuid = (TextView) view.findViewById(R.id.tv_uuid);
		hw_ver = (TextView) view.findViewById(R.id.hw_ver);
		sw_ver = (TextView) view.findViewById(R.id.sw_ver);

		setInfoText();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_back:
			((MainActivity) getActivity()).removeDeviceInfo();
			break;
		default:
			break;
		}
	}

	private void setInfoText() {
		if (mRemoteCam.getuuid() != null) {
			tv_uuid.setText(mRemoteCam.getuuid());
		}
		if (mRemoteCam.gethw_ver() != null) {
			hw_ver.setText(mRemoteCam.gethw_ver());
		}
		if (mRemoteCam.getsw_ver() != null) {
			sw_ver.setText(mRemoteCam.getsw_ver());
		}
	}
}
