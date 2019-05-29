package com.omarea.common.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Hello on 2018/05/12.
 */

public class OverScrollView extends ScrollView {

    public OverScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param deltaX         继续滑动x方向的距离
     * @param deltaY         继续滑动y方向的距离     负：表示顶部到头   正：表示底部到头
     * @param scrollX        x方向滑动的距离
     * @param scrollY        y方法滑动的距离
     * @param scrollRangeX
     * @param scrollRangeY
     * @param maxOverScrollX x方向最大可以滚动的距离
     * @param maxOverScrollY y方向最大可以滚动的距离
     * @param isTouchEvent   是手指拖动滑动     false:表示fling靠惯性滑动;
     * @return
     */
    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY,
                maxOverScrollX, 400, isTouchEvent);
    }
}
