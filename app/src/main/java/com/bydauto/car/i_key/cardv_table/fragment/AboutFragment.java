package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;
import com.bydauto.car.i_key.cardv_table.util.ServerConfig;
import com.bydauto.car.i_key.cardv_table.util.Utils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutFragment extends Fragment implements OnClickListener {
    private final static String TAG = "---->AboutFragment";

    private RemoteCam mRemoteCam;
    private IFragmentListener mListener;

    private TextView currentVer;
    private TextView btn_update;

    private JSONObject jsonObject;
    private String versionName;
    private int versionCode;
    private String content;
    private String apk_url;
    private PackageInfo packageInfo;
    private String ApkPath;
    private HttpHandler httphandler;

    private ProgressDialog mDownloadAPKDialog;
    private AlertDialog dialog;

    private boolean expand = false;

    @BindView(R.id.iv_back)
    ImageView ivBack;

    @OnClick(R.id.iv_back)
    public void back() {
        ((MainActivity) getActivity()).onBackPressed();
    }

    @BindView(R.id.rl_format)
    RelativeLayout rl_format;

    @OnClick(R.id.rl_format)
    public void formatSD() {
//        ((MainActivity)getActivity()).showAlertDialogWithButton("提示", "确定要格式化存储卡吗？", "确定", new
// DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                mListener.onFragmentAction(IFragmentListener.ACTION_FS_FORMAT_SD, "C:");
//            }
//        }, "取消", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//            }
//        });
        ((MainActivity) getActivity()).showCustomAlertDialog("提示", "确定要格式化存储卡吗？", "确定", new
                DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_FORMAT_SD, "C:");
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    @BindView(R.id.rl_default_setting)
    RelativeLayout rl_default_setting;

    @OnClick(R.id.rl_default_setting)
    public void resumeDefaultSetting() {
        ((MainActivity) getActivity()).showCustomAlertDialog("提示", "确定恢复出厂设置吗？", "确定", new
                DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onFragmentAction(IFragmentListener.ACTION_DEFAULT_SETTING, null);
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    @OnClick(R.id.rl_resolution)
    void resolution() {
        if (!expand) {
            ll_720P.setVisibility(View.VISIBLE);
            ll_1080P.setVisibility(View.VISIBLE);
        } else {
            ll_720P.setVisibility(View.GONE);
            ll_1080P.setVisibility(View.GONE);
        }

        expand = !expand;
    }

    @BindView(R.id.ll_720P)
    LinearLayout ll_720P;
    @BindView(R.id.ll_10800P)
    LinearLayout ll_1080P;

    @BindView(R.id.iv_arrow_720)
    ImageView iv_720;
    @BindView(R.id.iv_arrow_1080)
    ImageView iv_1080;
    @BindView(R.id.rl_720P)
    RelativeLayout rl_720P;

    @OnClick(R.id.rl_720P)
    public void resolution720() {
        iv_720.setSelected(true);
        iv_1080.setSelected(false);
        String cmd = "\"type\":\"" + "video_resolution" + "\",\"param\":\"" + "1280x720 30P 16:9"
                + "\"";
        mListener.onFragmentAction(IFragmentListener.ACTION_BC_SET_SETTING, cmd);
    }

    @BindView(R.id.rl_1080P)
    RelativeLayout rl_1080P;

    @OnClick(R.id.rl_1080P)
    public void resolution1080() {
        iv_720.setSelected(false);
        iv_1080.setSelected(true);
        String cmd = "\"type\":\"" + "video_resolution" + "\",\"param\":\"" + "1920x1080 30P " +
                "16:9" + "\"";
        mListener.onFragmentAction(IFragmentListener.ACTION_BC_SET_SETTING, cmd);
    }

    private HttpURLConnection conn;

    // 更新
    private static final int UPDATE_YES = 1;
    // 不更新
    private static final int UPDATE_NO = 2;
    // URL错误
    private static final int URL_ERROR = 3;
    // 没有网络
    private static final int IO_ERROR = 4;
    // 数据异常
    private static final int JSON_ERROR = 5;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 处理更新APK
                case UPDATE_YES:
                    Log.e(TAG, "有新版本~");
                    // showUpdateDialog();
                    btn_update.setVisibility(View.VISIBLE);
                    break;
                case UPDATE_NO:
                    // goHome();
                    btn_update.setVisibility(View.INVISIBLE);
                    break;

                /**
                 * 处理更新APK时发生的错误
                 */
                case URL_ERROR:
                    Log.e(TAG, "地址错误");

                    break;
                case IO_ERROR:
                    Log.e(TAG, "请检查网络");

                    break;
                case JSON_ERROR:
                    Log.e(TAG, "Json解析错误");

                    break;

            }
        }
    };

    public void setRemoteCam(RemoteCam cam) {
        mRemoteCam = cam;
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, null, false);
        ButterKnife.bind(this, view);
        initView(view);
        getJSON();
        mListener.onFragmentAction(IFragmentListener.ACTION_BC_GET_SINGLE_SETTING,
                "video_resolution");
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
        conn.disconnect();
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
        RelativeLayout btn_device_info = (RelativeLayout) view.findViewById(R.id.btn_device_info);

        btn_device_info.setOnClickListener(this);

        currentVer = (TextView) view.findViewById(R.id.currentVersion);
        currentVer.setText("V" + getVersionName());

        btn_update = (TextView) view.findViewById(R.id.btn_update);
        btn_update.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_device_info:
                mListener.onFragmentAction(IFragmentListener.ACTION_SHOW_DEVICE_INFO, null);
                break;
            case R.id.btn_update:
                showUpdateDialog();
                break;
            default:
                break;
        }
    }

    private void getJSON() {
        // 子线程访问，耗时操作

        new Thread() {

            @Override
            public void run() {

                Message msg = Message.obtain();


                try {
                    // JSON地址
                    conn = (HttpURLConnection) new URL(
                            // 模拟器一般有一个预留IP：10.0.2.2
                            ServerConfig.APP_INFO_JSON_ADDRESS).openConnection();
                    // 请求方式GRT
                    conn.setRequestMethod("GET");
                    // 连接超时
                    conn.setConnectTimeout(5000);

                    // 响应超时
                    conn.setReadTimeout(3000);
                    //  连接
                    conn.connect();
                    // 获取请求码
                    int responseCode = conn.getResponseCode();
                    // 等于200说明请求成功
                    if (responseCode == 200) {
                        //  拿到他的输入流
                        InputStream in = conn.getInputStream();
                        String stream = Utils.toStream(in);

                        Log.i("JSON", stream);
                        jsonObject = new JSONObject(stream);
                        versionName = jsonObject.getString("versionName");
                        versionCode = jsonObject.getInt("versionCode");
                        content = jsonObject.getString("content");
                        apk_url = jsonObject.getString("url");

                        //  版本判断
                        if (versionCode > getCode()) {
                            // 提示更新
                            msg.what = UPDATE_YES;
                        } else {
                            // 不更新
                            msg.what = UPDATE_NO;
                        }
                    }

                } catch (MalformedURLException e) {
                    // URL错误
                    e.printStackTrace();
                    msg.what = URL_ERROR;
                } catch (IOException e) {
                    // 没有网络
                    e.printStackTrace();
                    msg.what = IO_ERROR;
                } catch (JSONException e) {
                    //数据错误
                    e.printStackTrace();
                    msg.what = JSON_ERROR;
                } finally {

                    // 全部走完发消息
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * 获取versionCode
     */
    private int getCode() {
        //  PackageManager管理器
        PackageManager pm = getActivity().getPackageManager();
        //  获取相关信息
        try {
            packageInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
            //  版本号
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;

    }

    private String getVersionName() {
        PackageManager pm = getActivity().getPackageManager();
        try {
            packageInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showUpdateDialog() {
        dialog = new AlertDialog.Builder(getActivity()).setTitle("������Ϣ").setMessage("新版本" +
                versionName + "上线了！" + "\n\n" + "本次更新内容：\\n" + content +
                "\\n\\n是否需要现在更新").setPositiveButton("更新", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadAPK();
                showDownloadAPKProgress();
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();

    }

    /**
     * 下载apk更新
     */
    private void downloadAPK() {
        // 判断是否有SD卡
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            ApkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                    ServerConfig.APK_NAME;
            HttpUtils httpUtils = new HttpUtils();
            /**
             * 1.网络地址 2.存放地址 3.回调
             */
            httphandler = httpUtils.download(apk_url, ApkPath, true, true, new
                    RequestCallBack<File>() {

                // 下载进度
                @Override
                public void onLoading(long total, long current, boolean isUploading) {
                    // TODO Auto-generated method stub
                    super.onLoading(total, current, isUploading);

                    // 显示进度
                    Log.e(TAG, (100 * current / total + "%"));
                    mDownloadAPKDialog.setProgress((int) (100 * current / total));

                }

                // 成功
                @Override
                public void onSuccess(ResponseInfo<File> responseInfo) {
                    mDownloadAPKDialog.dismiss();
                    //跳转系统安装页面
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    i.setDataAndType(Uri.fromFile(new File(ApkPath)), "application/vnd.android" +
                            ".package-archive");
                    startActivity(i);
                }

                //失败
                @Override
                public void onFailure(HttpException error, String msg) {
                    Log.i("error", msg);
                }

                @Override
                public void onCancelled() {
                    // TODO Auto-generated method stub
                    super.onCancelled();
                    Log.e(TAG, "取消");
                }

            });
        } else {
            Log.e(TAG, "未找到SD卡");
        }

    }

    /**
     * 下载apk进度条
     */
    private void showDownloadAPKProgress() {
        dialog.dismiss();
        mDownloadAPKDialog = new ProgressDialog(getActivity());
        mDownloadAPKDialog.setMessage("新版本" + versionName + "正在下载");
        mDownloadAPKDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mDownloadAPKDialog.dismiss();
                httphandler.cancel();
                deleteCacheAPK();
            }
        });
        mDownloadAPKDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadAPKDialog.setCanceledOnTouchOutside(false);
        mDownloadAPKDialog.setTitle("下载进度");
        mDownloadAPKDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface
                .OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDownloadAPKDialog.dismiss();
                httphandler.cancel();
                deleteCacheAPK();
            }
        });
        mDownloadAPKDialog.show();
    }

    private void deleteCacheAPK() {
        File cache = new File(ApkPath);
        if (cache.isFile() && cache.exists()) {
            cache.delete();
        }
    }

    public void updateSettings(String type, String param) {
        if (type.equalsIgnoreCase("video_resolution")) {
            if ("1920x1080 30P 16:9".equals(param)) {
                iv_720.setSelected(false);
                iv_1080.setSelected(true);
            } else if ("1280x720 30P 16:9".equals(param)) {
                iv_720.setSelected(true);
                iv_1080.setSelected(false);
            }
        }
    }
}
