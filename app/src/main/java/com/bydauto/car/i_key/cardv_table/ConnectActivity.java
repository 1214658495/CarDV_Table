package com.bydauto.car.i_key.cardv_table;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.util.WifiUtils;

import java.util.List;


public class ConnectActivity extends Activity {
	private final static String TAG = "ConnectActivity";
	private final static int searchDone = 0;
	private final static int searchFailed = 1;

	private WifiUtils wifiUtils;
	private List<String> wifiList;
	private String wifiName;
	private TextView tvWifiName;

	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case searchDone:
				tvWifiName.setText(wifiName);
				break;
			case searchFailed:
				tvWifiName.setText("鏈悳绱㈠埌璁惧锛岃寮�鍚杞﹁褰曚华");
				break;
			default:
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_connect);

		wifiUtils = new WifiUtils(this);

		wifiList = wifiUtils.getScanWifiResult();
		for (int i = 0; i < wifiList.size(); i++) {
			Log.e("ddd", wifiList.get(i));
			if (wifiList.get(i).indexOf("行车记录仪") != -1) {
				wifiName = wifiList.get(i);
				handler.sendEmptyMessage(searchDone);
				break;
			}
			if (wifiName == null) {
				handler.sendEmptyMessage(searchFailed);
			}
		}

		initView();

	}

	private void initView() {
		tvWifiName = (TextView) findViewById(R.id.tv_wifi_name);
	}
}
