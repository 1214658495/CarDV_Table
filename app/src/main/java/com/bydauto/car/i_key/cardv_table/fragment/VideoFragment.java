package com.bydauto.car.i_key.cardv_table.fragment;

import android.app.Activity;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bydauto.car.i_key.cardv_table.MainActivity;
import com.bydauto.car.i_key.cardv_table.Model;
import com.bydauto.car.i_key.cardv_table.R;
import com.bydauto.car.i_key.cardv_table.RemoteCam;
import com.bydauto.car.i_key.cardv_table.connect.IFragmentListener;
import com.bydauto.car.i_key.cardv_table.custom.SegmentView;
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


public class VideoFragment extends Fragment implements OnItemClickListener, OnRefreshListener,
        SegmentView.onSegmentViewClickListener, View.OnClickListener {
    private final static String TAG = "---->VideoFragment";

    public int currentSegment = 0;

    private String mPWD;
    private String filePath;
    private RemoteCam mRemoteCam;
    private IFragmentListener mListener;

    private SegmentView segmentView;

    private TextView tvFileNum;
    private GridView mVideoWall;
    public PhotoWallAdapter mAdapter;
    public SwipeRefreshLayout refreshView;

    private ArrayList<Model> mPlaylist;

    private TextView tvMultiChoose;           // 打开多选
    private TextView tvMultiChooseCancel;     // 关闭多选
    private RelativeLayout chooseOperationMenu; //操作菜单
    public boolean isMultiChoose = false;
    private List<Model> FileList = new ArrayList<Model>(); //记录已选择的Model

    public VideoFragment() {
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
        Log.e(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_video, null, false);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
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
        currentSegment = 0;
        cancelMultiChoose();
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

        segmentView = (SegmentView) view.findViewById(R.id.video_segment);
        segmentView.setOnSegmentViewClickListener(this);

        tvMultiChoose = (TextView) view.findViewById(R.id.tv_multichoose);
        tvMultiChoose.setOnClickListener(this);

        chooseOperationMenu = (RelativeLayout) view.findViewById(R.id.video_operation_menu);

        ImageView multiChooseBtn = (ImageView) view.findViewById(R.id.btn_multi_delete);
        multiChooseBtn.setOnClickListener(this);

        ImageView downLoad = (ImageView) view.findViewById(R.id.iv_download);
        downLoad.setOnClickListener(this);

        ImageView back = (ImageView) view.findViewById(R.id.video_back);
        back.setOnClickListener(this);

        TextView chooseAllBtn = (TextView) view.findViewById(R.id.btn_choose_all);
        chooseAllBtn.setOnClickListener(this);

        tvFileNum = (TextView) view.findViewById(R.id.tv_num_file);

        Button chooseInverseBtn = (Button) view.findViewById(R.id.btn_inverse_choose);
        chooseInverseBtn.setOnClickListener(this);

        tvMultiChooseCancel = (TextView) view.findViewById(R.id.tv_multichoose_cancel);
        tvMultiChooseCancel.setOnClickListener(this);

        mVideoWall = (GridView) view.findViewById(R.id.nomal_video_gridview);
        mVideoWall.setOnItemClickListener(this);
        mVideoWall.setSelector(new ColorDrawable(Color.TRANSPARENT));
//        mVideoWall.setNumColumns(2);
//        mVideoWall.setVerticalSpacing(8);
//        RelativeLayout tv = (RelativeLayout) view.findViewById(R.id.empty_list_view);
//        mVideoWall.setEmptyView(tv);

        if (mAdapter == null && mRemoteCam.videoFolder() != null) {

//            mPWD = mRemoteCam.videoFolder() + "/M_video/";
            mPWD = mRemoteCam.videoFolder();  //删除二级文件夹
            listDirContents(mPWD);
        } else {
            showDirContents();
        }
    }

    private void listDirContents(String path) {
        if (path != null) mListener.onFragmentAction(IFragmentListener.ACTION_FS_LS, path);
    }

    private void showDirContents() {
        mVideoWall.setAdapter(mAdapter);
    }

    public void refreshDirContents() {
        listDirContents(mPWD);
    }

    @Override
    public void onSegmentViewClick(View v, int position) {
        switch (position) {
            case 0:
                currentSegment = 0;
                Log.e(TAG, "onSegmentViewClick: 00000");
                showNormalVideo();
                break;
            case 1:
                currentSegment = 1;
                Log.e(TAG, "onSegmentViewClick: 11111");
                showCollisionVideo();
                break;
            case 2:
                currentSegment = 2;
                Log.e(TAG, "onSegmentViewClick: 22222");
                showPhoto();
            default:
                break;
        }
    }

    public void showNormalVideo() {
        Log.e(TAG, "showNormalVideo: 1111");
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.cancelAllTasks();
        }
//        mVideoWall.setNumColumns(2);
//        mVideoWall.setVerticalSpacing(8);
//        mPWD = mRemoteCam.videoFolder() + "/M_video/";
        mPWD = mRemoteCam.videoFolder() + "/"; //删除二级文件夹
        listDirContents(mPWD);
        if (isMultiChoose) {
            cancelMultiChoose();
        }
    }

    public void showCollisionVideo() {
        Log.e(TAG, "showCollisionVideo: 1111");
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.cancelAllTasks();
        }
