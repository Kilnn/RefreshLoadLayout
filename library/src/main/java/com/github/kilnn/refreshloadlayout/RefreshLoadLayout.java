package com.github.kilnn.refreshloadlayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Kilnn on 2017/4/23.
 * {@link android.support.v4.widget.SwipeRefreshLayout}
 */

public class RefreshLoadLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {

    private static final String LOG_TAG = RefreshLoadLayout.class.getSimpleName();

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    // Default distance in dips from the trigger refresh and slingshot.
    private static final int DEFAULT_REFRESH_DISTANCE = 64;

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    public static final int TYPE_NONE = 0;
    public static final int TYPE_CANCEL = 1;
    public static final int TYPE_PASSIVE = 2;
    public static final int TYPE_ACTIVE = 3;
    public static final int TYPE_RESET = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_NONE, TYPE_CANCEL, TYPE_PASSIVE, TYPE_ACTIVE, TYPE_RESET})
    @interface AnimationType {
    }

    private View mTargetView; // the target of the gesture
    private RefreshLoadDelegate mTargetViewDelegate;


    private OnRefreshListener mListener;
    boolean mRefreshing = false;
    private int mTouchSlop;
    private float mRefreshTriggerDistance;
    private int mRefreshSlingshotDistance;
    private int mDragLimitDistance;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    // Motion event will be intercept if a animation is in progress
    private boolean mAnimateInProgress;

    private OnChildScrollUpCallback mChildScrollUpCallback;

    private RefreshView mRefreshView;

    private int mAnimationCancelDuration, mAnimationPassiveDuration, mAnimationActiveDuration, mAnimationResetDuration;
    private Interpolator mAnimationCancelInterpolator, mAnimationPassiveInterpolator, mAnimationActiveInterpolator, mAnimationResetInterpolator;

    public RefreshLoadLayout(Context context) {
        this(context, null);
    }

    public RefreshLoadLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setWillNotDraw(false);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        mRefreshSlingshotDistance = (int) (DEFAULT_REFRESH_DISTANCE * metrics.density);
        mRefreshTriggerDistance = mRefreshSlingshotDistance;
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        int mediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        mAnimationCancelDuration = mAnimationPassiveDuration = mAnimationActiveDuration = mAnimationResetDuration = mediumAnimationDuration;

        Interpolator decelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mAnimationCancelInterpolator = mAnimationPassiveInterpolator = mAnimationActiveInterpolator = mAnimationResetInterpolator = decelerateInterpolator;

        mTargetViewDelegate = new NormalTargetViewDelegate();
        mTargetViewDelegate.attachRefreshLoadLayout(this);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    public void setRefreshView(RefreshView view) {
        if (mRefreshView != null) {
            reset();
            removeView(mRefreshView);
        }

        mRefreshView = view;
        if (mRefreshView == null) {
            throw new NullPointerException();
        }

        //add view
        mRefreshView.setVisibility(View.GONE);
        mRefreshView.attachRefreshLoadLayout(this);
        addView(mRefreshView);
    }

    public void setTargetViewDelegate(RefreshLoadDelegate delegate) {
        reset();
        mTargetViewDelegate = delegate;
        invalidate();
    }

    public float getRefreshTriggerDistance() {
        return mRefreshTriggerDistance;
    }

    public void setRefreshTriggerDistance(float distance) {
        mRefreshTriggerDistance = distance;
    }

    public int getRefreshSlingshotDistance() {
        return mRefreshSlingshotDistance;
    }

    public void setRefreshSlingshotDistance(int distance) {
        mRefreshSlingshotDistance = distance;
    }

    public void setDragLimitDistance(int distance) {
        mDragLimitDistance = distance;
    }

    public View getTargetView() {
        ensureTarget();
        return mTargetView;
    }

    public RefreshView getRefreshView() {
        return mRefreshView;
    }


    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTargetView);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTargetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTargetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTargetView, -1) || mTargetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTargetView, -1);
        }
    }

    /**
     * Set a callback to override {@link SwipeRefreshLayout#canChildScrollUp()} method. Non-null
     * callback will return the value provided by the callback and ignore all internal logic.
     *
     * @param callback Callback that should be called when canChildScrollUp() is called.
     */
    public void setOnChildScrollUpCallback(@Nullable OnChildScrollUpCallback callback) {
        mChildScrollUpCallback = callback;
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public void setRefreshing(boolean refreshing) {
        setRefreshing(refreshing, true);
    }

    private void setRefreshing(boolean refreshing, boolean active) {
        if (mRefreshing == refreshing) return;
        mRefreshing = refreshing;
        if (mRefreshing) {
            if (active) {
                refreshAnimate(TYPE_ACTIVE);
            } else {
                refreshAnimate(TYPE_PASSIVE);
            }
        } else {
            refreshAnimate(TYPE_RESET);
        }
    }

    /**
     * @param success
     * @param delay
     */
    public void setRefreshingCompleted(boolean success, int delay) {
        if (!mRefreshing) return;
        mTargetViewDelegate.onRefreshCompleted(success);
        if (mRefreshView != null) {
            mRefreshView.onRefreshCompleted(success);
        }
        if (delay == 0) {
            mRefreshing = false;
            refreshAnimate(TYPE_RESET);
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRefreshing = false;
                    refreshAnimate(TYPE_RESET);
                }
            }, delay);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    public void setEnabled(boolean enabled, boolean reset) {
        super.setEnabled(enabled);
        if (!enabled && reset) {
            reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;

        if (!isEnabled() || mAnimateInProgress || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTargetViewDelegate.offsetTo(0);
                mRefreshView.offsetTo(0);

                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownY = ev.getY(pointerIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                startDragging(y);
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }


    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTargetView instanceof AbsListView)
                || (mTargetView != null && !ViewCompat.isNestedScrollingEnabled(mTargetView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTargetViewDelegate.measure();
        if (mRefreshView != null)
            mRefreshView.measure();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() == 0) {
            return;
        }
        mTargetViewDelegate.layout();
        if (mRefreshView != null)
            mRefreshView.layout();
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTargetView == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mRefreshView)) {
                    mTargetView = child;
                    break;
                }
            }
        }
    }

