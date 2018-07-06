package com.lqy;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lqy.usb.R;

/**
 * Created by lqy on 2018/7/6.
 */

public class EntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
    }

    public void toActivity(View view){
        startActivity(new Intent(this, PreviewActivity.class));
    }

    public void toWindow(View view){

    }

    public void toNoView(View view){

    }
}
