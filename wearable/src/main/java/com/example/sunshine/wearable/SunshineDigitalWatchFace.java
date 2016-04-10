/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sunshine.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.sunshine.wearable.ui.TextPaintHelper;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineDigitalWatchFace extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineDigitalWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineDigitalWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineDigitalWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private String LOG_TAG = Engine.class.getSimpleName();

        private static final String sDateFormat = "E, MMM d, yyyy";
        private static final int sSeparatorWidth = 80;
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private boolean mRegisteredTimeZoneReceiver = false;

        private Resources mResources;

        private TextPaintHelper mTextPaintHelper;

        private Paint mBackgroundPaint;
        private Paint mSeparatorPaint;

        private Paint mTextPaintTime;
        private Paint mTextPaintDate;
        private Paint mTextPaintHighTemperature;
        private Paint mTextPaintLowTemperature;

        private Bitmap mWeatherIcon;

        private boolean mAmbient;
        private Time mTime;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private int mTapCount;

        private float mXOffsetTime;
        private float mYOffsetTime;

        private float mXOffsetDate;
        private float mYOffsetDate;

        private float mYOffsetSeparator;

        private float mXOffsetWeatherIcon;
        private float mYOffsetWeatherIcon;

        private float mXOffsetHighTemperature;
        private float mYOffsetHighTemperature;

        private float mXOffsetLowTemperature;
        private float mYOffsetLowTemperature;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private float mMarginLeft;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineDigitalWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            mResources = SunshineDigitalWatchFace.this.getResources();

            mTextPaintHelper = new TextPaintHelper(mResources);

            mYOffsetTime = mResources.getDimension(R.dimen.digital_time_y_offset);
            mYOffsetDate = mResources.getDimension(R.dimen.digital_date_y_offset);

            mYOffsetSeparator = mResources.getDimension(R.dimen.digital_separator_y_offset);
            mYOffsetWeatherIcon = mResources.getDimension(R.dimen.digital_weather_icon_y_offset);

            mYOffsetHighTemperature = mResources.getDimension(R.dimen.digital_high_temperature_y_offset);
            mYOffsetLowTemperature = mResources.getDimension(R.dimen.digital_low_temperature_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mResources.getColor(R.color.background_default));

            mTextPaintTime = mTextPaintHelper.forType(TextPaintHelper.Type.TIME);
            mTextPaintDate = mTextPaintHelper.forType(TextPaintHelper.Type.DATE);

            mSeparatorPaint = new Paint();
            mSeparatorPaint.setColor(mResources.getColor(R.color.digital_text_secondary));

            mTextPaintHighTemperature = mTextPaintHelper.forType(TextPaintHelper.Type.HIGH_TEMPERATURE);
            mTextPaintLowTemperature = mTextPaintHelper.forType(TextPaintHelper.Type.LOW_TEMPERATURE);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineDigitalWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineDigitalWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            boolean isRound = insets.isRound();

            Log.d(LOG_TAG, "isRound: " + isRound);


            // TODO: check with instructors a better way to do this.
            calculateLeftMargin(insets);

            adjustPaintingForTime(isRound);
            adjustPaintingForDate(isRound);
            adjustPaintingForHighTemperature(isRound);
            adjustPaintingForLowTemperature(isRound);

            adjustPositioningForWeatherIcon(isRound);
        }

        private void calculateLeftMargin(WindowInsets insets) {
            boolean isChin = insets.getSystemWindowInsetBottom() > 0;
            mMarginLeft = 0f;
            if (isChin) {
                mMarginLeft = mResources.getDimension(R.dimen.digital_chin_left_margin);
            }
        }

        private void adjustPaintingForTime(boolean isRound) {
            mXOffsetTime = mResources.getDimension(isRound ? R.dimen.digital_time_interactive_x_offset_round : R.dimen.digital_time_interactive_x_offset) + mMarginLeft;
            float textSizeTime = mResources.getDimension(isRound ? R.dimen.digital_time_text_size_round : R.dimen.digital_time_text_size);
            mTextPaintTime.setTextSize(textSizeTime);
        }

        private void adjustPaintingForDate(boolean isRound) {
            mXOffsetDate = mResources.getDimension(isRound ? R.dimen.digital_date_interactive_x_offset_round : R.dimen.digital_date_interactive_x_offset) + mMarginLeft;
            float textSizeDate = mResources.getDimension(isRound ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            mTextPaintDate.setTextSize(textSizeDate);
        }

        private void adjustPaintingForHighTemperature(boolean isRound) {
            mXOffsetHighTemperature = mResources.getDimension(isRound ? R.dimen.digital_high_temperature_interactive_x_offset_round : R.dimen.digital_high_temperature_interactive_x_offset) + mMarginLeft;
            float textSize = mResources.getDimension(isRound ? R.dimen.digital_high_temperature_text_size_round : R.dimen.digital_high_temperature_text_size);
            mTextPaintHighTemperature.setTextSize(textSize);
        }

        private void adjustPaintingForLowTemperature(boolean isRound) {
            mXOffsetLowTemperature = mResources.getDimension(isRound ? R.dimen.digital_low_temperature_interactive_x_offset_round : R.dimen.digital_low_temperature_interactive_x_offset) + mMarginLeft;
            float textSize = mResources.getDimension(isRound ? R.dimen.digital_low_temperature_text_size_round : R.dimen.digital_low_temperature_text_size);
            mTextPaintLowTemperature.setTextSize(textSize);
        }

        private void adjustPositioningForWeatherIcon(boolean isRound) {
            mXOffsetWeatherIcon = mResources.getDimension(isRound ? R.dimen.digital_weather_icon_interactive_x_offset_round : R.dimen.digital_weather_icon_interactive_x_offset) + mMarginLeft;
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaintTime.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineDigitalWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background_default : R.color.background_tapped));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();

            String timeText = mAmbient
                    ? String.format("%02d:%02d", mTime.hour, mTime.minute)
                    : String.format("%02d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(timeText, mXOffsetTime, mYOffsetTime, mTextPaintTime);

            String dateText = DateFormat.format(sDateFormat, mTime.toMillis(false)).toString().toUpperCase();
            canvas.drawText(dateText, mXOffsetDate, mYOffsetDate, mTextPaintDate);

            canvas.drawLine(bounds.centerX() - (sSeparatorWidth / 2), mYOffsetSeparator, bounds.centerX() + (sSeparatorWidth / 2), mYOffsetSeparator, mSeparatorPaint);

            if (!isInAmbientMode()) {
                if (mWeatherIcon == null) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_light_clouds);
                    mWeatherIcon = ((BitmapDrawable) drawable).getBitmap();
                }

                float iconYOffset = mYOffsetWeatherIcon - mWeatherIcon.getHeight();
                canvas.drawBitmap(mWeatherIcon, mXOffsetWeatherIcon, iconYOffset, null);
            }

            String highTemperatureText = "25º";
            canvas.drawText(highTemperatureText, mXOffsetHighTemperature, mYOffsetHighTemperature, mTextPaintHighTemperature);

            String lowTemperatureText = "18º";
            canvas.drawText(lowTemperatureText, mXOffsetLowTemperature, mYOffsetLowTemperature, mTextPaintLowTemperature);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
