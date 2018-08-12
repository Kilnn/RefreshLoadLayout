package com.github.kilnn.refreshloadlayout.sample;//package com.kilnn.widget;
//
//import android.content.Context;
//import android.support.annotation.ColorInt;
//import android.support.annotation.ColorRes;
//import android.support.annotation.NonNull;
//import android.support.v4.content.ContextCompat;
//import android.util.DisplayMetrics;
//import android.view.View;
//import android.view.animation.Animation;
//import android.view.animation.Transformation;
//
//import com.kilnn.widget.refreshload.AlphaScaleable;
//import com.kilnn.widget.refreshload.Refresh1View;
//import com.kilnn.widget.refreshload.RefreshView;
//
///**
// * Created by Kilnn on 2017/4/22.
// */
//
//public class DefaultRefreshView extends RefreshView {
//
//    private static final int CIRCLE_DIAMETER = 40;//小的圆圈直径
//    private static final int CIRCLE_DIAMETER_LARGE = 56;//大的圆圈直径
//    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;//默认的进度条颜色
//    private static final int MAX_ALPHA = 255;
//    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);
//    private static final int ALPHA_ANIMATION_DURATION = 300;
//    // Max amount of circle that can be filled by progress during swipe gesture,
//    // where 1.0 is a full circle
//    private static final float MAX_PROGRESS_ANGLE = .8f;
//
//
//    private CircleImageView mCircleView;
//    private MaterialProgressDrawable mProgress;
//    private int mCircleDiameter;//圆圈的直径，提供两个尺寸，一大一小，默认使用小的尺寸
//
//    private Animation mAlphaStartAnimation;
//    private Animation mAlphaMaxAnimation;
//
//    public DefaultRefreshView(@NonNull Context context) {
//        super(context);
//
//        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
//        mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
//
//        mCircleView = new CircleImageView(context, CIRCLE_BG_LIGHT);
//        mProgress = new MaterialProgressDrawable(context, this);
//        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
//        mCircleView.setImageDrawable(mProgress);
//
//        addView(mCircleView);
//        measureContent();
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        measureContent();
//    }
//
//    private void measureContent() {
//        measure(
//                View.MeasureSpec.makeMeasureSpec(mCircleDiameter, View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(mCircleDiameter, View.MeasureSpec.EXACTLY)
//        );
//    }
//
//    @Override
//    public void startDragging() {
//        mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
//    }
//
//    private static boolean isAnimationRunning(Animation animation) {
//        return animation != null && animation.hasStarted() && !animation.hasEnded();
//    }
//
//    private void startProgressAlphaStartAnimation() {
//        mAlphaStartAnimation = startAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
//    }
//
//    private void startProgressAlphaMaxAnimation() {
//        mAlphaMaxAnimation = startAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
//    }
//
//    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
//        // Pre API 11, alpha is used in place of scale. Don't also use it to
//        // show the trigger point.
//        Animation alpha = new Animation() {
//            @Override
//            public void applyTransformation(float interpolatedTime, Transformation t) {
//                mProgress.setAlpha(
//                        (int) (startingAlpha + ((endingAlpha - startingAlpha) * interpolatedTime)));
//            }
//        };
//        alpha.setDuration(ALPHA_ANIMATION_DURATION);
//        // Clear out the previous animation listeners.
//        mCircleView.setAnimationListener(null);
//        mCircleView.clearAnimation();
//        mCircleView.startAnimation(alpha);
//        return alpha;
//    }
//
//    /**
//     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
//     */
//    private boolean isAlphaUsedForScale() {
//        return android.os.Build.VERSION.SDK_INT < 11;
//    }
//
//    @Override
//    public void onDragging(float originalDragPercent, float tensionPercent, boolean scale) {
//        mProgress.showArrow(true);
//
//        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
//        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
//
//        if (!scale || !isAlphaUsedForScale()) {
//            if (originalDragPercent < 1) {
//                if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
//                        && !isAnimationRunning(mAlphaStartAnimation)) {
//                    // Animate the alpha
//                    startProgressAlphaStartAnimation();
//                }
//            } else {
//                if (mProgress.getAlpha() < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
//                    // Animate the alpha
//                    startProgressAlphaMaxAnimation();
//                }
//            }
//        }
//
//        float strokeStart = adjustedPercent * .8f;
//        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
//        mProgress.setArrowScale(Math.min(1f, adjustedPercent));
//
//        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
//        mProgress.setProgressRotation(rotation);
//    }
//
//    @Override
//    public void cancelDragging() {
//        mProgress.setStartEndTrim(0f, 0f);
//        mProgress.showArrow(false);
//    }
//
//    @Override
//    public void reset() {
//        mProgress.stop();
//        setColorViewAlpha(MAX_ALPHA);
//    }
//
//    @Override
//    public void onRefreshing() {
//        // Make sure the progress view is fully visible
//        mProgress.setAlpha(MAX_ALPHA);
//        mProgress.start();
//    }
//
//
//    /**
//     * One of DEFAULT, or LARGE.
//     */
//    public void setSize(int size) {
//        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
//            return;
//        }
//        final DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
//        if (size == MaterialProgressDrawable.LARGE) {
//            mCircleDiameter = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
//        } else {
//            mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
//        }
//        // force the bounds of the progress circle inside the circle view to
//        // update by setting it to null before updating its size and then
//        // re-setting it
//        mCircleView.setImageDrawable(null);
//        mProgress.updateSizes(size);
//        mCircleView.setImageDrawable(mProgress);
//    }
//
//    private void setColorViewAlpha(int targetAlpha) {
//        mCircleView.getBackground().setAlpha(targetAlpha);
//        mProgress.setAlpha(targetAlpha);
//    }
//
//    @Override
//    public int getCurrentAlpha() {
//        return mProgress.getAlpha();
//    }
//
//    @Override
//    public void setCurrentAlpha(int alpha) {
//        setColorViewAlpha(alpha);
//    }
//
//    /**
//     * @deprecated Use {@link #setProgressBackgroundColorSchemeResource(int)}
//     */
//    @Deprecated
//    public void setProgressBackgroundColor(int colorRes) {
//        setProgressBackgroundColorSchemeResource(colorRes);
//    }
//
//    /**
//     * Set the background color of the progress spinner disc.
//     *
//     * @param colorRes Resource id of the color.
//     */
//    public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes) {
//        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getContext(), colorRes));
//    }
//
//    /**
//     * Set the background color of the progress spinner disc.
//     *
//     * @param color
//     */
//    public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
//        mCircleView.setBackgroundColor(color);
//        mProgress.setBackgroundColor(color);
//    }
//
//    /**
//     * @deprecated Use {@link #setColorSchemeResources(int...)}
//     */
//    @SuppressWarnings("ResourceType")
//    @Deprecated
//    public void setColorScheme(@ColorInt int... colors) {
//        setColorSchemeResources(colors);
//    }
//
//    /**
//     * Set the color resources used in the progress animation from color resources.
//     * The first color will also be the color of the bar that grows in response
//     * to a user swipe gesture.
//     *
//     * @param colorResIds
//     */
//    public void setColorSchemeResources(@ColorRes int... colorResIds) {
//        final Context context = getContext();
//        int[] colorRes = new int[colorResIds.length];
//        for (int i = 0; i < colorResIds.length; i++) {
//            colorRes[i] = ContextCompat.getColor(context, colorResIds[i]);
//        }
//        setColorSchemeColors(colorRes);
//    }
//
//    /**
//     * Set the colors used in the progress animation. The first
//     * color will also be the color of the bar that grows in response to a user
//     * swipe gesture.
//     *
//     * @param colors
//     */
//    public void setColorSchemeColors(@ColorInt int... colors) {
////        ensureTarget();
//        mProgress.setColorSchemeColors(colors);
//    }
//
//
//}
