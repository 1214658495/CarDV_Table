package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.Model;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class PhotoDetailFragment extends Fragment implements OnClickListener {
	private static final String TAG = "PhotoDetailFragment";
	private ImageView img_thumb;
	private static final String HOST = "http://192.168.42.1";

	private  String filePath;
	private IFragmentListener mListener;
	private  int currentIndex;
	private Model mCurrentItem;
	private  int totalIndex;
	private  ArrayList<Model> mPlaylist;
	private  String mPWD;
	private  String mListPath;

	private TextView btn_delete, btn_export, btn_full;
	private ImageView btn_back;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			// ImageView imgView = (ImageView)
			// findViewById(R.id.internet_imageview);

			img_thumb.setImageBitmap((Bitmap) msg.obj);
		};
	};

	public PhotoDetailFragment(){

	}

	public PhotoDetailFragment(ArrayList<Model> playlist, Model currentItem, String pwd) {
		// filePath = fileName;
		mCurrentItem = currentItem;
		currentIndex = playlist.indexOf(currentItem);
		totalIndex = playlist.size();
		mPlaylist = playlist;
		mPWD = pwd + "/M_photo/";
		mListPath = pwd;
		filePath = mPWD + currentItem.getName();
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.fragment_photo_detail, null, false);
		initView(view);
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
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.e(TAG, "onDestroy");
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		Log.e(TAG, "onDetach");
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.e(TAG, "onPause");
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.e(TAG, "onResume");
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.e(TAG, "onStop");
	}

	private void initView(View view) {
		TextView fileName = (TextView) view.findViewById(R.id.photo_name);
		fileName.setText(mCurrentItem.getName());

		btn_delete = (TextView) view.findViewById(R.id.btn_delete);
		btn_delete.setOnClickListener(this);

		btn_export = (TextView) view.findViewById(R.id.btn_export);
		btn_export.setOnClickListener(this);

		btn_full = (TextView) view.findViewById(R.id.btn_full);
		btn_full.setOnClickListener(this);

		btn_back = (ImageView) view.findViewById(R.id.btn_back);
		btn_back.setOnClickListener(this);

		img_thumb = (ImageView) view.findViewById(R.id.img_thumb);
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				String url = filePath.substring(4);
				String urlpath = HOST + url;
				Bitmap bm = getInternetPicture(urlpath);
				Message msg = new Message();
				// ��bm������Ϣ��,���͵����߳�
				msg.obj = bm;
				handler.sendMessage(msg);
			}
		}).start();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {

		case R.id.btn_delete:
			mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE, filePath);
			mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE_STHWRONG, null);
			break;
		case R.id.btn_export:
			mListener.onFragmentAction(IFragmentListener.ACTION_FS_DOWNLOAD, filePath);
			break;
		case R.id.btn_back:
			((MainActivity) getActivity()).removePhotoDetailFrag();
			break;
		case R.id.btn_full:
			mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_FULL, mCurrentItem);
			break;
		default:
			break;
		}
	}

	public Bitmap getInternetPicture(String UrlPath) {
		Bitmap bm = null;
		// 1��ȷ����ַ
		// http://pic39.nipic.com/20140226/18071023_164300608000_2.jpg
		String urlpath = UrlPath;
		// 2����ȡUri
		try {
			URL uri = new URL(urlpath);

			// 3����ȡ���Ӷ��󡢴�ʱ��û�н�������
			HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
			// 4����ʼ�����Ӷ���
			// ��������ķ�����ע���д
			connection.setRequestMethod("GET");
			// ��ȡ��ʱ
			connection.setReadTimeout(5000);
			// �������ӳ�ʱ
			connection.setConnectTimeout(5000);
			// 5����������
			connection.connect();

			// 6����ȡ�ɹ��ж�,��ȡ��Ӧ��
			if (connection.getResponseCode() == 200) {
				// 7���õ����������ص������ͻ����������ݣ��ͱ�����������
				InputStream is = connection.getInputStream();
				// 8�������ж�ȡ��ݣ�����һ��ͼƬ����GoogleAPI
				bm = BitmapFactory.decodeStream(is);
				// 9����ͼƬ���õ�UI���߳�
				// ImageView��,��ȡ������Դ�Ǻ�ʱ������������߳��н���,ͨ����Ϣ������Ϣ�����߳�ˢ�¿ؼ���

				Log.i("", "��������ɹ�");

			} else {
				Log.v("tag", "��������ʧ��");
				bm = null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bm;

	}

}
