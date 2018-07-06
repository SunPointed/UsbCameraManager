package com.lqy.libusbcameramanager.usb;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 前提：对于相同productId的usbCamera设备，android手机只连接一个；
 *      android手机可以连多个不同productId的usbCamera
 * Created by lqy on 2018/7/4.
 */

public class UsbCameraManager {
    private static volatile UsbCameraManager sInstance;

    public static UsbCameraManager getInstance() {
        if (sInstance == null) {
            synchronized (UsbCameraManager.class) {
                if (sInstance == null) {
                    sInstance = new UsbCameraManager();
                }
            }
        }
        return sInstance;
    }

    private final List<UsbCamera> mCameras = new ArrayList<>();

    private final Object fLock = new Object();

    public void registerUsbCamera(@NonNull UsbCamera usbCamera) {
        synchronized (fLock) {
            int productId = usbCamera.getProductId();
            for (UsbCamera camera : mCameras) {
                if (camera.getProductId() == productId) {
                    throw new IllegalStateException("this usb camera(productId = " + productId + ") is already added");
                }
            }
            mCameras.add(usbCamera);
        }
    }

    public void unregisterUsbCamera(int productId) {
        synchronized (fLock) {
            Iterator<UsbCamera> cameraIterator = mCameras.iterator();
            while (cameraIterator.hasNext()) {
                UsbCamera usbCamera = cameraIterator.next();
                if (usbCamera.getProductId() == productId) {
                    if(usbCamera.isUsed()){
                        UsbCameraLinker.getInstance().unregisterCamera(usbCamera.getProductId());
                    }
                    cameraIterator.remove();
                    break;
                }
            }
        }
    }

    public void openUsbCamera() {
        synchronized (fLock) {
            for (UsbCamera usbCamera : mCameras) {
                UsbCameraLinker.getInstance().registerCamera(usbCamera.getProductId(), usbCamera);
            }
        }
        UsbCameraLinker.getInstance().searchDevices();
    }

    public void closeUsbCamera() {
        synchronized (fLock) {
            for (UsbCamera usbCamera : mCameras) {
                UsbCameraLinker.getInstance().unregisterCamera(usbCamera.getProductId());
            }
        }
    }

    public void initial(Context context) {
        UsbCameraLinker.getInstance().register(context);
    }

    public void finish() {
        closeUsbCamera();
        UsbCameraLinker.getInstance().unregister();
        synchronized (fLock) {
            mCameras.clear();
        }
    }
}
