package com.github.kilnn.refreshloadlayout;

import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.Transformation;

import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_ACTIVE;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_CANCEL;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_PASSIVE;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_RESET;

/**
 * Created by Kilnn on 2017/4/24.
 */

public class NormalTargetViewDelegate implements RefreshLoadDelegate {

    private RefreshLoadLayout mLayout;

    private int mOriginalOffsetTop = Integer.MIN_VALUE;
    private int mCurrentOffsetTop;

    private int mFrom;

    @Override
    public void attachRefreshLoadLayout(RefreshLoadLayout layout) {
        mLayout = layout;
    }

    @Override
    public void measure() {
        View view = mLayout.getTargetView();
        if (view == null) return;
        int limitWidth = mLayout.getMeasuredWidth() - mLayout.getPaddingLeft() - mLayout.getPaddingRight();
        int limitHeight = mLayout.getMeasuredHeight() - mLayout.getPaddingTop() - mLayout.getPaddingBottom();
        view.measure(
                View.MeasureSpec.makeMeasureSpec(limitWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(limitHeight, View.MeasureSpec.EXACTLY)
        );
        //Because the target view if match the parent size.
        int originalOffsetTop = mLayout.getPaddingTop();
        if (mOriginalOffsetTop != originalOffsetTop) {
            mCurrentOffsetTop = mOriginalOffsetTop = originalOffsetTop;
        }
    }

    @Override
    public void layout() {
        View view = mLayout.getTargetView();
        if (view == null) return;
        int childWidth = mLayout.getMeasuredWidth() - mLayout.getPaddingLeft() - mLayout.getPaddingRight();
        int childHeight = mLayout.getMeasuredHeight() - mLayout.getPaddingTop() - mLayout.getPaddingBottom();
        int childLeft = mLayout.getPaddingLeft();
        view.layout(
                childLeft,
                mCurrentOffsetTop,
                childLeft + childWidth,
                mCurrentOffsetTop + childHeight
        );
    }

    @Override
    public int offsetBy(int offset) {
        View view = mLayout.getTargetView();
        if (view == null) return 0;

        int targetTop = Math.max(mCurrentOffsetTop + offset, mOriginalOffsetTop);
        int realOffset = mCurrentOffsetTop - targetTop;

        setOffsetTopAndBottom(view, -realOffset, true);
        return realOffset;
    }

    @Override
    public void offsetTo(int offset) {
        View view = mLayout.getTargetView();
        if (view == null) return;
        int targetTop = mOriginalOffsetTop + offset;
        setOffsetTopAndBottom(view, targetTop - mCurrentOffsetTop, true /* requires update */);
    }

    @Override
    public void onAnimationStart(@RefreshLoadLayout.AnimationType int animationType) {
        mFrom = mCurrentOffsetTop;
    }

    @Override
    public void onAnimationEnd(@RefreshLoadLayout.AnimationType int animationType) {

    }

    @Override
    public void onAnimationTransformation(@RefreshLoadLayout.AnimationType int animationType, float interpolatedTime, Transformation t) {
        if (animationType == TYPE_CANCEL || animationType == TYPE_RESET) {
            moveToStart(interpolatedTime);
        } else if (animationType == TYPE_PASSIVE || animationType == TYPE_ACTIVE) {
            moveToRefreshing(interpolatedTime);
        }
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

    }

    @Override
    public void onRefreshCompleted(boolean success) {

    }

    @Override
    public void reset() {
        offsetTo(0);
    }

    private void moveToStart(float interpolatedTime) {
        View view = mLayout.getTargetView();
        if (view == null) return;
        int targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - view.getTop();
        setOffsetTopAndBottom(view, offset, false /* requires update */);
    }

    private void moveToRefreshing(float interpolatedTime) {
        View view = mLayout.getTargetView();
        if (view == null) return;

        RefreshView refreshView = mLayout.getRefreshView();
        if (refreshView == null) return;

        int endTarget = mLayout.getPaddingTop() + refreshView.getContentHeight();
        int targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
        int offset = targetTop - view.getTop();
        setOffsetTopAndBottom(view, offset, false /* requires update */);
    }


    private void setOffsetTopAndBottom(View view, int offset, boolean requiresUpdate) {
        ViewCompat.offsetTopAndBottom(view, offset);
        mCurrentOffsetTop = view.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            mLayout.invalidate();
        }
    }


}
