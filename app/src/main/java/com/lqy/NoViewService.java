package com.lqy;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.lqy.usb.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by lqy on 2018/7/6.
 */

public class NoViewService extends Service {

    private boolean isStop;

    private WindowManager mWindowManager;

    private View mRootView;
    private ImageView mImageView;

    private float mWidth, mHeight;
    private float mAllWidth, mAllHeight;

    private float mX = 0f;
    private float mY = 0f;

    private float mPx = 0f;
    private float mPy = 0f;

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

    private void updateWindowPosition() {
        if (mRootView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mRootView.getLayoutParams();
            layoutParams.x = (int) mX;
            layoutParams.y = (int) mY;
            mWindowManager.updateViewLayout(mRootView, layoutParams);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (mWindowManager != null) {

            mRootView = LayoutInflater.from(this).inflate(R.layout.window_no_view, null);
            mImageView = mRootView.findViewById(R.id.window_no_view_iv);

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

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServerSocket serverSocket = new ServerSocket(8888);
                        final Socket socket = serverSocket.accept();
                        Log.d("lqy", "socket -> accept");
                        if (socket != null) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 1;
                            while (!isStop) {
                                InputStream is = socket.getInputStream();
                                byte[] data = new byte[4096];
                                int length = 0;
                                byte[] all = new byte[0];
                                byte[] show = null;
                                while ((length = is.read(data)) != -1) {
                                    if (all.length + length >= 460800) {
                                        show = new byte[460800];
                                        System.arraycopy(all, 0, show, 0, all.length);
                                        System.arraycopy(data, 0, show, all.length, 460800 - all.length);

                                        byte[] temp = new byte[all.length + length - 460800];
                                        System.arraycopy(data, 460800 - all.length, temp, 0, temp.length);
                                    } else {
                                        show = null;
                                        byte[] temp = new byte[all.length + length];
                                        System.arraycopy(all, 0, temp, 0, all.length);
                                        System.arraycopy(data, 0, temp, all.length, length);
                                        all = temp;
                                    }
                                    if (show != null) {
                                        // TODO: 2018/7/7 save data here， just a demo
//                                        Log.d("lqy", "data -> ready");
//                                        YuvImage image = new YuvImage(show, ImageFormat.NV21, 640, 480, null);
//                                        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
//                                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 50, outputSteam);
//                                        byte[] jpeg = outputSteam.toByteArray();
//                                        final Bitmap tempBitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
//                                        Log.d("lqy", "bitmap -> ready");
//                                        if (mImageView != null && mImageView.isAttachedToWindow()) {
//                                            mImageView.post(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    Log.d("lqy", "image -> set");
//                                                    mImageView.setImageBitmap(tempBitmap);
//                                                }
//                                            });
//                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.d("lqy", "service -> IOException");
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStop = true;
    }
}
