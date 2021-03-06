package com.ctman.adefault;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;

import com.ctman.adefault.Adapters.FullSizeAdapter_mainpage;

import java.util.ArrayList;

public class Activity_Fullscreen_mainpage extends AppCompatActivity {
    ViewPager viewPager;
    //수정 String[] images;
    ArrayList<String> images;
    int position;

    FullSizeAdapter_mainpage fullSizeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_mainpage);

        if(savedInstanceState==null)
        {
            Intent i = getIntent();
            images = i.getStringArrayListExtra("IMAGES");
            //images = i.getStringArrayExtra("IMAGES");
            position = i.getIntExtra("POSITION",0);
        }

        viewPager = (ViewPager)findViewById(R.id.viewpager);

        fullSizeAdapter = new FullSizeAdapter_mainpage(this,images);
        viewPager.setAdapter(fullSizeAdapter);
        viewPager.setCurrentItem(position,true);
    }

    //줌인 줌 아웃 기능 막음

    @Override
    public boolean onTouchEvent(MotionEvent ev){

        try{
            return super.onTouchEvent(ev);
        }catch (IllegalArgumentException e){
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try{
            return super.dispatchTouchEvent(ev);
        }catch (Exception e){
            return false;
        }
    }
}
