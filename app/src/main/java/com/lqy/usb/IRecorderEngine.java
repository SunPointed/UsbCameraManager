package com.lqy.usb;

/**
 * Created by lqy on 2018/6/13.
 */

public interface IRecorderEngine {
    void startRecording();

    void stopRecording();

    void handleAudioData(byte[] audio);

    void handleVideoData(byte[] video);

    void handleLiveVideoData(byte[] video);

    void setSize(int width, int height);

    boolean isLive();
}
