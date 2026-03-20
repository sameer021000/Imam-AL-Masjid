package com.example.imam_al_masjid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * A custom view that illustrates the celestial orbit (Sun/Moon position) for prayer times.
 */
public class CelestialOrbitView extends View {

    private Paint ringPaint;
    private Paint nodePaint;
    private Paint glowPaint;
    
    private float angleDegrees = 270f; // Default to Fajr (Left Horizon)
    private boolean isNightMode = false; // If true, draws a Crescent Moon
    private float pulseAlpha = 1.0f;
    private int accentColor;
    private int moonColor;
    private int activeMoonColor;

    public CelestialOrbitView(Context context) {
        super(context);
        init(context);
    }

    public CelestialOrbitView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode()) {
            accentColor = Color.parseColor("#FFD23F");
            moonColor = Color.parseColor("#AAB8C2");
            activeMoonColor = Color.parseColor("#FAF9F6");
        } else {
            accentColor = ContextCompat.getColor(context, R.color.celestial_gold);
            moonColor = ContextCompat.getColor(context, R.color.lunar_moon_white);
            activeMoonColor = ContextCompat.getColor(context, R.color.prayer_card_active_moon_color);
        }
        
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ScalingUtils.getScaledSize(context, 0.003f));
        ringPaint.setColor(accentColor);
        ringPaint.setAlpha(80); 

        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setColor(accentColor);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
    }

    public void setOrbitState(float degrees, boolean isNightEmblem, int color, boolean isActive) {
        this.angleDegrees = degrees;
        this.isNightMode = isNightEmblem;
        
        // If it's active moon, use the high-contrast color. Otherwise use standard moon white.
        int currentMoonColor = isActive ? activeMoonColor : moonColor;
        
        // If it's the moon, we use the moon color logic for the ring too
        int finalColor = isNightEmblem ? currentMoonColor : color;
        this.accentColor = finalColor;
        
        ringPaint.setColor(finalColor);
        // Ensure the moon's orbit is solid enough to see on any background (especially white active card in dark mode)
        ringPaint.setAlpha(isNightEmblem ? (isActive ? 180 : 100) : 120); 
        nodePaint.setColor(finalColor);
        invalidate();
    }




    public void setPulse(float alpha) {
        this.pulseAlpha = alpha;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = (Math.min(getWidth(), getHeight()) / 2f) - ringPaint.getStrokeWidth();
        
        // Safety guard for layout preview (Requirement #4): if radius is 0, skip drawing to avoid RadialGradient exception
        if (radius <= 0) return;

        // 1. Draw Orbit Ring
        canvas.drawCircle(centerX, centerY, radius, ringPaint);

        // 2. Calculate Node Position on circumference
        // 0 degrees is top in our logic (subtracting 90 because math polar 0 is right)
        double angleRad = Math.toRadians(angleDegrees - 90);
        float nodeX = (float) (centerX + radius * Math.cos(angleRad));
        float nodeY = (float) (centerY + radius * Math.sin(angleRad));

        float nodeSize = radius * 0.35f;

        // 3. Draw Pulse Glow (if active)
        if (pulseAlpha < 1.0f || pulseAlpha > 0.0f) {
            // Use the determined accentColor (which is moonColor if isNightMode is true) for the glow aura
            glowPaint.setShader(new RadialGradient(nodeX, nodeY, nodeSize * 2.5f,
                    new int[]{accentColor, Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            glowPaint.setAlpha((int) (150 * pulseAlpha));
            canvas.drawCircle(nodeX, nodeY, nodeSize * 2.5f, glowPaint);
        }


        // 4. Draw Celestial Emblem (Sun or Moon)
        if (isNightMode) {
            drawCrescentMoon(canvas, nodeX, nodeY, nodeSize);
        } else {
            canvas.drawCircle(nodeX, nodeY, nodeSize, nodePaint);
        }
    }

    private void drawCrescentMoon(Canvas canvas, float x, float y, float size) {
        Paint moonPaint = new Paint(nodePaint);
        // Ensure it uses the correct moon color depending on state (already stored in accentColor)
        moonPaint.setColor(accentColor);
        Path moonPath = new Path();
        moonPath.addCircle(x, y, size, Path.Direction.CW);
        // Subtract another circle offset to create crescent
        Path innerCircle = new Path();
        innerCircle.addCircle(x + (size * 0.4f), y - (size * 0.2f), size, Path.Direction.CW);
        moonPath.op(innerCircle, Path.Op.DIFFERENCE);
        canvas.drawPath(moonPath, moonPaint);
    }

}
