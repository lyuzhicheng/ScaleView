package com.lichfaker.scaleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * 水平滚动刻度尺
 *
 * @author LichFaker on 16/3/4.
 * @Email lichfaker@gmail.com
 */
public class HorizontalScaleScrollView extends BaseScaleView {

    private VelocityTracker velocityTracker;
    private ViewConfiguration configuration;
    private int minFlingVelocity;
    private int minTouchSlop;
    private boolean isScrolling;
    private boolean isFirstTime = true;

    public HorizontalScaleScrollView(Context context) {
        super(context);
    }

    public HorizontalScaleScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalScaleScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HorizontalScaleScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void initVar() {
        mRectWidth = (mMax - mMin) * mScaleMargin;
        mRectHeight = mScaleHeight * 8;
        mScaleMaxHeight = mScaleHeight * 2;

        // 设置layoutParams
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(mRectWidth, mRectHeight);
        this.setLayoutParams(lp);
        velocityTracker = VelocityTracker.obtain();
        configuration = ViewConfiguration.get(this.getContext());
        minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        minTouchSlop = configuration.getScaledTouchSlop();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height=MeasureSpec.makeMeasureSpec(mRectHeight, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, height);
        mScaleScrollViewRange = getMeasuredWidth();
        mTempScale = mScaleScrollViewRange / mScaleMargin / 2 + mMin;
        mMidCountScale = mScaleScrollViewRange / mScaleMargin / 2 + mMin;
    }

    @Override
    protected void onDrawLine(Canvas canvas, Paint paint) {
        canvas.drawLine(0, mRectHeight, mRectWidth, mRectHeight, paint);
    }

    //绘制竖线条
    @Override
    protected void onDrawScale(Canvas canvas, Paint paint) {

        paint.setTextSize(mRectHeight / 4);

        int start = mCountScale - (mMidCountScale - mMin);
        int end = mCountScale + (mMidCountScale - mMin);//只绘制显示的一部分.
        if (start < mMin) {
            start = mMin;
        }
        if (end > mMax) {
            end = mMax;
        }
        Log.d("cheng", "curPos:" + mCountScale +" start:" + start + " end:" + end);
        for (int i = start, k = start; i <= end; i++) {
            if (i % 10 == 0) { //整值
                canvas.drawLine(i * mScaleMargin, mRectHeight, i * mScaleMargin, mRectHeight - mScaleMaxHeight, paint);
                //整值文字
                canvas.drawText(String.valueOf(i * 100), i * mScaleMargin, mRectHeight - mScaleMaxHeight - 20, paint);
//                k += 10;
            } else {
                canvas.drawLine(i * mScaleMargin, mRectHeight, i * mScaleMargin, mRectHeight - mScaleHeight, paint);
            }
        }

    }
    //绘制中间的指示器.
    @Override
    protected void onDrawPointer(Canvas canvas, Paint paint) {
        Log.d("test", "draw pointer");
        paint.setColor(Color.RED);

        //每一屏幕刻度的个数/2
        int countScale = mScaleScrollViewRange / mScaleMargin / 2;
        //根据滑动的距离，计算指针的位置【指针始终位于屏幕中间】
        int currX = mScroller.getCurrX();

        //滑动的刻度
        int tmpCountScale = (int) Math.rint((double) currX / (double) mScaleMargin); //四舍五入取整
        //总刻度 = 滑动的刻度数目 +  半屏的刻度数 + 最小值
        mCountScale = tmpCountScale + countScale + mMin;
        //绘制的位置起点： 滑动的距离 +屏幕的一半宽度
        canvas.drawLine(countScale * mScaleMargin + currX, mRectHeight,
                countScale * mScaleMargin + currX, mRectHeight - mScaleMaxHeight - mScaleHeight, paint);
        if (mScrollListener != null && isFirstTime) { //回调方法
            mScrollListener.onScaleScroll(mCountScale);
            isFirstTime = false;
        }
    }

    @Override
    public void scrollToScale(int val) {
        if (val < mMin || val > mMax) {
            return;
        }
        int dx = (val - mCountScale) * mScaleMargin;
        smoothScrollBy(dx, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mScroller != null && !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                addToVelocity(event);
                mScrollLastX = x;
                return true;
            case MotionEvent.ACTION_MOVE:
                int dataX = mScrollLastX - x;
                if (mCountScale - mTempScale < 0) { //向右边滑动
                    if (mCountScale <= mMin && dataX <= 0) //禁止继续向右滑动
                        return false;
                } else if (mCountScale - mTempScale > 0) { //向左边滑动
                    if (mCountScale >= mMax && dataX >= 0) //禁止继续向左滑动
                        return false;
                }
                smoothScrollBy(dataX, 0);
                mScrollLastX = x;
                postInvalidate();
                mTempScale = mCountScale;
                addToVelocity(event);
                return true;
            case MotionEvent.ACTION_UP:

                if (mCountScale < mMin) mCountScale = mMin;
                if (mCountScale > mMax) mCountScale = mMax;
                int finalX = (mCountScale - mMidCountScale) * mScaleMargin;
                mScroller.setFinalX(finalX); //纠正指针位置
                postInvalidate();
                float xVelocity = computeVelocity();
                if (Math.abs(xVelocity) > minFlingVelocity ) {
                    isScrolling = true;
                    mScroller.fling(getScrollX(), getScrollY(), - (int) xVelocity , 0, - (mScaleScrollViewRange / mScaleMargin / 2) * mScaleMargin,  - (mScaleScrollViewRange / mScaleMargin / 2) * mScaleMargin + (mMax - mMin) * mScaleMargin, 0, 0);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void addToVelocity(MotionEvent event) {
        velocityTracker.addMovement(event);
    }

    protected void releaseVelocity() {
        if (velocityTracker != null) {
            velocityTracker.clear();
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    protected float computeVelocity() {
        velocityTracker.computeCurrentVelocity(1000);
        float velocityY = velocityTracker.getXVelocity();
        return velocityY;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (!mScroller.computeScrollOffset() && isScrolling) {
            isScrolling = false;
            //做纠正
            int finalX = (mCountScale - mMidCountScale) * mScaleMargin;
            mScroller.setFinalX(finalX); //纠正指针位置
            postInvalidate();
        } else if (!mScroller.computeScrollOffset()) {//纠正后的滑动
            if (mScrollListener != null) { //回调方法
                mScrollListener.onScaleScroll(mCountScale);
            }
        }
    }
}
