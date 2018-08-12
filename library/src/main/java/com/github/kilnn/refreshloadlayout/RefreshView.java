package com.github.kilnn.refreshloadlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by Kilnn on 2017/4/22.
 * You can add any view in RefreshView.
 * But the most important is refresh content, which can changed follow different refresh state.
 * You should layout the refresh content in a striking position if your RefreshView have other views,
 * like a huge background image which higher than refresh content.
 * Style:
 * 1.Normal , move follow drag, and original layout above target view.
 * 2.Reveal , don't move when drag(), and original layout behind target view.
 * 3.Scale , scale follow drag, and original layout behind target view.
 * 4.Float , move follow drag, and original layout float on target view.
 * 5.Resize , size change follow drag ,and original layout behind target view.
 * <p>
 * Your can enable or disable effect after drag distance greater then refresh distance.
 */

public abstract class RefreshView extends FrameLayout implements RefreshLoadDelegate {

    public RefreshView(@NonNull Context context) {
        this(context, null);
    }

    public RefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Refresh content height。
     *
     * @return Default return the view's measured height. If the view has not measured, if will be 0.
     */
    public int getContentHeight() {
        return getMeasuredHeight();
    }

}

