package com.example.imam_al_masjid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this, true);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.img_logo);
        TextView txtAppName = findViewById(R.id.txt_app_name);
        com.google.android.material.card.MaterialCardView logoContainer = findViewById(R.id.logo_container);

        // --- STRICT DYNAMIC SCALING ---
        // Every component must increase/decrease depends on its parent or screen width/height.
        // Scaling with screen width is the safest way to maintain proportions.
        
        // 1. Logo Container (Box) - 50% of screen width, perfectly centered.
        ScalingUtils.applyScaledLayout(logoContainer, 0.50f, 0.50f, 0, 0, 0, 0); 
        
        // --- STRICT DYNAMIC CLAY BACKGROUND ---
        // Every shadow, inset, and radius now depends on screen width percentages.
        logoContainer.setBackground(ScalingUtils.createClayDrawable(this, 
                                                                    0.080f, // 8.0% Radius
                                                                    0.015f, // 1.5% Shadow Offset
                                                                    0.025f, // 2.5% Inner Inset
                                                                    0.005f  // 0.5% Stroke Width
                                                                   ));

        // Dynamic Inner Padding for the image within the container
        int innerPadding = ScalingUtils.getScaledSize(this, 0.085f);
        imgLogo.setPadding(innerPadding, innerPadding, innerPadding, innerPadding);

        // 2. App Name Text - 5.5% of screen width (Dynamic Text Size)
        txtAppName.setTextSize(ScalingUtils.getScaledTextSize(this, 0.045f));
        // Margin between Logo and Text - 4% of screen dimensions
        ScalingUtils.applyScaledLayout(txtAppName, -1, -1, 0.04f, 0, 0, 0);

        // 3. Arabic App Name - 4.5% of screen width
        TextView txtArabicName = findViewById(R.id.txt_arabic_name);
        txtArabicName.setTextSize(ScalingUtils.getScaledTextSize(this, 0.065f));
        ScalingUtils.applyScaledLayout(txtArabicName, -1, -1, 0.015f, 0, 0, 0);
        // --- END STRICT DYNAMIC SCALING ---

        // Load Animations (Excellent Animation)
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.anim_fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.anim_slide_up);

        // Apply Primary Animations
        imgLogo.startAnimation(fadeIn);
        txtAppName.startAnimation(slideUp);
        txtArabicName.startAnimation(slideUp);

        // EXTRA: Subtle continuous pulse for 'Excellent' animation effect
        // Started earlier (600ms instead of 2200ms) to fit in the shorter splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> 
            imgLogo.animate().scaleX(1.1f).scaleY(1.1f).setDuration(700).withEndAction(() -> 
                imgLogo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(700).start()
            ).start(), 600);

        // Transition to LoginActivity (Phase 2)
        // Reduced from 5000ms to 2000ms for faster startup
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 2000);
    }
}