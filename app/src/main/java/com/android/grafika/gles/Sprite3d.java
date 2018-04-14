package com.android.grafika.gles;

import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by rish on 2/11/17.
 */

public class Sprite3d {

    private static final String TAG = Sprite3d.class.getSimpleName();
    private float[] modelMatrix;
    private Drawable2d drawable;
    private int textureId;
    private float[] scratchMatrix = new float[32];

    public Sprite3d(Drawable2d drawable2d) {
        drawable = drawable2d;
        textureId = -1;

        modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
    }

    public void resetModelViewMatrix() {
        Matrix.setIdentityM(modelMatrix, 0);
    }

    public void transform(float[] transformationMatrix) {
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix.clone(), 0, transformationMatrix, 0);
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    public void draw(Texture2dProgram program, float[] projectionMatrix, float[] texMatrix) {
        Matrix.multiplyMM(scratchMatrix, 0, projectionMatrix, 0, modelMatrix, 0);
        program.draw(scratchMatrix, drawable.getVertexArray(), 0,
                drawable.getVertexCount(), drawable.getCoordsPerVertex(),
                drawable.getVertexStride(), texMatrix, drawable.getTexCoordArray(),
                textureId, drawable.getTexCoordStride());

    }

    public void draw(LutProgram program, float[] projectionMatrix, float[] texMatrix,
                     int lutTextureId) {
        Matrix.multiplyMM(scratchMatrix, 0, projectionMatrix, 0, modelMatrix, 0);
        program.draw(scratchMatrix, drawable.getVertexArray(), 0,
                drawable.getVertexCount(), drawable.getCoordsPerVertex(),
                drawable.getVertexStride(), texMatrix, drawable.getTexCoordArray(),
                textureId, drawable.getTexCoordStride(), lutTextureId);

    }

    /*following work only if order of rotation multiplication is ZYX*/

    public float getRotationX() {
        return (float) Math.atan2(modelMatrix[6], modelMatrix[10]);
    }

    public float getRotationY() {
        float cosY = (float) Math.sqrt(Math.pow(modelMatrix[6], 2) + Math.pow(modelMatrix[10], 2));
        return (float) Math.atan2(-modelMatrix[2], cosY);
    }

    public float getRotationZ() {
        return (float) Math.atan2(modelMatrix[1], modelMatrix[0]);
    }

    public String printModelMatrix() {
        return t(modelMatrix[0]) + ", " + t(modelMatrix[4]) + ", " + t(modelMatrix[8]) + ", " + t(modelMatrix[12])
                + "\n" + t(modelMatrix[1]) + ", " + t(modelMatrix[5]) + ", " + t(modelMatrix[9]) + ", " + t(modelMatrix[13])
                + "\n" + t(modelMatrix[2]) + ", " + t(modelMatrix[6]) + ", " + t(modelMatrix[10]) + ", " + t(modelMatrix[14])
                + "\n" + t(modelMatrix[3]) + ", " + t(modelMatrix[7]) + ", " + t(modelMatrix[11]) + ", " + t(modelMatrix[15]);
    }

    public float getX() {
        return modelMatrix[12];
    }

    public float getY() {
        return modelMatrix[13];
    }

    public float getZ() {
        return modelMatrix[14];
    }

    private float t(float f) {
        return Math.round(f * 100.0f) / 100.f;
    }

    public static class Transformer {

        private float rotationMatrix[] = new float[16];

        public Transformer() {
            Matrix.setIdentityM(rotationMatrix, 0);
        }

        public Transformer translate(float x, float y, float z) {
            Matrix.translateM(rotationMatrix, 0, x, y, z);
            return this;
        }

        public Transformer scale(float x, float y, float z) {
            Matrix.scaleM(rotationMatrix, 0, x, y, z);
            return this;
        }

        /*in degrees*/
        public Transformer rotateAroundX(float angle) {
            Matrix.rotateM(rotationMatrix, 0, angle, 1, 0, 0);
            return this;
        }

        /*in degrees*/
        public Transformer rotateAroundY(float angle) {
            Matrix.rotateM(rotationMatrix, 0, angle, 0, 1, 0);
            return this;
        }

        /*in degrees*/
        public Transformer rotateAroundZ(float angle) {
            Matrix.rotateM(rotationMatrix, 0, angle, 0, 0, 1);
            return this;
        }

        public float[] build() {
            return rotationMatrix;
        }

        public Transformer reset() {
            Matrix.setIdentityM(rotationMatrix, 0);
            return this;
        }

        public Transformer print() {
            String s = t(rotationMatrix[0]) + ", " + t(rotationMatrix[4]) + ", " + t(rotationMatrix[8]) + ", " + t(rotationMatrix[12])
                    + "\n" + t(rotationMatrix[1]) + ", " + t(rotationMatrix[5]) + ", " + t(rotationMatrix[9]) + ", " + t(rotationMatrix[13])
                    + "\n" + t(rotationMatrix[2]) + ", " + t(rotationMatrix[6]) + ", " + t(rotationMatrix[10]) + ", " + t(rotationMatrix[14])
                    + "\n" + t(rotationMatrix[3]) + ", " + t(rotationMatrix[7]) + ", " + t(rotationMatrix[11]) + ", " + t(rotationMatrix[15]);
            Log.d(TAG, s);
            return this;
        }

        private float t(float f) {
            return Math.round(f * 100.0f) / 100.f;
        }

    }
}

