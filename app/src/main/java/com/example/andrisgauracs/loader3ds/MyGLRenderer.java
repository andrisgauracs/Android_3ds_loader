package com.example.andrisgauracs.loader3ds;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

/**
 * Created by andrisgauracs on 21/10/2016.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final Context mActivityContext;

    /**
     * Model view projection matrices for 3d transforms
     */
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private float[] mLightModelMatrix = new float[16];
    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    /** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];
    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private final float[] mLightPosInEyeSpace = new float[4];
    /** This will be used to pass in the light position. */
    int mLightPosHandle;

    private Parser3ds[] model;

    private int currentObject = 0;

    // These still work without volatile, but refreshes are not guaranteed to happen.
    public volatile float mDeltaX;
    public volatile float mDeltaY;

    int mMVPMatrixHandle;
    int mMVMatrixHandle;
    /** This will be used to pass in model texture coordinate information. */
    int mTextureCoordinateHandle;
    int mTextureUniformHandle;

    private int mProgramHandle;
    int mPositionHandle;
    int mNormalHandle;

    /** This is a handle to our light point program. */
    private int mPointProgramHandle;

    /** Store the accumulated rotation. */
    private final float[] mAccumulatedRotation = new float[16];

    /** Store the current rotation. */
    private final float[] mCurrentRotation = new float[16];

    /** A temporary matrix. */
    private float[] mTemporaryMatrix = new float[16];


    public MyGLRenderer(final Context activityContext) {

        //We need to pass the Main activity context
        mActivityContext = activityContext;

    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        /**
         * @param model All the 3ds models from the "raw" resource folder are loaded in this array
         */
        model = new Parser3ds[]{
                //This is passed with a default constructor
                new Parser3ds(mActivityContext.getResources().openRawResource(R.raw.crate),mActivityContext),
                //In this case, the 3rd parameter "red_car" is the name of the texture, since a default texture was not provided in the original 3ds file
                new Parser3ds(mActivityContext.getResources().openRawResource(R.raw.car),mActivityContext,"red_car"),
                new Parser3ds(mActivityContext.getResources().openRawResource(R.raw.fighter),mActivityContext,"fighter"),
                new Parser3ds(mActivityContext.getResources().openRawResource(R.raw.landlord),mActivityContext),
                new Parser3ds(mActivityContext.getResources().openRawResource(R.raw.mobile_nokia),mActivityContext),
        };

        //This will be used for touch rotation
        Matrix.setIdentityM(mAccumulatedRotation, 0);

        //Read the default shaders
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.vertex_shader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.fragment_shader);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position", "a_TexCoordinate","a_Normal"});

        // Define a simple shader program for our point.
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[] {"a_Position"});

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

    }



    @Override
    public void onDrawFrame(GL10 unused) {

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 2.0f, -2.0f);
        //Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 3.5f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);


        //Set the model matrix for our object
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -4.5f);


        // Set a matrix that contains the current rotation.
        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f);
        mDeltaX = 0.0f;
        mDeltaY = 0.0f;

        // Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

        // Rotate the object taking the overall rotation into account.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

        //These are the matrices, that we need to pass to our 3ds object for computation
        float[][] mvp_matrices = new float[][]{mModelMatrix,mViewMatrix,mProjectionMatrix,mTemporaryMatrix,mMVPMatrix};

        /**
         * The matrices are set, and now we can execute the draw function. This draws the current 3D model.
         * @see The draw function of the Parser3ds class, for more info on the input parameters
         */
        model[currentObject].draw(mvp_matrices,mMVPMatrixHandle,mLightPosHandle,mLightPosInEyeSpace,mPositionHandle,mNormalHandle,mTextureCoordinateHandle,mMVMatrixHandle);

        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

    public void setCurrentObject(int pos) {
        currentObject = pos;
    }

    public void changeScale(float val) {
        model[currentObject].changeScale(val);
    }

}
