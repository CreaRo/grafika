package com.android.grafika;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.grafika.camera.InputCamera;
import com.android.grafika.gles.Drawable2d;
import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.Sprite3d;
import com.android.grafika.gles.Texture2dProgram;
import com.android.grafika.gles.WindowSurface;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Demonstrates capturing video into a ring buffer.  When the "capture" button is clicked,
 * the buffered video is saved.
 * <p>
 * Capturing and storing raw frames would be slow and require lots of memory.  Instead, we
 * feed the frames into the video encoder and buffer the output.
 * <p>
 * Whenever we receive a new frame from the camera, our SurfaceTexture callback gets
 * notified.  That can happen on an arbitrary thread, so we use it to send a message
 * through our Handler.  That causes us to render the new frame to the display and to
 * our video encoder.
 */
public class TextOnPreviewActivity extends Activity implements SurfaceHolder.Callback,
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = MainActivity.TAG;
    private final float[] mTexMatrix = new float[16];
    private InputCamera inputCamera;
    private float[] mDisplayProjectionMatrix = new float[16];

    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private SurfaceTexture mCameraTexture;  // receives the output from the camera preview
    private Sprite3d mVideoSprite;
    private Texture2dProgram mProgram;
    private int mTextureId;

    private Camera mCamera;

    private MainHandler mHandler;

    private SurfaceView displaySurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_on_preview);

        inputCamera = InputCamera.useBackCamera();

        displaySurfaceView = (SurfaceView) findViewById(R.id.text_on_preview_surfaceview);
        SurfaceHolder sh = displaySurfaceView.getHolder();
        sh.addCallback(this);

        mHandler = new MainHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera(inputCamera.getCameraWidth(), inputCamera.getCameraHeight(),
                inputCamera.getFps(), inputCamera.getDisplayOrientation());
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();

        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mVideoSprite != null) {
            mVideoSprite = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        Log.d(TAG, "onPause() done");
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewFps to the expected frame rate (which might actually be variable).
     */
    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps, int orientation) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }
        if (orientation != 0 && orientation != 90 && orientation != 180 && orientation != 270) {
            throw new RuntimeException("Orientation values must be in {0,90,180,270}");
        }

        mCamera = Camera.open(inputCamera.getCameraIndex());
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Try to set the frame rate to a constant value.
        float cameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms,
                desiredFps * 1000);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        mCamera.setDisplayOrientation(orientation);
        mCamera.setParameters(parms);

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (cameraPreviewThousandFps / 1000.0f) + "fps";
        Log.i(TAG, "Camera config: " + previewFacts);
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    @Override   // SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated holder=" + holder);

        // Set up everything that requires an EGL context.
        //
        // We had to wait until we had a surface because you can't make an EGL context current
        // without one, and creating a temporary 1x1 pbuffer is a waste of time.
        //
        // The display surface that we use for the SurfaceView, and the encoder surface we
        // use for video, use the same EGL context.
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
        mDisplaySurface = new WindowSurface(mEglCore, holder.getSurface(), false);
        mDisplaySurface.makeCurrent();

        float screenAspect = ((float) mDisplaySurface.getWidth()) / mDisplaySurface.getHeight();

        float near = -1.0f, far = 1.0f,
                right = inputCamera.getDisplayWidth() / 2,
                top = right / screenAspect;

        Matrix.orthoM(mDisplayProjectionMatrix, 0,
                -right, right,
                -top, top,
                near, far);

        mVideoSprite = new Sprite3d(new Drawable2d(inputCamera.getDisplayWidth(),
                inputCamera.getDisplayHeight()));
        mProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
        mTextureId = mProgram.createTextureObject();
        mVideoSprite.setTextureId(mTextureId);

        mVideoSprite.transform(new Sprite3d.Transformer()
                .reset()
                .translate(0, 0, 0)
                .rotateAroundZ(0)
                .scale(1, 1, 1)
                .build());

        mCameraTexture = new SurfaceTexture(mTextureId);
        mCameraTexture.setOnFrameAvailableListener(this);

        try {
            mCamera.setPreviewTexture(mCameraTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    @Override   // SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);
    }

    @Override   // SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed holder=" + holder);
    }

    @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.d(TAG, "frame available");
        mHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
    }

    /**
     * Draws a frame onto the SurfaceView and the encoder surface.
     * <p>
     * This will be called whenever we get a new preview frame from the camera.  This runs
     * on the UI thread, which ordinarily isn't a great idea -- you really want heavy work
     * to be on a different thread -- but we're really just throwing a few things at the GPU.
     * The upside is that we don't have to worry about managing state changes between threads.
     * <p>
     * If there was a pending frame available notification when we shut down, we might get
     * here after onPause().
     */
    private void drawFrame() {
        //Log.d(TAG, "drawFrame");
        if (mEglCore == null) {
            Log.d(TAG, "Skipping drawFrame after shutdown");
            return;
        }

        // Latch the next frame from the camera.
        mDisplaySurface.makeCurrent();
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTexMatrix);

        GLES20.glViewport(0, 0, displaySurfaceView.getWidth(), displaySurfaceView.getHeight());
        mVideoSprite.draw(mProgram, mDisplayProjectionMatrix, mTexMatrix);
        mDisplaySurface.swapBuffers();
    }

    /**
     * Custom message handler for main UI thread.
     * <p>
     * Used to handle camera preview "frame available" notifications
     */
    private static class MainHandler extends Handler {
        private static final int MSG_FRAME_AVAILABLE = 1;

        private WeakReference<TextOnPreviewActivity> mWeakActivity;

        private MainHandler(TextOnPreviewActivity activity) {
            mWeakActivity = new WeakReference<TextOnPreviewActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TextOnPreviewActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.d(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_FRAME_AVAILABLE: {
                    activity.drawFrame();
                    break;
                }
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }
}
