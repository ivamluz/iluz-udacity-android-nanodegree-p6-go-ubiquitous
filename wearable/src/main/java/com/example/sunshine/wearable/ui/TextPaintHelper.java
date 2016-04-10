package com.example.sunshine.wearable.ui;

import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.example.sunshine.wearable.R;

/**
 * Created by iluz on 4/10/16.
 */
public class TextPaintHelper {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private final Resources mResources;

    public enum Type {
        TIME(R.color.digital_text_primary, BOLD_TYPEFACE, true),
        DATE(R.color.digital_text_secondary, NORMAL_TYPEFACE, true),
        HIGH_TEMPERATURE(R.color.digital_text_primary, BOLD_TYPEFACE, true),
        LOW_TEMPERATURE(R.color.digital_text_secondary, NORMAL_TYPEFACE, true);

        private int mColor;
        private Typeface mTypeface;
        private boolean mShouldApplyAntialias;

        Type(int color, Typeface typeface, boolean shouldApplyAntialias) {
            mColor = color;
            mTypeface = typeface;
            mShouldApplyAntialias = shouldApplyAntialias;
        }

        public int getColor() {
            return mColor;
        }

        public Typeface getTypeface() {
            return mTypeface;
        }

        public boolean shouldApplyAntialias() {
            return mShouldApplyAntialias;
        }
    }

    public TextPaintHelper(Resources resources) {
        mResources = resources;
    }

    public Paint forType(Type type) {
        Paint paint = new Paint();
        paint.setColor(mResources.getColor(type.getColor()));
        paint.setTypeface(type.getTypeface());
        paint.setAntiAlias(type.shouldApplyAntialias());

        return paint;
    }
}
