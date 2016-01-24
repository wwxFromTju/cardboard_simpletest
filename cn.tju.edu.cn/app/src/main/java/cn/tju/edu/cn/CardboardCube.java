package cn.tju.edu.cn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.util.Log;

import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CardboardCube extends CardboardObject {
    private static final String TAG = "CardboardCube";
    private  int uTextureLocation;
    private  int aTextureCoordinatesLocation;
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    private int texture;
    private  FloatBuffer floatBuffer;
    private static final int STRIDE = (3 + 2) * Constants.BYTES_PER_FLOAT;

    //来读入drawable中的图片，来之后进行纹理贴图
    public static int loadTexture(Context context, int resourcedId){

        //申请相关的句柄，并设置
        final int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1,textureObjectIds,0);

        //如果没有取到，应该要报错，
        //或者做一些其他的
        //目前没想好要写什么。
        //于是空着
        if(textureObjectIds[0] == 0){

        }

        //设置对应的参数
        final BitmapFactory.Options opentions = new BitmapFactory.Options();
        opentions.inScaled = false;

        //对应的bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourcedId, opentions);

        //删除纹理
        GLES20.glDeleteTextures(1,textureObjectIds,0);
        return 0;
    }

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private float[] mView;
    private float[] mHeadView;

    private FloatBuffer mFoundColors;

    private int mScore;
    private float objectDistance = 12f;
    private static final float TIME_DELTA = 0.3f;


    private FloatBuffer texBuffer;

    private Vibrator mVibrator;
    private CardboardOverlayView mOverlayView;


    public CardboardCube(Activity activity, CardboardScene scene) {
        super(activity, scene);

        //这个是设置对应的矩阵，之后通过运算去变换等
        setModel(new float[16]);
        mHeadView = new float[16];

        //获得系统服务，这里是设置为句柄的
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        //这个是设置对应的控件
        //这样Android才能产生对应的效果
        mOverlayView = (CardboardOverlayView) activity.findViewById(R.id.overlay);
        mOverlayView.show3DToast("你可以点击屏幕，或者拖动旁边的东西");

    }


    //在绘制之前的初始化
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        super.onSurfaceCreated(config);
        checkGLError("onSurfaceCreated");

        //将Cube的顶点坐标存在缓冲区中
        //Java中对应大的数据不建议直接放在一个文件里面，建议通过这样的缓冲去读
        //这一点是和Cpp中不同的东西
        ByteBuffer bbVertices = ByteBuffer.allocateDirect(CUBE_COORDS.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        //这个是一个holder效果的东西，你存进去之后，方便之后的使用
        setVertices(bbVertices.asFloatBuffer());
        getVertices().put(CUBE_COORDS);
        getVertices().position(0);

        //将颜色数值也存到缓冲中
        ByteBuffer bbColors = ByteBuffer.allocateDirect(CUBE_COLORS.length * 4);
        bbColors.order(ByteOrder.nativeOrder());
        //同样放到了相关里面
        setColors(bbColors.asFloatBuffer());
        getColors().put(CUBE_COLORS);
        getColors().position(0);

        //这个被发现的时候，Cube被设置为相关的黄色，这个颜色数组也要放到相关的缓冲区中
        ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(CUBE_FOUND_COLORS.length * 4);
        bbFoundColors.order(ByteOrder.nativeOrder());
        mFoundColors = bbFoundColors.asFloatBuffer();
        mFoundColors.put(CUBE_FOUND_COLORS);
        mFoundColors.position(0);

        //这个normal的数组，同理，必须要放到了缓冲区中
        ByteBuffer bbNormals = ByteBuffer.allocateDirect(CUBE_NORMALS.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        setNormals(bbNormals.asFloatBuffer());
        getNormals().put(CUBE_NORMALS);
        getNormals().position(0);


        // 设置纹理坐标数组缓冲区，数据类型为浮点数据
        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texBuffer = tbb.asFloatBuffer();
        texBuffer.put(texCoords);
        texBuffer.position(0);


        //设置对应的shader
        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);


        setProgram(GLES20.glCreateProgram());
        GLES20.glAttachShader(getProgram(), vertexShader);
        GLES20.glAttachShader(getProgram(), passthroughShader);
        GLES20.glLinkProgram(getProgram());
        GLES20.glUseProgram(getProgram());

        checkGLError("Cube program");


        //得到在shader中的各种变量
        setPositionParam(GLES20.glGetAttribLocation(getProgram(), "a_Position"));
        setNormalParam(GLES20.glGetAttribLocation(getProgram(), "a_Normal"));
        setColorParam(GLES20.glGetAttribLocation(getProgram(), "a_Color"));
        setModelParam(GLES20.glGetUniformLocation(getProgram(), "u_Model"));
        setModelViewParam(GLES20.glGetUniformLocation(getProgram(), "u_MVMatrix"));
        setModelViewProjectionParam(GLES20.glGetUniformLocation(getProgram(), "u_MVP"));
        setLightPosParam(GLES20.glGetUniformLocation(getProgram(), "u_LightPos"));

        //绑定texture
        texture = TextureHelper.loadTexture(getContext(), R.drawable.air_hockey_surface);



        uTextureLocation = GLES20.glGetUniformLocation(getProgram(), U_TEXTURE_UNIT);
        aTextureCoordinatesLocation = GLES20.glGetAttribLocation(getProgram(), A_TEXTURE_COORDINATES);





// Set the active texture unit to texture unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0
        GLES20.glUniform1i(uTextureLocation, 0);
        // bind texture attributes for table from shader program
//        floatBuffer = ByteBuffer
//                .allocateDirect(vertexData.length * Constants.BYTES_PER_FLOAT)
//                .order(ByteOrder.nativeOrder())
//                .asFloatBuffer()
//                .put(vertexData);
//
//
//        floatBuffer.position(dataOffset);
//
//        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT, false, stride, floatBuffer);
//        GLES20.glEnableVertexAttribArray(attributeLocation);
//
//        floatBuffer.position(0)
//
//
//        vertexArray.setVertexAttribPointer(
//                POSITION_COMPONENT_COUNT,
//                textureProgram.getTextureCoordinatesAttributeLocation(),
//                TEXTURE_COORDINATES_COMPONENT_COUNT,
//                STRIDE);


        GLES20.glEnable(GLES20.GL_CULL_FACE);


        //进入相关的数组，注意顺序，方便之后的计算
        GLES20.glEnableVertexAttribArray(getPositionParam());
        GLES20.glEnableVertexAttribArray(getNormalParam());
        GLES20.glEnableVertexAttribArray(getColorParam());


        //设置单位矩阵
        Matrix.setIdentityM(getModel(), 0);

        //移动相应的距离
        Matrix.translateM(getModel(), 0, 0, 0, -objectDistance);

        //因为，这样设置出来的是同样的，为了在第一次生成对应的不同的Cube的时候
        //就可以先调用一次hide()然后随机变换
        hide();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        super.onNewFrame(headTransform);

        //来绘制每一Frame

        //那个特殊的Cube
        //设置它旋转,这个调用其实是一个异步的用法，这样才能绘制出在每一Frame中旋转的效果
        //设置时间和旋转的矩阵
        Matrix.rotateM(getModel(), 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
        float det_x = (float) Math.random();
        float det_y = (float) Math.random();
        float det_z = (float) Math.random();
//        Matrix.translateM(getModel(),0,0,0,10);

        headTransform.getHeadView(mHeadView, 0);

    }

    @Override
    public void onDrawEye(Eye eye) {
        super.onDrawEye(eye);

        //eye.getPerspective()是google写的，来获得对应的矩阵
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        //设置对应的modelview 的矩阵
        //将得到的放在getModelView()中
        Matrix.multiplyMM(getModelView(), 0, getView(), 0, getModel(), 0);

        //和上面的相似
        //设置对应的modelview的projection
        Matrix.multiplyMM(getModelViewProjection(), 0, perspective, 0, getModelView(), 0);

        //进行画图
        draw();
    }


    public void draw() {

        //绑定对应的程序,相当与一个句柄
        GLES20.glUseProgram(getProgram());


        GLES20.glUniform3fv(getLightPosParam(), 1, getLightPosInEyeSpace(), 0);

        //设置对应的模型。计算光照
        GLES20.glUniformMatrix4fv(getModelParam(), 1, false, getModel(), 0);

        //设置模型的view，计算光照
        GLES20.glUniformMatrix4fv(getModelViewParam(), 1, false, getModelView(), 0);

        //设置cubu的顶点的坐标组
        GLES20.glVertexAttribPointer(getPositionParam(), COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, getVertices());


        //设置对应的projection矩阵
        GLES20.glUniformMatrix4fv(getModelViewProjectionParam(), 1, false, getModelViewProjection(), 0);

        //设置对应的法向量的位置
        GLES20.glVertexAttribPointer(getNormalParam(), 3, GLES20.GL_FLOAT, false, 0, getNormals());
        //设置对应的颜色
        GLES20.glVertexAttribPointer(getColorParam(), 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject() ? mFoundColors : getColors());

        //进行绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("Drawing cube");
    }



    //当磁环下拉的时候，或者屏幕被点击的时候触发
    @Override
    public void onCardboardTrigger() {
        super.onCardboardTrigger();
        Log.i(TAG, "onCardboardTrigger");

        //判断是否是视线范围内是
        if (isLookingAtObject()) {  //如果成功找到了
            //加分
            mScore++;
            //给用用户一个提醒
            mOverlayView.show3DToast("_(:з」∠)_竟然被你找了，找到你个正方体\n分数 = " + mScore);
            //将旋转的Cube隐藏
            hide();
            MainActivity.mCube.hide();

            //将那些用来迷惑的Cube变换位置
            for(int i = 0; i <10; ++i)
                MainActivity.mCubeNUM[i].hide();
        } else {
            mOverlayView.show3DToast("没有找到哦，请在找找哦，( ;￣ω￣)ゞ ");
        }

        //给予震动
        mVibrator.vibrate(50);
    }



    //即通过产生随机数的方法，来随机产生新的位置
    private void hide() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];


        //为了我们的camera是放在Y轴上的，所以这个可以看成相应的距离
        //产生一个随机旋转的角度
        float angleXZ = (float) Math.random() * 180 + 90;

        //产生一个旋转的
