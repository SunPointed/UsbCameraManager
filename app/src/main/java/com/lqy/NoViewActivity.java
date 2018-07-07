package com.lqy;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lqy.libusbcameramanager.usb.UsbCameraManager;
import com.lqy.usb.IRecorderEngine;
import com.lqy.usb.NoViewUsbCamera;
import com.lqy.usb.R;
import com.serenegiant.usb.UVCCamera;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by lqy on 2018/7/6.
 */

public class NoViewActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mControlBtn;
    private volatile boolean isOpen;

    private Socket mSocket;
    private boolean isStop;
    private BlockingQueue<byte[]> mBytes = new LinkedBlockingQueue<>();

    private IRecorderEngine mRecorderEngine = new IRecorderEngine() {

        @Override
        public void startRecording() {

        }

        @Override
        public void stopRecording() {

        }

        @Override
        public void handleAudioData(byte[] audio) {

        }

        @Override
        public void handleVideoData(byte[] video) {
            if (mSocket != null && isOpen) {
                mBytes.add(video);
            }
        }

        @Override
        public void handleLiveVideoData(byte[] video) {

        }

        @Override
        public void setSize(int width, int height) {

        }

        @Override
        public boolean isLive() {
            return false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_view);

        UsbCameraManager.getInstance().initial(this);

        NoViewUsbCamera.Builder builder = new NoViewUsbCamera.Builder()
                .setFrameFormat(UVCCamera.FRAME_FORMAT_YUYV)
                .setMinFps(1)
                .setMaxFps(31)
                .setProductId(866)
                .setProductName("test")
                .setVendorId(1234)
                .setHeight(480)
                .setWidth(640)
                .setRecorderEngine(mRecorderEngine);
        UsbCameraManager.getInstance().registerUsbCamera(builder.build());

        mControlBtn = findViewById(R.id.ac_no_view_control_btn);
        mControlBtn.setOnClickListener(this);
        mControlBtn.setText("open");

        startService(new Intent(this, NoViewService.class));
        mControlBtn.postDelayed(new Runnable() {
            @Override
            public void run() {
                startSocketThread();
            }
        }, 2000);
    }

    private void startSocketThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket("localhost", 8888);
                    while (!isStop) {
                        byte[] data = mBytes.poll(500, TimeUnit.MILLISECONDS);
                        if (data != null && data.length > 0) {
                            Log.d("lqy","size -> " + mBytes.size());
                            mSocket.getOutputStream().write(data);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isStop = true;
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
