package com.example.andrisgauracs.loader3ds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by andrisgauracs on 26/10/2016.
 */
public class Object3ds {

    private float[] vertices;
    private int[] faces;
    private float[] textureUV;
    private float[] normals;
    float[] vertexData;
    float[] textureData;
    private float  scaleFactor;
    private int numFaces;
    private int textureHandle;
    private boolean hasTexture = false;

    private FloatBuffer positionBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer normalBuffer;
    final int mBytesPerFloat = 4;

    public void setVertices(float[] inputVertices) { vertices = inputVertices; }

    public float[] getVertices() { return vertices; }

    public void setFaces(int[] inputFaces) { faces = inputFaces; }

    public void setTextures(float[] inputTextures) { textureUV = inputTextures; }

    public void setTextureHandle(int ID) { textureHandle = ID; }

    public int getTextureHandle() { return textureHandle; }

    public void resetPositionBuffer() { positionBuffer.position(0); }

    public void resetNormalBuffer() { normalBuffer.position(0); }

    public void resetTextureBuffer() { textureBuffer.position(0); }

    public FloatBuffer getPositionBuffer() { return positionBuffer; }

    public FloatBuffer getNormalBuffer() { return normalBuffer; }

    public FloatBuffer getTextureBuffer() { return textureBuffer; }

    public void setNumFaces(int nr) { numFaces = nr; }

    public void setHasTexture() { hasTexture = true; }

    public boolean hasTexture() { return hasTexture; }

    public int getNumFaces() { return numFaces; }

    public float getScaleFactor() { return scaleFactor; }

    private void setupBuffers() {

        if (textureUV == null) {
            generateUV();
        }
        int vertexIndex = 0;
        int textureIndex = 0;
        vertexData = new float[faces.length * 3];
        textureData = new float[faces.length * 2];
        for (int i = 0; i < faces.length; i++) {
            vertexData[vertexIndex++] = vertices[faces[i]*3];
            vertexData[vertexIndex++] = vertices[faces[i]*3+1];
            vertexData[vertexIndex++] = vertices[faces[i]*3+2];

            textureData[textureIndex++] = textureUV[faces[i]*2];
            textureData[textureIndex++] = -textureUV[faces[i]*2+1];
        }
        // Initialize the buffers.
        positionBuffer = ByteBuffer.allocateDirect(vertexData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        positionBuffer.put(vertexData).position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(textureData).position(0);

        normalBuffer = ByteBuffer.allocateDirect(normals.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalBuffer.put(normals).position(0);
    }

    /**
     * After all input data is recieved, we need to prepare the model for the draw function.
     */
    public void prepareModel() {
        //Since many models have different scale amplitudes, we calculate a uniform scale factor for the model
        setScaleFactor();
        //Since the 3ds file does not provide normal values, we must calculate them ourselves
        calculateNormals();
        //Finally we need to setup the buffers for drawing
        setupBuffers();
    }

    /**
     * This function is rewritten in Java from the tutorial on <a href="http://spacesimulator.net/tutorials/OpenGL_lighting_tutorial.html">this site.</a>
     * for computing surface normals
     */
    private void calculateNormals() {
        int i;
        float[] l_vect1 = new float[3];
        float[] l_vect2 = new float[3];
        float[] l_vect3 = new float[3];
        float[] l_vect_b1;
        float[] l_vect_b2;
        float[] l_normal;

        normals = new float[faces.length*3];
        int normalIndex = 0;

        for (i=0; i<faces.length; i=i+3) {

            l_vect1[0] = vertices[faces[i] * 3];
            l_vect1[1] = vertices[faces[i] * 3 + 1];
            l_vect1[2] = vertices[faces[i] * 3 + 2];
            l_vect2[0] = vertices[faces[(i + 1)] * 3];
            l_vect2[1] = vertices[faces[(i + 1)] * 3 + 1];
            l_vect2[2] = vertices[faces[(i + 1)] * 3 + 2];
            l_vect3[0] = vertices[faces[(i + 2)] * 3];
            l_vect3[1] = vertices[faces[(i + 2)] * 3 + 1];
            l_vect3[2] = vertices[faces[(i + 2)] * 3 + 2];

            l_vect_b1 = VectorCreate(l_vect1, l_vect2);
            l_vect_b2 = VectorCreate(l_vect1, l_vect3);

            l_normal = VectorDotProduct(l_vect_b1, l_vect_b2);
            l_normal = VectorNormalize(l_normal);

            normals[normalIndex++]+=l_normal[0];
            normals[normalIndex++]+=l_normal[1];
            normals[normalIndex++]+=l_normal[2];
            normals[normalIndex++]+=l_normal[0];
            normals[normalIndex++]+=l_normal[1];
            normals[normalIndex++]+=l_normal[2];
            normals[normalIndex++]+=l_normal[0];
            normals[normalIndex++]+=l_normal[1];
            normals[normalIndex++]+=l_normal[2];
        }
    }

    private void setScaleFactor() {
        float max = 0.0f;
        float min = 0.0f;
        for (int i = 0; i<vertices.length; i++) {
            if (vertices[i] > max) max = vertices[i];
            if (vertices[i] < min) min = vertices[i];
        }
        scaleFactor = 2.0f / (Math.abs(max) + Math.abs(min));
    }

    private void generateUV() {
        float max_X = 0;
        float min_X= 0;
        float max_Y = 0;
        float min_Y = 0;
        for (int i=0; i<vertices.length; i = i+3) {
            if (vertices[i] > max_X) max_X = vertices[i]; //x max value
            if (vertices[i] < min_X) min_X = vertices[i]; //x min value

            if (vertices[i+1] > max_Y) max_Y = vertices[i+1]; //x max value
            if (vertices[i+1] < min_Y) min_Y = vertices[i+1]; //x min value
        }
        float k_X = 1/(max_X - min_X);
        float k_Y = 1/(max_Y - min_Y);

        int textureIndex = 0;
        textureUV = new float[vertices.length/3*2];

        for (int i=0; i<vertices.length; i=i+3) {
            textureUV[textureIndex++] = (vertices[i] - min_X) * k_X;
            textureUV[textureIndex++] = (vertices[i+1] - min_Y) * k_Y;
        }
    }

    private float[] VectorCreate (float[] start_vector, float[] end_vector)
    {
        float[] result_vector = new float[]{
                (end_vector[0]  - start_vector[0]),
                (end_vector[1]  - start_vector[1]),
                (end_vector[2]  - start_vector[2])};
        result_vector = VectorNormalize(result_vector);
        return result_vector;
    }

    private float[] VectorNormalize(float[] p_vector)
    {
        float length;
        length = VectorLength(p_vector);
        if (length==0) length=1;
        p_vector[0] /= length;
        p_vector[1] /= length;
        p_vector[2] /= length;
        return p_vector;
    }

    private float VectorLength (float[] p_vector)
    {
        return (float)(Math.sqrt(p_vector[0] * p_vector[0] + p_vector[1] * p_vector[1] + p_vector[2] * p_vector[2]));
    }

    float[] VectorDotProduct(float[] p_vector1, float[] p_vector2)
    {
        float[] result_vector = new float[3];
        result_vector[0]=(p_vector1[1] * p_vector2[2]) - (p_vector1[2] * p_vector2[1]);
        result_vector[1]=(p_vector1[2] * p_vector2[0]) - (p_vector1[0] * p_vector2[2]);
        result_vector[2]=(p_vector1[0] * p_vector2[1]) - (p_vector1[1] * p_vector2[0]);
        return result_vector;
    }
 }
