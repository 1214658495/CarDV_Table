package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.Model;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;
import com.bydauto.car.i_key.cardv_table.util.ServerConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PhotoFragment extends Fragment implements OnClickListener, OnItemClickListener,
        OnRefreshListener {
    private final static String TAG = "---->PhotoFragment";
//    private final static String ServerConfig.HOST = "192.168.42.1";

    private String mPWD;
    private String filePath;
    private IFragmentListener mListener;
    private RemoteCam mRemoteCam;

    private GridView mPhotoWall;
    private PicturePhotoWallAdapter mAdapter;
    public SwipeRefreshLayout refreshView;

    private ArrayList<Model> mPlaylist;

    private TextView tvMultiChoose;          // 打开多选
    private TextView tvMultiChooseCancel;    // 关闭多选
    private LinearLayout chooseOperationMenu; //操作菜单
    private boolean isMultiChoose = false;
    private List<Model> arrayDelete = new ArrayList<Model>(); //记录待删除的Model

    public PhotoFragment() {
        reset();
    }

    public void reset() {
        mPWD = null;
        mAdapter = null;
    }

    public void setRemoteCam(RemoteCam cam) {
        mRemoteCam = cam;
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, null, false);
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
        if (mAdapter != null) {
            mAdapter.cancelAllTasks();
            mAdapter.clear();
        }
        reset();
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
        refreshView = (SwipeRefreshLayout) view.findViewById(R.id.refreshView);
        refreshView.setOnRefreshListener(this);
        refreshView.setColorSchemeColors(Color.RED);

        tvMultiChoose = (TextView) view.findViewById(R.id.tv_multichoose);
        tvMultiChoose.setOnClickListener(this);

        tvMultiChooseCancel = (TextView) view.findViewById(R.id.tv_multichoose_cancel);
        tvMultiChooseCancel.setOnClickListener(this);

        chooseOperationMenu = (LinearLayout) view.findViewById(R.id.select_operation_menu);

        Button multiChooseBtn = (Button) view.findViewById(R.id.btn_multi_delete);
        multiChooseBtn.setOnClickListener(this);

        Button chooseAllBtn = (Button) view.findViewById(R.id.btn_choose_all);
        chooseAllBtn.setOnClickListener(this);

        Button chooseInverseBtn = (Button) view.findViewById(R.id.btn_inverse_choose);
        chooseInverseBtn.setOnClickListener(this);

        mPhotoWall = (GridView) view.findViewById(R.id.photo_gridview);
        mPhotoWall.setSelector(new ColorDrawable(Color.TRANSPARENT));// gridview���ʱ�ޱ���
        mPhotoWall.setOnItemClickListener(this);
//		RelativeLayout tv = (RelativeLayout) view.findViewById(R.id.empty_list_view);
//		mPhotoWall.setEmptyView(tv);

        if (mAdapter == null) {// ����ʼ��������
//            mPWD = mRemoteCam.photoFolder() + "/M_photo/"; // ����ͼ��·��
            mPWD = mRemoteCam.photoFolder() + "/"; // 删除二级文件夹
            listPicDirContents(mPWD);
        } else {
            showPicDirContents();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.tv_multichoose:
                enterMultiChoose();
                break;
            case R.id.tv_multichoose_cancel:
                cancelMultiChoose();
                break;
            case R.id.btn_multi_delete:
                if (arrayDelete.size() > 0) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle("提示")
                            .setMessage("确定删除所选文件？").setPositiveButton("确定", new DialogInterface
                                    .OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE_MULTI,
                                            arrayDelete.size());//有多少个待删除文件
                                    for (Model m : arrayDelete) {
//                                            Log.e(TAG, "选中-->" + mRemoteCam.photoFolder() +
//                                                    "/M_photo/" +
//                                                    m.getName());
//                                            mListener.onFragmentAction(IFragmentListener
//                                                    .ACTION_FS_DELETE, mRemoteCam.photoFolder() +
//                                                    "/M_photo/" +
//                                                    m.getName());
                                        mListener.onFragmentAction(IFragmentListener
                                                    .ACTION_FS_DELETE, mRemoteCam.photoFolder() + "/" + m.getName());// 删除二级文件夹
                                    }
                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();
                } else {
                    ((MainActivity) getActivity()).showToastText("请先选择一个文件", Toast.LENGTH_SHORT);
                }

                break;
            case R.id.btn_choose_all:
                mAdapter.isSelectedMap.clear();
                arrayDelete.clear();
                for (int i = 0; i < mPlaylist.size(); i++) {
                    mAdapter.setItemisSelectedMap(i, true);
                    arrayDelete.add(mPlaylist.get(i));
                }
                break;
            case R.id.btn_inverse_choose:
                arrayDelete.clear();
                for (int i = 0; i < mPlaylist.size(); i++) {
                    boolean isSelect = mAdapter.getisSelectedAt(i);
                    mAdapter.setItemisSelectedMap(i, !isSelect);
                    if (mAdapter.getisSelectedAt(i)) {
                        arrayDelete.add(mPlaylist.get(i));
                    }
                }
                break;
            default:
                break;
        }
    }

    private void listPicDirContents(String path) {
        if (path != null) {
            mListener.onFragmentAction(IFragmentListener.ACTION_FS_LS, path); // ���ͻ�ȡ��Ŀ����
        }
    }

    private void showPicDirContents() {
        mPhotoWall.setAdapter(mAdapter);
    }

    public void showPhoto() {
        Log.e(TAG, "showPhoto: 1111");
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.cancelAllTasks();
        }

        // TODO: 2017/8/29 如下到底是不是我屏蔽的？ 应该不是
