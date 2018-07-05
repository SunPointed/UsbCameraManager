package com.lqy.usbcameramanager.record;

/**
 * Created by lqy on 2018/6/13.
 */

public interface IRecorderEngine {
    void startRecording(boolean isVoiceClose, boolean isLive);

    void stopRecording(boolean needRestart);

    void restartRecording();

    void handleAudioData(byte[] audio);

    void handleVideoData(byte[] video);

    void register();

    void unregister();

    // 是否是直播
    boolean isLive();

    void setSize(int width, int height);
}
