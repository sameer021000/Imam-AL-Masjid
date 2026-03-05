package com.example.imam_al_masjid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

public class ScalingUtils {

    /**
     * Strictly creates a ClayMorphic Background at runtime using screen percentages.
     * Every value (Radius, Shift, Inset) is dynamic to satisfy Requirement #4.
     */
    public static Drawable createClayDrawable(Context context, float radiusPercent, float shadowPercent, float insetPercent, float strokePercent) {
        
        int radius = getScaledSize(context, radiusPercent);
        int shadowOffset = getScaledSize(context, shadowPercent);
        int innerInset = getScaledSize(context, insetPercent);
        int strokeWidth = getScaledSize(context, strokePercent);

        // 1. Dark Shadow (Bottom-Right)
        GradientDrawable darkShadow = new GradientDrawable();
        darkShadow.setShape(GradientDrawable.RECTANGLE);
        darkShadow.setCornerRadius(radius);
        darkShadow.setColor(ContextCompat.getColor(context, R.color.clay_dark_shadow)); // Corrected Color ID

        // 2. Light Shadow (Top-Left)
        GradientDrawable lightShadow = new GradientDrawable();
        lightShadow.setShape(GradientDrawable.RECTANGLE);
        lightShadow.setCornerRadius(radius);
        lightShadow.setColor(ContextCompat.getColor(context, R.color.clay_light_shadow));

        // 3. Main Body
        GradientDrawable mainBody = new GradientDrawable();
        mainBody.setShape(GradientDrawable.RECTANGLE);
        mainBody.setCornerRadius(radius);
        mainBody.setColor(ContextCompat.getColor(context, R.color.off_white_primary));
        mainBody.setStroke(strokeWidth, ContextCompat.getColor(context, R.color.off_white_surface_highlight));

        Drawable[] layers = {darkShadow, lightShadow, mainBody};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // Set dynamic insets (The "Scaling" logic)
        // Dark Shadow shifted down-right
        layerDrawable.setLayerInset(0, shadowOffset, shadowOffset, 0, 0);
        // Light Shadow shifted up-left
        layerDrawable.setLayerInset(1, 0, 0, shadowOffset, shadowOffset);
        // Main Body centered in the middle
        layerDrawable.setLayerInset(2, innerInset, innerInset, innerInset, innerInset);

        return layerDrawable;
    }

    /**
     * Calculates a value based on a percentage of screen width or height.
     */
    public static int getScaledSize(Context context, float percentage, boolean useHeight) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float base = useHeight ? metrics.heightPixels : metrics.widthPixels;
        return (int) (base * percentage);
    }

    public static int getScaledSize(Context context, float percentage) {
        return getScaledSize(context, percentage, false);
    }

    /**
     * Calculates scaled text size based on screen width.
     * Dividing by scaledDensity ensures the result is in 'SP' units 
     * which setTextSize() expects, resulting in a true percentage of screen width.
     */
    public static float getScaledTextSize(Context context, float percentage) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (metrics.widthPixels * percentage) / (metrics.scaledDensity);
    }

    /**
     * Strictly applies dynamic scaling to a view's layout parameters.
     */
    public static void applyScaledLayout(View view, float widthPercent, float heightPercent,
                                       float marginTop, float marginBottom, 
                                       float marginLeft, float marginRight) {
        Context context = view.getContext();
        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (widthPercent > 0) params.width = getScaledSize(context, widthPercent);
        if (heightPercent > 0) params.height = getScaledSize(context, heightPercent);

        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(
                getScaledSize(context, marginLeft),
                getScaledSize(context, marginTop),
                getScaledSize(context, marginRight),
                getScaledSize(context, marginBottom)
            );
        }
        view.setLayoutParams(params);
    }
}
