package com.lqy;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.lqy.libusbcameramanager.usb.DefaultFrameRateCallback;
import com.lqy.libusbcameramanager.usb.UsbCamera;
import com.lqy.libusbcameramanager.usb.UsbCameraManager;
import com.lqy.usb.R;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;

/**
 * Created by lqy on 2018/7/6.
 */

public class PreviewActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private TextureView mTextureView;
    private Button mButton;

    private PreviewUsbCamera mPreviewUsbCamera;

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
        mPreviewUsbCamera = new PreviewUsbCamera(new Surface(surface));
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

    static class PreviewUsbCamera extends UsbCamera {

        private Surface mSurface;

        public PreviewUsbCamera(Surface surface) {
            mSurface = surface;
            setResolution(640, 480, true);
        }

        @Override
        public int getProductId() {
            return 866;
        }

        @Override
        public String getProductName() {
            return PreviewUsbCamera.class.getSimpleName();
        }

        @Override
        public int getVendorId() {
            return PreviewUsbCamera.class.getSimpleName().length();
        }

        @Override
        protected int getFrameFormat() {
            return UVCCamera.FRAME_FORMAT_YUYV;
        }

        @Override
        protected int getMinFps() {
            return 1;
        }

        @Override
        protected int getMaxFps() {
            return 31;
        }

        @Override
        protected Surface getSurface() {
            return mSurface;
        }

        @Override
        protected IFrameCallback getFrameCallback() {
            return new DefaultFrameRateCallback() {
                @Override
                protected void doFrameWithFixFrameRate(byte[] data) {
                    // TODO: 2018/7/6 处理固定帧率的流
                }

                @Override
                protected void doFrameWithAll(byte[] data) {
                    // TODO: 2018/7/6 处理所有帧率的流
                }
            };
        }

        @Override
        protected void onConnect() {
            openCamera();
        }

        @Override
        protected void onDisconnect() {
            closeCamera();
        }

        @Override
        protected void onFinish() {

        }

        @Override
        protected void onOpenCamera() {

        }

        @Override
        protected void onCloseCamera() {

        }
    }
}
