package com.omarea.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.omarea.vtools.R;

public class NavItem extends LinearLayout {
    public NavItem(Context context) {
        super(context);
        setLayout(context, null);
    }

    public NavItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayout(context, attrs);
    }

    public NavItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayout(context, attrs);
    }

    public NavItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayout(context, attrs);
    }

    private void setLayout(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.NavItem);

        LayoutInflater.from(context).inflate(R.layout.nav_item, this, true);
        if (attrs != null) {
            String text = "" + attributes.getText(R.styleable.NavItem_text);
            Drawable icon = attributes.getDrawable(R.styleable.NavItem_drawable);
            ((ImageView) (this.findViewById(android.R.id.icon))).setImageDrawable(icon);
            ((TextView) (this.findViewById(android.R.id.title))).setText(text);
        }

        attributes.recycle();
    }

    public CharSequence getText() {
        return ((TextView) (this.findViewById(android.R.id.title))).getText();
    }

    public void setEnabled(boolean enabled) {
        setAlpha(enabled ? 1 : 0.5f);
    }
}
