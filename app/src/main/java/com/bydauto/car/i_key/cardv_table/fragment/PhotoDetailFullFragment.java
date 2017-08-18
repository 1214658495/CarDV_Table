package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import android.widget.LinearLayout;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.Model;
import com.bydauto.car.i_key.cardv_table.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class PhotoDetailFullFragment extends Fragment implements OnClickListener {
	private static final String TAG = "PhotoDetailFullFragment";
	private static final String HOST = "http://192.168.42.1";
	private ImageView img_full;
	private Bitmap bitmap;
	private ImageView btn_close, btn_rotation;
	private final String filePath;
	private final int currentIndex;
	private final Model mCurrentItem;
	private final int totalIndex;
	private final ArrayList<Model> mPlaylist;
	private final String mPWD;
	private LinearLayout menuOperation;
	private boolean showMenu = true;
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			// ImageView imgView = (ImageView)
			// findViewById(R.id.internet_imageview);
			bitmap = (Bitmap) msg.obj;

			img_full.setImageBitmap(bitmap);
		};
	};

	public PhotoDetailFullFragment(ArrayList<Model> playlist, Model currentItem, String pwd) {
		mCurrentItem = currentItem;
		currentIndex = playlist.indexOf(currentItem);
		totalIndex = playlist.size();
		mPlaylist = playlist;
		mPWD = pwd + "/M_photo/";
		filePath = mPWD + currentItem.getName();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_full, null, false);
		initView(view);
		return view;
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		Log.e(TAG, "detach");

	}

	private void initView(View view) {
		btn_close = (ImageView) view.findViewById(R.id.btn_close);
		btn_close.setOnClickListener(this);
		btn_rotation = (ImageView) view.findViewById(R.id.btn_rotation);
		btn_rotation.setOnClickListener(this);

		menuOperation = (LinearLayout) view.findViewById(R.id.operation_menu);

		img_full = (ImageView) view.findViewById(R.id.photo_full);
		img_full.setOnClickListener(this);
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

	/**
	 * ��ݸ�Ŀ�͸߽�������
	 * 
	 * @param origin
	 *            ԭͼ
	 * @param newWidth
	 *            ��ͼ�Ŀ�
	 * @param newHeight
	 *            ��ͼ�ĸ�
	 * @return new Bitmap
	 */
	private Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
		if (origin == null) {
			return null;
		}
		int height = origin.getHeight();
		int width = origin.getWidth();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);// ʹ�ú��
		Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
		if (!origin.isRecycled()) {
			origin.recycle();
		}
		return newBM;
	}

	/**
	 * ����������ͼƬ
	 * 
	 * @param origin
	 *            ԭͼ
	 * @param ratio
	 *            ����
	 * @return �µ�bitmap
	 */
	private Bitmap scaleBitmap(Bitmap origin, float ratio) {
		if (origin == null) {
			return null;
		}
		int width = origin.getWidth();
		int height = origin.getHeight();
		Matrix matrix = new Matrix();
		matrix.preScale(ratio, ratio);
		Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
		if (newBM.equals(origin)) {
			return newBM;
		}
		origin.recycle();
		return newBM;
	}

	/**
	 * �ü�
	 * 
	 * @param bitmap
	 *            ԭͼ
	 * @return �ü����ͼ��
	 */
	private Bitmap cropBitmap(Bitmap bitmap) {
		int w = bitmap.getWidth(); // �õ�ͼƬ�Ŀ?��
		int h = bitmap.getHeight();
		int cropWidth = w >= h ? h : w;// ���к���ȡ����������߳�
		cropWidth /= 2;
		int cropHeight = (int) (cropWidth / 1.2);
		return Bitmap.createBitmap(bitmap, w / 3, 0, cropWidth, cropHeight, null, false);
	}

	/**
	 * ѡ��任
	 * 
	 * @param origin
	 *            ԭͼ
	 * @param alpha
	 *            ��ת�Ƕȣ�����ɸ�
	 * @return ��ת���ͼƬ
	 */
	private Bitmap rotateBitmap(Bitmap origin, float alpha) {
		if (origin == null) {
			return null;
		}
		int width = origin.getWidth();
		int height = origin.getHeight();
		Matrix matrix = new Matrix();
		matrix.setRotate(alpha);
		// Χ��ԭ�ؽ�����ת
		Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
		if (newBM.equals(origin)) {
			return newBM;
		}
		origin.recycle();
		return newBM;
	}

	/**
	 * ƫ��Ч��
	 * 
	 * @param origin
	 *            ԭͼ
	 * @return ƫ�ƺ��bitmap
	 */
	private Bitmap skewBitmap(Bitmap origin) {
		if (origin == null) {
			return null;
		}
		int width = origin.getWidth();
		int height = origin.getHeight();
		Matrix matrix = new Matrix();
		matrix.postSkew(-0.6f, -0.3f);
		Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
		if (newBM.equals(origin)) {
			return newBM;
		}
		origin.recycle();
		return newBM;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_close:
			((MainActivity) getActivity()).onBackPressed();
			break;
		case R.id.photo_full:
			// ((MainActivity)
			// getActivity()).onBackPressed();
			if (showMenu) {
				menuOperation.setVisibility(View.INVISIBLE);
				showMenu = !showMenu;
			} else {
				menuOperation.setVisibility(View.VISIBLE);
				showMenu = !showMenu;
			}
			break;
		case R.id.btn_rotation:
			bitmap = rotateBitmap(bitmap, -90);
			Message msg = new Message();
			// ��bm������Ϣ��,���͵����߳�
			msg.obj = bitmap;
			handler.sendMessage(msg);
			break;
		default:
			break;
		}
	}

	private boolean isTheFirstPhoto() {
		return (currentIndex == 0);
	}

	private boolean isTheLastPhoto() {
		return (currentIndex == totalIndex - 1);
	}

}
