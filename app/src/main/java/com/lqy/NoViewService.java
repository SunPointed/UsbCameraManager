package com.lqy;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.lqy.usb.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by lqy on 2018/7/6.
 */

public class NoViewService extends Service {

    private boolean isStop;

    private WindowManager mWindowManager;

    private View mRootView;
    private SurfaceView mSurfaceView;

    private float mWidth, mHeight;
    private float mAllWidth, mAllHeight;

    private float mX = 0f;
    private float mY = 0f;

    private float mPx = 0f;
    private float mPy = 0f;

    private SurfaceHolder mSurfaceHolder;
    private Thread mDataThread;
    private Thread mDrawThread;

    int[] mColors = new int[640 * 480];
    private Bitmap mBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

    private BlockingQueue<byte[]> mBytes = new LinkedBlockingQueue<>();

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
            mSurfaceView = mRootView.findViewById(R.id.window_no_view_iv);

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

            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mSurfaceHolder = holder;
                    startThread();
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    holder.getSurface().release();
                    isStop = true;
                }
            });
        }
    }

    private void startThread() {
        mDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(8888);
                    final Socket socket = serverSocket.accept();
                    Log.d("lqy", "socket -> accept");
                    if (socket != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 1;
                        InputStream is = socket.getInputStream();
                        byte[] data = new byte[4096];
                        byte[] all = new byte[0];
                        byte[] show = null;
                        int length = 0;
                        while ((length = is.read(data)) != -1 && !isStop) {
                            if (all.length + length >= 460800) {
                                show = new byte[460800];
                                System.arraycopy(all, 0, show, 0, all.length);
                                System.arraycopy(data, 0, show, all.length, 460800 - all.length);

                                byte[] temp = new byte[all.length + length - 460800];
                                System.arraycopy(data, 460800 - all.length, temp, 0, temp.length);
                                all = temp;
                            } else {
                                show = null;
                                byte[] temp = new byte[all.length + length];
                                System.arraycopy(all, 0, temp, 0, all.length);
                                System.arraycopy(data, 0, temp, all.length, length);
                                all = temp;
                            }
                            if (show != null) {
                                mBytes.add(show);
                            }
                        }
                        socket.close();
                    }
                } catch (IOException e) {
                    Log.d("lqy", "service -> IOException");
                    e.printStackTrace();
                }
            }
        });

        mDrawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean is = true;
                while (!isStop) {
                    byte[] data = null;
                    try {
                        data = mBytes.poll(15, TimeUnit.MILLISECONDS);
                        Log.d("lqy", "service size -> " + mBytes.size());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    if (data != null) {
                        if (is) {
                            is = false;
                            long time = System.currentTimeMillis();
                            int[] rgb = YuvToRGBUtils.NV21ToRGB(data, 640, 480);
                            if (mSurfaceHolder != null) {
                                Canvas canvas = mSurfaceHolder.lockCanvas();
                                render(canvas, rgb);
                                mSurfaceHolder.unlockCanvasAndPost(canvas);
                            }
                            Log.d("lqy", "show bitmap time ->" + (System.currentTimeMillis() - time));
                        }
                    }
                }
            }
        });

        mDataThread.start();
        mDrawThread.start();
    }

    private void render(Canvas canvas, int[] temp) {
        canvas.drawColor(Color.BLACK);

        if (temp != null) {
            for (int i = 0, j = 0; i < temp.length; i += 3, j++) {
                mColors[j] = Color.rgb(temp[i], temp[i + 1], temp[i + 2]);
            }
            mBitmap.setPixels(mColors, 0, 640, 0, 0, 640, 480);
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStop = true;
    }
}
