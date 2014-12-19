/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2014 Richard Banasiak
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


package com.banasiak.android.devopstime;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class WatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = WatchFaceService.class.getSimpleName();

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    // Update rate in milliseconds for normal (not ambient) mode.
    private static final long NORMAL_UPDATE_RATE_MS = 500;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


        static final int MSG_UPDATE_TIME = 0;

        // How often {@link #mUpdateTimeHandler} ticks in milliseconds.
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        // Handler to update the time periodically in interactive mode.
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        //Log.v(TAG, "updating time");
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(WatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TimeZone tz = TimeZone.getTimeZone(intent.getStringExtra("time-zone"));
                updateTimeZone(tz);
            }
        };

        private static final String TIME_FORMAT_12 = "h:mm";

        private static final String TIME_FORMAT_24 = "H:mm";

        private static final String PERIOD_FORMAT = "a";

        private static final String TIMEZONE_FORMAT = "zzz";

        private static final String DATESTAMP_FORMAT = "EEE, dd MMM yyyy";

        private static final String TIMESTAMP_FORMAT = "HH:mm:ss Z";

        SimpleDateFormat timeSdf;

        SimpleDateFormat periodSdf;

        SimpleDateFormat timezoneSdf;

        SimpleDateFormat dateStampSdf;

        SimpleDateFormat timeStampSdf;

        Bitmap mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.kevlar);

        Bitmap mBackgroundScaledBitmap;

        Paint mBackgroundPaint;

        Paint mClockPaint;

        Paint mPeriodPaint;

        Paint mTimezonePaint;

        Paint mDatestampPaint;

        Paint mTimestampPaint;

        Paint mEpochPaint;

        int mInteractiveTextColor = getResources().getColor(R.color.white);

        int mAmbientTextColor = getResources().getColor(R.color.silver);

        int mBackgroundColor = getResources().getColor(R.color.black);

        float mXOffset;

        float mYOffset;

        float mPadding;

        Date mDate;

        boolean clockDim = true;

        boolean periodDim = true;

        boolean tzDim = false;

        boolean dateDim = false;

        boolean timeDim = false;

        boolean epochShow = true;

        boolean alwaysUtc = true;

        boolean mIsMute;

        boolean mIsLowBitAmbient;

        boolean mIsRound;

        boolean mRegisteredTimeZoneReceiver = false;


        private void updateTimeZone(TimeZone tz) {
            timeSdf.setTimeZone(tz);
            periodSdf.setTimeZone(tz);
            timezoneSdf.setTimeZone(tz);
            dateStampSdf.setTimeZone(tz);
            timeStampSdf.setTimeZone(tz);
            mDate.setTime(System.currentTimeMillis());
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM | Gravity.RIGHT)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setShowSystemUiTime(false)
                    .setShowUnreadCountIndicator(true)
                    .setStatusBarGravity(Gravity.TOP | Gravity.LEFT)
                    .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .build());

            Resources resources = WatchFaceService.this.getResources();

            if (DateFormat.is24HourFormat(getApplicationContext())) {
                timeSdf = new SimpleDateFormat(TIME_FORMAT_24);
            } else {
                timeSdf = new SimpleDateFormat(TIME_FORMAT_12);
            }

            periodSdf = new SimpleDateFormat(PERIOD_FORMAT);
            timezoneSdf = new SimpleDateFormat(TIMEZONE_FORMAT);
            dateStampSdf = new SimpleDateFormat(DATESTAMP_FORMAT);
            timeStampSdf = new SimpleDateFormat(TIMESTAMP_FORMAT);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);
            mClockPaint = createTextPaint(mInteractiveTextColor);
            mPeriodPaint = createTextPaint(mInteractiveTextColor);
            mTimezonePaint = createTextPaint(mInteractiveTextColor);
            mDatestampPaint = createTextPaint(mAmbientTextColor);
            mTimestampPaint = createTextPaint(mAmbientTextColor);
            mEpochPaint = createTextPaint(mAmbientTextColor);

            // initial setup, can be overridden by companion app
            float clockSize = resources.getDimension(R.dimen.clock_size);
            float markerSize = resources.getDimension(R.dimen.marker_size);
            float timezoneSize = resources.getDimension(R.dimen.timezone_size);
            float datestampSize = resources.getDimension(R.dimen.datestamp_size);
            float timestampSize = resources.getDimension(R.dimen.timestamp_size);
            float epochSize = resources.getDimension(R.dimen.epoch_size);

            mClockPaint.setTextSize(clockSize);
            mPeriodPaint.setTextSize(markerSize);
            mTimezonePaint.setTextSize(timezoneSize);
            mDatestampPaint.setTextSize(datestampSize);
            mTimestampPaint.setTextSize(timestampSize);
            mEpochPaint.setTextSize(epochSize);

            mDate = new Date();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged: " + visible);
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                updateTimeZone(TimeZone.getDefault());

            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
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
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            super.onApplyWindowInsets(insets);

            mIsRound = insets.isRound();

            Resources resources = WatchFaceService.this.getResources();

            if (mIsRound) {
                // dimensions and alignment for round watches
                mXOffset = resources.getDimension(R.dimen.x_offset_round);
                mYOffset = resources.getDimension(R.dimen.y_offset_round);
                mPadding = resources.getDimension(R.dimen.padding);

                mClockPaint.setTextAlign(Paint.Align.CENTER);
                mPeriodPaint.setTextAlign(Paint.Align.CENTER);
                mTimezonePaint.setTextAlign(Paint.Align.CENTER);
                mDatestampPaint.setTextAlign(Paint.Align.CENTER);
                mTimestampPaint.setTextAlign(Paint.Align.CENTER);
                mEpochPaint.setTextAlign(Paint.Align.CENTER);
            } else {
                // dimensions and alignment for square watches
                mXOffset = resources.getDimension(R.dimen.x_offset);
                mYOffset = resources.getDimension(R.dimen.y_offset);
                mPadding = resources.getDimension(R.dimen.padding);

                mClockPaint.setTextAlign(Paint.Align.RIGHT);
                mPeriodPaint.setTextAlign(Paint.Align.RIGHT);
                mTimezonePaint.setTextAlign(Paint.Align.RIGHT);
                mDatestampPaint.setTextAlign(Paint.Align.RIGHT);
                mTimestampPaint.setTextAlign(Paint.Align.RIGHT);
                mEpochPaint.setTextAlign(Paint.Align.RIGHT);
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            mIsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                    + ", low-bit ambient = " + mIsLowBitAmbient);

        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            adjustPaintColorToCurrentMode(mClockPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mPeriodPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mTimezonePaint, mInteractiveTextColor, mAmbientTextColor);

            // these are always ambient
            //adjustPaintColorToCurrentMode(mDatestampPaint, mInteractiveTextColor, mAmbientTextColor);
            //adjustPaintColorToCurrentMode(mTimestampPaint, mInteractiveTextColor, mAmbientTextColor);
            //adjustPaintColorToCurrentMode(mEpochPaint, mInteractiveTextColor, mAmbientTextColor);
            //adjustPaintColorToCurrentMode(mEpochLabelPaint, mInteractiveTextColor, mAmbientTextColor);

            // When this property is set to true, the screen supports fewer bits for each color in
            // ambient mode. In this case, watch faces should disable anti-aliasing in ambient mode.
            if (mIsLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mClockPaint.setAntiAlias(antiAlias);
                mPeriodPaint.setAntiAlias(antiAlias);
                mTimezonePaint.setAntiAlias(antiAlias);
                mDatestampPaint.setAntiAlias(antiAlias);
                mTimestampPaint.setAntiAlias(antiAlias);
                mEpochPaint.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter
                    == android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE;

            if (mIsMute != inMuteMode) {
                mIsMute = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            // where the magic happens...
            mDate.setTime(System.currentTimeMillis());

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            if (isInAmbientMode()) {
                // black background
                canvas.drawRect(0, 0, width, height, mBackgroundPaint);
            } else {
                if (mBackgroundScaledBitmap == null
                        || mBackgroundScaledBitmap.getWidth() != width
                        || mBackgroundScaledBitmap.getHeight() != height) {
                    mBackgroundScaledBitmap = Bitmap
                            .createScaledBitmap(mBackgroundBitmap, width, height, true);
                }
                // fancy image background
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            // Update the strings
            String clockString = timeSdf.format(mDate);
            String periodString = periodSdf.format(mDate);
            String timezoneString = timezoneSdf.format(mDate);
            String datestampString = dateStampSdf.format(mDate);
            String timestampString = timeStampSdf.format(mDate);
            String epochString = getResources().getString(R.string.epoch) + " " + String
                    .valueOf(mDate.getTime() / 1000);

            float xClock, yClock;
            float xPeriod, yPeriod;
            float xTimezone, yTimezone;
            float xDatestamp, yDatestamp;
            float xTimestamp, yTimestamp;
            float xEpoch, yEpoch;

            // calculate text paint bounds
            Rect periodBounds = new Rect();
            mPeriodPaint.getTextBounds(periodString, 0, periodString.length(), periodBounds);

            Rect timezoneBounds = new Rect();
            mTimezonePaint
                    .getTextBounds(timezoneString, 0, timezoneString.length(), timezoneBounds);

            Rect clockBounds = new Rect();
            mClockPaint.getTextBounds(clockString, 0, clockString.length(), clockBounds);

            Rect datestampBounds = new Rect();
            mDatestampPaint.getTextBounds(datestampString, 0, datestampString.length(),
                    datestampBounds);

            Rect timestampBounds = new Rect();
            mTimestampPaint.getTextBounds(timestampString, 0, timestampString.length(),
                    timestampBounds);

            Rect epochBounds = new Rect();
            mEpochPaint.getTextBounds(epochString, 0, epochString.length(), epochBounds);

            // calculate offsets
            if (mIsRound) {
                // round offsets == align center
                xClock = width / 2 - timezoneBounds.width() / 2;
                yClock = 0 + mYOffset + clockBounds.height();

                xPeriod = xClock + clockBounds.width() / 2 + mPadding + periodBounds.width() / 2;
                yPeriod = 0 + mYOffset + clockBounds.height() / 2 - (
                        (clockBounds.height() / 2 - periodBounds.height()) / 2);

                xTimezone = xClock + clockBounds.width() / 2 + mPadding
                        + timezoneBounds.width() / 2;
                yTimezone = 0 + mYOffset + clockBounds.height() / 2 + (
                        (clockBounds.height() / 2 - timezoneBounds.height()) / 2) + timezoneBounds
                        .height();

                xDatestamp = width / 2;
                yDatestamp = yClock + mPadding + datestampBounds.height();

                xTimestamp = width / 2;
                yTimestamp = yDatestamp + mPadding + timestampBounds.height();

                xEpoch = width / 2;
                yEpoch = yTimestamp + mPadding * 2 + epochBounds.height();

            } else {
                // square offsets == align right
                xPeriod = width - mXOffset;
                yPeriod = 0 + mYOffset + clockBounds.height() / 2 - (
                        (clockBounds.height() / 2 - periodBounds.height()) / 2);

                xTimezone = width - mXOffset;
                yTimezone = 0 + mYOffset + clockBounds.height() / 2 + (
                        (clockBounds.height() / 2 - timezoneBounds.height()) / 2) + timezoneBounds
                        .height();

                xClock = xTimezone - mPadding - mTimezonePaint.measureText(timezoneString);
                yClock = 0 + mYOffset + clockBounds.height();

                xDatestamp = width - mXOffset;
                yDatestamp = yClock + mPadding + datestampBounds.height();

                xTimestamp = width - mXOffset;
                yTimestamp = yDatestamp + mPadding + timestampBounds.height();

                xEpoch = width - mXOffset;
                yEpoch = yTimestamp + mPadding * 3 + epochBounds.height();
            }

            if (isInAmbientMode()) {
                // draw these when ambient
                if (clockDim) {
                    canvas.drawText(clockString, xClock, yClock, mClockPaint);
                }
                if (periodDim) {
                    if (!DateFormat.is24HourFormat(getApplicationContext())) {
                        canvas.drawText(periodString, xPeriod, yPeriod, mPeriodPaint);
                    }
                }
                if (tzDim) {
                    canvas.drawText(timezoneString, xTimezone, yTimezone, mTimezonePaint);
                }
                if (dateDim) {
                    canvas.drawText(datestampString, xDatestamp, yDatestamp, mDatestampPaint);
                }
            } else {
                // draw these when interactive
                canvas.drawText(clockString, xClock, yClock, mClockPaint);
                if (!DateFormat.is24HourFormat(getApplicationContext())) {
                    canvas.drawText(periodString, xPeriod, yPeriod, mPeriodPaint);
                }
                canvas.drawText(timezoneString, xTimezone, yTimezone, mTimezonePaint);
                canvas.drawText(datestampString, xDatestamp, yDatestamp, mDatestampPaint);
                canvas.drawText(timestampString, xTimestamp, yTimestamp, mTimestampPaint);
                if (epochShow) {
                    canvas.drawText(epochString, xEpoch, yEpoch, mEpochPaint);
                }
            }

        }

        // Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
        // stops it if it shouldn't be running but currently is.
        private void updateTimer() {
            Log.d(TAG, "updateTimer");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        // Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
        // should only run when we're visible and in interactive mode.
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void updateConfigDataItemAndUiOnStartup() {
            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new WatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // use the newly received settings
                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    if (!dataItem.getUri().getPath().equals(WatchFaceUtil.PATH_WITH_FEATURE)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    Log.d(TAG, "Config DataItem updated:" + config);
                    updateUiForConfigDataMap(config);
                }
            } finally {
                dataEvents.close();
            }
        }

        private void updateUiForConfigDataMap(final DataMap dataMap) {
            Log.d(TAG, "updateUiForConfigDataMap: " + dataMap);

            float density = getResources().getDisplayMetrics().density;

            // font sizes scaled appropriately based on screen density
            float clockSize =
                    dataMap.getInt(WatchFaceUtil.KEY_CLOCK_SIZE, WatchFaceUtil.KEY_CLOCK_SIZE_DEF)
                            * density;
            float periodSize =
                    dataMap.getInt(WatchFaceUtil.KEY_MARKER_SIZE, WatchFaceUtil.KEY_MARKER_SIZE_DEF)
                            * density;
            float tzSize = dataMap.getInt(WatchFaceUtil.KEY_TZ_SIZE, WatchFaceUtil.KEY_TZ_SIZE_DEF)
                    * density;
            float dateSize =
                    dataMap.getInt(WatchFaceUtil.KEY_DATE_SIZE, WatchFaceUtil.KEY_DATE_SIZE_DEF)
                            * density;
            float timeSize =
                    dataMap.getInt(WatchFaceUtil.KEY_TIME_SIZE, WatchFaceUtil.KEY_TIME_SIZE_DEF)
                            * density;
            float epochSize =
                    dataMap.getInt(WatchFaceUtil.KEY_EPOCH_SIZE, WatchFaceUtil.KEY_EPOCH_SIZE_DEF)
                            * density;

            // visability flags
            clockDim = dataMap
                    .getBoolean(WatchFaceUtil.KEY_CLOCK_DIM, WatchFaceUtil.KEY_CLOCK_DIM_DEF);
            periodDim = dataMap
                    .getBoolean(WatchFaceUtil.KEY_MARKER_DIM, WatchFaceUtil.KEY_MARKER_DIM_DEF);
            tzDim = dataMap.getBoolean(WatchFaceUtil.KEY_TZ_DIM, WatchFaceUtil.KEY_TZ_DIM_DEF);
            dateDim = dataMap
                    .getBoolean(WatchFaceUtil.KEY_DATE_DIM, WatchFaceUtil.KEY_DATE_DIM_DEF);
            timeDim = false;
            epochShow = dataMap
                    .getBoolean(WatchFaceUtil.KEY_EPOCH_SHOW, WatchFaceUtil.KEY_EPOCH_SHOW_DEF);
            alwaysUtc = dataMap
                    .getBoolean(WatchFaceUtil.KEY_ALWAYS_UTC, WatchFaceUtil.KEY_ALWAYS_UTC_DEF);

            // set the text sizes accordingly
            mClockPaint.setTextSize(clockSize);
            mPeriodPaint.setTextSize(periodSize);
            mTimezonePaint.setTextSize(tzSize);
            mDatestampPaint.setTextSize(dateSize);
            mTimestampPaint.setTextSize(timeSize);
            mEpochPaint.setTextSize(epochSize);

            // show the timestamp in UTC timezone if appropriate
            if (alwaysUtc) {
                timeStampSdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
            } else {
                timeStampSdf.setTimeZone(TimeZone.getDefault());
            }

            // redraw the canvas
            invalidate();
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected: " + connectionHint);
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup();
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }
}
