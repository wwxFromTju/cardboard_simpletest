
package cn.tju.edu.cn;

import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

import cn.tju.edu.cn.skybox.Skybox;
import cn.tju.edu.cn.skybox.SkyboxShaderProgram;

import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;

//必须继承CardboardActivity
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {
    private static final String TAG = "MainActivity";

    private CardboardOverlayView mOverlayView;
    public static CardboardCube mCube;
    public static CardboardCube[] mCubeNUM;
    private CardboardFloor mFloor;
    private CardboardCamera mCamera;
    private CardboardScene mScene;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];

    private SkyboxShaderProgram skyboxProgram;
    private Skybox skybox;

    private int skyboxTexture;

    private float xRotation, yRotation;


//    初始化view，和初始化对应的矩阵 并且使用对应的着色器
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //这里是android的载入资源的
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);

        //这里是也是载入对应的资源
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRestoreGLStateEnabled(false);
        //设置对应的render 和 设置对应的cardboardview
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        //创建对应的场景
        mScene = new CardboardScene();

        //初始化正方形，mCube是会转的
        mCube = new CardboardCube(this, mScene);
        mCubeNUM = new CardboardCube[20];
        for(int i = 0; i < 20; i++)
            mCubeNUM[i] = new CardboardCube(this, mScene);


        mFloor = new CardboardFloor(this, mScene);
        mCamera = new CardboardCamera(this, mScene);

        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        mOverlayView.show3DToast("去找正方体");
    }

    @Override
    public void onRendererShutdown() {}

    @Override
    public void onSurfaceChanged(int width, int height) {}

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        mCube.onSurfaceCreated(config);
        for(int i = 0; i < 20; i++)
            mCubeNUM[i].onSurfaceCreated(config);
        mFloor.onSurfaceCreated(config);

//
//        skyboxProgram = new SkyboxShaderProgram();
//        skybox = new Skybox();
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        mCube.onNewFrame(headTransform);
        mCamera.onNewFrame(headTransform);
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */


    @Override
    public void onDrawEye(Eye eye) {
        mCamera.onDrawEye(eye);
        mCube.onDrawEye(eye);
        for(int i = 0; i < 20; i++)
            mCubeNUM[i].onDrawEye(eye);
        mFloor.onDrawEye(eye);
    }

    //最后的FRAME应该做的操作
    @Override
    public void onFinishFrame(Viewport viewport) {}




    //这里是监听对应的旁边磁环的拉动
    //或者监听对应的屏幕点击事件
    @Override
    public void onCardboardTrigger() {
            mCube.onCardboardTrigger();

    }




    private void drawSkybox() {
        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        skyboxProgram.useProgram();
        skyboxProgram.setUniforms(viewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        skybox.draw();
    }
}
