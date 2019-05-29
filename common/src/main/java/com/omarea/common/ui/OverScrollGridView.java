package com.omarea.common.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class OverScrollGridView extends GridView {
    private int mMaxOverScrollY = 400;//默认200

    public OverScrollGridView(Context context) {
        super(context);
    }

    public OverScrollGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverScrollGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * 设置最大的回弹距离
     *
     * @param maxOverScrollY
     */
    public void setMaxOverScrollY(int maxOverScrollY) {
        this.mMaxOverScrollY = maxOverScrollY;
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
                maxOverScrollX, mMaxOverScrollY, isTouchEvent);
    }
}
