package com.example.imam_al_masjid;

import android.os.Bundle;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navDock;
    private View activeNavView;
    private int currentNavIndex = -1;

    private static final String KEY_NAV_INDEX = "active_nav_index";
    private static final int DEFAULT_NAV_INDEX = 2; // Home/Dashboard

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply persisted theme BEFORE inflating any layout
        ThemeManager.applyTheme(this, false);
        androidx.activity.EdgeToEdge.enable(this,
            androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        );
        setContentView(R.layout.activity_main);
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initViews();
        
        // Determine which nav item to highlight (restore after theme-change recreate)
        int navIndex = DEFAULT_NAV_INDEX;
        if (savedInstanceState != null) {
            navIndex = savedInstanceState.getInt(KEY_NAV_INDEX, DEFAULT_NAV_INDEX);
        }
        
        setupNavigation(navIndex);
        switchFragment(navIndex);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_NAV_INDEX, currentNavIndex);
    }

    private void initViews() {
        navDock = findViewById(R.id.nav_dock);
        applyDynamicScaling();
        applyClaymorphism();
    }

    private void setupNavigation(int initialNavIndex) {
        addNavItem(R.drawable.ic_profile, "Profile", 0, initialNavIndex);
        addNavItem(R.drawable.ic_calendar, "Events", 1, initialNavIndex);
        addNavItem(R.drawable.ic_home, "Home", 2, initialNavIndex);
        addNavItem(R.drawable.ic_edit, "Edit", 3, initialNavIndex);
        addNavItem(R.drawable.ic_settings, "Settings", 4, initialNavIndex);
    }

    private void addNavItem(int iconRes, String title, int index, int initialNavIndex) {
        View itemView = getLayoutInflater().inflate(R.layout.nav_item, navDock, false);
        ImageView icon = itemView.findViewById(R.id.nav_icon);
        android.widget.TextView titleView = itemView.findViewById(R.id.nav_title);
        
        icon.setImageResource(iconRes);
        titleView.setText(title);

        // Scaling for nav item (Reduced to prevent text wrapping)
        int padding = ScalingUtils.getScaledSize(this, 0.02f); 
        int iconSize = ScalingUtils.getScaledSize(this, 0.052f);
        
        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        iconParams.width = iconSize;
        iconParams.height = iconSize;
        icon.setLayoutParams(iconParams);
        
        // Dynamically scale text size and force single line
        titleView.setTextSize(ScalingUtils.getScaledTextSize(this, 0.027f));
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // Dynamically scale margins and heights to strictly remove hardcoded values in XML
        ScalingUtils.applyScaledLayout(titleView, -1, -1, 0.01f, 0, 0, 0); // 1% margin top
        View underlineView = itemView.findViewById(R.id.nav_underline);
        ScalingUtils.applyScaledLayout(underlineView, -1, 0.005f, 0.01f, 0, 0, 0); // 0.5% height, 1% margin top
        
        itemView.setPadding(padding, padding, padding, padding);

        // Add Horizontal Margin between items
        LinearLayout.LayoutParams itemParams = (LinearLayout.LayoutParams) itemView.getLayoutParams();
        int horizontalMargin = ScalingUtils.getScaledSize(this, 0.015f); // Reduced horizontal spacing
        itemParams.setMargins(horizontalMargin, 0, horizontalMargin, 0);
        itemView.setLayoutParams(itemParams);

        itemView.setOnClickListener(v -> {
            if (currentNavIndex != index) {
                switchFragment(index);
                updateNavUI(itemView);
            }
        });

        navDock.addView(itemView);
        
        // Highlight the restored/initial nav item
        if (index == initialNavIndex) {
            updateNavUI(itemView);
        }
    }

    private void switchFragment(int index) {
        Fragment fragment;
        switch (index) {
            case 0: fragment = new ProfileFragment(); break;
            case 1: fragment = new EventsFragment(); break;
            case 3: fragment = new EditTimingsFragment(); break;
            case 4: fragment = new SettingsFragment(); break;
            default: fragment = new DashboardFragment(); break;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        
        // Premium Transitions (Slide + Scale/Fade)
        if (currentNavIndex != -1) {
            if (index > currentNavIndex) {
                ft.setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_left);
            } else {
                ft.setCustomAnimations(R.anim.slide_in_left, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right);
            }
        }
        
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
        
        currentNavIndex = index;
    }

    private void updateNavUI(View selectedView) {
        // Reset previous active view
        if (activeNavView != null) {
            ImageView oldIcon = activeNavView.findViewById(R.id.nav_icon);
            TextView oldTitle = activeNavView.findViewById(R.id.nav_title);
            View oldUnderline = activeNavView.findViewById(R.id.nav_underline);

            if (oldIcon != null) oldIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
            if (oldTitle != null) oldTitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            if (oldUnderline != null) oldUnderline.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        }

        // Apply new active view
        activeNavView = selectedView;
        if (selectedView != null) {
            ImageView newIcon = selectedView.findViewById(R.id.nav_icon);
            TextView newTitle = selectedView.findViewById(R.id.nav_title);
            View newUnderline = selectedView.findViewById(R.id.nav_underline);

            if (newIcon != null) newIcon.setColorFilter(ContextCompat.getColor(this, R.color.input_active));
            if (newTitle != null) newTitle.setTextColor(ContextCompat.getColor(this, R.color.input_active));
            if (newUnderline != null) newUnderline.setBackgroundColor(ContextCompat.getColor(this, R.color.input_active));
        }
    }

    private void applyDynamicScaling() {
        // Dock scaling
        ScalingUtils.applyScaledLayout(navDock, -1, -1, 0, 0, 0, 0);
        navDock.setPadding(
            ScalingUtils.getScaledSize(this, 0.02f),
            ScalingUtils.getScaledSize(this, 0.01f),
            ScalingUtils.getScaledSize(this, 0.02f),
            ScalingUtils.getScaledSize(this, 0.01f)
        );
        
        // Dynamic bottom padding for the container (replaces hardcoded 16dp)
        View navDockContainer = findViewById(R.id.nav_dock_container);
        navDockContainer.setPadding(0, 0, 0, ScalingUtils.getScaledSize(this, 0.04f));
    }

    private void applyClaymorphism() {
        // Main Dock is Raised (Convex) - Restoring the floating effect
        ScalingUtils.applyClaymorphism(navDock, 
            0.05f, 
            false, 
            ContextCompat.getColor(this, R.color.off_white_primary));
    }
}