//    private boolean isNestedScrollMatchTargetView(View view) {
//        ensureTarget();
//        if (mTargetView == null) return false;
//        if (view == mTargetView) {//The nested scroll view is mTargetView.
//            return true;
//        } else {//The nested scroll view is a child of mTargetView, and it's height match mTargetView's height.
//            return view.getTop() <= 0 && view.getBottom() >= mTargetView.getHeight();
//        }
//    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mAnimateInProgress && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveOffset(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }


    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishOffset(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            moveOffset(mTotalUnconsumed);
        }
    }


    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private int getSlingshotDistance() {
        if (mRefreshView == null || mDragLimitDistance <= mRefreshView.getContentHeight())
            return mRefreshSlingshotDistance;
        return (mDragLimitDistance - mRefreshView.getContentHeight()) / 2;
    }

    @SuppressLint("NewApi")
    private void moveOffset(float overscrollTop) {
        float originalDragPercent = overscrollTop / mRefreshTriggerDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));

        float extraOS = Math.abs(overscrollTop) - mRefreshTriggerDistance;
        float slingshotDist = getSlingshotDistance();
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);

        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;

        float extraMove = (slingshotDist) * tensionPercent * 2;

        int offset = (int) ((slingshotDist * dragPercent) + extraMove);
        offset = (int) Math.min(offset, overscrollTop);

        if (mRefreshView.getVisibility() != View.VISIBLE) {
            mRefreshView.setVisibility(View.VISIBLE);
        }

        mTargetViewDelegate.offsetTo(offset);
        mTargetViewDelegate.onDrag(originalDragPercent, tensionPercent);
        if (mRefreshView != null) {
            mRefreshView.offsetTo(offset);
            mRefreshView.onDrag(originalDragPercent, tensionPercent);
        }
    }


    private void finishOffset(float overscrollTop) {
        if (overscrollTop > mRefreshTriggerDistance) {
            setRefreshing(true, false);
        } else {
            // cancel refresh
            mRefreshing = false;
            mTargetViewDelegate.onCancelDrag();
            if (mRefreshView != null) {
                mRefreshView.onCancelDrag();
            }
            refreshAnimate(TYPE_CANCEL);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = INVALID_POINTER;

        if (!isEnabled() || mAnimateInProgress || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                startDragging(y);

                if (mIsBeingDragged) {
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    if (overscrollTop > 0) {
                        moveOffset(overscrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG,
                            "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                if (mIsBeingDragged) {
                    final float y = ev.getY(pointerIndex);
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    mIsBeingDragged = false;
                    finishOffset(overscrollTop);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

    @SuppressLint("NewApi")
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
            mTargetViewDelegate.onStartDrag();
            if (mRefreshView != null) {
                mRefreshView.onStartDrag();
            }
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void notifyRefreshOrReset() {
        if (mRefreshing) {
            if (mListener != null) {
                mListener.onRefresh();
            }
            mTargetViewDelegate.onRefresh();
            if (mRefreshView != null) {
                mRefreshView.onRefresh();
            }
        } else {
            //reset
            reset();
        }
    }

    private void reset() {
        mRefreshAnimation.reset();
        mRefreshAnimation.cancel();
        clearAnimation();

        mTargetViewDelegate.reset();
        if (mRefreshView != null) {
            mRefreshView.setVisibility(View.GONE);
            mRefreshView.reset();
        }
    }

    private void refreshAnimate(@AnimationType int type) {
        //reset at first,  so the mListenerHandler in Animation can be null.
        //And when you call cancel(), the previous animation can get onAnimationEnd() callback before
        //we change the mRefreshAnimationType.
        mRefreshAnimation.reset();
        mRefreshAnimation.cancel();

        mRefreshAnimationType = type;
        mRefreshAnimation.setDuration(getAnimationDuration(mRefreshAnimationType));
        mRefreshAnimation.setInterpolator(getAnimationInterpolator(mRefreshAnimationType));
        mRefreshAnimation.setAnimationListener(mRefreshAnimationListener);
        clearAnimation();
        startAnimation(mRefreshAnimation);
    }

    private
    @AnimationType int mRefreshAnimationType = TYPE_NONE;

    private Animation mRefreshAnimation = new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (interpolatedTime == 0) return;
            mTargetViewDelegate.onAnimationTransformation(mRefreshAnimationType, interpolatedTime, t);
            if (mRefreshView != null) {
                mRefreshView.onAnimationTransformation(mRefreshAnimationType, interpolatedTime, t);
            }
        }
    };

    private Animation.AnimationListener mRefreshAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mTargetViewDelegate.onAnimationStart(mRefreshAnimationType);
            if (mRefreshView != null) {
                mRefreshView.onAnimationStart(mRefreshAnimationType);
            }
            mAnimateInProgress = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTargetViewDelegate.onAnimationEnd(mRefreshAnimationType);
            if (mRefreshView != null) {
                mRefreshView.onAnimationEnd(mRefreshAnimationType);
            }
            mAnimateInProgress = false;
            notifyRefreshOrReset();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };


    public int getAnimationDuration(@AnimationType int type) {
        switch (type) {
            case TYPE_CANCEL:
                return mAnimationCancelDuration;
            case TYPE_PASSIVE:
                return mAnimationPassiveDuration;
            case TYPE_ACTIVE:
                return mAnimationActiveDuration;
            case TYPE_RESET:
                return mAnimationResetDuration;
            default:
                return 0;
        }
    }

    public void setAnimationDuration(@AnimationType int type, int duration) {
        switch (type) {
            case TYPE_CANCEL:
                mAnimationCancelDuration = duration;
            case TYPE_PASSIVE:
                mAnimationPassiveDuration = duration;
            case TYPE_ACTIVE:
                mAnimationActiveDuration = duration;
            case TYPE_RESET:
                mAnimationResetDuration = duration;
        }
    }

    public Interpolator getAnimationInterpolator(@AnimationType int type) {
        switch (type) {
            case TYPE_CANCEL:
                return mAnimationCancelInterpolator;
            case TYPE_PASSIVE:
                return mAnimationPassiveInterpolator;
            case TYPE_ACTIVE:
                return mAnimationActiveInterpolator;
            case TYPE_RESET:
                return mAnimationResetInterpolator;
            default:
                return null;
        }
    }

    public void setAnimationInterpolator(@AnimationType int type, Interpolator interpolator) {
        switch (type) {
            case TYPE_CANCEL:
                mAnimationCancelInterpolator = interpolator;
            case TYPE_PASSIVE:
                mAnimationPassiveInterpolator = interpolator;
            case TYPE_ACTIVE:
                mAnimationActiveInterpolator = interpolator;
            case TYPE_RESET:
                mAnimationResetInterpolator = interpolator;
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        /**
         * Called when a swipe gesture triggers a refresh.
         */
        void onRefresh();
    }

    /**
     * Classes that wish to override {@link RefreshLoadLayout#canChildScrollUp()} method
     * behavior should implement this interface.
     */
    public interface OnChildScrollUpCallback {
        /**
         * Callback that will be called when {@link RefreshLoadLayout#canChildScrollUp()} method
         * is called to allow the implementer to override its behavior.
         *
         * @param parent RefreshLoadLayout that this callback is overriding.
         * @param child  The child view of SwipeRefreshLayout.
         * @return Whether it is possible for the child view of parent layout to scroll up.
         */
        boolean canChildScrollUp(RefreshLoadLayout parent, @Nullable View child);
    }

}
