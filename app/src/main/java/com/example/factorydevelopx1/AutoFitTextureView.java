package com.example.factorydevelopx1;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {

    private int mRationWidth = 0;
    private int mRationHeight = 0;

    public AutoFitTextureView(Context context) {
        super(context);
        Log.d("jack","AutoFitTextureView(Context context)");
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d("jack","AutoFitTextureView(Context context  AttributeSet attrs)");
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d("jack","(Context context, AttributeSet attrs, int defStyleAttr)");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("jack","onMeasure  widthMeasureSpec=="+width+"  heightMeasureSpec=="+height);
        if(mRationHeight == 0 || mRationWidth == 0){
            setMeasuredDimension(width,height);
        }else{
            if(width < height * mRationWidth/mRationHeight){
                setMeasuredDimension(width,width*mRationHeight/mRationWidth);
            }else {
                setMeasuredDimension(height*mRationWidth/mRationHeight,height);
            }

        }
    }

    public void setAspectRadio(int width,int height){
        Log.d("jack","setAspectRadio with = "+width+" height="+height);
        mRationHeight = height;
        mRationWidth = width;
        requestLayout();
    }
}