//        mVideoWall.setNumColumns(1);
//        mVideoWall.setVerticalSpacing(0);
//        mPWD = mRemoteCam.eventFolder() + "/M_video/";
        mPWD = mRemoteCam.eventFolder() + "/"; //删除二级文件夹
        listDirContents(mPWD);
        if (isMultiChoose) {
            cancelMultiChoose();
        }
    }

    public void showPhoto() {
        Log.e(TAG, "showPhoto: 11111");
        {
            if (mAdapter != null) {
                mAdapter.clear();
                mAdapter.cancelAllTasks();
            }
//        mVideoWall.setNumColumns(1);
//        mVideoWall.setVerticalSpacing(0);
//            mPWD = mRemoteCam.photoFolder() + "/M_photo/";
            mPWD = mRemoteCam.photoFolder() + "/";  //删除二级文件夹
            listDirContents(mPWD);
            if (isMultiChoose) {
                cancelMultiChoose();
            }
        }
    }

    private void enterMultiChoose() {
        isMultiChoose = true;
        tvMultiChoose.setVisibility(View.GONE);
        tvMultiChooseCancel.setVisibility(View.VISIBLE);
        chooseOperationMenu.setVisibility(View.VISIBLE);
        segmentView.setVisibility(View.GONE);
        ((MainActivity) getActivity()).setControlMenu(false);
        FileList.clear();
    }

    public void cancelMultiChoose() {
        isMultiChoose = false;
        tvMultiChooseCancel.setVisibility(View.GONE);
        tvMultiChoose.setVisibility(View.VISIBLE);
        chooseOperationMenu.setVisibility(View.GONE);
        segmentView.setVisibility(View.VISIBLE);
        ((MainActivity) getActivity()).setControlMenu(true);
        FileList.clear();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            if (!mAdapter.isSelectedMap.isEmpty()) {
                mAdapter.isSelectedMap.clear();
            }
        }

    }

    public void updateDirContents(JSONObject parser) {
        refreshView.setRefreshing(false);

        ArrayList<Model> models = new ArrayList<Model>();

        try {
            JSONArray contents = parser.getJSONArray("listing");//


            for (int i = 0; i < contents.length(); i++) {
                Model item = new Model(contents.getJSONObject(i).toString());//

                if ((!item.isDirectory() && item.getName().endsWith(".MP4")) || (!item
                        .isDirectory() && item.getName().endsWith(".mp4")) || (!item.isDirectory
                        () && item.getName().endsWith(".JPG"))) {
                    models.add(item);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        Collections.sort(models, new order());
        if (currentSegment == 0 && models.size() > 0) {
            models.remove(0);//
        }
        mPlaylist = models;
        if (mListener != null)
            mListener.onFragmentAction(IFragmentListener.ACTION_UPDATE_PLAYLIST, mPlaylist);
        mAdapter = new PhotoWallAdapter(getActivity(), 0, mPlaylist, mVideoWall);
        showDirContents();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.tv_multichoose:
                enterMultiChoose();
                break;
            case R.id.tv_multichoose_cancel:
                cancelMultiChoose();
                break;
            case R.id.video_back:
                cancelMultiChoose();
                break;
            case R.id.btn_multi_delete:
                if (FileList.size() > 0) {
                    ((MainActivity) getActivity()).showCustomAlertDialog("提示", "确定要删除吗？", "确定",
                            new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE_MULTI,
                                    FileList);//删除列表
                            for (Model m : FileList) {
                                if (currentSegment == 0) {
//                                    mListener.onFragmentAction(IFragmentListener
//                                            .ACTION_FS_DELETE, mRemoteCam.videoFolder() +
//                                            "/M_video/" +
//                                            m.getName());
                                    mListener.onFragmentAction(IFragmentListener
                                            .ACTION_FS_DELETE, mRemoteCam.videoFolder() + "/" +
                                            m.getName());
                                } else if (currentSegment == 1) {
//                                    mListener.onFragmentAction(IFragmentListener
//                                            .ACTION_FS_DELETE, mRemoteCam.eventFolder() +
//                                            "/M_video/" +
//                                            m.getName());
                                    mListener.onFragmentAction(IFragmentListener
                                            .ACTION_FS_DELETE, mRemoteCam.eventFolder() + "/" + m.getName());
                                } else if (currentSegment == 2) {
//                                    mListener.onFragmentAction(IFragmentListener
//                                            .ACTION_FS_DELETE, mRemoteCam.photoFolder() +
//                                            "/M_photo/" +
//                                            m.getName());
                                    mListener.onFragmentAction(IFragmentListener
                                            .ACTION_FS_DELETE, mRemoteCam.photoFolder() + "/" +
                                            m.getName());  //删除二级文件夹
                                }
                            }
//                        dialog.dismiss();
                        }
                    }, "取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                } else {
                    ((MainActivity) getActivity()).showCustomToastText("请先选择一个文件", Toast
                            .LENGTH_SHORT);
                }

                break;
            case R.id.iv_download:
                if (FileList.size() > 0) {

                    if (mListener != null) {
                        mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE_MULTI,
                                FileList);//更新选择列表
                        mListener.onFragmentAction(IFragmentListener.ACTION_FS_DOWNLOAD, null);
                        //param传null代表多文件下载任务
                    }
                } else {
                    ((MainActivity) getActivity()).showCustomToastText("请先选择一个文件", Toast
                            .LENGTH_SHORT);
                }
                break;
            case R.id.btn_choose_all:
                mAdapter.isSelectedMap.clear();
                FileList.clear();
                for (int i = 0; i < mPlaylist.size(); i++) {
                    mAdapter.setItemisSelectedMap(i, true);
                    FileList.add(mPlaylist.get(i));
                }
                break;
            case R.id.btn_inverse_choose:
                FileList.clear();
                for (int i = 0; i < mPlaylist.size(); i++) {
                    boolean isSelect = mAdapter.getisSelectedAt(i);
                    mAdapter.setItemisSelectedMap(i, !isSelect);
                    if (mAdapter.getisSelectedAt(i)) {
                        FileList.add(mPlaylist.get(i));
                    }
                }
                break;
            default:
                break;
        }
        //用于显示或者隐藏Checkbox
        mAdapter.notifyDataSetChanged();
    }

    /**
     * @author byd
     */
    private class order implements Comparator<Model> {

        @Override
        public int compare(Model lhs, Model rhs) {
            return rhs.getName().compareTo(lhs.getName());
        }

    }

    private class PhotoWallAdapter extends ArrayAdapter<Model>
            // implements OnScrollListener
    {
        final private ArrayList<Model> mArrayList;

        private Set<BitmapWorkerTask> taskCollection;

        private LruCache<String, Bitmap> mMemoryCache;

        private GridView mPhotoWall;

        public HashMap<Integer, Boolean> isSelectedMap;//记录选择的项目和是否选中状态


        //isMultiChoose 表示是否需要重新加载缩略图
        public PhotoWallAdapter(Context context, int textViewResourceId, ArrayList<Model>
                arrayList, GridView photoWall) {

            super(context, textViewResourceId, arrayList);

            mArrayList = arrayList;
            mPhotoWall = photoWall;
            isSelectedMap = new HashMap<Integer, Boolean>();

            taskCollection = new HashSet<BitmapWorkerTask>();
            int maxMemory = (int) Runtime.getRuntime().maxMemory();
            int cacheSize = maxMemory / 2;
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };
            loadBitmaps(0, mArrayList.size());

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Model model = mArrayList.get(position);
            View view;
            if (convertView == null) {
                if (currentSegment == 0) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout
                            .layout_normal_video, null);
                } else if (currentSegment == 1) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout
                            .layout_collision_video, null);
                } else {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.layout_photo, null);
                }
            } else {
                view = convertView;
            }


            CheckBox cbMultiChoose = (CheckBox) view.findViewById(R.id.cb_multi);

            if (isMultiChoose) {
                cbMultiChoose.setVisibility(View.VISIBLE);
                cbMultiChoose.setChecked(getisSelectedAt(position)); //从hash表中获取位置选中状态，不会导致错位
                if (currentSegment != 2) {
                    tvFileNum.setText("已选择" + FileList.size() + "个视频");
                } else {
                    tvFileNum.setText("已选择" + FileList.size() + "张照片");
                }
            } else {
                cbMultiChoose.setVisibility(View.GONE);
            }

            TextView nameView = (TextView) view.findViewById(R.id.tv_filename);

