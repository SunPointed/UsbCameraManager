package com.lqy.usb;

import android.view.Surface;

import com.lqy.libusbcameramanager.usb.DefaultFrameRateCallback;
import com.lqy.libusbcameramanager.usb.UsbCamera;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;

/**
 * Created by lqy on 2018/7/6.
 */

public class DefaultUsbCamera extends UsbCamera {

    private Surface mSurface;

    public DefaultUsbCamera(Surface surface) {
        mSurface = surface;
        setResolution(640, 480, true);
    }

    @Override
    public int getProductId() {
        return 866;
    }

    @Override
    public String getProductName() {
        return DefaultUsbCamera.class.getSimpleName();
    }

    @Override
    public int getVendorId() {
        return DefaultUsbCamera.class.getSimpleName().length();
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
