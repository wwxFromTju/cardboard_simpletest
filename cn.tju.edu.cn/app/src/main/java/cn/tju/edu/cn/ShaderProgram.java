package cn.tju.edu.cn;

import android.content.Context;
import android.opengl.GLES20;

import cn.tju.edu.cn.util.ShaderHelper;
import cn.tju.edu.cn.util.TextResourceReader;


public class ShaderProgram {
	// Uniform constatns
	protected static final String U_COLOR = "u_Color";
	protected static final String U_MATRIX = "u_Matrix";
	protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
	
	// Attribute constants
	protected static final String A_POSITION = "a_Position";
	protected static final String A_COLOR = "a_Color";
	protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
	
	// Shader program
	protected final int program;
	protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
		// compile the shader and link the program
		program = ShaderHelper.buildProgram(
				TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId),
				TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId));
	}
	
	public void useProgram() {
		// Set the current OpenGL shader program to this program
		GLES20.glUseProgram(program);
	}
}