//        float angleXZ = (float) Math.random() * 360 ;


        //产生对应的旋转的矩阵
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);

        float oldObjectDistance = objectDistance;

        //产生一个随机的距离的值
        objectDistance = (float) Math.random() * 15 + 5;

        //因为是用矩阵来计算的，所以使用相当于乘上对应的参数即可
        float objectScalingFactor = objectDistance / oldObjectDistance;

        //矩阵乘上对应的参数即可，这样就完成了对应的大小上的变化
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor,
                objectScalingFactor);

        //乘上对应的旋转矩阵即可
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, getModel(), 12);


        //获得对应的随机的角度
        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.

        //把角度变成对应的弧度
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * objectDistance;

        //设置对应
        Matrix.setIdentityM(getModel(), 0);
        Matrix.translateM(getModel(), 0, posVec[0], newY, posVec[2]);
    }




    //判断是是否在屏幕中间附近，也就是时候可以视为被你在视线看到的
    private boolean isLookingAtObject() {


        float[] initVec = { 0, 0, 0, 1.0f };
        float[] objPositionVec = new float[4];


        //转换到对应的camera的空间上
        //计算出来的存在getModelView
        Matrix.multiplyMM(getModelView(), 0, mHeadView, 0, getModel(), 0);
        //得到对应我视线上的坐标
        Matrix.multiplyMV(objPositionVec, 0, getModelView(), 0, initVec, 0);


        //通过判断距离是否是我看的中心位置
        float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }


    //下面是对应的数据
    //分别是点的坐标 法线的数据 颜色的数据
    public static final float[] CUBE_COORDS = new float[] {
        // Front face
        -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,

            // Right face
            1.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            // Back face
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,

            // Left face
            -1.0f, 1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,

            // Top face
            -1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,

            // Bottom face
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
    };

    public static final float[] CUBE_COLORS = new float[] {
        // front, green
        0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,

            // right, blue
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,

            // back, also green
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,
            0f, 0.5273f, 0.2656f, 1.0f,

            // left, also blue
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,
            0.0f, 0.3398f, 0.9023f, 1.0f,

            // top, red
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,

            // bottom, also red
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
            0.8359375f,  0.17578125f,  0.125f, 1.0f,
    };

    public static final float[] CUBE_FOUND_COLORS = new float[] {
        // front, yellow
        1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,

            // right, yellow
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,

            // back, yellow
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,

            // left, yellow
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,

            // top, yellow
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,

            // bottom, yellow
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
            1.0f,  0.6523f, 0.0f, 1.0f,
    };

    public static final float[] CUBE_NORMALS = new float[] {
        // Front face
        0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,

            // Right face
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,

            // Back face
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,

            // Left face
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,

            // Top face
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,

            // Bottom face
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f
    };
    float[] texCoords = { // 定义上面的面的纹理坐标
            0.0f, 1.0f,  // A. 左-下
            1.0f, 1.0f,  // B. 右-下
            0.0f, 0.0f,  // C. 左-上
            1.0f, 0.0f   // D. 右-上
    };



}
