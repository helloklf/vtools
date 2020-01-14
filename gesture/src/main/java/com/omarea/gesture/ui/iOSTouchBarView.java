package com.omarea.gesture.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.omarea.gesture.util.GlobalState;

public class iOSTouchBarView extends View {
    private Context context = getContext();

    private Paint p = new Paint();
    private float shadowSize = 16f;
    private float lineWeight = 16f;
    private float strokeWidth = 0f;
    private int lineColor = 0;
    private int strokeColor = 0;
    private float margin = 24f;

    public iOSTouchBarView(Context context) {
        super(context);
        init();
    }

    public iOSTouchBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public iOSTouchBarView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init();
    }

    private void init() {
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xf0f0f0f0);
        p.setStrokeWidth(dp2px(context, 8));
        p.setStrokeCap(Paint.Cap.ROUND);
        // p.setShadowLayer(dp2px(context, 4), 0, 0, 0x88000000);

        // setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    void setStyle(int widthPx, int heightPx, int color, int shadowColor, int shadowSizeDp, int lineWeightDp, int strokeWidth, int strokeColor) {
        this.shadowSize = dp2px(context, shadowSizeDp);
        this.lineWeight = dp2px(context, lineWeightDp);
        this.strokeWidth = dp2px(context, strokeWidth);
        this.margin = this.shadowSize + (this.lineWeight / 2f) + this.strokeWidth;
        this.lineColor = color;
        this.strokeColor = strokeColor;

        if (this.shadowSize > 0) {
            p.setShadowLayer(this.shadowSize, 0, 0, shadowColor);
        }

        if (this.strokeWidth > 0) {
            p.setStrokeWidth(this.strokeWidth);
        } else {
            p.setStrokeWidth(0);
        }

        ViewGroup.LayoutParams lp = this.getLayoutParams();
        int h = heightPx;
        int w = widthPx;
        if (h < 1) {
            h = 1;
        }
        if (w < 1) {
            w = 1;
        }
        lp.width = w;
        lp.height = h;
        this.setLayoutParams(lp);
    }

    /**
     * dp转换成px
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    @SuppressLint("DrawAllocation")
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (GlobalState.iosBarColor != -1) {
            if (strokeWidth > 0) {
                p.setStyle(Paint.Style.STROKE);
                if (GlobalState.iosBarColor == Color.WHITE) {
                    p.setColor(Color.BLACK);
                } else {
                    p.setColor(Color.WHITE);
                }
                canvas.drawRoundRect(margin, margin, getWidth() - margin, margin + lineWeight, 20, 20, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(GlobalState.iosBarColor);
            canvas.drawRoundRect(margin, margin, getWidth() - margin, margin + lineWeight, 20, 20, p);
        } else {
            if (strokeWidth > 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setColor(strokeColor);
                canvas.drawRoundRect(margin, margin, getWidth() - margin, margin + lineWeight, 20, 20, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(lineColor);
            canvas.drawRoundRect(margin, margin, getWidth() - margin, margin + lineWeight, 20, 20, p);
        }
    }
}
