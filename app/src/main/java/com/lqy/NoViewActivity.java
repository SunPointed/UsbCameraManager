package com.lqy;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lqy.libusbcameramanager.usb.UsbCameraManager;
import com.lqy.usb.R;

/**
 * Created by lqy on 2018/7/6.
 */

public class NoViewActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mControlBtn;
    private boolean isOpen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_view);

        UsbCameraManager.getInstance().initial(this);

        mControlBtn = findViewById(R.id.ac_no_view_control_btn);
        mControlBtn.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UsbCameraManager.getInstance().finish();
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        switch (id) {
            case R.id.ac_no_view_control_btn: {
                v.setEnabled(false);
                if (isOpen) {
                    mControlBtn.setText("open");
                    isOpen = false;
                    UsbCameraManager.getInstance().closeUsbCamera();
                } else {
                    mControlBtn.setText("close");
                    isOpen = true;
                    UsbCameraManager.getInstance().openUsbCamera();
                }
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setEnabled(true);
                    }
                }, 2000);
            }
            break;
        }
    }
}
