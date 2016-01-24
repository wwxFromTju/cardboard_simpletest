package cn.tju.edu.cn.util;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderHelper {
	private static final String TAG = "ShaderHelper";
	
	public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
		int program;
		
		// Compile the shaders
		int vertexShader = compileVertexShader(vertexShaderSource);
		int fragmentShader = compileFragmentShader(fragmentShaderSource);
		
		// Link them into the shader program
		program = linkProgram(vertexShader, fragmentShader);
	
		if (LoggerConfig.ON) {
			validateProgram(program);
		}
		
		return program;
	}
	
	public static int compileVertexShader(String shaderCode) {
		return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
	}
	
	public static int compileFragmentShader(String shaderCode) {
		return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
	}
	
	public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
		// Create a new OpenGL object
		final int programObjectId = GLES20.glCreateProgram();
		
		if (programObjectId == 0){
			if (LoggerConfig.ON){
				Log.w(TAG, "Could not create new program");
			}
			
			return 0;
		}
		
		// Attach the vertex/fragment objects (referenced by their OpenGL id)
		GLES20.glAttachShader(programObjectId, vertexShaderId);
		GLES20.glAttachShader(programObjectId, fragmentShaderId);
		
		// Link the shaders together
		GLES20.glLinkProgram(programObjectId);
		
		// check the link for the shaders
		final int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0);
		
		if (programObjectId == 0) {
			// If it failed, delete the program object
			GLES20.glDeleteProgram(programObjectId);
			if (LoggerConfig.ON) {
				Log.w(TAG, "Linking of program failed.");
			}
			
			return 0;
		}
		
		return programObjectId;
	}

	public static boolean validateProgram(int programObjectId) {
		GLES20.glValidateProgram(programObjectId);
		
		final int[] validateStatus = new int[1];
		GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
		Log.v(TAG, "Result of validating program:\n\t" + validateStatus[0] + "\n\t" + GLES20.glGetProgramInfoLog(programObjectId));
		
		return validateStatus[0] != 0;
	}
	
	private static int compileShader(int type, String shaderCode) {
		// Create shader object. VERTEX/FRAGMENT shader
		/*
		 * The glCreateShader returns a unique non-zero int which is a referance
		 * to an OpenGL object. When we want to refer to the same object, we pass
		 * the integer as referance.
		 */
		final int shaderObjectId = GLES20.glCreateShader(type);
		
		if (shaderObjectId == 0) {
			if (LoggerConfig.ON) {
				Log.w(TAG, "Could not create new shader.");
			}
			
			return 0;
		}
		
		// Upload source to the GL shader object created above
		GLES20.glShaderSource(shaderObjectId, shaderCode);
		
		// Compile the GL shader object
		GLES20.glCompileShader(shaderObjectId);
		
		// Check to see if OpenGL was successfully able to compile the shader object.
		final int[] compileStatus = new int[1];
		GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
		
		if (LoggerConfig.ON) {
			Log.v(TAG, "Result of compiling source:\n" + shaderCode + "\n" + GLES20.glGetShaderInfoLog(shaderObjectId));
		}
		
		if (compileStatus[0] == 0) {
			// If it failed, delete the shader object
			GLES20.glDeleteShader(shaderObjectId);
			
			if (LoggerConfig.ON) {
				Log.w(TAG, "Compilation of shader failed");
			}
			
			return 0;
		}
		
		return shaderObjectId;
	}
}