//            int i = model.getName().indexOf('_');
//            int i2 = model.getName().lastIndexOf('_');
//            int i3 = model.getName().indexOf('.');
//            String date = model.getName().substring(i + 1, i2);
//            StringBuilder sb = new StringBuilder(date);
//            sb.insert(6, '-');
//            sb.insert(4, '-');
//            String time = model.getName().substring(i2 + 1, i3 - 1);
//            StringBuilder sb2 = new StringBuilder(time);
//            sb2.insert(4, ':').insert(2, ':');
//            nameView.setText(sb.toString() + "  " + sb2.toString());
//           // nameView.setText(model.getName());

            String mData = model.getName().substring(0, 10);
            String mTime = model.getName().substring(11, 19);
            nameView.setText(mData + " " + mTime);

            String url;

            if (currentSegment == 0) {
//                url = "http://" + ServerConfig.HOST + mRemoteCam.videoFolder().substring(4) + "/Thumb/" +
//                        model.getThumbFileName();
                url = "http://" + ServerConfig.HOST + mRemoteCam.videoFolder().substring(4) + "/" +
                        model.getThumbFileName();
//                imageUrl=http://192.168.42.1/SD0/NORMAL/2017-08-30-16-48-20T.JPG
            } else if (currentSegment == 1) {
//                url = "http://" + ServerConfig.HOST + mRemoteCam.eventFolder().substring(4) + "/Thumb/" +
//                        model.getThumbFileName();
                url = "http://" + ServerConfig.HOST + mRemoteCam.eventFolder().substring(4)+ "/" +
                        model.getThumbFileName();
            } else {
//                url = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) + "/Thumb/" +
//                        model.getThumbFileName();
                url = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4)+ "/" +
                        model.getThumbFileName();  //删除二级文件夹
            }
            ImageView photo;
            if (currentSegment == 2) {
                photo = (ImageView) view.findViewById(R.id.pic_photo);
            } else {
                photo = (ImageView) view.findViewById(R.id.photo);
            }
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
            try {
                for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
                    Model model = mArrayList.get(i);
                    String imageUrl;
                    if (currentSegment == 0) {
//                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.videoFolder().substring(4) +
//                                "/Thumb/" + model.getThumbFileName();
                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.videoFolder().substring(4) + "/" + model.getThumbFileName();
                    } else if (currentSegment == 1) {
//                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.eventFolder().substring(4) +
//                                "/Thumb/" + model.getThumbFileName();
                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.eventFolder().substring(4)+ "/" + model.getThumbFileName();
                    } else {
//                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4) +
//                                "/Thumb/" + model.getThumbFileName();
                        imageUrl = "http://" + ServerConfig.HOST + mRemoteCam.photoFolder().substring(4)+ "/" + model.getThumbFileName();  //删除二级文件夹
                    }
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
//如何下载yuv
            private Bitmap downloadBitmap(String imageUrl) {
                Bitmap bitmap = null;
                HttpURLConnection con = null;
                try {
//                    Log.e(TAG, "downloadBitmap: 1111 tryHttpURLConnection");
                    URL url = new URL(imageUrl);
                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5 * 1000);
                    con.setReadTimeout(10 * 1000);
                    con.setDoInput(true);
                    con.setDoOutput(true);
                    bitmap = BitmapFactory.decodeStream(con.getInputStream());
                } catch (Exception e) {
//                    Log.e(TAG, "downloadBitmap: Exception e");
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

    private static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int px2dp(Context context, float pxVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxVal / scale);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!isMultiChoose) {
            if (null != mListener) {
                Model item = (Model) parent.getItemAtPosition(position);
                filePath = mPWD + item.getName();
                if (currentSegment == 0) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_VIDEO_DETAIL, item);
                } else if (currentSegment == 1) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_COLLISION_DETAIL, item);
                } else if (currentSegment == 2) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_DETAIL, item);
                }
            }
        } else {


            boolean isSelect = mAdapter.getisSelectedAt(position);

            if (!isSelect) {
//                // 当前未被选中，记录下来，用于删除
                FileList.add(mPlaylist.get(position));
            } else {
                FileList.remove(mPlaylist.get(position));
            }

            // 选中状态的切换
            mAdapter.setItemisSelectedMap(position, !isSelect);

        }
    }

    @Override
    public void onRefresh() {
        if (isMultiChoose) {
            cancelMultiChoose();
        }
        if (currentSegment == 0) {
            showNormalVideo();
        } else if (currentSegment == 1) {
            showCollisionVideo();
        } else if (currentSegment == 2) {
            showPhoto();
        }
    }

}
