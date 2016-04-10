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
        TIME(R.color.digital_text_interactive_primary, R.color.digital_text_ambient_primary, BOLD_TYPEFACE, true),
        DATE(R.color.digital_text_interactive_secondary, R.color.digital_text_ambient_secondary, NORMAL_TYPEFACE, true),
        HIGH_TEMPERATURE(R.color.digital_text_interactive_primary, R.color.digital_text_ambient_primary, BOLD_TYPEFACE, true),
        LOW_TEMPERATURE(R.color.digital_text_interactive_secondary, R.color.digital_text_ambient_secondary, NORMAL_TYPEFACE, true);

        private int mInteractiveColor;
        private int mAmbientColor;
        private Typeface mTypeface;
        private boolean mShouldApplyAntialias;

        Type(int interactiveColor, int ambientColor, Typeface typeface, boolean shouldApplyAntialias) {
            mInteractiveColor = interactiveColor;
            mAmbientColor = ambientColor;
            mTypeface = typeface;
            mShouldApplyAntialias = shouldApplyAntialias;
        }

        public int getInteractiveColor() {
            return mInteractiveColor;
        }

        public int getAmbientColor() {
            return mAmbientColor;
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
        paint.setColor(mResources.getColor(type.getInteractiveColor()));
        paint.setTypeface(type.getTypeface());
        paint.setAntiAlias(type.shouldApplyAntialias());

        return paint;
    }
}
