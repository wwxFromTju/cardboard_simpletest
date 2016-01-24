package cn.tju.edu.cn;

import android.content.Context;
import android.opengl.GLES20;

public class TextureShaderProgram extends ShaderProgram {
	// Uniform locations
	private final int uTextureLocation;
	
	// Attribute locations
	private final int aTextureCoordinatesLocation;
	
	public TextureShaderProgram(Context context) {
		super(context, R.raw.light_vertex, R.raw.passthrough_fragment);
		
		// Retrieve uniform locations for the shader program
		uTextureLocation = GLES20.glGetUniformLocation(program, U_TEXTURE_UNIT);
		
		// Retrieve attribute location for the shader program
		aTextureCoordinatesLocation = GLES20.glGetAttribLocation(program, A_TEXTURE_COORDINATES);
	}
	
	public void setUniforms(float[] matrix, int textureId) {
		
		// Set the active texture unit to texture unit 0
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		
		// Bind the texture to this unit
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
		
		// Tell the texture uniform sampler to use this texture in the shader by
		// telling it to read from texture unit 0
		GLES20.glUniform1i(uTextureLocation, 0);
	}
	

	public int getTextureCoordinatesAttributeLocation() {
		return aTextureCoordinatesLocation;
	}
}
