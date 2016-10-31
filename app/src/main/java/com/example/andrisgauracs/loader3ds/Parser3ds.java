package com.example.andrisgauracs.loader3ds;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by andrisgauracs on 23/10/2016.
 */
public class Parser3ds {

    private BufferedInputStream stream;
    private int pos;
    private int limit;
    private final Context mActivityContext;
    private int defaultTextureID;
    private ArrayList<Float> scales = new ArrayList<>();
    private float scaleFactor = 0.0f;
    private float initialScaleFactor = 0.0f;

    float[] vertices;
    ArrayList<Object3ds> models = new ArrayList<Object3ds>();
    int[] faces;
    int numFaces;
    ArrayList<String[]> materials = new ArrayList<String[]>();

    private final int mPositionDataSize = 3;
    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    public boolean objReady = false;

    /**
     * This is the constructor, when a texture is specified, or no texture is specified, in which case, we use our gray "default_texture"
     * @param file - the 3ds object from "raw" resource folder
     * @param context - Main application context (needed to access resources)
     */
    public Parser3ds(InputStream file, final Context context) {
        mActivityContext = context;
        stream = new BufferedInputStream(file);
        pos = 0;
        defaultTextureID = mActivityContext.getResources().getIdentifier("default_texture", "drawable", mActivityContext.getPackageName());

        try {
            limit = readChunk();
            while (pos < limit) {
                readChunk();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Object3ds model : models) {
            if (model.getVertices() != null) {
                model.prepareModel();
                scales.add(model.getScaleFactor());
            }
        }
        for (int i =0; i < scales.size(); i++) {
            if (scales.get(i) > scaleFactor) { scaleFactor = scales.get(i); initialScaleFactor = scales.get(i); }
        }
        objReady = true;
    }

    /**
     *
     * @param file - the 3ds object from "raw" resource folder
     * @param context - Main application context (needed to access resources)
     * @param model_texture - Since the original file has not specified a texture filename, we will "force" it to use this texture
     */
    public Parser3ds(InputStream file, final Context context,String model_texture) {
        mActivityContext = context;
        stream = new BufferedInputStream(file);
        pos = 0;
        switch (model_texture) {
            case "red_car":
                defaultTextureID = mActivityContext.getResources().getIdentifier(model_texture, "drawable", mActivityContext.getPackageName());
                break;
            case "fighter":
                defaultTextureID = mActivityContext.getResources().getIdentifier(model_texture, "drawable", mActivityContext.getPackageName());
                break;
            default:
                defaultTextureID = mActivityContext.getResources().getIdentifier("default_texture", "drawable", mActivityContext.getPackageName());
                break;
        }

        //Read through the file
        try {
            limit = readChunk();
            while (pos < limit) {
                readChunk();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Object3ds model : models) {
            if (model.getVertices() != null) {
                model.prepareModel();
                //Get each object's scale factor
                scales.add(model.getScaleFactor());
            }
        }
        //We must choose one scale factor (if there is more than one object)
        for (int i =0; i < scales.size(); i++) {
            if (scales.get(i) > scaleFactor) { scaleFactor = scales.get(i); initialScaleFactor = scales.get(i); }
        }
        //Object is ready for drawing
        objReady = true;
    }

    private byte getByte() throws IOException {
        int read = stream.read();
        if (read == -1) {
            throw new EOFException();
        }
        pos++;
        return (byte) read;
    }

    public short getShort() throws IOException {
        byte b0 = getByte();
        byte b1 = getByte();
        return makeShort(b1, b0);
    }

    public int getInt() throws IOException {
        byte b0 = getByte();
        byte b1 = getByte();
        byte b2 = getByte();
        byte b3 = getByte();
        return makeInt(b3, b2, b1, b0);
    }

    public float getFloat() throws IOException {
        return Float.intBitsToFloat(getInt());
    }

    static private short makeShort(byte b1, byte b0) {
        return (short)((b1 << 8) | (b0 & 0xff));
    }

    static private int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3       ) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    public String readString() throws IOException {
        StringBuilder sb = new StringBuilder(64);
        byte ch = getByte();
        while (ch != 0) {
            sb.append((char)ch);
            ch = getByte();
        }
        return sb.toString();
    }

    public void skip(int i) throws IOException {
        int skipped = 0;
        do {
            skipped += stream.skip(i - skipped);
        } while (skipped < i);

        pos += i;
    }

    private int readChunk() throws IOException {
        short type = getShort();
        int size = getInt(); // this is probably unsigned but for convenience we use signed int
        parseChunk(type, size);
        return size;
    }

    /**
     * Most of the byte reading functions are implemented from <a href="https://github.com/kjetilos/3ds-parser">this java 3ds parser</a>
     * @param type
     * @param size
     * @throws IOException
     */
    private void parseChunk(short type, int size) throws IOException {
        switch (type) {
            case 0x3d3d: //3D Editor Chunk
                break;
            case 0x4000:
                parseObjectChunk();
                break;
            case 0x4100: // Triangular Mesh
                break;
            case 0x4110:
                parseVerticesList();
                break;
            case 0x4120:
                parseFaces();
                break;
            case 0x4130:
                parseFaceMaterial();
                break;
            case 0x4140:
                parseUVTexture();
                break;
            case 0x4d4d:
                parseMainChunk();
                break;
            case (short)0xafff: // Material block
                break;
            case (short)0xa000: // Material name
                parseTextureName();
                break;
            case (short)0xa200: // Texture map 1
                break;
            case (short)0xa300: // Mapping filename
                parseTextureFilename();
                break;
            default:
                skipChunk(type, size);
        }
    }

    private void skipChunk(int type, int size) throws IOException {
        move(size - 6); // size includes headers. header is 6 bytes
    }

    private void move(int i) throws IOException {
        skip(i);
    }

    private void parseMainChunk() throws IOException {

        Log.v("Status", "Found Main object");
    }

    private void parseObjectChunk() throws IOException {
        String name = readString();
        models.add(models.size(), new Object3ds());

    }

    private void parseVerticesList() throws IOException {
        short numVertices = getShort();
        vertices = new float[numVertices * 3];
        for (int i=0; i<vertices.length; i++) {
            //getFloat();
            vertices[i] = getFloat();
        }
        models.get(models.size()-1).setVertices(vertices);
    }

    private void parseFaces() throws IOException {
        numFaces = getShort();
        faces = new int[numFaces * 3];
        for (int i=0; i<numFaces; i++) {
            faces[i*3] = getShort();
            faces[i*3 + 1] = getShort();
            faces[i*3 + 2] = getShort();
            getShort(); // Discard face flag
        }
        models.get(models.size()-1).setFaces(faces);
        models.get(models.size()-1).setNumFaces(numFaces);
    }

    private void parseFaceMaterial() throws IOException {
        String name = readString();
        for (int i=0; i<materials.size(); i++) {
            if (materials.get(i)[0].equals(name) && materials.get(i)[1] != null) {
                int rID = mActivityContext.getResources().getIdentifier(materials.get(i)[1].toLowerCase(), "drawable", mActivityContext.getPackageName());
                if (rID != 0) {
                    models.get(models.size() - 1).setTextureHandle(TextureHelper.loadTexture(mActivityContext, rID));
                    models.get(models.size() - 1).setHasTexture();
                }
                break;
            }
        }
        if (!models.get(models.size()-1).hasTexture()) {

            models.get(models.size()-1).setTextureHandle(TextureHelper.loadTexture(mActivityContext, defaultTextureID));
        }
        int size = getShort();
        for (int i = 0; i < size; i++) {
            getByte(); getByte(); //Just skipping these chunks
        }
    }

    private void parseUVTexture() throws IOException {
        short numVertices = getShort();
        float[] uv = new float[numVertices * 2];
        for (int i=0; i<numVertices; i++) {
            uv[i*2] = getFloat();
            uv[i *2+1] = getFloat();
        }
        models.get(models.size()-1).setTextures(uv);
    }

    private void parseTextureName() throws IOException {
        String materialName = readString();
        materials.add(materials.size(), new String[2]);
        materials.get(materials.size()-1)[0] = materialName;
    }

    private void parseTextureFilename() throws IOException {
        String mappingFile = readString();
        mappingFile = mappingFile.substring(0, mappingFile.lastIndexOf('.'));
        materials.get(materials.size()-1)[1] = mappingFile;

    }

    /**
     * @param mvp - All the transformation matrices stored in a single array for convenience
     * @param mMVPMatrixHandle - MVP matrix Handle for the shader
     * @param mLightPosHandle - Light position Handle for the shader
     * @param mLightPosInEyeSpace - Light position in Eye Space Handle for the shader
     * @param mPositionHandle - Model position Handle for the shader
     * @param mNormalHandle - Normal Handle for the shader
     * @param mTextureCoordinateHandle - Texture Coordinate Handle for the shader
     * @param mMVMatrixHandle - Model View matrix Handle for the shader
     */
    public void draw(float[][] mvp,int mMVPMatrixHandle, int mLightPosHandle,float[] mLightPosInEyeSpace,int mPositionHandle, int mNormalHandle, int mTextureCoordinateHandle, int mMVMatrixHandle) {

        //For each object of the 3d model, bind the buffers and draw the elements
        for (Object3ds obj : models) {
            //Provided that, the object is not empty (in some cases, there were empty objects)
            if (obj.getVertices() != null) {

                // Set the active texture unit to texture unit 0.
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                // Bind the texture to this unit.
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, obj.getTextureHandle());

                // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
                GLES20.glUniform1i(obj.getTextureHandle(), 0);

                obj.resetPositionBuffer();
                GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                        0, obj.getPositionBuffer());
                GLES20.glEnableVertexAttribArray(mPositionHandle);

                // Pass in the normal information
                obj.resetNormalBuffer();
                GLES20.glVertexAttribPointer(mNormalHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                        0, obj.getNormalBuffer());

                GLES20.glEnableVertexAttribArray(mNormalHandle);

                // Pass in the texture coordinate information

                obj.resetTextureBuffer();
                GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                        0, obj.getTextureBuffer());

                GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);


                // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
                // (which currently contains model * view).
                Matrix.multiplyMM(mvp[4], 0, mvp[1], 0, mvp[0], 0);

                // Pass in the modelview matrix.
                GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvp[4], 0);

                // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
                // (which now contains model * view * projection).
                Matrix.multiplyMM(mvp[3], 0, mvp[2], 0, mvp[4], 0);
                System.arraycopy(mvp[3], 0, mvp[4], 0, 16);

                Matrix.scaleM(mvp[4], 0, scaleFactor, scaleFactor, scaleFactor);

                // Pass in the combined matrix.
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvp[4], 0);

                // Pass in the light position in eye space.
                GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

                //Finally we can draw the actual elements
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, obj.getNumFaces() * 3);
            }

        }
    }

    /**
     * This function is executed, when the seek bar value is changed.
     */
    public void changeScale(float val) {
        scaleFactor = initialScaleFactor + initialScaleFactor * val;
    }

}
