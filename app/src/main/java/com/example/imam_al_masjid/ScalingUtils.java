package com.example.imam_al_masjid;
 
import android.content.Context;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

public class ScalingUtils {

    /*
     * Strictly creates a ClayMorphic Background at runtime using screen percentages.
     * Every value (Radius, Shift, Inset) is dynamic.
     */
    /*
     * Creates an INSET (Concave) ClayMorphic background for Input Fields.
     * This creates the "carved into surface" look.
     */
    /**
     * Strictly creates an INSET (Concave) ClayMorphic background with custom colors.
     */
    public static Drawable createInsetClayDrawable(Context context, float radiusPercent, float shadowPercent, float insetPercent, int bodyColor, int shadowColor, int highlightColor) {
        int radius = getScaledSize(context, radiusPercent);
        int shadowOffset = getScaledSize(context, shadowPercent);
        int innerInset = getScaledSize(context, insetPercent);

        GradientDrawable darkShadow = new GradientDrawable();
        darkShadow.setShape(GradientDrawable.RECTANGLE);
        darkShadow.setCornerRadius(radius);
        darkShadow.setColor(shadowColor);

        GradientDrawable lightShadow = new GradientDrawable();
        lightShadow.setShape(GradientDrawable.RECTANGLE);
        lightShadow.setCornerRadius(radius);
        lightShadow.setColor(highlightColor);

        GradientDrawable mainBody = new GradientDrawable();
        mainBody.setShape(GradientDrawable.RECTANGLE);
        mainBody.setCornerRadius(radius);
        mainBody.setColor(bodyColor);

        Drawable[] layers = {darkShadow, lightShadow, mainBody};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        layerDrawable.setLayerInset(0, 0, 0, shadowOffset, shadowOffset); 
        layerDrawable.setLayerInset(1, shadowOffset, shadowOffset, 0, 0);
        layerDrawable.setLayerInset(2, innerInset, innerInset, innerInset, innerInset);

        return layerDrawable;
    }

    public static Drawable createInsetClayDrawable(Context context, float radiusPercent, float shadowPercent, float insetPercent) {
        return createInsetClayDrawable(context, radiusPercent, shadowPercent, insetPercent,
                ContextCompat.getColor(context, R.color.off_white_primary),
                ContextCompat.getColor(context, R.color.clay_dark_shadow),
                ContextCompat.getColor(context, R.color.clay_light_shadow));
    }

    /**
     * Strictly creates a CONVEX (Raised) ClayMorphic background with custom colors.
     */
    public static Drawable createClayDrawable(Context context, float radiusPercent, float shadowPercent, float insetPercent, float strokePercent, int bodyColor, int shadowColor, int highlightColor, int strokeColor) {
        int radius = getScaledSize(context, radiusPercent);
        int shadowOffset = getScaledSize(context, shadowPercent);
        int innerInset = getScaledSize(context, insetPercent);
        int strokeWidth = getScaledSize(context, strokePercent);

        GradientDrawable darkShadow = new GradientDrawable();
        darkShadow.setShape(GradientDrawable.RECTANGLE);
        darkShadow.setCornerRadius(radius);
        darkShadow.setColor(shadowColor);

        GradientDrawable lightShadow = new GradientDrawable();
        lightShadow.setShape(GradientDrawable.RECTANGLE);
        lightShadow.setCornerRadius(radius);
        lightShadow.setColor(highlightColor);

        GradientDrawable mainBody = new GradientDrawable();
        mainBody.setShape(GradientDrawable.RECTANGLE);
        mainBody.setCornerRadius(radius);
        mainBody.setColor(bodyColor);
        if (strokeWidth > 0) {
            mainBody.setStroke(strokeWidth, strokeColor);
        }

        Drawable[] layers = {darkShadow, lightShadow, mainBody};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        layerDrawable.setLayerInset(0, shadowOffset, shadowOffset, 0, 0);
        layerDrawable.setLayerInset(1, 0, 0, shadowOffset, shadowOffset);
        layerDrawable.setLayerInset(2, innerInset, innerInset, innerInset, innerInset);

        return layerDrawable;
    }

