package com.MarcosDiez.shareviahttp.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.MarcosDiez.shareviahttp.R;

public class RoundedImageButtonWithTextView extends RelativeLayout {

    public RoundedImageButtonWithTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundedImageButtonWithTextView, 0, 0);
        Drawable drawable = typedArray.getDrawable(R.styleable.RoundedImageButtonWithTextView_imageSrc);
        String text = typedArray.getString(R.styleable.RoundedImageButtonWithTextView_text);
        typedArray.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.rounded_image_button_with_text_view, this, true);

        setupImageButton(drawable);
        setupTextView(text);
    }

    private void setupTextView(String text) {
        TextView textView = (TextView) getChildAt(1);
        textView.setText(text);
    }

    private void setupImageButton(Drawable drawable) {
        ImageButton imageButton = (ImageButton) getChildAt(0);
        imageButton.setImageDrawable(drawable);
    }
}
