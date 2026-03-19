package com.example.imam_al_masjid;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sacred Chronos Dial v4: A 24-hour interactive partitioned clock.
 */
public class ChronosDialView extends View {

    public static class WaqtSegment {
        public String name;
        public long startTotalSeconds;
        public long endTotalSeconds;
        public String startTimeStr;        // Precise version
        public String endTimeStr;          // Precise version
        public String startTimeRounded;    // Border version
        public String endTimeRounded;      // Border version

        public WaqtSegment(String name, long startTotalSeconds, long endTotalSeconds, 
                          String startTimeStr, String endTimeStr,
                          String startTimeRounded, String endTimeRounded) {
            this.name = name;
            this.startTotalSeconds = startTotalSeconds;
            this.endTotalSeconds = endTotalSeconds;
            this.startTimeStr = startTimeStr;
            this.endTimeStr = endTimeStr;
            this.startTimeRounded = startTimeRounded;
            this.endTimeRounded = endTimeRounded;
        }
    }

    private Paint arcPaint, linePaint, markerPaint, centerTextPaint, timerPaint;
    private List<WaqtSegment> segments = new ArrayList<>();
    private int activeSegmentIndex = -1;
    private int tappedSegmentIndex = -1;
    private String centerCountdown = "00:00:00";
    
    private RectF dialRect = new RectF();
    private Typeface jannaBold;
    
    // Animation/State
    private float entryProgress = 0f;
    private boolean isWaqtTappedState = false;

    public ChronosDialView(Context context) {
        super(context);
        init(context);
    }

    public ChronosDialView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        jannaBold = ResourcesCompat.getFont(context, R.font.janna_it_bold_font);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(ScalingUtils.getScaledSize(context, 0.002f));
        linePaint.setColor(ContextCompat.getColor(context, R.color.emerald_primary));
        linePaint.setAlpha(120);

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setTextAlign(Paint.Align.CENTER);
        markerPaint.setTypeface(jannaBold);

        centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setTypeface(jannaBold);

        timerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timerPaint.setTextAlign(Paint.Align.CENTER);
        timerPaint.setTypeface(jannaBold);

