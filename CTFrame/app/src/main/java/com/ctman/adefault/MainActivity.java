package com.ctman.adefault;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ctman.adefault.Adapters.GalleryImageAdapter_mainpage;
import com.ctman.adefault.Interfaces.IRecyclerViewClickListener;
import com.ctman.adefault.Interfaces.SendDataToServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static String loginId = null;
    Uri photoUri,albumUri = null;
    public static boolean selectMode = false;
    final int REQUEST_TAKE_PHOTO = 1;
    final int REQUEST_CROP_IMAGE = 2;
    final int REQUEST_DRIVE = 3;
    final int REQUEST_PIXABAY = 4;
    String mCurrentPhotoPath;
    String upLoadServerUrl = "http://27.113.62.168:8080/index.php/insert_image";
    private TextView mTextMessage;
    static ArrayList<String> imageArray = new ArrayList<>();
    ArrayList<CheckBox> checkBoxes = new ArrayList<>();
    ArrayList<String> deleteImageArray = new ArrayList<>();
    RecyclerView recyclerView;
    static GalleryImageAdapter_mainpage galleryImageAdapter;
    //?????? ?????? ????????? ????????????
    static SwipeRefreshLayout swipeRefreshLayout;

    Intent intent;
    ImageView btn_cancel,btn_delete;
    ArrayList<ImageView> imageViews = new ArrayList<>();

    GridView gridView;
    //????????? ?????? ??????
    public static  final int redown_pixa = 101;        //?????????
    public static  final int refresh = 102;        //?????????

    private final MainActivity.MyHandler mHandler = new MyHandler(MainActivity.this);

    //????????? ??????
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        public MyHandler(MainActivity mainactivity) {
            weakReference = new WeakReference<MainActivity>(mainactivity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity mainactivity = weakReference.get();

            if (mainactivity != null) {
                switch (msg.what) {

                    case redown_pixa:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //??? ?????? ??????
                                redownpixa(1);
                            }
                        }).start();

                        break;

                    case refresh:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //?????? ??????
                                image_list_view();
                            }
                        }).start();
                        galleryImageAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                }
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_mypage:
                    //mTextMessage.setText("Mypage");
                    intent = new Intent(MainActivity.this, Activity_MyPage.class);
                    startActivity(intent);
                    break;
                case R.id.navigation_pixabay:
                    //mTextMessage.setText(R.string.title_pixabay);
                    intent = new Intent(MainActivity.this, Activity_Pixabay.class);
                    startActivityForResult(intent, REQUEST_PIXABAY);
                    break;
                case R.id.navigation_googledrive:
                    intent = new Intent(MainActivity.this, Activity_Drive.class);
                    intent.putExtra("loginID",loginId);
                    startActivityForResult(intent,REQUEST_DRIVE);
                    break;
                case R.id.navigation_mygallery:
                    makeFolder();
                    albumAction();
                    break;

            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        //????????? ?????? ????????????
        MainActivity.loginId = intent.getStringExtra("email");
        Init();
        btn_cancel = (ImageView)findViewById(R.id.btn_cancel);
        btn_delete = (ImageView)findViewById(R.id.btn_delete);
        btn_cancel.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                btn_cancel.setVisibility(View.INVISIBLE);
                btn_delete.setVisibility(View.INVISIBLE);
                selectMode = false;
                deleteImageArray.clear();
                imageView_all_clear_filter();
                imageViews.clear();
                checkBoxes.clear();
                GalleryImageAdapter_mainpage.itemStateArray.clear();
            }
        });
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView_all_clear_filter();
                btn_cancel.setVisibility(View.INVISIBLE);
                btn_delete.setVisibility(View.INVISIBLE);
                msgToast("?????????????????????!");
                for(int i=0;i<deleteImageArray.size();++i)
                {
                    //Log.i("CTFrame",deleteImageArray.get(i));
                }
                int success = send_to_server_delete_image();

                deleteImageArray.clear();
                image_refresh();
                imageViews.clear();
                checkBoxes.clear();
                GalleryImageAdapter_mainpage.itemStateArray.clear();


                //Log.i("CTFrame",String.valueOf(success));
                selectMode = false;

            }
        });
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //?????? ????????? ????????? ????????????
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //????????? ????????? ?????? : ???????????? ?????? ??????
                        Message msg = mHandler.obtainMessage(refresh);
                        mHandler.sendMessage(msg);
                        deleteImageArray.clear();
                        Snackbar.make(recyclerView,"???????????? ??????",Snackbar.LENGTH_SHORT).show();
                    }
                },500);
            }
        });
    }

    //*******************************************************************************************/
    // Album to MainActivity cropImage
    //*******************************************************************************************/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK)
        {
            if(requestCode == REQUEST_CROP_IMAGE)
            {
                File f = new File(albumUri.getPath());
                f.delete();
            }
        }
        else
        {
            switch (requestCode)
            {
                case REQUEST_TAKE_PHOTO :   //???????????? ????????????

                    //Log.i("CTtest", "??????????????? ?????? ??????");
                    File albumFile= null;

                    try
                    {
                        //??? file??? ?????? ??????
                        albumFile = createImageFile();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    if(albumFile != null)
                    {
                        albumUri = Uri.fromFile(albumFile);     //?????? ????????? Crop??? ????????? ????????? ?????? ??????
                    }

                    photoUri = data.getData();  //?????? ???????????? ???f???

                    //Log.i("CTFrame","??????????????? ?????? : "+photoUri.getPath());
                    cropImage(photoUri);
                    break;
                case REQUEST_CROP_IMAGE :
                    //Bitmap photo = BitmapFactory.decodeFile(photoUri.getPath());      //??????????????? ???????????? imageView??? ????????? ??????
                    //album = true;
                    //photoUri = data.getData();  //?????? ???????????? ??????

                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);     //?????????
                    mediaScanIntent.setData(albumUri);      //?????????
                    this.sendBroadcast(mediaScanIntent);        //?????????

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SendDataToServer sendImage = new SendDataToServer();
                            int serverResponseCode = sendImage.uploadFile(upLoadServerUrl,loginId, albumUri.getPath());

                            if(serverResponseCode == 200){
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        msgToast("?????????????????????.");
                                        image_refresh();
                                    }
                                });
                            }
                            else{
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        msgToast("?????????????????????.");
                                    }
                                });
                            }
                        }

                    }).start();

                    break;
                case REQUEST_DRIVE:

                    image_refresh();
                    break;
                case REQUEST_PIXABAY :
                    image_refresh();
                    break;
            }
        }
    }

    //*******************************************************************************************/
    // xml connect source
    //*******************************************************************************************/

    private void Init() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        //?????? ?????? ????????? ???????????? layout
         swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swip);
        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        image_list_view();

        /*****************************************************************************************/
        final IRecyclerViewClickListener listener = new IRecyclerViewClickListener() {

            @Override
            public void onClick(View view, int position, ImageView imageView) {

            }

            @Override
            public void onLongClick(View view, int position, ImageView imageView) {

            }

            @Override
            public void onClick(int position, ImageView imageView, CheckBox checkBox) {
                if(selectMode)
                {
                    try{
                        String s = imageView.getColorFilter().toString();
                        imageView.clearColorFilter();

                        for(int i=0;i<deleteImageArray.size();++i) {
                            if (deleteImageArray.get(i).equals(imageArray.get(position))) {
                                //Log.i("CTFrame", "i :::"+deleteImageArray.get(i));
                                deleteImageArray.remove(i);
                            }
                        }
                        deleteImageArray.remove(imageArray.get(position));
                        //Log.i("CTFrame","---------------------------------------------");
                        for(int i=0;i<deleteImageArray.size();++i) {
                            //Log.i("CTFrame", deleteImageArray.get(i));
                        }
                        checkBoxes.remove(checkBox);
                        imageViews.remove(imageView);
                        checkBox.setVisibility(View.INVISIBLE);
                        checkBox.setChecked(false);
                    }catch (Exception e){
                        //Log.i("CTFrameTest",e.toString());
                        imageView.setColorFilter(Color.argb(140,150,150,150));
                        deleteImageArray.add(imageArray.get(position));
                        if(!imageViews.contains(imageView)){
                            imageViews.add(imageView);
                        }
                        if(!checkBoxes.contains(checkBox))
                        {
                            checkBoxes.add(checkBox);
                        }
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setChecked(true);
                    }
                }
                else
                {
                    //open full screen activity with omage clicked
                    Intent i = new Intent(MainActivity.this, Activity_Fullscreen_mainpage.class);
                    i.putExtra("IMAGES", imageArray);
                    i.putExtra("POSITION", position);
                    startActivity(i);
                }
            }
            @Override
            public void onLongClick(View view, int position, ImageView imageView,CheckBox checkBox){
                //Log.i("CTFrameTest",imageView.toString());
                imageView.setColorFilter(Color.argb(140,150,150,150));
                selectMode = true;
                btn_cancel.setVisibility(View.VISIBLE);
                btn_delete.setVisibility(View.VISIBLE);
                imageViews.add(imageView);
                checkBoxes.add(checkBox);
                deleteImageArray.add(imageArray.get(position));
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(true);
            }
        };

        // this?????? getActivity ??????
        galleryImageAdapter = new GalleryImageAdapter_mainpage(this, imageArray, listener);
        recyclerView.setAdapter(galleryImageAdapter);
    }

    //*******************************************************************************************/
    // Create Temp Image File
    //*******************************************************************************************/

    private File createImageFile() throws IOException
    {

        String imageFileName = "CTFrame_" + String.valueOf(System.currentTimeMillis());
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"CTFrame/");

        File file = File.createTempFile(imageFileName, ".jpg",storageDir);
        mCurrentPhotoPath = file.getAbsolutePath();
        return file;
    }

    //*******************************************************************************************/
    // cropImage
    //*******************************************************************************************/

    private void cropImage(Uri photoUri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(photoUri, "image/*");

        cropIntent.putExtra("aspectX", 16);       //x??????
        cropIntent.putExtra("aspectY", 9);       //y??? ??????

        cropIntent.putExtra("scale", true);

        cropIntent.putExtra("output", albumUri);
        startActivityForResult(cropIntent, REQUEST_CROP_IMAGE);

    }

    //*******************************************************************************************/
    // MainActivity to Album Activity
    //*******************************************************************************************/

    private void albumAction() {
        Intent albumintent = new Intent(Intent.ACTION_PICK);
        albumintent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(albumintent, REQUEST_TAKE_PHOTO);
    }

    //*******************************************************************************************/
    // Make Folder CTFrame
    //*******************************************************************************************/

    private void makeFolder()
    {
        //????????? ?????? ???????????? DownLoad????????? CTFrame????????? ????????? ?????????
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CTFrame");
        dir.mkdir();
    }

    //*******************************************************************************************/
    // Toast Message
    //*******************************************************************************************/

    void msgToast(String msg){
        Toast.makeText(MainActivity.this, msg,
                Toast.LENGTH_SHORT).show();
    }

    //*******************************************************************************************/
    // List View
    //*******************************************************************************************/
    static void image_list_view()
    {
        imageArray.clear();
        JSONObject obj = new JSONObject();
        SendDataToServer sendDataToServer = new SendDataToServer();

        JSONObject post_dict = new JSONObject();

        try {
            post_dict.put("email" , loginId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (post_dict.length() > 0) {
            try
            {
                obj = new JSONObject(sendDataToServer.execute(String.valueOf(post_dict),"list_view").get());
                //Log.i("CTFrame","????????????");

                try
                {
                    JSONArray imgArray = obj.getJSONArray("responseMsg");
                    for(int i=0;i<imgArray.length();i++)
                    {
                        JSONObject tempobj = imgArray.getJSONObject(i);
                        String picURL = tempobj.getString("pic");
                        imageArray.add("http://"+picURL);
                    }
                }
                catch (JSONException e)
                {
                    //Log.i("CTFrame", "JSONError : " + e.toString());
                }
            }
            catch (Exception e)
            {
                //Log.i("CTFrame",e.toString());
            }
        }

    }
    //*******************************************************************************************/
    // Delete_image_to_server Function
    //*******************************************************************************************/
    int send_to_server_delete_image()
    {
        int responseMsg=0;
        JSONObject obj = new JSONObject();
        SendDataToServer sendDataToServer = new SendDataToServer();

        JSONObject post_dict = new JSONObject();

        try {
            post_dict.put("email", loginId);
            JSONArray jsonArray = new JSONArray();
            for (int i=0; i < deleteImageArray.size(); i++) {
                jsonArray.put(deleteImageArray.get(i));

                //??????
                if(deleteImageArray.get(i).contains("is_auto")) {
                    //Log.i("pixa_down", "?????? ?????? ??????");
                    //????????? ?????? ?????? ?????? ?????? ????????? ??????
                    Message msg = mHandler.obtainMessage(redown_pixa);
                    mHandler.sendMessage(msg);
                }
            }
            post_dict.put("image",jsonArray);

            //Log.i("CTFrame",String.valueOf(post_dict));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (post_dict.length() > 0) {
            try {
                obj = new JSONObject(sendDataToServer.execute(String.valueOf(post_dict), "delete_image").get());
                //Log.i("CTFrame", "????????????");

                responseMsg = obj.getInt("responseMsg");

            } catch (Exception e) {
                //Log.i("CTFrame", e.toString());
            }
        }
        return responseMsg;
    }

    //???????????? ?????? ?????? ?????? ????????????
    public static void redownpixa(int count)
    {
        //Log.i("pixa_down", ">>>>>>?????? ???????????? ?????? ??????  :::::: "+count+"???");
        //section 0 ?????? ????????????
        JSONObject obj = new JSONObject();
        SendDataToServer sendDataToServer = new SendDataToServer();
        //section 0 ?????? ????????????

        //section 2 ????????? ???????????????//
        JSONObject post_dict = new JSONObject();
        //section 2 ?????? ??????//

        //section 3 ????????? ?????? ??? ?????? ?????????????????? ????????????//
        try {
            post_dict.put("email" , loginId);
            post_dict.put("count", count);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //section 3 ????????????//

        if (post_dict.length() > 0) {
            try
            {
                //section 4   "signUpCheck ?????? ???????????? ????????? ????????? ??? ????????? ????????? ????????? //
                sendDataToServer.execute(String.valueOf(post_dict),"reload_pixa_pic").get();
                //section 4//
            }
            catch (Exception e)
            {
                //Log.i("CTFrame",e.toString());
            }
        }
    }

    //*******************************************************************************************/
    // ImageView Filter all clear function
    //*******************************************************************************************/
    void imageView_all_clear_filter()
    {
        try{
            for(ImageView iv : imageViews)
            {
                iv.clearColorFilter();
            }
            for(CheckBox cb : checkBoxes)
            {
                cb.setChecked(false);
                cb.setVisibility(View.INVISIBLE);
            }
        }catch (Exception e)
        {

        }
    }
    //*******************************************************************************************/
    // MainActivity image refresh
    //*******************************************************************************************/
    void image_refresh()
    {
        image_list_view();
        galleryImageAdapter.notifyDataSetChanged();
    }
}