//        mPWD = mRemoteCam.photoFolder() + "/M_photo/";
        listPicDirContents(mPWD);
        if (isMultiChoose) {
            cancelMultiChoose();
        }
    }

    private void enterMultiChoose() {
        isMultiChoose = true;
        tvMultiChoose.setVisibility(View.GONE);
        tvMultiChooseCancel.setVisibility(View.VISIBLE);
        chooseOperationMenu.setVisibility(View.VISIBLE);
        arrayDelete.clear();
    }

    private void cancelMultiChoose() {
        isMultiChoose = false;
        tvMultiChooseCancel.setVisibility(View.GONE);
        tvMultiChoose.setVisibility(View.VISIBLE);
        chooseOperationMenu.setVisibility(View.GONE);
        arrayDelete.clear();
        if (mAdapter != null && !mAdapter.isSelectedMap.isEmpty()) {
            mAdapter.isSelectedMap.clear();
        }
    }

    public void updatePicDirContents(JSONObject parser) {
        refreshView.setRefreshing(false);

        ArrayList<Model> models = new ArrayList<Model>();

        try {
            JSONArray contents = parser.getJSONArray("listing");

            for (int i = 0; i < contents.length(); i++) {
                Model item = new Model(contents.getJSONObject(i).toString());//
                if (

                        (!item.isDirectory() && item.getName().endsWith(".JPG")

                        )) {
                    models.add(item);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        Collections.sort(models, new order());
        mPlaylist = models;
        if (mListener != null) {
            mListener.onFragmentAction(IFragmentListener.ACTION_UPDATE_PLAYLIST, mPlaylist);
        }
        mAdapter = new PicturePhotoWallAdapter(getActivity(), 0, mPlaylist, mPhotoWall);
        showPicDirContents();
    }

    private class order implements Comparator<Model> {

        @Override
        public int compare(Model lhs, Model rhs) {
            return rhs.getName().compareTo(lhs.getName());
        }

    }

    private class PicturePhotoWallAdapter extends ArrayAdapter<Model> {
        final private ArrayList<Model> mArrayList;

        private Set<BitmapWorkerTask> taskCollection;

        private LruCache<String, Bitmap> mMemoryCache;

        private GridView mPhotoWall;

        public HashMap<Integer, Boolean> isSelectedMap;//记录选择的项目和是否选中状态

        public PicturePhotoWallAdapter(Context context, int textViewResourceId, ArrayList<Model>
                arrayList, GridView photoWall) {
            super(context, textViewResourceId, arrayList);
            mArrayList = arrayList;
            mPhotoWall = photoWall;
            isSelectedMap = new HashMap<Integer, Boolean>();

            taskCollection = new HashSet<BitmapWorkerTask>();
            int maxMemory = (int) Runtime.getRuntime().maxMemory();
            int cacheSize = maxMemory / 8;
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };
            // mPhotoWall.setOnScrollListener(this);
            loadBitmaps(0, mArrayList.size());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Model model = mArrayList.get(position);
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.layout_photo, null);
            } else {
                view = convertView;
            }

            CheckBox cbMultiChoose = (CheckBox) view.findViewById(R.id.cb_multi);
            if (isMultiChoose) {
                cbMultiChoose.setVisibility(View.VISIBLE);
                cbMultiChoose.setChecked(getisSelectedAt(position)); //从hash表中获取位置选中状态，不会导致错位
            } else {
                cbMultiChoose.setVisibility(View.GONE);
            }

//            String url = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) + "/Thumb/" +
//                    model.getThumbFileName();
            String url = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) + "/" +
                    model.getThumbFileName();   //删除二级文件夹
            Log.e(TAG, "getView: 11111111  url" + url);
            final ImageView photo = (ImageView) view.findViewById(R.id.pic_photo);
            photo.setTag(url);
            setImageView(url, photo);
            return view;
        }

        private void setImageView(String imageUrl, ImageView imageView) {
            Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.mipmap.empty_photo);
            }
        }

        public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
            if (getBitmapFromMemoryCache(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }

        public Bitmap getBitmapFromMemoryCache(String key) {
            return mMemoryCache.get(key);
        }


        private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {
            Log.e(TAG, "loadBitmaps: 1111");
            try {
                for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
                    Model model = mArrayList.get(i);
//                    String imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) +
//                            "/Thumb/" + model.getThumbFileName();
                    String imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) +
                            "/" + model.getThumbFileName(); //删除二级文件夹
                    Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
                    if (bitmap == null) {
                        BitmapWorkerTask task = new BitmapWorkerTask();
                        taskCollection.add(task);
                        task.execute(imageUrl);
                    } else {
                        ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(imageUrl);
                        if (imageView != null && bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancelAllTasks() {
            if (taskCollection != null) {
                for (BitmapWorkerTask task : taskCollection) {
                    task.cancel(false);
                }
            }
        }

        public boolean getisSelectedAt(int position) {

            //如果当前位置的key值为空，则表示该item未被选择过，返回false，否则返回true
            if (isSelectedMap.get(position) != null) {
                return isSelectedMap.get(position);
            }
            return false;
        }

        public void setItemisSelectedMap(int position, boolean isSelectedMap) {
            this.isSelectedMap.put(position, isSelectedMap);
            notifyDataSetChanged();
        }

        class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

            private String imageUrl;

            @Override
            protected Bitmap doInBackground(String... params) {
                imageUrl = params[0];
                Bitmap bitmap = downloadBitmap(params[0]);
                if (bitmap != null) {
                    addBitmapToMemoryCache(params[0], bitmap);
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(imageUrl);
                if (imageView != null && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
                taskCollection.remove(this);
            }

            private Bitmap downloadBitmap(String imageUrl) {
                Bitmap bitmap = null;
                HttpURLConnection con = null;
                try {
                    URL url = new URL(imageUrl);
                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5 * 1000);
                    con.setReadTimeout(10 * 1000);
                    con.setDoInput(true);
                    con.setDoOutput(true);
                    bitmap = BitmapFactory.decodeStream(con.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
                return bitmap;
            }

        }

    }

    // TODO: 2017/8/22 
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e(TAG, "onItemClick: 1111点击照片");
        if (!isMultiChoose) {
            if (null != mListener) {
                Model item = (Model) parent.getItemAtPosition(position);
                Log.e(TAG, "onItemClick: 1111"+",item+"+item);
                filePath = mPWD + item.getName();
                mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_DETAIL, item);
            }
        } else {
            boolean isSelect = mAdapter.getisSelectedAt(position);

            if (!isSelect) {
//                // 当前未被选中，记录下来，用于删除
                arrayDelete.add(mPlaylist.get(position));
            } else {
                arrayDelete.remove(mPlaylist.get(position));
            }

            // 选中状态的切换
            mAdapter.setItemisSelectedMap(position, !isSelect);
        }
    }

    @Override
    public void onRefresh() {
        cancelMultiChoose();
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.cancelAllTasks();
        }
        listPicDirContents(mPWD);
    }
}
