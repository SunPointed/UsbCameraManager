package com.lqy;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.lqy.libusbcameramanager.usb.UsbCameraManager;
import com.lqy.usb.DefaultUsbCamera;
import com.lqy.usb.R;

/**
 * Created by lqy on 2018/7/6.
 */

public class PreviewActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private TextureView mTextureView;
    private Button mButton;

    private DefaultUsbCamera mPreviewUsbCamera;

    private boolean isSurfaceInitial;
    private boolean isOpen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);

        UsbCameraManager.getInstance().initial(this);

        mTextureView = findViewById(R.id.ac_preview_tv);
        mButton = findViewById(R.id.ac_preview_btn);

        mTextureView.setSurfaceTextureListener(this);

        mButton.setOnClickListener(this);
        mButton.setText("open");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UsbCameraManager.getInstance().finish();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mPreviewUsbCamera = new DefaultUsbCamera(new Surface(surface));
        UsbCameraManager.getInstance().registerUsbCamera(mPreviewUsbCamera);
        isSurfaceInitial = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        UsbCameraManager.getInstance().unregisterUsbCamera(mPreviewUsbCamera.getProductId());
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(final View v) {
        v.setEnabled(false);
        if (isSurfaceInitial) {
            if (isOpen) {
                mButton.setText("open");
                isOpen = false;
                UsbCameraManager.getInstance().closeUsbCamera();
            } else {
                mButton.setText("close");
                isOpen = true;
                UsbCameraManager.getInstance().openUsbCamera();
            }
        } else {
            Toast.makeText(this, "surface not initial now", Toast.LENGTH_SHORT).show();
        }
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setEnabled(true);
            }
        }, 2000);
    }
}
