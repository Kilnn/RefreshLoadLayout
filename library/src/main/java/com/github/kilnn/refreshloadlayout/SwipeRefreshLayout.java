//package com.github.kilnn.refreshloadlayout;//package com.kilnn.widget.refreshload;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.res.TypedArray;
//import android.support.annotation.Nullable;
//import android.support.v4.view.MotionEventCompat;
//import android.support.v4.view.NestedScrollingChild;
//import android.support.v4.view.NestedScrollingChildHelper;
//import android.support.v4.view.NestedScrollingParent;
//import android.support.v4.view.NestedScrollingParentHelper;
//import android.support.v4.view.ViewCompat;
//import android.util.AttributeSet;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewConfiguration;
//import android.view.ViewGroup;
//import android.view.animation.Animation;
//import android.view.animation.DecelerateInterpolator;
//import android.view.animation.Transformation;
//import android.view.animation.TranslateAnimation;
//import android.widget.AbsListView;
//
///**
// * Created by Kilnn on 2017/4/21.
// * {@link android.support.v4.widget.SwipeRefreshLayout} (version:24.2.1)
// */
//
//public class SwipeRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
//
//    private static final String TAG = SwipeRefreshLayout.class.getSimpleName();
//
//    private static final int INVALID_POINTER = -1;
//    private static final int MAX_ALPHA = 255;
//    private static final float DRAG_RATE = 0.5f;
//    private static final int DEFAULT_TRIGGER_REFRESH_DISTANCE = 64;
//
//    private static final int SCALE_DOWN_DURATION = 150;
//    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;
//    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
//    private static final int ANIMATE_TO_START_DURATION = 200;
//
//    private static final int[] LAYOUT_ATTRS = new int[]{
//            android.R.attr.enabled
//    };
//
//    //constant values
//    private final int mTouchSlop;
//    private final int mMediumAnimationDuration;
//    private final DecelerateInterpolator mDecelerateInterpolator;
//
//    // If nested scrolling is enabled, the total amount that needed to be
//    // consumed by this as the nested scrolling parent is used in place of the
//    // overscroll determined by MOVE events in the onTouch handler
//    private float mTotalUnconsumed;
//    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
//    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
//    private final int[] mParentScrollConsumed = new int[2];
//    private final int[] mParentOffsetInWindow = new int[2];
//    private boolean mNestedScrollInProgress;
//
//    //motion event values
//    private float mInitialMotionY;
//    private float mInitialDownY;
//    private boolean mIsBeingDragged;
//    private int mActivePointerId = INVALID_POINTER;
//
//    //animations
//    private Animation mScaleAnimation;
//    private Animation mScaleDownAnimation;
//    private Animation mScaleDownToStartAnimation;
//
//    //listener
//    private OnRefreshListener mListener;
//    private OnChildScrollUpCallback mChildScrollUpCallback;
//
//    private View mTarget; // the target of the gesture
//    private RefreshView mRefreshView;
//    private int mRefreshViewIndex = -1;
//
//    private boolean mRefreshing = false;
//    // Target is returning to its start offset because it was cancelled or a
//    // refresh was triggered.
//    private boolean mReturningToStart;
//    private int mFrom;
//    private float mStartingScale;
//
//    /**
//     * 下拉过程中是否可以缩放
//     */
//    private boolean mScale;
//
//    private boolean mOffsetInit;
//
//    /**
//     * 在dragging过程中，mRefreshView出现的最开始位置的Top。
//     * 这个值应该是 getPaddingTop-mRefreshViewHeight，但是为了能在视图未完成onLayout前，也能调用setRefresh，所以使其成为变量，能够完成主动赋值
//     */
//    private int mRefreshViewOriginalOffsetTop;
//
//    /**
//     * 整个视图当前偏移的距离
//     */
//    private int mCurrentTargetOffsetTop;
//
//    /**
//     * 手势移动多长距离就认为可以触发刷新。这个手势移动的距离，进行了一个缩放{@link #DRAG_RATE}。
//     * 所以是实际 MOTION_DOWN 到 MOTION_MOVE 两倍的距离时，才会>=mTriggerRefreshDistance，触发刷新。
//     * 而且SwipeRefreshLayout的实现中，手势滑动的距离和视图本身偏移不同，滑动的距离会进行一个阻尼系数的计算。
//     * 简单讲，这个值只是结合滑动事件的距离来触发刷新，并不是视图本身的偏移。
//     */
//    private float mTriggerRefreshDistance;
//
//    private boolean mNotify;
//
//    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
//        @Override
//        public void onAnimationStart(Animation animation) {
//        }
//
//        @Override
//        public void onAnimationRepeat(Animation animation) {
//        }
//
//        @Override
//        public void onAnimationEnd(Animation animation) {
//            if (mRefreshing) {
//                mRefreshView.onRefreshing();
//                if (mNotify) {
//                    if (mListener != null) {
//                        mListener.onRefresh();
//                    }
//                }
//                mCurrentTargetOffsetTop = mRefreshView.getTop() - mRefreshViewOriginalOffsetTop;
//            } else {
//                reset();
//            }
//        }
//    };
//
//    void reset() {
//        if (mRefreshView != null) {
//            mRefreshView.clearAnimation();
//            mRefreshView.reset();
//            mRefreshView.setVisibility(View.GONE);
//            // Return the circle to its start position
//            if (mScale) {
//                setAnimationProgress(0 /* animation complete and view is hidden */);
//            } else {
//                setTargetOffsetTopAndBottom(mRefreshViewOriginalOffsetTop + mCurrentTargetOffsetTop,
//                        true /* requires update */);
//            }
//            mCurrentTargetOffsetTop = mRefreshView.getTop() - mRefreshViewOriginalOffsetTop;
//        }
//    }
//
//    @Override
//    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
//        if (!enabled) {
//            reset();
//        }
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        reset();
//    }
//
//    public void setRefreshViewScaleable(boolean scaleable) {
//        mScale = scaleable;
//    }
//
//    public boolean getRefreshViewScaleable() {
//        return mScale;
//    }
//
//
//    public SwipeRefreshLayout(Context context) {
//        this(context, null);
//    }
//
//    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
//        super(context, attrs);
//
//        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
//
//        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
//
//        setWillNotDraw(false);
//        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
//
//        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
//
//        final DisplayMetrics metrics = getResources().getDisplayMetrics();
//        mTriggerRefreshDistance = (int) (DEFAULT_TRIGGER_REFRESH_DISTANCE * metrics.density);
//
//        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
//
//        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
//        setNestedScrollingEnabled(true);
//
//        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
//        setEnabled(a.getBoolean(0, true));
//        a.recycle();
//    }
//
//    public void setRefreshView(RefreshView view) {
//        if (mRefreshView != null) removeView(mRefreshView);
//
//        mRefreshView = view;
//        if (mRefreshView == null) {
//            throw new NullPointerException();
//        }
//
//        //reset status
//        mOffsetInit = false;
//
//        initOffset(mRefreshView.getPreMeasureHeight());
//
//        //add view
//        mRefreshView.setVisibility(View.GONE);
//        addView(mRefreshView);
//    }
//
//    private void initOffset(int refreshViewHeight) {
//        if (refreshViewHeight == 0) return;
//        if (!mOffsetInit) {
//            mRefreshViewOriginalOffsetTop = getPaddingTop() - refreshViewHeight;
//            mCurrentTargetOffsetTop = 0;
//            mOffsetInit = true;
//        }
//    }
//
//    @Override
//    protected int getChildDrawingOrder(int childCount, int i) {
//        if (mRefreshViewIndex < 0) {
//            return i;
//        } else if (i == childCount - 1) {
//            // Draw the selected child last
//            return mRefreshViewIndex;
//        } else if (i >= mRefreshViewIndex) {
//            // Move the children after the selected child earlier one
//            return i + 1;
//        } else {
//            // Keep the children before the selected child the same
//            return i;
//        }
//    }
//
//    /**
//     * Set the listener to be notified when a refresh is triggered via the swipe
//     * gesture.
//     */
//    public void setOnRefreshListener(OnRefreshListener listener) {
//        mListener = listener;
//    }
//
//    /**
//     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
//     */
//    private boolean isAlphaUsedForScale() {
//        return android.os.Build.VERSION.SDK_INT < 11;
//    }
//
//    /**
//     * Notify the widget that refresh state has changed. Do not call this when
//     * refresh is triggered by a swipe gesture.
//     *
//     * @param refreshing Whether or not the view should show refresh progress.
//     */
//    public void setRefreshing(boolean refreshing) {
//        if (mRefreshing == refreshing) return;
//        if (refreshing) {
//            // scale and show
////            mRefreshing = true;
////            int endTarget = mRefreshViewEndOffsetTop + mRefreshViewOriginalOffsetTop;
////            setTargetOffsetTopAndBottom(endTarget - mCurrentOffsetTop,
////                    true /* requires update */);
////            mNotify = true;
////            animateOffsetToCorrectPosition(mCurrentOffsetTop, mRefreshListener);
////            startScaleUpAnimation(mRefreshListener);
//        } else {
//            setRefreshing(false, false /* notify */);
//        }
//    }
//
//    @SuppressLint("NewApi")
//    private void startScaleUpAnimation(Animation.AnimationListener listener) {
//        mRefreshView.setVisibility(View.VISIBLE);
//        if (android.os.Build.VERSION.SDK_INT >= 11) {
//            // Pre API 11, alpha is used in place of scale up to show the
//            // progress circle appearing.
//            // Don't adjust the alpha during appearance otherwise.
//            mRefreshView.setCurrentAlpha(MAX_ALPHA);
//        }
//        Log.e("Kilnn", "mScale:" + ViewCompat.getScaleX(mRefreshView));
//
//        ViewCompat.setScaleX(mRefreshView, 1.0f);
//        ViewCompat.setScaleY(mRefreshView, 1.0f);
//        mScaleAnimation = new TranslateAnimation(
//                Animation.RELATIVE_TO_SELF, 0,
//                Animation.RELATIVE_TO_SELF, 0,
//                Animation.RELATIVE_TO_SELF, -1,
//                Animation.RELATIVE_TO_SELF, 0
//        );
////        mScaleAnimation = new Animation() {
////            @Override
////            public void applyTransformation(float interpolatedTime, Transformation t) {
////                setAnimationProgress(interpolatedTime);
////            }
////        };
//        mScaleAnimation.setDuration(mMediumAnimationDuration);
//        if (listener != null) {
//            mRefreshView.setAnimationListener(listener);
//        }
//        mRefreshView.clearAnimation();
//        mRefreshView.startAnimation(mScaleAnimation);
//    }
//
//
//    /**
//     * Pre API 11, this does an alpha animation.
//     *
//     * @param progress
//     */
//    void setAnimationProgress(float progress) {
//        if (isAlphaUsedForScale()) {
//            mRefreshView.setCurrentAlpha((int) (progress * MAX_ALPHA));
//        } else {
//            ViewCompat.setScaleX(mRefreshView, progress);
//            ViewCompat.setScaleY(mRefreshView, progress);
//        }
//    }
//
//    private void setRefreshing(boolean refreshing, final boolean notify) {
//        if (mRefreshing != refreshing) {
//            mNotify = notify;
//            ensureTarget();
//            mRefreshing = refreshing;
//            if (mRefreshing) {
//                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop + mRefreshViewOriginalOffsetTop, mRefreshListener);
//            } else {
////                animateOffsetToStartPosition(mCurrentOffsetTop, mRefreshListener);
//                startScaleDownAnimation(mRefreshListener);
//            }
//        }
//    }
//
//    void startScaleDownAnimation(Animation.AnimationListener listener) {
//        mScaleDownAnimation = new Animation() {
//            @Override
//            public void applyTransformation(float interpolatedTime, Transformation t) {
//                setAnimationProgress(1 - interpolatedTime);
//            }
//        };
//        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
//        mRefreshView.setAnimationListener(listener);
//        mRefreshView.clearAnimation();
//        mRefreshView.startAnimation(mScaleDownAnimation);
//    }
//
//    /**
//     * @return Whether the SwipeRefreshWidget is actively showing refresh
//     * progress.
//     */
//    public boolean isRefreshing() {
//        return mRefreshing;
//    }
//
//
//    private void ensureTarget() {
//        // Don't bother getting the parent height if the parent hasn't been laid
//        // out yet.
//        if (mTarget == null) {
//            for (int i = 0; i < getChildCount(); i++) {
//                View child = getChildAt(i);
//                if (!child.equals(mRefreshView)) {
//                    mTarget = child;
//                    break;
//                }
//            }
//        }
//    }
//
//    /**
//     * Set the distance to trigger a sync in dips
//     *
//     * @param distance
//     */
//    public void setDistanceToTriggerRefresh(int distance) {
//        mTriggerRefreshDistance = distance;
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        if (getChildCount() == 0) {
//            return;
//        }
//        if (mTarget == null) {
//            ensureTarget();
//        }
//        if (mTarget == null) {
//            return;
//        }
//
//        final int width = getMeasuredWidth();
//        final int height = getMeasuredHeight();
//
//        final View child = mTarget;
//        final int childLeft = getPaddingLeft();
//        final int childTop = getPaddingTop();
//        final int childWidth = width - getPaddingLeft() - getPaddingRight();
//        final int childHeight = height - getPaddingTop() - getPaddingBottom();
//        child.layout(childLeft, childTop + mCurrentTargetOffsetTop, childLeft + childWidth, childTop + childHeight + mCurrentTargetOffsetTop);
//
//        if (mRefreshView != null) {
//            int headerViewWidth = mRefreshView.getMeasuredWidth();
//            int headerViewHeight = mRefreshView.getMeasuredHeight();
//            mRefreshView.layout((width / 2 - headerViewWidth / 2), childTop - headerViewHeight + mCurrentTargetOffsetTop,
//                    (width / 2 + headerViewWidth / 2), childTop + childHeight + mCurrentTargetOffsetTop);
//        }
//    }
//
//    @Override
//    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        if (mTarget == null) {
//            ensureTarget();
//        }
//        if (mTarget == null) {
//            return;
//        }
//        mTarget.measure(MeasureSpec.makeMeasureSpec(
//                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
//                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
//                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
//
//        mRefreshViewIndex = -1;
//
//        if (mRefreshView != null) {
//            mRefreshView.measure();
//
//            initOffset(mRefreshView.getMeasuredHeight());
//
//            // Get the index of the circleview.
//            for (int index = 0; index < getChildCount(); index++) {
//                if (getChildAt(index) == mRefreshView) {
//                    mRefreshViewIndex = index;
//                    break;
//                }
//            }
//
//        }
//    }
//
//    /**
//     * @return Whether it is possible for the child view of this layout to
//     * scroll up. Override this if the child view is a custom view.
//     */
//    public boolean canChildScrollUp() {
//        if (mChildScrollUpCallback != null) {
//            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
//        }
//        if (android.os.Build.VERSION.SDK_INT < 14) {
//            if (mTarget instanceof AbsListView) {
//                final AbsListView absListView = (AbsListView) mTarget;
//                return absListView.getChildCount() > 0
//                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
//                        .getTop() < absListView.getPaddingTop());
//            } else {
//                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
//            }
//        } else {
//            return ViewCompat.canScrollVertically(mTarget, -1);
//        }
//    }
//
//    /**
//     * Set a callback to override {@link SwipeRefreshLayout#canChildScrollUp()} method. Non-null
//     * callback will return the value provided by the callback and ignore all internal logic.
//     *
//     * @param callback Callback that should be called when canChildScrollUp() is called.
//     */
//    public void setOnChildScrollUpCallback(@Nullable OnChildScrollUpCallback callback) {
//        mChildScrollUpCallback = callback;
//    }
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        ensureTarget();
//
//        final int action = MotionEventCompat.getActionMasked(ev);
//        int pointerIndex;
//
//        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
//            mReturningToStart = false;
//        }
//
//        if (!isEnabled() || mReturningToStart || canChildScrollUp()
//                || mRefreshing || mNestedScrollInProgress || mRefreshView == null) {
//            // Fail fast if we're not in a state where a swipe is possible
//            return false;
//        }
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                setTargetOffsetTopAndBottom(mRefreshViewOriginalOffsetTop - mRefreshView.getTop(), true);//还原至开始偏移的位置
//                mActivePointerId = ev.getPointerId(0);
//                mIsBeingDragged = false;
//
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    return false;
//                }
//                mInitialDownY = ev.getY(pointerIndex);
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                if (mActivePointerId == INVALID_POINTER) {
//                    Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
//                    return false;
//                }
//
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    return false;
//                }
//                final float y = ev.getY(pointerIndex);
//                startDragging(y);
//                break;
//
//            case MotionEventCompat.ACTION_POINTER_UP:
//                onSecondaryPointerUp(ev);
//                break;
//
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                mIsBeingDragged = false;
//                mActivePointerId = INVALID_POINTER;
//                break;
//        }
//
//        return mIsBeingDragged;
//    }
//
//    @Override
//    public void requestDisallowInterceptTouchEvent(boolean b) {
//        // if this is a List < L or another view that doesn't support nested
//        // scrolling, ignore this request so that the vertical scroll event
//        // isn't stolen
//        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
//                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
//            // Nope.
//        } else {
//            super.requestDisallowInterceptTouchEvent(b);
//        }
//    }
//
//
//    // NestedScrollingParent
//
//    @Override
//    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
//        return isEnabled() && !mReturningToStart && !mRefreshing
//                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
//    }
//
//    @Override
//    public void onNestedScrollAccepted(View child, View target, int axes) {
//        // Reset the counter of how much leftover scroll needs to be consumed.
//        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
//        // Dispatch up to the nested parent
//        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
//        mTotalUnconsumed = 0;
//        mNestedScrollInProgress = true;
//    }
//
//    @Override
//    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
//        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
//        // before allowing the list to scroll
//        if (dy > 0 && mTotalUnconsumed > 0) {
//            if (dy > mTotalUnconsumed) {
//                consumed[1] = dy - (int) mTotalUnconsumed;
//                mTotalUnconsumed = 0;
//            } else {
//                mTotalUnconsumed -= dy;
//                consumed[1] = dy;
//            }
//            moveSpinner(mTotalUnconsumed);
//        }
//
//        // If a client layout is using a custom start position for the circle
//        // view, they mean to hide it again before scrolling the child view
//        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
//        // the circle so it isn't exposed if its blocking content is moved
////        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
////                && Math.abs(dy - consumed[1]) > 0) {
////            mRefreshView.setVisibility(View.GONE);
////        }
//
//        // Now let our nested parent consume the leftovers
//        final int[] parentConsumed = mParentScrollConsumed;
//        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
//            consumed[0] += parentConsumed[0];
//            consumed[1] += parentConsumed[1];
//        }
//    }
//
//    @Override
//    public int getNestedScrollAxes() {
//        return mNestedScrollingParentHelper.getNestedScrollAxes();
//    }
//
//    @Override
//    public void onStopNestedScroll(View target) {
//        mNestedScrollingParentHelper.onStopNestedScroll(target);
//        mNestedScrollInProgress = false;
//        // Finish the spinner for nested scrolling if we ever consumed any
//        // unconsumed nested scroll
//        if (mTotalUnconsumed > 0) {
//            finishSpinner(mTotalUnconsumed);
//            mTotalUnconsumed = 0;
//        }
//        // Dispatch up our nested parent
//        stopNestedScroll();
//    }
//
//    @Override
//    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
//                               final int dxUnconsumed, final int dyUnconsumed) {
//        // Dispatch up to the nested parent first
//        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
//                mParentOffsetInWindow);
//
//        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
//        // sometimes between two nested scrolling views, we need a way to be able to know when any
//        // nested scrolling parent has stopped handling events. We do that by using the
//        // 'offset in window 'functionality to see if we have been moved from the event.
//        // This is a decent indication of whether we should take over the event stream or not.
//        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
//        if (dy < 0 && !canChildScrollUp()) {
//            mTotalUnconsumed += Math.abs(dy);
//            moveSpinner(mTotalUnconsumed);
//        }
//    }
//
//    // NestedScrollingChild
//
//    @Override
//    public void setNestedScrollingEnabled(boolean enabled) {
//        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
//    }
//
//    @Override
//    public boolean isNestedScrollingEnabled() {
//        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
//    }
//
//    @Override
//    public boolean startNestedScroll(int axes) {
//        return mNestedScrollingChildHelper.startNestedScroll(axes);
//    }
//
//    @Override
//    public void stopNestedScroll() {
//        mNestedScrollingChildHelper.stopNestedScroll();
//    }
//
//    @Override
//    public boolean hasNestedScrollingParent() {
//        return mNestedScrollingChildHelper.hasNestedScrollingParent();
//    }
//
//    @Override
//    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
//        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
//                dxUnconsumed, dyUnconsumed, offsetInWindow);
//    }
//
//    @Override
//    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
//        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
//    }
//
//    @Override
//    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
//        return dispatchNestedPreFling(velocityX, velocityY);
//    }
//
//    @Override
//    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
//        return dispatchNestedFling(velocityX, velocityY, consumed);
//    }
//
//    @Override
//    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
//        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
//    }
//
//    @Override
//    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
//        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
//    }
//
//    private void moveSpinner(float overscrollTop) {
//        float originalDragPercent = overscrollTop / mTriggerRefreshDistance;
//
//        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
//
//        float extraOS = Math.abs(overscrollTop) - mTriggerRefreshDistance;
//        float slingshotDist = mRefreshView.getHeight();
//        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
//                / slingshotDist);
//        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
//                (tensionSlingshotPercent / 4), 2)) * 2f;
//        float extraMove = (slingshotDist) * tensionPercent * 2;
//
//        int targetY = mRefreshViewOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
//        // where 1.0f is a full circle
//        if (mRefreshView.getVisibility() != View.VISIBLE) {
//            mRefreshView.setVisibility(View.VISIBLE);
//        }
//        if (!mScale) {
//            ViewCompat.setScaleX(mRefreshView, 1f);
//            ViewCompat.setScaleY(mRefreshView, 1f);
//        }
//
//        if (mScale) {
//            setAnimationProgress(Math.min(1f, overscrollTop / mTriggerRefreshDistance));
//        }
//
//        mRefreshView.onDragging(originalDragPercent, tensionPercent, mScale);
//
//        setTargetOffsetTopAndBottom(targetY - (mCurrentTargetOffsetTop + mRefreshViewOriginalOffsetTop), true /* requires update */);
//    }
//
//    private void finishSpinner(float overscrollTop) {
//        if (overscrollTop > mTriggerRefreshDistance) {
//            setRefreshing(true, true /* notify */);
//        } else {
//            // cancel refresh
//            mRefreshing = false;
//            Animation.AnimationListener listener = null;
//            if (!mScale) {
//                listener = new Animation.AnimationListener() {
//
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        if (!mScale) {
//                            startScaleDownAnimation(null);
//                        }
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//                    }
//
//                };
//            }
//            animateOffsetToStartPosition(mCurrentTargetOffsetTop + mRefreshViewOriginalOffsetTop, listener);
//            mRefreshView.cancelDragging();
//        }
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        final int action = MotionEventCompat.getActionMasked(ev);
//        int pointerIndex = -1;
//
//        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
//            mReturningToStart = false;
//        }
//
//        if (!isEnabled() || mReturningToStart || canChildScrollUp()
//                || mRefreshing || mNestedScrollInProgress || mRefreshView == null) {
//            // Fail fast if we're not in a state where a swipe is possible
//            return false;
//        }
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                mActivePointerId = ev.getPointerId(0);
//                mIsBeingDragged = false;
//                break;
//
//            case MotionEvent.ACTION_MOVE: {
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
//                    return false;
//                }
//
//                final float y = ev.getY(pointerIndex);
//                startDragging(y);
//
//                if (mIsBeingDragged) {
//                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
//                    if (overscrollTop > 0) {
//                        moveSpinner(overscrollTop);
//                    } else {
//                        return false;
//                    }
//                }
//                break;
//            }
//            case MotionEventCompat.ACTION_POINTER_DOWN: {
//                pointerIndex = MotionEventCompat.getActionIndex(ev);
//                if (pointerIndex < 0) {
//                    Log.e(TAG,
//                            "Got ACTION_POINTER_DOWN event but have an invalid action index.");
//                    return false;
//                }
//                mActivePointerId = ev.getPointerId(pointerIndex);
//                break;
//            }
//
//            case MotionEventCompat.ACTION_POINTER_UP:
//                onSecondaryPointerUp(ev);
//                break;
//
//            case MotionEvent.ACTION_UP: {
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    Log.e(TAG, "Got ACTION_UP event but don't have an active pointer id.");
//                    return false;
//                }
//
//                if (mIsBeingDragged) {
//                    final float y = ev.getY(pointerIndex);
//                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
//                    mIsBeingDragged = false;
//                    finishSpinner(overscrollTop);
//                }
//                mActivePointerId = INVALID_POINTER;
//                return false;
//            }
//            case MotionEvent.ACTION_CANCEL:
//                return false;
//        }
//
//        return true;
//    }
//
//    private void startDragging(float y) {
//        final float yDiff = y - mInitialDownY;
//        if (yDiff > mTouchSlop && !mIsBeingDragged) {
//            mInitialMotionY = mInitialDownY + mTouchSlop;
//            mIsBeingDragged = true;
//            mRefreshView.startDragging();
//        }
//    }
//
//    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
//        mFrom = from;
//        mAnimateToCorrectPosition.reset();
//        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
//        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
//        if (listener != null) {
//            mRefreshView.setAnimationListener(listener);
//        }
//        mRefreshView.clearAnimation();
//        mRefreshView.startAnimation(mAnimateToCorrectPosition);
//    }
//
//    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
//        if (mScale) {
//            // Scale the item back down
//            startScaleDownReturnToStartAnimation(from, listener);
//        } else {
//            mFrom = from;
//            mAnimateToStartPosition.reset();
//            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
//            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
//            if (listener != null) {
//                mRefreshView.setAnimationListener(listener);
//            }
//            mRefreshView.clearAnimation();
//            mRefreshView.startAnimation(mAnimateToStartPosition);
//        }
//    }
//
//    private final Animation mAnimateToCorrectPosition = new Animation() {
//        @Override
//        public void applyTransformation(float interpolatedTime, Transformation t) {
////            int targetTop = 0;
////            int endTarget = mRefreshViewEndOffsetTop - Math.abs(mRefreshViewOriginalOffsetTop);
////
////            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
////            int offset = targetTop - mRefreshView.getTop();
////            setTargetOffsetTopAndBottom(offset, false /* requires update */);
////            mProgress.setArrowScale(1 - interpolatedTime);TODO
//        }
//    };
//
//    /**
//     * 移动到初始位置
//     *
//     * @param interpolatedTime
//     */
//    void moveToStart(float interpolatedTime) {
//        int targetTop = 0;
//        targetTop = (mFrom + (int) ((mRefreshViewOriginalOffsetTop - mFrom) * interpolatedTime));
//        int offset = targetTop - mRefreshView.getTop();
//        setTargetOffsetTopAndBottom(offset, false /* requires update */);
//    }
//
//    /**
//     * 移动到视图刷新位置
//     *
//     * @param interpolatedTime
//     */
//    void moveToEnd(float interpolatedTime) {
////        int targetTop = 0;
////        targetTop = (mFrom + (int) ((mRefreshViewEndOffsetTop - mTriggerRefreshDistance - mFrom) * interpolatedTime));
////        int offset = targetTop - mRefreshView.getTop();
////        setTargetOffsetTopAndBottom(offset, false /* requires update */);
//    }
//
//    private final Animation mAnimateToStartPosition = new Animation() {
//        @Override
//        public void applyTransformation(float interpolatedTime, Transformation t) {
//            moveToStart(interpolatedTime);
//        }
//    };
//
//    /**
//     * 当视图返回到初始位置时，并且需要缩放是，所执行的过度动画。
//     *
//     * @param from
//     * @param listener
//     */
//    private void startScaleDownReturnToStartAnimation(int from, Animation.AnimationListener listener) {
//        mFrom = from;
//        if (isAlphaUsedForScale()) {
//            mStartingScale = mRefreshView.getCurrentAlpha();
//        } else {
//            mStartingScale = ViewCompat.getScaleX(mRefreshView);
//        }
//        mScaleDownToStartAnimation = new Animation() {
//            @Override
//            public void applyTransformation(float interpolatedTime, Transformation t) {
//                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
//                setAnimationProgress(targetScale);
//                moveToStart(interpolatedTime);
//            }
//        };
//        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
//        if (listener != null) {
//            mRefreshView.setAnimationListener(listener);
//        }
//        mRefreshView.clearAnimation();
//        mRefreshView.startAnimation(mScaleDownToStartAnimation);
//    }
//
//    void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
//        ViewCompat.offsetTopAndBottom(mRefreshView, offset);
//        ensureTarget();
//        if (mTarget != null) {
//            ViewCompat.offsetTopAndBottom(mTarget, offset);
//        }
//        mCurrentTargetOffsetTop = mRefreshView.getTop() - mRefreshViewOriginalOffsetTop;
//        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
//            invalidate();
//        }
//    }
//
//    private void onSecondaryPointerUp(MotionEvent ev) {
//        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
//        final int pointerId = ev.getPointerId(pointerIndex);
//        if (pointerId == mActivePointerId) {
//            // This was our active pointer going up. Choose a new
//            // active pointer and adjust accordingly.
//            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
//            mActivePointerId = ev.getPointerId(newPointerIndex);
//        }
//    }
//
//    /**
//     * Classes that wish to be notified when the swipe gesture correctly
//     * triggers a refresh should implement this interface.
//     */
//    public interface OnRefreshListener {
//        /**
//         * Called when a swipe gesture triggers a refresh.
//         */
//        void onRefresh();
//    }
//
//    /**
//     * Classes that wish to override {@link SwipeRefreshLayout#canChildScrollUp()} method
//     * behavior should implement this interface.
//     */
//    public interface OnChildScrollUpCallback {
//        /**
//         * Callback that will be called when {@link SwipeRefreshLayout#canChildScrollUp()} method
//         * is called to allow the implementer to override its behavior.
//         *
//         * @param parent SwipeRefreshLayout that this callback is overriding.
//         * @param child  The child view of SwipeRefreshLayout.
//         * @return Whether it is possible for the child view of parent layout to scroll up.
//         */
//        boolean canChildScrollUp(SwipeRefreshLayout parent, @Nullable View child);
//    }
//}
