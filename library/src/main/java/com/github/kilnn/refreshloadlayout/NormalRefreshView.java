package com.github.kilnn.refreshloadlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.animation.Transformation;

import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_ACTIVE;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_CANCEL;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_PASSIVE;
import static com.github.kilnn.refreshloadlayout.RefreshLoadLayout.TYPE_RESET;

/**
 * Created by Kilnn on 2017/4/24.
 */

public abstract class NormalRefreshView extends RefreshView {

    private RefreshLoadLayout mLayout;

    private int mOriginalOffsetTop = Integer.MIN_VALUE;
    private int mCurrentOffsetTop;

    private int mFrom;

    public NormalRefreshView(@NonNull Context context) {
        this(context, null);
    }

    public NormalRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void attachRefreshLoadLayout(RefreshLoadLayout layout) {
        mLayout = layout;
    }

    @Override
    public void measure() {
        int limitWidth = mLayout.getMeasuredWidth() - mLayout.getPaddingLeft() - mLayout.getPaddingRight();
        int limitHeight = mLayout.getMeasuredHeight() - mLayout.getPaddingTop() - mLayout.getPaddingBottom();
        // The RefreshView can be whatever height it wants.
        measure(
                MeasureSpec.makeMeasureSpec(limitWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(limitHeight, MeasureSpec.UNSPECIFIED)
        );
        //Because the refresh view if above the target view.
        int originalOffsetTop = mLayout.getPaddingTop() - getMeasuredHeight();
        if (mOriginalOffsetTop != originalOffsetTop) {
            mCurrentOffsetTop = mOriginalOffsetTop = originalOffsetTop;
        }
    }

    @Override
    public void layout() {
        int limitWidth = mLayout.getMeasuredWidth() - mLayout.getPaddingLeft() - mLayout.getPaddingRight();
        int childWidth = getMeasuredWidth();
        int childHeight = getMeasuredHeight();
        int childLeft = limitWidth / 2 - childWidth / 2;

        layout(
                childLeft,
                mCurrentOffsetTop,
                childLeft + childWidth,
                mCurrentOffsetTop + childHeight
        );
    }

    @Override
    public int offsetBy(int offset) {
        int targetTop = Math.max(mCurrentOffsetTop + offset, mOriginalOffsetTop);
        int realOffset = mCurrentOffsetTop - targetTop;
        setOffsetTopAndBottom(-realOffset, true);
        return realOffset;
    }

    @Override
    public void offsetTo(int offset) {
        int targetTop = mOriginalOffsetTop + offset;
        setOffsetTopAndBottom(targetTop - mCurrentOffsetTop, true /* requires update */);
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
    public void reset() {
        offsetTo(0);
        resetAll();
    }

    public abstract void resetAll();

    private void moveToStart(float interpolatedTime) {
        int targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - getTop();
        setOffsetTopAndBottom(offset, false /* requires update */);
    }

    private void moveToRefreshing(float interpolatedTime) {
        int endTarget = mLayout.getPaddingTop() + getContentHeight() - getMeasuredHeight();
        int targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
        int offset = targetTop - getTop();
        setOffsetTopAndBottom(offset, false /* requires update */);
    }

    private void setOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        ViewCompat.offsetTopAndBottom(this, offset);
        mCurrentOffsetTop = getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            mLayout.invalidate();
        }
    }

}
