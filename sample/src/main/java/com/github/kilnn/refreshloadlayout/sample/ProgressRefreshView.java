package com.github.kilnn.refreshloadlayout.sample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.kilnn.refreshloadlayout.NormalRefreshView;

/**
 * Created by Kilnn on 2017/4/23.
 */

public class ProgressRefreshView extends NormalRefreshView {

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_MARGIN = 15;
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;

    private int mCircleDiameter;
    private int mCircleMargin;

    private ImageView mBackgroundView;
    private CircleImageView mCircleView;
    private MaterialProgressDrawable mProgress;

    public ProgressRefreshView(@NonNull Context context) {
        this(context, null);
    }

    public ProgressRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //add background image
        mBackgroundView = new AppCompatImageView(context);
        mBackgroundView.setBackgroundResource(R.drawable.ic_home_page_bg_big);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        addView(mBackgroundView, params);

        //add refresh content
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleMargin = (int) (CIRCLE_MARGIN * metrics.density);

        mCircleView = new CircleImageView(context, CIRCLE_BG_LIGHT);
        mProgress = new MaterialProgressDrawable(context, this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);

        params = new FrameLayout.LayoutParams(mCircleDiameter, mCircleDiameter, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.topMargin = mCircleMargin;
        params.bottomMargin = mCircleMargin;
        addView(mCircleView, params);
    }

    @Override
    public int getContentHeight() {
        return mCircleDiameter + 2 * mCircleMargin;
    }

    @Override
    public void resetAll() {

    }

    @Override
    public void onStartDrag() {

    }

    @Override
    public void onDrag(float dragPercent, float tensionPercent) {

    }

    @Override
    public void onCancelDrag() {

    }

    @Override
    public void onRefresh() {
        Log.e("Kilnn", "正在刷新，请更新UI");
    }

    @Override
    public void onRefreshCompleted(boolean success) {

    }
}
