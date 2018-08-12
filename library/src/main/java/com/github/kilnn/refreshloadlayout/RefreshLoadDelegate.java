package com.github.kilnn.refreshloadlayout;

import android.view.animation.Transformation;


/**
 * Created by Kilnn on 2017/4/24.
 */

public interface RefreshLoadDelegate {

    //attach RefreshLoadLayout
    void attachRefreshLoadLayout(RefreshLoadLayout layout);

    //layout
    void measure();

    void layout();

    /**
     * offset by a vertical distance.
     *
     * @param offset
     * @return consumed offset
     */
    int offsetBy(int offset);

    void offsetTo(int offset);

    //animate
    void onAnimationStart(@RefreshLoadLayout.AnimationType int animationType);

    void onAnimationEnd(@RefreshLoadLayout.AnimationType int animationType);

    void onAnimationTransformation(@RefreshLoadLayout.AnimationType int animationType, float interpolatedTime, Transformation t);

    //drag states

    /**
     * 开始下拉刷新
     */
    void onStartDrag();

    /**
     * @param dragPercent    下拉系数，如果超过1，说明下拉的距离大于触发刷新的距离。
     * @param tensionPercent 回弹系数
     */
    void onDrag(float dragPercent, float tensionPercent);

    /**
     * 下拉未到一定的距离，刷新取消
     */
    void onCancelDrag();

    /**
     * 触发了刷新
     */
    void onRefresh();

    /**
     * 刷新完成
     *
     * @param success 是刷新数据成功，还是刷新数据失败
     */
    void onRefreshCompleted(boolean success);

    /**
     * 所有动作结束
     */
    void reset();
}
