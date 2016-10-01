package com.MarcosDiez.shareviahttp.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.MarcosDiez.shareviahttp.R;

public class FloatingActionButtonWithText extends RelativeLayout {

    public FloatingActionButtonWithText(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.fab_with_text, this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.FloatingActionButtonWithText, 0, 0);
        try {
            final int drawableRes = typedArray
                    .getResourceId(R.styleable.FloatingActionButtonWithText_imageSrc, -1);
            final String text = typedArray.getString(R.styleable.FloatingActionButtonWithText_text);

            setupImageButton(drawableRes);
            setupTextView(text);
        } finally {
            typedArray.recycle();
        }
    }

    private void setupTextView(String text) {
        TextView textView = (TextView) getChildAt(1);
        textView.setText(text);
    }

    private void setupImageButton(@DrawableRes int drawableRes) {
        FloatingActionButton fab = (FloatingActionButton) getChildAt(0);
        fab.setImageResource(drawableRes);
        fab.setClickable(false);
    }
}
