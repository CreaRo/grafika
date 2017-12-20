package com.android.grafika.camera;

import android.hardware.Camera;

import java.util.Arrays;

/**
 * Created by rish on 7/7/17.
 */

public class InputCamera {

    private static final String TAG = InputCamera.class.getSimpleName();
    private static final int[] VALID_PREVIEW_ORIENTATION = new int[]{0, 90, 180, 270};

    private int cameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT; /*one of {CAMERA_FACING_FRONT, CAMERA_FACING_BACK}*/
    private float cameraWidth = 640, cameraHeight = 480; /*this is camera's input - almost always; width > height*/
    private float displayWidth = 640, displayHeight = 480; /*this depends on display orientation*/
    private float cameraAspect = cameraWidth / cameraHeight;
    private float displayAspect = displayWidth / displayHeight;
    private float fps = 30;
    private int displayOrientation = 0; /* display to camera orientation; one of {0,90,180,270}*/

    private InputCamera() {
    }

    public static InputCamera useFrontCamera() {
        return new InputCamera.Builder()
                .videoWidth(640)
                .videoHeight(480)
                .fps(30)
                .orientation(0)
                .cameraIndex(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .build();
    }

    public static InputCamera useBackCamera() {
        return new InputCamera.Builder()
                .videoWidth(640)
                .videoHeight(480)
                .fps(30)
                .orientation(0)
                .cameraIndex(Camera.CameraInfo.CAMERA_FACING_BACK)
                .build();
    }

    public float getCameraHeight() {
        return cameraHeight;
    }

    private void setCameraHeight(float cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    public float getCameraWidth() {
        return cameraWidth;
    }

    private void setCameraWidth(float cameraWidth) {
        this.cameraWidth = cameraWidth;
    }

    public float getCameraAspect() {
        return cameraAspect;
    }

    private void setCameraAspect(float cameraAspect) {
        this.cameraAspect = cameraAspect;
    }

    public float getFps() {
        return fps;
    }

    private void setFps(float fps) {
        this.fps = fps;
    }

    public int getDisplayOrientation() {
        return displayOrientation;
    }

    private void setDisplayOrientation(int displayOrientation) {
        this.displayOrientation = -1;
        for (int validOrientation : VALID_PREVIEW_ORIENTATION) {
            if (displayOrientation == validOrientation) {
                this.displayOrientation = displayOrientation;
            }
        }
        if (this.displayOrientation == -1) {
            throw new RuntimeException("Preview Orientation must be one of " + Arrays.toString(VALID_PREVIEW_ORIENTATION));
        }
    }

    public int getCameraIndex() {
        return cameraIndex;
    }

    private void setCameraIndex(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    public float getDisplayHeight() {
        return displayHeight;
    }

    private void setDisplayHeight(float displayHeight) {
        this.displayHeight = displayHeight;
    }

    public float getDisplayWidth() {
        return displayWidth;
    }

    private void setDisplayWidth(float displayWidth) {
        this.displayWidth = displayWidth;
    }

    public float getDisplayAspect() {
        return displayAspect;
    }

    private void setDisplayAspect(float displayAspect) {
        this.displayAspect = displayAspect;
    }

    private static class Builder {

        private InputCamera camera;

        private Builder() {
            camera = new InputCamera();
        }

        private Builder fps(float fps) {
            camera.setFps(fps);
            return this;
        }

        private Builder videoHeight(float videoHeight) {
            camera.setCameraHeight(videoHeight);
            return this;
        }

        private Builder videoWidth(float videoWidth) {
            camera.setCameraWidth(videoWidth);
            return this;
        }

        private Builder orientation(int orientation) {
            if (orientation != 0 && orientation != 90 && orientation != 180 && orientation != 270) {
                throw new RuntimeException("Orientation value must be in {0,90,180,270}");
            }
            camera.setDisplayOrientation(orientation);
            return this;
        }

        private Builder cameraIndex(int index) {
            if (index != Camera.CameraInfo.CAMERA_FACING_BACK
                    && index != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                throw new RuntimeException("Index value must be in {0,1}");
            }
            camera.setCameraIndex(index);
            return this;
        }

        private InputCamera build() {
            /*calculate dependent params*/
            camera.setCameraAspect(camera.getCameraWidth() / camera.getCameraHeight());

            /*set display dimensions based on orientation*/
            if (camera.getDisplayOrientation() == 90 || camera.getDisplayOrientation() == 270) {
                camera.setDisplayWidth(camera.getCameraHeight());
                camera.setDisplayHeight(camera.getCameraWidth());
            } else {
                camera.setDisplayWidth(camera.getCameraWidth());
                camera.setDisplayHeight(camera.getCameraHeight());
            }
            camera.setDisplayAspect(camera.getDisplayWidth() / camera.getDisplayHeight());

            return camera;
        }


    }
}

