package com.lqy;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lqy.libusbcameramanager.usb.UsbCameraManager;
import com.lqy.usb.DefaultUsbCamera;
import com.lqy.usb.R;

/**
 * Created by lqy on 2018/7/6.
 */

public class PreviewService extends Service {

    private WindowManager mWindowManager;

    private View mRootView;
    private TextureView mTextureView;
    private TextView mControlTv;
    private TextView mFinishTv;
    private View mCoverV;

    private boolean isSurfaceInitial;
    private boolean isOpen;

    private DefaultUsbCamera mDefaultUsbCamera;

    private float mWidth, mHeight;
    private float mAllWidth, mAllHeight;

    private float mX = 0f;
    private float mY = 0f;

    private float mPx = 0f;
    private float mPy = 0f;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mDefaultUsbCamera = new DefaultUsbCamera(new Surface(surface));
            UsbCameraManager.getInstance().registerUsbCamera(mDefaultUsbCamera);
            isSurfaceInitial = true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            UsbCameraManager.getInstance().unregisterUsbCamera(mDefaultUsbCamera.getProductId());
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mPx = event.getRawX();
                    mPy = event.getRawY();
                }
                break;
                case MotionEvent.ACTION_MOVE: {
                    float tx = event.getRawX() - mPx;
                    float ty = event.getRawY() - mPy;

                    boolean needUpdate = false;
                    if (!(tx + mX >= mAllWidth - mWidth || tx + mX <= 0)) {
                        mX += tx;
                        needUpdate = true;
                    }

                    if (!(ty + mY >= mAllHeight - mHeight || ty + mY <= 0)) {
                        mY += ty;
                        needUpdate = true;
                    }

                    if (needUpdate) {
                        updateWindowPosition();
                    }

                    mPx = event.getRawX();
                    mPy = event.getRawY();
                }
                break;
            }
            return false;
        }
    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.window_close_btn: {
                    stopSelf();
                    startActivity(new Intent(PreviewService.this, EntryActivity.class));
                }
                break;
                case R.id.window_preview_btn: {
                    v.setEnabled(false);
                    if (isSurfaceInitial) {
                        if (isOpen) {
                            mControlTv.setText("open");
                            isOpen = false;
                            UsbCameraManager.getInstance().closeUsbCamera();
                            mCoverV.setVisibility(View.VISIBLE);
                        } else {
                            mControlTv.setText("close");
                            isOpen = true;
                            UsbCameraManager.getInstance().openUsbCamera();
                            mCoverV.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        Toast.makeText(PreviewService.this, "surface not initial now", Toast.LENGTH_SHORT).show();
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
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        UsbCameraManager.getInstance().initial(this);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (mWindowManager == null) {
            stopSelf();
        } else {
            mRootView = LayoutInflater.from(this).inflate(R.layout.window_preview, null);
            mTextureView = mRootView.findViewById(R.id.window_preview_tv);
            mControlTv = mRootView.findViewById(R.id.window_preview_btn);
            mFinishTv = mRootView.findViewById(R.id.window_close_btn);
            mCoverV = mRootView.findViewById(R.id.window_preview_cover);

            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

            mFinishTv.setOnClickListener(mClickListener);
            mControlTv.setOnClickListener(mClickListener);

            mControlTv.setText("open");

            DisplayMetrics displayMetrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            mAllWidth = displayMetrics.widthPixels;
            mAllHeight = displayMetrics.heightPixels - Utils.getStatusBarHeight(this);
            mWidth = displayMetrics.widthPixels / 3f;
            mHeight = displayMetrics.heightPixels / 3f;

            int layoutFlag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    (int) mWidth,
                    (int) mHeight,
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSPARENT
            );
            layoutParams.gravity = Gravity.START | Gravity.TOP;
            mWindowManager.addView(mRootView, layoutParams);

            mRootView.setOnTouchListener(mOnTouchListener);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbCameraManager.getInstance().finish();
        if (mWindowManager != null) {
            mWindowManager.removeView(mRootView);
            mWindowManager = null;
        }
    }

    private void updateWindowPosition() {
        if (mRootView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mRootView.getLayoutParams();
            layoutParams.x = (int) mX;
            layoutParams.y = (int) mY;
            mWindowManager.updateViewLayout(mRootView, layoutParams);
        }
    }
}