    public static Drawable createClayDrawable(Context context, float radiusPercent, float shadowPercent, float insetPercent, float strokePercent) {
        return createClayDrawable(context, radiusPercent, shadowPercent, insetPercent, strokePercent,
                ContextCompat.getColor(context, R.color.off_white_primary),
                ContextCompat.getColor(context, R.color.clay_dark_shadow),
                ContextCompat.getColor(context, R.color.clay_light_shadow),
                ContextCompat.getColor(context, R.color.off_white_surface_highlight));
    }

    /**
     * Strictly creates the Layered Papyrus background (used for Address bar) using percentages.
     */
    public static Drawable createLayeredPapyrusDrawable(Context context) {
        float baseRadius = getScaledSize(context, 0.035f); // ~14dp
        float offsetMajor = getScaledSize(context, 0.015f); // ~6dp
        float offsetMinor = getScaledSize(context, 0.0075f); // ~3dp
        float smallGap = getScaledSize(context, 0.01f); // ~4dp

        // Layer 3: Bottom shadow layer
        GradientDrawable shadow = new GradientDrawable();
        shadow.setShape(GradientDrawable.RECTANGLE);
        shadow.setCornerRadius(baseRadius);
        shadow.setColor(Color.parseColor("#0D000000"));

        // Layer 2: Middle stacked paper layer
        GradientDrawable middle = new GradientDrawable();
        middle.setShape(GradientDrawable.RECTANGLE);
        middle.setCornerRadius(baseRadius);
        middle.setColor(ContextCompat.getColor(context, R.color.off_white_dark));
        middle.setStroke(getScaledSize(context, 0.0025f), ContextCompat.getColor(context, R.color.off_white_grayish));

        // Layer 1: Top main papyrus layer
        GradientDrawable top = new GradientDrawable();
        top.setShape(GradientDrawable.RECTANGLE);
        top.setCornerRadius(baseRadius);
        top.setColor(ContextCompat.getColor(context, R.color.off_white_primary));
        top.setStroke(getScaledSize(context, 0.0035f), ContextCompat.getColor(context, R.color.emerald_alpha_20));

        // Accent: The 'folded' edge highlight (Ivory-Emerald)
        GradientDrawable accent = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#10007F5F"), Color.TRANSPARENT}
        );
        accent.setShape(GradientDrawable.RECTANGLE);
        accent.setCornerRadius(baseRadius);

        Drawable[] layers = {shadow, middle, top, accent};
        LayerDrawable ld = new LayerDrawable(layers);

        // Mirroring the exact inset logic from the XML for visual fidelity
        // setLayerInset(index, left, top, right, bottom)
        
        // Shadow (index 0): android:top="6dp" android:left="4dp"
        ld.setLayerInset(0, (int)smallGap, (int)offsetMajor, 0, 0);
        
        // Middle (index 1): top="3dp" left="2dp" right="2dp" bottom="3dp"
        int mI = (int)offsetMinor;
        ld.setLayerInset(1, mI/2, mI, mI/2, mI);
        
        // Top & Accent (index 2 & 3): bottom="6dp" right="4dp"
        ld.setLayerInset(2, 0, 0, (int)smallGap, (int)offsetMajor);
        ld.setLayerInset(3, 0, 0, (int)smallGap, (int)offsetMajor);

        return ld;
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

    /**
     * Helper to apply Claymorphism to any view using percentages.
     */
    public static void applyClaymorphism(View view, float radiusPercent, boolean isInset, int bodyColor) {
        Context context = view.getContext();
        Drawable clay;
        if (isInset) {
            clay = createInsetClayDrawable(context, radiusPercent, 0.005f, 0.003f, 
                    bodyColor, 
                    ContextCompat.getColor(context, R.color.clay_dark_shadow), 
                    ContextCompat.getColor(context, R.color.clay_light_shadow));
        } else {
            clay = createClayDrawable(context, radiusPercent, 0.008f, 0.005f, 0.002f, 
                    bodyColor, 
                    ContextCompat.getColor(context, R.color.clay_dark_shadow), 
                    ContextCompat.getColor(context, R.color.clay_light_shadow),
                    bodyColor); // No stroke by default
        }
        view.setBackground(clay);
    }
}