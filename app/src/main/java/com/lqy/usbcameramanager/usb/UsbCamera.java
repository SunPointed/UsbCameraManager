package com.lqy.usbcameramanager.usb;

import android.view.Surface;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.util.List;

/**
 * Created by lqy on 2018/7/3.
 */

public abstract class UsbCamera {
    protected UVCCamera mUVCCamera;
    protected USBMonitor.UsbControlBlock mUsbControlBlock;
    protected int mWidth, mHeight;
    protected boolean isAutoAdapt;

    public abstract int getProductId();

    public abstract String getProductName();

    public abstract int getVendorId();

    protected abstract int getFrameFormat();

    protected abstract int getMinFps();

    protected abstract int getMaxFps();

    protected abstract Surface getSurface();

    protected abstract IFrameCallback getFrameCallback();

    protected abstract void onConnect();

    protected abstract void onDisconnect();

    protected abstract void onFinish();

    protected abstract void onOpenCamera();

    protected abstract void onCloseCamera();

    public String getDeviceName() {
        return mUsbControlBlock == null ? null : mUsbControlBlock.getDeviceName();
    }


    public void setResolution(int width, int height, boolean autoAdapt) {
        mWidth = width;
        mHeight = height;
        isAutoAdapt = autoAdapt;
    }

    public void connect(USBMonitor.UsbControlBlock usbControlBlock) {
        mUVCCamera = new UVCCamera();
        mUsbControlBlock = usbControlBlock;
        onConnect();
    }

    public void disconnect() {
        closeCamera();
        onDisconnect();
        mUVCCamera = null;
        mUsbControlBlock = null;
    }

    public void openCamera() {
        if (mUsbControlBlock == null) {
            return;
        }

        mUVCCamera.open(mUsbControlBlock);

        try {
            UVCCamera.class.getDeclaredField("mCurrentFrameFormat").set(mUVCCamera, getFrameFormat());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        List<Size> sizeList = mUVCCamera.getSupportedSizeList();
        if (sizeList == null || sizeList.size() == 0) {
            throw new IllegalStateException(getClass().getName() + " has wrong FrameFormat, please check method getFrameFormat()");
        } else {
            if (isAutoAdapt) {
                int targetArea = mWidth * mHeight;
                int differ = Integer.MAX_VALUE;
                for (Size size : sizeList) {
                    int temp = Math.abs(size.width * size.height - targetArea);
                    if (temp < differ) {
                        differ = temp;
                        mWidth = size.width;
                        mHeight = size.height;
                    }
                }
            } else {
                boolean hasTargetSize = false;
                for (Size size : sizeList) {
                    if (size.width == mWidth && size.height == mHeight) {
                        hasTargetSize = true;
                        break;
                    }
                }
                if (!hasTargetSize) {
                    throw new IllegalStateException("camera has no width = " + mWidth + " height = " + mHeight);
                }
            }
        }

        if (!mUVCCamera.getAutoFocus()) {
            mUVCCamera.setAutoFocus(true);
        }

        try {
            mUVCCamera.setPreviewSize(
                    mWidth,
                    mHeight,
                    getMinFps(),
                    getMaxFps(),
                    getFrameFormat(),
                    UVCCamera.DEFAULT_BANDWIDTH);
        } catch (Exception e) {
            throw new IllegalStateException("camera setPreviewSize failed");
        }

        mUVCCamera.setPreviewDisplay(getSurface());
        mUVCCamera.setFrameCallback(getFrameCallback(), UVCCamera.PIXEL_FORMAT_NV21);
        mUVCCamera.startPreview();

        onOpenCamera();
    }

    public void closeCamera() {
        if (mUsbControlBlock == null) {
            return;
        }

        if (mUVCCamera != null) {
            mUVCCamera.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_NV21);
            mUVCCamera.close();
            mUsbControlBlock = null;
            mUVCCamera = null;
        }
        onCloseCamera();
    }

    public void finish() {
        closeCamera();
        onFinish();
    }
}
