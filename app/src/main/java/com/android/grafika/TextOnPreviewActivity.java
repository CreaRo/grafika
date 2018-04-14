package com.android.grafika;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
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
import com.android.grafika.gles.LutProgram;
import com.android.grafika.gles.Sprite3d;
import com.android.grafika.gles.WindowSurface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;

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
public class TextOnPreviewActivity extends Activity implements
        SurfaceHolder.Callback,
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = MainActivity.TAG;
    private final float[] mTexMatrix = new float[16];
    Random random = new Random();
    private InputCamera inputCamera;
    private float[] mDisplayProjectionMatrix = new float[16];
    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private SurfaceTexture mCameraTexture;  // receives the output from the camera preview
    private Sprite3d mVideoSprite;
    private LutProgram mLutProgram;
    private Camera mCamera;
    private MainHandler mHandler;
    private SurfaceView displaySurfaceView;
    private int lutTextureId;
    private int[] colors = new int[]{
            Color.BLUE,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
    };
    private float[] jet = {
            0.000000f, 0.000000f, 127.500000f,
            0.000000f, 0.000000f, 131.500000f,
            0.000000f, 0.000000f, 135.500000f,
            0.000000f, 0.000000f, 139.500000f,
            0.000000f, 0.000000f, 143.500000f,
            0.000000f, 0.000000f, 147.500000f,
            0.000000f, 0.000000f, 151.500000f,
            0.000000f, 0.000000f, 155.500000f,
            0.000000f, 0.000000f, 159.500000f,
            0.000000f, 0.000000f, 163.500000f,
            0.000000f, 0.000000f, 167.500000f,
            0.000000f, 0.000000f, 171.500000f,
            0.000000f, 0.000000f, 175.500000f,
            0.000000f, 0.000000f, 179.500000f,
            0.000000f, 0.000000f, 183.500000f,
            0.000000f, 0.000000f, 187.500000f,
            0.000000f, 0.000000f, 191.500000f,
            0.000000f, 0.000000f, 195.500000f,
            0.000000f, 0.000000f, 199.500000f,
            0.000000f, 0.000000f, 203.500000f,
            0.000000f, 0.000000f, 207.500000f,
            0.000000f, 0.000000f, 211.500000f,
            0.000000f, 0.000000f, 215.500000f,
            0.000000f, 0.000000f, 219.500000f,
            0.000000f, 0.000000f, 223.500000f,
            0.000000f, 0.000000f, 227.500000f,
            0.000000f, 0.000000f, 231.500000f,
            0.000000f, 0.000000f, 235.500000f,
            0.000000f, 0.000000f, 239.500000f,
            0.000000f, 0.000000f, 243.500000f,
            0.000000f, 0.000000f, 247.500000f,
            0.000000f, 0.000000f, 251.500000f,
            0.000000f, 0.500000f, 255.000000f,
            0.000000f, 4.500000f, 255.000000f,
            0.000000f, 8.500000f, 255.000000f,
            0.000000f, 12.500000f, 255.000000f,
            0.000000f, 16.500000f, 255.000000f,
            0.000000f, 20.500000f, 255.000000f,
            0.000000f, 24.500000f, 255.000000f,
            0.000000f, 28.500000f, 255.000000f,
            0.000000f, 32.500000f, 255.000000f,
            0.000000f, 36.500000f, 255.000000f,
            0.000000f, 40.500000f, 255.000000f,
            0.000000f, 44.500000f, 255.000000f,
            0.000000f, 48.500000f, 255.000000f,
            0.000000f, 52.500000f, 255.000000f,
            0.000000f, 56.500000f, 255.000000f,
            0.000000f, 60.500000f, 255.000000f,
            0.000000f, 64.500000f, 255.000000f,
            0.000000f, 68.500000f, 255.000000f,
            0.000000f, 72.500000f, 255.000000f,
            0.000000f, 76.500000f, 255.000000f,
            0.000000f, 80.500000f, 255.000000f,
            0.000000f, 84.500000f, 255.000000f,
            0.000000f, 88.500000f, 255.000000f,
            0.000000f, 92.500000f, 255.000000f,
            0.000000f, 96.500000f, 255.000000f,
            0.000000f, 100.500000f, 255.000000f,
            0.000000f, 104.500000f, 255.000000f,
            0.000000f, 108.500000f, 255.000000f,
            0.000000f, 112.500000f, 255.000000f,
            0.000000f, 116.500000f, 255.000000f,
            0.000000f, 120.500000f, 255.000000f,
            0.000000f, 124.500000f, 255.000000f,
            0.000000f, 128.500000f, 255.000000f,
            0.000000f, 132.500000f, 255.000000f,
            0.000000f, 136.500000f, 255.000000f,
            0.000000f, 140.500000f, 255.000000f,
            0.000000f, 144.500000f, 255.000000f,
            0.000000f, 148.500000f, 255.000000f,
            0.000000f, 152.500000f, 255.000000f,
            0.000000f, 156.500000f, 255.000000f,
            0.000000f, 160.500000f, 255.000000f,
            0.000000f, 164.500000f, 255.000000f,
            0.000000f, 168.500000f, 255.000000f,
            0.000000f, 172.500000f, 255.000000f,
            0.000000f, 176.500000f, 255.000000f,
            0.000000f, 180.500000f, 255.000000f,
            0.000000f, 184.500000f, 255.000000f,
            0.000000f, 188.500000f, 255.000000f,
            0.000000f, 192.500000f, 255.000000f,
            0.000000f, 196.500000f, 255.000000f,
            0.000000f, 200.500000f, 255.000000f,
            0.000000f, 204.500000f, 255.000000f,
            0.000000f, 208.500000f, 255.000000f,
            0.000000f, 212.500000f, 255.000000f,
            0.000000f, 216.500000f, 255.000000f,
            0.000000f, 220.500000f, 255.000000f,
            0.000000f, 224.500000f, 255.000000f,
            0.000000f, 228.500000f, 255.000000f,
            0.000000f, 232.500000f, 255.000000f,
            0.000000f, 236.500000f, 255.000000f,
            0.000000f, 240.500000f, 255.000000f,
            0.000000f, 244.500000f, 255.000000f,
            0.000000f, 248.500000f, 255.000000f,
            0.000000f, 252.500000f, 255.000000f,
            1.500000f, 255.000000f, 253.500000f,
            5.500000f, 255.000000f, 249.500000f,
            9.500000f, 255.000000f, 245.500000f,
            13.500000f, 255.000000f, 241.500000f,
            17.500000f, 255.000000f, 237.500000f,
            21.500000f, 255.000000f, 233.500000f,
            25.500000f, 255.000000f, 229.500000f,
            29.500000f, 255.000000f, 225.500000f,
            33.500000f, 255.000000f, 221.500000f,
            37.500000f, 255.000000f, 217.500000f,
            41.500000f, 255.000000f, 213.500000f,
            45.500000f, 255.000000f, 209.500000f,
            49.500000f, 255.000000f, 205.500000f,
            53.500000f, 255.000000f, 201.500000f,
            57.500000f, 255.000000f, 197.500000f,
            61.500000f, 255.000000f, 193.500000f,
            65.500000f, 255.000000f, 189.500000f,
            69.500000f, 255.000000f, 185.500000f,
            73.500000f, 255.000000f, 181.500000f,
            77.500000f, 255.000000f, 177.500000f,
            81.500000f, 255.000000f, 173.500000f,
            85.500000f, 255.000000f, 169.500000f,
            89.500000f, 255.000000f, 165.500000f,
            93.500000f, 255.000000f, 161.500000f,
            97.500000f, 255.000000f, 157.500000f,
            101.500000f, 255.000000f, 153.500000f,
            105.500000f, 255.000000f, 149.500000f,
            109.500000f, 255.000000f, 145.500000f,
            113.500000f, 255.000000f, 141.500000f,
            117.500000f, 255.000000f, 137.500000f,
            121.500000f, 255.000000f, 133.500000f,
            125.500000f, 255.000000f, 129.500000f,
            129.500000f, 255.000000f, 125.500000f,
            133.500000f, 255.000000f, 121.500000f,
            137.500000f, 255.000000f, 117.500000f,
            141.500000f, 255.000000f, 113.500000f,
            145.500000f, 255.000000f, 109.500000f,
            149.500000f, 255.000000f, 105.500000f,
            153.500000f, 255.000000f, 101.500000f,
            157.500000f, 255.000000f, 97.500000f,
            161.500000f, 255.000000f, 93.500000f,
            165.500000f, 255.000000f, 89.500000f,
            169.500000f, 255.000000f, 85.500000f,
            173.500000f, 255.000000f, 81.500000f,
            177.500000f, 255.000000f, 77.500000f,
            181.500000f, 255.000000f, 73.500000f,
            185.500000f, 255.000000f, 69.500000f,
            189.500000f, 255.000000f, 65.500000f,
            193.500000f, 255.000000f, 61.500000f,
            197.500000f, 255.000000f, 57.500000f,
            201.500000f, 255.000000f, 53.500000f,
            205.500000f, 255.000000f, 49.500000f,
            209.500000f, 255.000000f, 45.500000f,
            213.500000f, 255.000000f, 41.500000f,
            217.500000f, 255.000000f, 37.500000f,
            221.500000f, 255.000000f, 33.500000f,
            225.500000f, 255.000000f, 29.500000f,
            229.500000f, 255.000000f, 25.500000f,
            233.500000f, 255.000000f, 21.500000f,
            237.500000f, 255.000000f, 17.500000f,
            241.500000f, 255.000000f, 13.500000f,
            245.500000f, 255.000000f, 9.500000f,
            249.500000f, 255.000000f, 5.500000f,
            253.500000f, 255.000000f, 1.500000f,
            255.000000f, 252.500000f, 0.000000f,
            255.000000f, 248.500000f, 0.000000f,
            255.000000f, 244.500000f, 0.000000f,
            255.000000f, 240.500000f, 0.000000f,
            255.000000f, 236.500000f, 0.000000f,
            255.000000f, 232.500000f, 0.000000f,
            255.000000f, 228.500000f, 0.000000f,
            255.000000f, 224.500000f, 0.000000f,
            255.000000f, 220.500000f, 0.000000f,
            255.000000f, 216.500000f, 0.000000f,
            255.000000f, 212.500000f, 0.000000f,
            255.000000f, 208.500000f, 0.000000f,
            255.000000f, 204.500000f, 0.000000f,
            255.000000f, 200.500000f, 0.000000f,
            255.000000f, 196.500000f, 0.000000f,
            255.000000f, 192.500000f, 0.000000f,
            255.000000f, 188.500000f, 0.000000f,
            255.000000f, 184.500000f, 0.000000f,
            255.000000f, 180.500000f, 0.000000f,
            255.000000f, 176.500000f, 0.000000f,
            255.000000f, 172.500000f, 0.000000f,
            255.000000f, 168.500000f, 0.000000f,
            255.000000f, 164.500000f, 0.000000f,
            255.000000f, 160.500000f, 0.000000f,
            255.000000f, 156.500000f, 0.000000f,
            255.000000f, 152.500000f, 0.000000f,
            255.000000f, 148.500000f, 0.000000f,
            255.000000f, 144.500000f, 0.000000f,
            255.000000f, 140.500000f, 0.000000f,
            255.000000f, 136.500000f, 0.000000f,
            255.000000f, 132.500000f, 0.000000f,
            255.000000f, 128.500000f, 0.000000f,
            255.000000f, 124.500000f, 0.000000f,
            255.000000f, 120.500000f, 0.000000f,
            255.000000f, 116.500000f, 0.000000f,
            255.000000f, 112.500000f, 0.000000f,
            255.000000f, 108.500000f, 0.000000f,
            255.000000f, 104.500000f, 0.000000f,
            255.000000f, 100.500000f, 0.000000f,
            255.000000f, 96.500000f, 0.000000f,
            255.000000f, 92.500000f, 0.000000f,
            255.000000f, 88.500000f, 0.000000f,
            255.000000f, 84.500000f, 0.000000f,
            255.000000f, 80.500000f, 0.000000f,
            255.000000f, 76.500000f, 0.000000f,
            255.000000f, 72.500000f, 0.000000f,
            255.000000f, 68.500000f, 0.000000f,
            255.000000f, 64.500000f, 0.000000f,
            255.000000f, 60.500000f, 0.000000f,
            255.000000f, 56.500000f, 0.000000f,
            255.000000f, 52.500000f, 0.000000f,
            255.000000f, 48.500000f, 0.000000f,
            255.000000f, 44.500000f, 0.000000f,
            255.000000f, 40.500000f, 0.000000f,
            255.000000f, 36.500000f, 0.000000f,
            255.000000f, 32.500000f, 0.000000f,
            255.000000f, 28.500000f, 0.000000f,
            255.000000f, 24.500000f, 0.000000f,
            255.000000f, 20.500000f, 0.000000f,
            255.000000f, 16.500000f, 0.000000f,
            255.000000f, 12.500000f, 0.000000f,
            255.000000f, 8.500000f, 0.000000f,
            255.000000f, 4.500000f, 0.000000f,
            255.000000f, 0.500000f, 0.000000f,
            251.500000f, 0.000000f, 0.000000f,
            247.500000f, 0.000000f, 0.000000f,
            243.500000f, 0.000000f, 0.000000f,
            239.500000f, 0.000000f, 0.000000f,
            235.500000f, 0.000000f, 0.000000f,
            231.500000f, 0.000000f, 0.000000f,
            227.500000f, 0.000000f, 0.000000f,
            223.500000f, 0.000000f, 0.000000f,
            219.500000f, 0.000000f, 0.000000f,
            215.500000f, 0.000000f, 0.000000f,
            211.500000f, 0.000000f, 0.000000f,
            207.500000f, 0.000000f, 0.000000f,
            203.500000f, 0.000000f, 0.000000f,
            199.500000f, 0.000000f, 0.000000f,
            195.500000f, 0.000000f, 0.000000f,
            191.500000f, 0.000000f, 0.000000f,
            187.500000f, 0.000000f, 0.000000f,
            183.500000f, 0.000000f, 0.000000f,
            179.500000f, 0.000000f, 0.000000f,
            175.500000f, 0.000000f, 0.000000f,
            171.500000f, 0.000000f, 0.000000f,
            167.500000f, 0.000000f, 0.000000f,
            163.500000f, 0.000000f, 0.000000f,
            159.500000f, 0.000000f, 0.000000f,
            155.500000f, 0.000000f, 0.000000f,
            151.500000f, 0.000000f, 0.000000f,
            147.500000f, 0.000000f, 0.000000f,
            143.500000f, 0.000000f, 0.000000f,
            139.500000f, 0.000000f, 0.000000f,
            135.500000f, 0.000000f, 0.000000f,
            131.500000f, 0.000000f, 0.000000f,
            127.500000f, 0.000000f, 0.000000f
    };

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

        // Give the camera a hint that we're recording video. This can have a big
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
        mLutProgram = new LutProgram();
        int videoTextureId = mLutProgram.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mVideoSprite.setTextureId(videoTextureId);

        mVideoSprite.transform(new Sprite3d.Transformer()
                .reset()
                .translate(0, 0, 0)
                .rotateAroundZ(0)
                .scale(1, 1, 1)
                .build());

        setLut();

        mCameraTexture = new SurfaceTexture(videoTextureId);
        mCameraTexture.setOnFrameAvailableListener(this);

        try {
            mCamera.setPreviewTexture(mCameraTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    private void setLut() {
        lutTextureId = mLutProgram.createTextureObject(GLES20.GL_TEXTURE_2D);

        int[] colorArray = new int[256];
        byte[] data = new byte[4 * colorArray.length];
        for (int i = 0; i < colorArray.length; i++) {
            colorArray[i] = getRandomColor(i);
            data[i * 4 + 0] = byteToInt(colorArray[i], 16);
            data[i * 4 + 1] = byteToInt(colorArray[i], 8);
            data[i * 4 + 2] = byteToInt(colorArray[i], 0);
            data[i * 4 + 3] = byteToInt(colorArray[i], 24);
        }

        /*for (int i = 0; i < data.length; i++) {
            if (i < 256) data[i] = 0x44; // red
            else if (i < 2 * 256) data[i] = 0x44; // green
            else if (i < 3 * 256) data[i] = 0x44; // blue
            else if (i < 4 * 256) data[i] = 0x79; // alpha
        }*/
        mLutProgram.setLutTexture(data, lutTextureId);
    }

    private int getRandomColor(int index) {
        return makeColor(((int) jet[3 * index]), ((int) jet[3 * index + 1]), ((int) jet[3 * index + 2]));
    }

    private int makeColor(int v0, int v1, int v2) {
        return (v0 << 16) | (v1 << 8) | (v2);
    }

    private byte byteToInt(int i, int j) {
        return (byte) (i >> j);
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

        mLutProgram.clearScreen();
        mVideoSprite.draw(mLutProgram, mDisplayProjectionMatrix, mTexMatrix, lutTextureId);

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
