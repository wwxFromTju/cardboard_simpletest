
package cn.tju.edu.cn;

public class CardboardScene {
    private float[] mView;
    private float[] mModelView;
    private float[] mModelViewProjection;

    public CardboardScene() {
        mView = new float[16];
        mModelView = new float[16];
        mModelViewProjection = new float[16];
    }

    public float[] getView() {
        return mView;
    }

    public float[] getModelView() {
        return mModelView;
    }

    public float[] getModelViewProjection() {
        return mModelViewProjection;
    }
}
