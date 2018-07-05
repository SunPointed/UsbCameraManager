package com.lqy.usbcameramanager.usb;

import com.serenegiant.usb.IFrameCallback;

import java.nio.ByteBuffer;

/**
 * Created by lqy on 2018/6/30.
 */

public abstract class DefaultFrameRateCallback implements IFrameCallback {
    private static final long FRAME_RATE = 15; // 只要15帧
    private static final long SECOND = 1000;
    private static final long MILLS_SCHEDULE = SECOND / FRAME_RATE;

    private long preTime = -1;

    @Override
    public void onFrame(ByteBuffer byteBuffer) {
        byte[] data = new byte[byteBuffer.capacity()];
        byteBuffer.get(data, 0, data.length);

        long curTime = System.currentTimeMillis();
        if (preTime == -1) { //初始化并发送初始帧
            preTime = curTime;
            doFrameWithFixFrameRate(data.clone());
        } else {
            if (curTime - preTime > MILLS_SCHEDULE) {
                preTime = curTime;
                doFrameWithFixFrameRate(data.clone());
            }
            /**
             * 如果当前MILLS_SCHEDULE内还有其他帧，则不处理（丢弃），因为该MILLS_SCHEDULE已经拿到一帧
             */
        }

        doFrameWithAll(data);
    }

    /**
     * 该方法会把固定帧率的帧给调用者
     * @param data
     */
    abstract protected void doFrameWithFixFrameRate(byte[] data);

    /**
     * 该方法会把所有帧都给调用者
     * @param data
     */
    abstract protected void doFrameWithAll(byte[] data);
}
