package com.lqy.usb;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.lqy.libusbcameramanager.usb.DefaultFrameRateCallback;
import com.lqy.libusbcameramanager.usb.UsbCamera;
import com.serenegiant.usb.IFrameCallback;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

/**
 * Created by lqy on 2018/7/4.
 */

public class NoViewUsbCamera extends UsbCamera {

    private SurfaceTexture mSurfaceTexture;
    private int mProductId;
    private String mProductName;
    private int mVendorId;
    private int mFrameFormat;
    private int mMinFps;
    private int mMaxFps;

    private IRecorderEngine mRecorderEngine;

    // test fps
    private long time1 = 0;
    private int count1 = 0;
    private long time2 = 0;
    private int count2 = 0;
    private final boolean fpsDebug = false;

    NoViewUsbCamera(Builder builder) {
        mProductId = builder.mProductId;
        mProductName = builder.mProductName;
        mVendorId = builder.mVendorId;
        mFrameFormat = builder.mFrameFormat;
        mMinFps = builder.mMinFps;
        mMaxFps = builder.mMaxFps;
        mRecorderEngine = builder.mRecorderEngine;
        setResolution(builder.mWidth, builder.mHeight, builder.isAutoAdapt);
    }

    @Override
    public int getProductId() {
        return mProductId;
    }

    @Override
    public String getProductName() {
        return mProductName;
    }

    @Override
    public int getVendorId() {
        return mVendorId;
    }

    @Override
    protected int getFrameFormat() {
        return mFrameFormat;
    }

    @Override
    protected int getMinFps() {
        return mMinFps;
    }

    @Override
    protected int getMaxFps() {
        return mMaxFps;
    }

    @Override
    protected Surface getSurface() {
        createSurface();
        return new Surface(mSurfaceTexture);
    }

    @Override
    protected void onConnect() {
        openCamera();
    }

    @Override
    protected void onDisconnect() {
        // TODO: 2018/7/5
    }

    @Override
    protected void onFinish() {
        closeEngine();
    }

    @Override
    protected void onCloseCamera() {
        if(mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    @Override
    protected void onOpenCamera() {
        openEngine();
    }

    @Override
    protected IFrameCallback getFrameCallback() {
        return new DefaultFrameRateCallback() {
            @Override
            protected void doFrameWithFixFrameRate(byte[] data) {
                if (mRecorderEngine != null) {
                    mRecorderEngine.handleVideoData(data);
                }

                if (fpsDebug) {
                    long curTime = System.currentTimeMillis();
                    if (time1 == 0) {
                        time1 = curTime;
                    } else {
                        if (curTime - time1 >= 1000) {
                            Log.d("test", "fix -> " + count1);
                            time1 = curTime;
                            count1 = 0;
                        } else {
                            count1++;
                        }
                    }
                }

            }

            @Override
            protected void doFrameWithAll(byte[] data) {
                if (mRecorderEngine != null && mRecorderEngine.isLive()) {
                    mRecorderEngine.handleLiveVideoData(data);
                }

                if (fpsDebug) {
                    long curTime = System.currentTimeMillis();
                    if (time2 == 0) {
                        time2 = curTime;
                    } else {
                        if (curTime - time2 >= 1000) {
                            Log.d("test", "all -> " + count2);
                            time2 = curTime;
                            count2 = 0;
                        } else {
                            count2++;
                        }
                    }
                }
            }
        };
    }

    private void createSurface() {
        int[] textureHandles = new int[1];
        int textureId;
        GLES20.glGenTextures(1, textureHandles, 0);
        textureId = textureHandles[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
        mSurfaceTexture = new SurfaceTexture(textureId);
    }

    private void openEngine() {
        mRecorderEngine.setSize(mWidth, mHeight);
        mRecorderEngine.startRecording();
    }

    private void closeEngine() {
        if (mRecorderEngine != null) {
            mRecorderEngine.stopRecording();
//            mRecorderEngine = null;
        }
    }

    public static class Builder {
        private int mProductId = -1;
        private String mProductName = null;
        private int mVendorId = -1;
        private int mFrameFormat = -1;
        private int mMinFps = -1;
        private int mMaxFps = -1;
        private IRecorderEngine mRecorderEngine = null;

        private int mWidth, mHeight;
        private boolean isAutoAdapt = true;

        public Builder setProductId(int productId) {
            mProductId = productId;
            return this;
        }

        public Builder setProductName(String productName) {
            mProductName = productName;
            return this;
        }

        public Builder setVendorId(int vendorId) {
            mVendorId = vendorId;
            return this;
        }

        public Builder setFrameFormat(int frameFormat) {
            mFrameFormat = frameFormat;
            return this;
        }

        public Builder setMinFps(int minFps) {
            mMinFps = minFps;
            return this;
        }

        public Builder setMaxFps(int maxFps) {
            mMaxFps = maxFps;
            return this;
        }

        public Builder setRecorderEngine(IRecorderEngine recorderEngine) {
            mRecorderEngine = recorderEngine;
            return this;
        }

        public Builder setWidth(int width) {
            mWidth = width;
            return this;
        }

        public Builder setHeight(int height) {
            mHeight = height;
            return this;
        }

        public Builder setAutoAdaptSize(boolean autoAdapt) {
            isAutoAdapt = autoAdapt;
            return this;
        }

        public NoViewUsbCamera build() {
            if (mProductId == -1) {
                throw new IllegalStateException("productId not set");
            }

            if (TextUtils.isEmpty(mProductName)) {
                throw new IllegalStateException("productName not set");
            }

            if (mVendorId == -1) {
                throw new IllegalStateException("vendorId not set");
            }

            if (mFrameFormat == -1) {
                throw new IllegalStateException("frameFormat not set");
            }

            if (mMinFps == -1) {
                throw new IllegalStateException("minFps not set");
            }

            if (mMaxFps == -1) {
                throw new IllegalStateException("maxFps not set");
            }

            if (mRecorderEngine == null) {
                throw new IllegalStateException("recorderEngine not set");
            }

            if (mWidth == -1) {
                throw new IllegalStateException("width not set");
            }

            if (mHeight == -1) {
                throw new IllegalStateException("height not set");
            }

            return new NoViewUsbCamera(this);
        }
    }
}
