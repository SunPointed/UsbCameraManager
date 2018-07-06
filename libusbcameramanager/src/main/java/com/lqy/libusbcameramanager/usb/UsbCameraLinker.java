package com.lqy.libusbcameramanager.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

import com.serenegiant.usb.USBMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lqy on 2018/7/3.
 */

public class UsbCameraLinker {
    private static volatile UsbCameraLinker sInstance;

    public static UsbCameraLinker getInstance() {
        if (sInstance == null) {
            synchronized (UsbCameraLinker.class) {
                if (sInstance == null) {
                    sInstance = new UsbCameraLinker();
                }
            }
        }
        return sInstance;
    }

    private UsbCameraLinker() {
        mRegisterCameraMap = new ConcurrentHashMap<>();
        mLinkedCameraMap = new ConcurrentHashMap<>();
        isRegister = false;
    }

    private final Map<Integer, List<UsbCamera>> mRegisterCameraMap;
    private final Map<Integer, Map<String, UsbCamera>> mLinkedCameraMap;

    private USBMonitor mUSBMonitor;

    private boolean isRegister;

    private USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice usbDevice) {
            if (isTargetDevice(usbDevice)) {
                int productId = usbDevice.getProductId();
                UsbCamera usbCamera = getAndRemoveRegisterCameraById(productId);
                if (usbCamera != null) {
                    Map<String, UsbCamera> map = mLinkedCameraMap.get(productId);
                    if (map == null) {
                        map = new HashMap<>();
                        mLinkedCameraMap.put(productId, map);
                    }
                    map.put(usbDevice.getDeviceName(), usbCamera);
                }
            }

            mUSBMonitor.requestPermission(usbDevice);
        }

        @Override
        public void onDettach(UsbDevice usbDevice) {
            if (isTargetDevice(usbDevice)) {
                int productId = usbDevice.getProductId();
                Map<String, UsbCamera> map = mLinkedCameraMap.get(productId);
                if (map != null) {
                    UsbCamera usbCamera = map.remove(usbDevice.getDeviceName());
                    if (usbCamera != null) {
                        registerCamera(productId, usbCamera);
                    }

                    if (map.isEmpty()) {
                        mLinkedCameraMap.remove(productId);
                    }
                }
            }
        }

        @Override
        public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
            if (isTargetDevice(usbDevice)) {
                Map<String, UsbCamera> map = mLinkedCameraMap.get(usbDevice.getProductId());
                if (map != null && map.containsKey(usbDevice.getDeviceName())) {
                    UsbCamera usbCamera = map.get(usbDevice.getDeviceName());
                    if (usbCamera != null) {
                        usbCamera.connect(usbControlBlock);
                        usbCamera.setUsed(true);
                    }
                }
            }
        }

        @Override
        public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
            if (isTargetDevice(usbDevice)) {
                Map<String, UsbCamera> map = mLinkedCameraMap.get(usbDevice.getProductId());
                if (map != null && map.containsKey(usbDevice.getDeviceName())) {
                    UsbCamera usbCamera = map.get(usbDevice.getDeviceName());
                    if (usbCamera != null) {
                        usbCamera.disconnect();
                        usbCamera.setUsed(false);
                    }
                }
            }
        }

        @Override
        public void onCancel(UsbDevice usbDevice) {

        }
    };

    private UsbCamera getAndRemoveRegisterCameraById(@NonNull Integer productId) {
        List<UsbCamera> list = mRegisterCameraMap.get(productId);
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.remove(0);
        }
    }

    private boolean isTargetDevice(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return false;
        }

        Set<Integer> ids = mRegisterCameraMap.keySet();
        for (Integer id : ids) {
            if (id == usbDevice.getProductId()) {
                return true;
            }
        }

        ids = mLinkedCameraMap.keySet();
        for (Integer id : ids) {
            if (id == usbDevice.getProductId()) {
                return true;
            }
        }

        return false;
    }

    void searchDevices() {
        Iterator<UsbDevice> deviceIterator = mUSBMonitor.getDevices();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            mOnDeviceConnectListener.onAttach(device);
        }
    }

    void registerCamera(@NonNull Integer productId, @NonNull UsbCamera usbCamera) {
        List<UsbCamera> list = mRegisterCameraMap.get(productId);
        if (list == null) {
            list = new ArrayList<>();
            mRegisterCameraMap.put(productId, list);
        }
        list.add(usbCamera);
    }

    void unregisterCamera(@NonNull Integer productId) {
        List<UsbCamera> list = mRegisterCameraMap.remove(productId);
        if (list != null) {
            list.clear();
        }

        Map<String, UsbCamera> map = mLinkedCameraMap.remove(productId);
        if (map != null) {
            Set<Map.Entry<String, UsbCamera>> linkedEntries = map.entrySet();
            for (Map.Entry<String, UsbCamera> entry : linkedEntries) {
                if (entry.getValue() != null) {
                    entry.getValue().finish();
                    entry.getValue().setUsed(false);
                }
            }
        }
    }

    void register(Context context) {
        if (!isRegister) {
            isRegister = true;

            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
            }

            mUSBMonitor = new USBMonitor(context, mOnDeviceConnectListener);
            mUSBMonitor.register();
        }
    }

    void unregister() {
        if (isRegister) {
            isRegister = false;
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
    }
}
