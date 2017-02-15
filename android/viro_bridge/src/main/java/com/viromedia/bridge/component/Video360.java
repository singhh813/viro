/**
 * Copyright © 2016 Viro Media. All rights reserved.
 */
package com.viromedia.bridge.component;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.viro.renderer.jni.RenderContextJni;
import com.viro.renderer.jni.VideoTextureJni;
import com.viromedia.bridge.component.node.Scene;
import com.viromedia.bridge.utility.ViroEvents;

/**
 * Contains a VideoTexture that is set as a background video on the scene.
 */
public class Video360 extends Component {
    private static final float[] sDefaultRotation = {0, 0, 0};

    private String mSource;
    private boolean mPaused = false;
    private boolean mLoop = false;
    private boolean mMuted = false;
    private float mVolume = 1;
    private float[] mRotation;

    private VideoTextureJni mVideoTextureJni = null;

    public Video360(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    protected void onTearDown() {
        if (mVideoTextureJni != null){
            mVideoTextureJni.delete();
            mVideoTextureJni = null;
        }
        super.onTearDown();
    }

    private void resetVideo() {
        if (mRenderContext == null || mSource == null) {
            return;
        }

        if (mVideoTextureJni != null) {
            mVideoTextureJni.delete();
            mVideoTextureJni = null;
        }

        // Create Texture
        mVideoTextureJni = new VideoTextureJni();
        mVideoTextureJni.setVideoDelegate(new VideoTextureJni.VideoDelegate() {
            @Override
            public void onVideoFinish() {
                if (isTornDown()){
                    return;
                }

                reactVideoFinishedCallback();
            }

            @Override
            public void onReady() {
                if (isTornDown()){
                    return;
                }

                updateVideoTexture();
                mVideoTextureJni.loadSource(mSource, mRenderContext);
                setLoop(mLoop);
                setMuted(mMuted);
                setVolume(mVolume);
                setPaused(mPaused);
            }
        });

        if (mScene != null) {
            updateVideoTexture();
            mScene.setBackgroundRotation(mRotation);
        }
    }

    @Override
    public void setRenderContext(RenderContextJni context) {
        super.setRenderContext(context);
        resetVideo();
    }

    @Override
    public void setScene(Scene scene){
        super.setScene(scene);
        updateVideoTexture();
        mScene.setBackgroundRotation(mRotation);
    }

    private void updateVideoTexture(){
        if (mScene != null && mVideoTextureJni != null && mVideoTextureJni.isReady()) {
            mScene.setBackgroundVideoTexture(mVideoTextureJni);
        }
    }

    public void setSource(String source) {
        mSource = source;
        resetVideo();
    }

    public void setPaused(boolean paused) {
        mPaused = paused;

        if (mVideoTextureJni == null) {
            return;
        }

        if (mPaused) {
            mVideoTextureJni.pause();
        } else {
            mVideoTextureJni.play();
        }
    }

    public void setLoop(boolean loop) {
        mLoop = loop;
        if (mVideoTextureJni != null) {
            mVideoTextureJni.setLoop(loop);
        }
    }

    public void setMuted(boolean muted) {
        mMuted = muted;
        if (mVideoTextureJni != null) {
            mVideoTextureJni.setMuted(muted);
        }
    }

    public void setVolume(float volume) {
        mVolume = volume;
        if (mVideoTextureJni != null) {
            mVideoTextureJni.setVolume(volume);
        }
    }

    public void seekToTime(int time) {
        if (mVideoTextureJni != null) {
            mVideoTextureJni.seekToTime(time);
        }
    }

    public void setRotation(ReadableArray rotation) {
        if (rotation == null) {
            mRotation = sDefaultRotation;
        } else {
            float[] rotationArr = {(float) rotation.getDouble(0),
                    (float) rotation.getDouble(1), (float) rotation.getDouble(2)};
            mRotation = rotationArr;
        }
        if (mScene != null) {
            mScene.setBackgroundRotation(mRotation);
        }
    }

    private void reactVideoFinishedCallback() {
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                ViroEvents.ON_FINISH,
                null);
    }
}
