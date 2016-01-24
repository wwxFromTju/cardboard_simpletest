package cn.tju.edu.cn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import cn.tju.edu.cn.util.LoggerConfig;

public class TextureHelper {
	private static final String TAG = "TextureHelper";

	public static int loadTexture(Context context, int resourceId) {
		final int[] textureObjectIds = new int[1];
		GLES20.glGenTextures(1, textureObjectIds, 0);

		if (textureObjectIds[0] == 0){
			if(LoggerConfig.ON) {
				Log.w(TAG, "Could not generate a new OpenGL texture object");
			}


			return 0;
		}

		// Generic bitmap read that can read in jpeg and png and take the original image (not scaled)
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

		if (bitmap == null) {
			if (LoggerConfig.ON) {
				Log.w(TAG, "Resource ID " + resourceId + " could not be decoded.");
			}

			GLES20.glDeleteTextures(1, textureObjectIds, 0);
			return 0;
		}

		// future texture calls should be applied to this texture
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);

		// set modes for minimizing and maximizing texture.
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR); // trilinear filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR); // bilinear filtering

		// load bitmap data into OpenGL
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		bitmap.recycle();

		// generate mipmaps for texture
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

		// unbind from the texture so we do not change anything
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		return textureObjectIds[0];
	}
}