        // Animation now triggered manually via triggerEntrance() to sync with UI tabs
    }

    public void triggerEntrance() {
        startEntryAnimation();
    }

    private void startEntryAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1500);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            entryProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void setSegments(List<WaqtSegment> segments) {
        this.segments = segments;
        updateActiveSegment();
        invalidate();
    }

    public void setCenterCountdown(String countdown) {
        this.centerCountdown = countdown;
        updateActiveSegment(); // Continually update active highlight
        invalidate();
    }

    private void updateActiveSegment() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        long nowSec = now.get(java.util.Calendar.HOUR_OF_DAY) * 3600L + 
                      now.get(java.util.Calendar.MINUTE) * 60L + 
                      now.get(java.util.Calendar.SECOND);
        
        activeSegmentIndex = -1;
        for (int i = 0; i < segments.size(); i++) {
            WaqtSegment s = segments.get(i);
            if (isTimeInSegment(nowSec, s)) {
                activeSegmentIndex = i;
                break;
            }
        }
    }

    private boolean isTimeInSegment(long timeSec, WaqtSegment s) {
        if (s.startTotalSeconds <= s.endTotalSeconds) {
            return timeSec >= s.startTotalSeconds && timeSec < s.endTotalSeconds;
        } else {
            // Over midnight wrap
            return timeSec >= s.startTotalSeconds || timeSec < s.endTotalSeconds;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (segments.isEmpty()) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        
        // Use 78% of half-width to leave room for outer markers (12AM, 3AM etc.)
        float rOuter = Math.min(cx, cy) * 0.78f; 
        float rInner = rOuter * 0.6f;

        dialRect.set(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter);

        // 1. Draw Arcs and Partitions
        drawPartitions(canvas, cx, cy, rOuter, rInner);

        // 2. Draw Center Content
        drawCenterDisplay(canvas, cx, cy, rInner);

        // 3. Draw Markers (24h or Tapped Waqt)
        if (isWaqtTappedState && tappedSegmentIndex != -1) {
            drawTappedWaqtMarkers(canvas, cx, cy, rOuter);
        } else {
            draw24hClockMarkers(canvas, cx, cy, rOuter);
        }
    }

    private void drawPartitions(Canvas canvas, float cx, float cy, float rOuter, float rInner) {
        int inactiveColor = ContextCompat.getColor(getContext(), R.color.emerald_primary);
        int activeColor = ContextCompat.getColor(getContext(), R.color.celestial_gold);
        
        RectF innerRect = new RectF(cx - rInner, cy - rInner, cx + rInner, cy + rInner);

        for (int i = 0; i < segments.size(); i++) {
            WaqtSegment s = segments.get(i);
            // 86400 seconds in 24 hours
            float startAngle = (s.startTotalSeconds / 86400f) * 360f - 90f;
            float sweepAngle = calculateSweep(s.startTotalSeconds, s.endTotalSeconds);

            // Entry Animation effect on sweep
            float animSweep = sweepAngle * entryProgress;

            // Highlight Active
            boolean isActive = (i == activeSegmentIndex);
            arcPaint.setColor(isActive ? activeColor : inactiveColor);
            arcPaint.setAlpha(isActive ? 180 : 25);

            // Draw Segment Arc
            Path segmentPath = new Path();
            segmentPath.arcTo(dialRect, startAngle, animSweep);
            segmentPath.arcTo(innerRect, startAngle + animSweep, -animSweep);
            segmentPath.close();
            canvas.drawPath(segmentPath, arcPaint);

            // Draw Boundary Lines
            float lineAngleRad = (float) Math.toRadians(startAngle);
            canvas.drawLine(
                (float) (cx + rInner * Math.cos(lineAngleRad)),
                (float) (cy + rInner * Math.sin(lineAngleRad)),
                (float) (cx + rOuter * Math.cos(lineAngleRad)),
                (float) (cy + rOuter * Math.sin(lineAngleRad)),
                linePaint
            );
        }
    }

    private float calculateSweep(long start, long end) {
        long diff = (end - start + 86400) % 86400;
        return (diff / 86400f) * 360f;
    }

    private void drawCenterDisplay(Canvas canvas, float cx, float cy, float rInner) {
        if (activeSegmentIndex == -1) return;
        WaqtSegment active = segments.get(activeSegmentIndex);

        // Waqt Name
        centerTextPaint.setTextSize(rInner * 0.25f);
        centerTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.emerald_primary));
        canvas.drawText(active.name.toUpperCase(), cx, cy - (rInner * 0.25f), centerTextPaint);

        // Start - End
        centerTextPaint.setTextSize(rInner * 0.12f);
        centerTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        String range = active.startTimeStr + " - " + active.endTimeStr;
        canvas.drawText(range, cx, cy - (rInner * 0.05f), centerTextPaint);

        // Countdown (Large)
        timerPaint.setTextSize(rInner * 0.45f);
        timerPaint.setColor(ContextCompat.getColor(getContext(), R.color.emerald_primary));
        canvas.drawText(centerCountdown, cx, cy + (rInner * 0.45f), timerPaint);
    }

    private void draw24hClockMarkers(Canvas canvas, float cx, float cy, float rOuter) {
        markerPaint.setTextSize(ScalingUtils.getScaledSize(getContext(), 0.028f));
        markerPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        markerPaint.setAlpha(150);

        float markerOffset = ScalingUtils.getScaledSize(getContext(), 0.05f); // Dynamic offset (~35-40dp)
        String[] times = {"12AM", "3AM", "6AM", "9AM", "12PM", "3PM", "6PM", "9PM"};
        for (int i = 0; i < 8; i++) {
            float angle = i * 45f - 90f;
            float angleRad = (float) Math.toRadians(angle);
            float x = (float) (cx + (rOuter + markerOffset) * Math.cos(angleRad));
            float y = (float) (cy + (rOuter + markerOffset) * Math.sin(angleRad)) + (markerPaint.getTextSize() / 2f);
            canvas.drawText(times[i], x, y, markerPaint);
        }
    }

    private void drawTappedWaqtMarkers(Canvas canvas, float cx, float cy, float rOuter) {
        WaqtSegment s = segments.get(tappedSegmentIndex);
        markerPaint.setTextSize(ScalingUtils.getScaledSize(getContext(), 0.032f));
        markerPaint.setColor(ContextCompat.getColor(getContext(), R.color.emerald_primary));
        markerPaint.setAlpha(255);

        float markerOffset = ScalingUtils.getScaledSize(getContext(), 0.06f); // Slightly further for tapped times (~45dp)

        // Start Time at start angle
        float startAngle = (s.startTotalSeconds / 86400f) * 360f - 90f;
        drawTimeMarkerAt(canvas, cx, cy, rOuter, startAngle, s.startTimeRounded, markerOffset);

        // End Time at end angle
        float endAngle = (s.endTotalSeconds / 86400f) * 360f - 90f;
        drawTimeMarkerAt(canvas, cx, cy, rOuter, endAngle, s.endTimeRounded, markerOffset);
    }

    private void drawTimeMarkerAt(Canvas canvas, float cx, float cy, float rOuter, float angle, String time, float offset) {
        float angleRad = (float) Math.toRadians(angle);
        float x = (float) (cx + (rOuter + offset) * Math.cos(angleRad));
        float y = (float) (cy + (rOuter + offset) * Math.sin(angleRad)) + (markerPaint.getTextSize() / 2f);
        canvas.drawText(time, x, y, markerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX() - getWidth() / 2f;
            float y = event.getY() - getHeight() / 2f;
            
            // 1. Calculate radial distance from center
            double distance = Math.sqrt(x * x + y * y);
            float rOuter = Math.min(getWidth() / 2f, getHeight() / 2f) * 0.78f;
            float rInner = rOuter * 0.6f;

            // 2. Ignore center taps (the void area)
            if (distance < rInner) {
                isWaqtTappedState = false;
                invalidate();
                return true; 
            }

            // 3. Determine angle and segment
            double angle = Math.toDegrees(Math.atan2(y, x)) + 90;
            if (angle < 0) angle += 360;

            long clickedSec = (long) (angle / 360f * 86400f);
            boolean matched = false;
            for (int i = 0; i < segments.size(); i++) {
                if (isTimeInSegment(clickedSec, segments.get(i))) {
                    tappedSegmentIndex = i;
                    isWaqtTappedState = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) isWaqtTappedState = false;
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    // Tap away logic (call from Fragment)
    public void resetTapState() {
        isWaqtTappedState = false;
        invalidate();
    }
}
