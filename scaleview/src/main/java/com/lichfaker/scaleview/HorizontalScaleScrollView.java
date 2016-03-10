package com.lichfaker.scaleview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * 水平滚动刻度尺
 *
 * @author LichFaker on 16/3/4.
 * @Email lichfaker@gmail.com
 */
public class HorizontalScaleScrollView extends View {

    private static final int[] mAttr = {
            R.attr.scale_view_min,
            R.attr.scale_view_max,
            R.attr.scale_view_margin,
            R.attr.scale_view_height,
    };

    public static final int SCALE_MIN = 0;
    public static final int SCALE_MAX = 1;
    public static final int SCALE_MARGIN = 2;
    public static final int SCALE_HEIGHT = 3;

    private int mMax; //最大刻度
    private int mMin; // 最小刻度
    private int mCountScale; //滑动的总刻度

    private int mScaleScrollViewWidth;

    private int mScaleMargin; //刻度间距
    private int mScaleHeight; //刻度线的高度
    private int mScaleMaxHeight; //整刻度线高度

    private int mRectWidth; //总宽度
    private int mRectHeight; //高度

    private Scroller mScroller;
    private int mScrollLastX;

    private int mTempScale; // 用于判断滑动方向
    private int mMidCountScale; //中间刻度

    private OnScrollListener mScrollListener;

    public interface OnScrollListener {
        void onScaleScroll(int scale);
    }

    public HorizontalScaleScrollView(Context context) {
        super(context);
        init(null);
    }

    public HorizontalScaleScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public HorizontalScaleScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalScaleScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    /**
     * 初始化方法
     */
    private void init(AttributeSet attrs) {

        // 获取自定义属性
        TypedArray ta = getContext().obtainStyledAttributes(attrs, mAttr);
        mMin = ta.getInteger(SCALE_MIN, 0);
        mMax = ta.getInteger(SCALE_MAX, 200);
        mScaleMargin = ta.getDimensionPixelOffset(SCALE_MARGIN, 15);
        mScaleHeight = ta.getDimensionPixelOffset(SCALE_HEIGHT, 20);
        ta.recycle();

        mScroller = new Scroller(getContext());

        mRectWidth = (mMax - mMin) * mScaleMargin;
        mRectHeight = mScaleHeight * 8;
        mScaleMaxHeight = mScaleHeight * 2;

        // 设置layoutParams
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(mRectWidth, mRectHeight);
        this.setLayoutParams(lp);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mScaleScrollViewWidth = getMeasuredWidth();
        mTempScale = mScaleScrollViewWidth / mScaleMargin / 2 + mMin;
        mMidCountScale = mScaleScrollViewWidth / mScaleMargin / 2 + mMin;

    }

    /**
     * 设置回调监听
     *
     * @param listener
     */
    public void setOnScrollListener(OnScrollListener listener) {
        this.mScrollListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 画笔
        Paint mPaint = new Paint();
        mPaint.setColor(Color.GRAY);
        // 抗锯齿
        mPaint.setAntiAlias(true);
        // 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        mPaint.setDither(true);
        // 空心
        mPaint.setStyle(Paint.Style.STROKE);

        canvas.drawLine(0, mRectHeight, mRectWidth, mRectHeight, mPaint);

        onDrawScale(canvas); //画刻度
        onDrawPointer(canvas); //画指针

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mScroller != null && !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mScrollLastX = x;
                return true;
            case MotionEvent.ACTION_MOVE:
                int dataX = mScrollLastX - x;
//                Log.d("lichfaker", "mScrollLastX:" + mScrollLastX + "======X:" + x + ";" + event.getX());
                if (mCountScale - mTempScale < 0) { //向右边滑动
                    if (mCountScale <= mMin && dataX <= 0) //禁止继续向右滑动
                        return super.onTouchEvent(event);
                } else if (mCountScale - mTempScale > 0) { //向左边滑动
                    if (mCountScale >= mMax && dataX >= 0) //禁止继续向左滑动
                        return super.onTouchEvent(event);
                }
                smoothScrollBy(dataX, 0);
                mScrollLastX = x;
                postInvalidate();
                mTempScale = mCountScale;
                return true;
            case MotionEvent.ACTION_UP:
                if (mCountScale < mMin) mCountScale = mMin;
                if (mCountScale > mMax) mCountScale = mMax;
                int finalX = (mCountScale - mMidCountScale) * mScaleMargin;
                mScroller.setFinalX(finalX); //纠正指针位置
                postInvalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void onDrawScale(Canvas canvas) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.GRAY);
        mPaint.setTextAlign(Paint.Align.CENTER); //文字居中
        // 抗锯齿
        mPaint.setAntiAlias(true);
        // 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        mPaint.setDither(true);
        mPaint.setTextSize(mRectHeight / 4);
        for (int i = 0, k = mMin; i <= mMax - mMin; i++) {
            if (i % 10 == 0) { //整值
                canvas.drawLine(i * mScaleMargin, mRectHeight, i * mScaleMargin, mRectHeight - mScaleMaxHeight, mPaint);
                //整值文字
                canvas.drawText(String.valueOf(k), i * mScaleMargin, mRectHeight - mScaleMaxHeight - 20, mPaint);
                k += 10;
            } else {
                canvas.drawLine(i * mScaleMargin, mRectHeight, i * mScaleMargin, mRectHeight - mScaleHeight, mPaint);
            }
        }

    }

    private void onDrawPointer(Canvas canvas) {

        Paint mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setTextAlign(Paint.Align.CENTER);
        // 抗锯齿
        mPaint.setAntiAlias(true);
        // 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
//        mPaint.setDither(true);
        //每一屏幕刻度的个数/2
        int countScale = mScaleScrollViewWidth / mScaleMargin / 2;
        //根据滑动的距离，计算指针的位置【指针始终位于屏幕中间】
        int finalX = mScroller.getFinalX();
        //滑动的刻度
        int tmpCountScale = (int) Math.rint((double) finalX / (double) mScaleMargin); //四舍五入取整
        //总刻度
        mCountScale = tmpCountScale + countScale + mMin;
        if (mScrollListener != null) { //回调方法
            mScrollListener.onScaleScroll(mCountScale);
        }
        canvas.drawLine(countScale * mScaleMargin + finalX, mRectHeight,
                countScale * mScaleMargin + finalX, mRectHeight - mScaleMaxHeight - mScaleHeight, mPaint);
//        canvas.drawText(String.valueOf(mCountScale), countScale * mScaleMargin + finalX, mRectHeight - mScaleMaxHeight - 10, mPaint);

    }

    /**
     * 使用Scroller时需重写
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        // 判断Scroller是否执行完毕
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            // 通过重绘来不断调用computeScroll
            invalidate();
        }
    }

    public void smoothScrollBy(int dx, int dy) {
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy);
    }

    public void smoothScrollTo(int fx, int fy) {
        int dx = fx - mScroller.getFinalX();
        int dy = fy - mScroller.getFinalY();
        smoothScrollBy(dx, dy);
    }

}