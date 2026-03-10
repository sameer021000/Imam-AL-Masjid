package com.example.imam_al_masjid;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

/**
 * SettingsFragment.java
 * Implements Settings screen: Theme Switching (with persistence), Notification/Vibration/Sound icon
 * toggles (design only), Data Management, and Version display.
 */
public class SettingsFragment extends BaseFragment {

    // Theme toggle tabs
    private TextView themeSystem, themeLight, themeDark;

    // Icon toggles (Design only – ON/OFF state shown via icon)
    private ImageView toggleNotifications, toggleVibration, toggleSound;

    private boolean notifOn = true;
    private boolean vibOn   = true;
    private boolean soundOn = true;

    // Data management
    private LinearLayout syncStatusPill, backupButton;
    private View syncStatusDot;
    private TextView syncStatusText;

    // Current theme selection index (0=System, 1=Light, 2=Dark)
    private int selectedThemeIndex = 0;

    @Override
    protected int getLayoutId() { return R.layout.fragment_settings; }

    @Override
    protected void initViews(View view) {
        if (getContext() == null) return;

        // --- Theme Toggle Tabs ---
        themeSystem = view.findViewById(R.id.settings_theme_system);
        themeLight  = view.findViewById(R.id.settings_theme_light);
        themeDark   = view.findViewById(R.id.settings_theme_dark);

        // --- Icon Toggles ---
        toggleNotifications = view.findViewById(R.id.settings_toggle_notifications);
        toggleVibration     = view.findViewById(R.id.settings_toggle_vibration);
        toggleSound         = view.findViewById(R.id.settings_toggle_sound);

        // --- Data Management ---
        syncStatusPill  = view.findViewById(R.id.settings_sync_status_pill);
        syncStatusDot   = view.findViewById(R.id.settings_sync_status_dot);
        syncStatusText  = view.findViewById(R.id.settings_sync_status_text);
        backupButton    = view.findViewById(R.id.settings_button_backup);

        // === Restore saved theme selection (no recreate, just highlight) ===
        selectedThemeIndex = ThemeManager.getThemeSelectionIndex(getContext());
        // updateThemeToggleUI is called after scaling in applyClaymorphism order,
        // but we need it here too since initViews runs before scaling.
        // It is safe because it only sets colours/backgrounds, not sizes.
        updateThemeToggleUI();

        // === Theme toggle clicks – FIX #6: use post() to avoid immediate navigation ===
        themeSystem.setOnClickListener(v -> onThemeSelected(0));
        themeLight.setOnClickListener( v -> onThemeSelected(1));
        themeDark.setOnClickListener(  v -> onThemeSelected(2));

        // === Icon toggle clicks ===
        toggleNotifications.setOnClickListener(v -> {
            notifOn = !notifOn;
            updateNotifIcon();
        });
        toggleVibration.setOnClickListener(v -> {
            vibOn = !vibOn;
            toggleVibration.setImageResource(vibOn
                    ? R.drawable.ic_settings_vibration_on
                    : R.drawable.ic_settings_vibration_off);
        });
        toggleSound.setOnClickListener(v -> {
            soundOn = !soundOn;
            toggleSound.setImageResource(soundOn
                    ? R.drawable.ic_settings_sound_on
                    : R.drawable.ic_settings_sound_off);
        });

        // === Mock Sync Status (design) ===
        setSyncStatus();

        // === Backup button (sink-on-touch animation) ===
        backupButton.setOnClickListener(v -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        ).start());
    }

    // ================================================================
    //  THEME SELECTION  (FIX #6: Do NOT call setDefaultNightMode if recreate
    //  would navigate away; instead use recreate() only when truly needed)
    // ================================================================
    private void onThemeSelected(int index) {
        if (getContext() == null) return;
        if (index == selectedThemeIndex) return; // Already selected, do nothing

        selectedThemeIndex = index;
        updateThemeToggleUI();

        int mode;
        switch (index) {
            case 1:  mode = AppCompatDelegate.MODE_NIGHT_NO;            break;
            case 2:  mode = AppCompatDelegate.MODE_NIGHT_YES;           break;
            default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        // Save first, then apply – AppCompatDelegate.setDefaultNightMode causes an
        // Activity recreate automatically; the Fragment back-stack is preserved.
        ThemeManager.setTheme(getContext(), mode);
        // The activity recreates itself; no manual navigation happens.
    }

    private void updateThemeToggleUI() {
        if (getContext() == null) return;
        Context ctx = getContext();

        int activeBodyColor   = ContextCompat.getColor(ctx, R.color.status_active);
        int activeTextColor   = ContextCompat.getColor(ctx, R.color.text_inverse);
        int inactiveTextColor = ContextCompat.getColor(ctx, R.color.text_secondary);

        TextView[] tabs = { themeSystem, themeLight, themeDark };
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == selectedThemeIndex);
            if (active) {
                tabs[i].setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.006f, 0.003f, 0f,
                        activeBodyColor,
                        ContextCompat.getColor(ctx, R.color.clay_dark_shadow),
                        ContextCompat.getColor(ctx, R.color.clay_light_shadow),
                        activeBodyColor));
                tabs[i].setTextColor(activeTextColor);
            } else {
                tabs[i].setBackground(null);
                tabs[i].setTextColor(inactiveTextColor);
            }
        }
    }

    // ================================================================
    //  NOTIFICATION ICON
    // ================================================================
    private void updateNotifIcon() {
        toggleNotifications.setImageResource(notifOn
                ? R.drawable.ic_settings_notifications
                : R.drawable.ic_settings_notifications_off);
    }

    // ================================================================
    //  SYNC STATUS DOT
    // ================================================================
    private void setSyncStatus() {
        if (getContext() == null) return;
        // Design: Defaulting to Live status
        syncStatusText.setText(R.string.settings_sync_status_live);
        syncStatusText.setTextColor(ContextCompat.getColor(getContext(), R.color.emerald_primary));
        syncStatusDot.setBackground(makeDotDrawable(ContextCompat.getColor(getContext(), R.color.emerald_primary)));
    }

    private GradientDrawable makeDotDrawable(int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        return dot;
    }

    // ================================================================
    //  DYNAMIC SCALING
    // ================================================================
    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        // Root padding (Applies side padding to both Title and Scroll content)
        View root = view.findViewById(R.id.settings_root);
        int sidePadding = ScalingUtils.getScaledSize(ctx, 0.05f);
        int topPadding  = ScalingUtils.getScaledSize(ctx, 0.04f);
        root.setPadding(sidePadding, topPadding, sidePadding, 0);

        // Scroll root (Bottom padding for scroll space)
        View scrollRoot = view.findViewById(R.id.settings_scroll_root);
        scrollRoot.setPadding(0, 0, 0, topPadding);

        // Screen Title
        TextView screenTitle = view.findViewById(R.id.settings_screen_title);
        screenTitle.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.065f));
        ScalingUtils.applyScaledLayout(screenTitle, -1, -1, 0, 0.01f, 0.03f, 0);

        // Card margins & padding
        int cardHorizPad = ScalingUtils.getScaledSize(ctx, 0.04f);
        int cardVertPad  = ScalingUtils.getScaledSize(ctx, 0.03f);
        int cardMarginV  = ScalingUtils.getScaledSize(ctx, 0.03f);
        for (int id : new int[]{R.id.settings_card_theme, R.id.settings_card_preferences, R.id.settings_card_data_management}) {
            View card = view.findViewById(id);
            card.setPadding(cardHorizPad, cardVertPad, cardHorizPad, cardVertPad);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
            p.setMargins(0, cardMarginV, 0, cardMarginV);
            card.setLayoutParams(p);
        }

        // Section labels
        float sectionLabelSize = ScalingUtils.getScaledTextSize(ctx, 0.034f);
        for (int id : new int[]{R.id.settings_label_theme, R.id.settings_label_preferences, R.id.settings_label_data}) {
            TextView t = view.findViewById(id);
            t.setTextSize(sectionLabelSize);
            ScalingUtils.applyScaledLayout(t, -1, -1, 0, 0.015f, 0, 0);
        }

        // Theme toggle container: fixed height so tabs are vertically centered (FIX #7)
        LinearLayout themeContainer = view.findViewById(R.id.settings_theme_toggle_container);
        themeContainer.getLayoutParams().height = ScalingUtils.getScaledSize(ctx, 0.13f);
        themeContainer.requestLayout();

        // Theme tab text size & inner padding
        int tabPadH = ScalingUtils.getScaledSize(ctx, 0.02f);
        int tabPadV = ScalingUtils.getScaledSize(ctx, 0.015f);
        float tabSize = ScalingUtils.getScaledTextSize(ctx, 0.038f);
        for (TextView tab : new TextView[]{themeSystem, themeLight, themeDark}) {
            tab.setTextSize(tabSize);
            tab.setPadding(tabPadH, tabPadV, tabPadH, tabPadV);
        }

        // Preference rows & icons
        int rowPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        int iconSize = ScalingUtils.getScaledSize(ctx, 0.065f);
        int iconMarginRight = ScalingUtils.getScaledSize(ctx, 0.04f);
        int toggleIconSize = ScalingUtils.getScaledSize(ctx, 0.07f);

        for (int rowId : new int[]{R.id.settings_row_notifications, R.id.settings_row_vibration, R.id.settings_row_sound}) {
            view.findViewById(rowId).setPadding(0, rowPadV, 0, rowPadV);
        }

        // Row left icons (removed from Preferences card - only sync & backup remain)
        for (int iconId : new int[]{R.id.settings_icon_sync, R.id.settings_icon_backup}) {
            ImageView icon = view.findViewById(iconId);
            icon.getLayoutParams().width = iconSize;
            icon.getLayoutParams().height = iconSize;
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
            m.setMarginEnd(iconMarginRight);
            icon.setLayoutParams(m);
        }

        // Row right toggle icons (tappable)
        for (int id : new int[]{R.id.settings_toggle_notifications, R.id.settings_toggle_vibration, R.id.settings_toggle_sound}) {
            ImageView toggle = view.findViewById(id);
            toggle.getLayoutParams().width  = toggleIconSize;
            toggle.getLayoutParams().height = toggleIconSize;
            toggle.setLayoutParams(toggle.getLayoutParams());
        }

        // Title text sizes
        float titleSize    = ScalingUtils.getScaledTextSize(ctx, 0.042f);
        float subtitleSize = ScalingUtils.getScaledTextSize(ctx, 0.033f);
        for (int id : new int[]{R.id.settings_text_notifications, R.id.settings_text_vibration,
                R.id.settings_text_sound, R.id.settings_text_sync, R.id.settings_text_backup}) {
            ((TextView) view.findViewById(id)).setTextSize(titleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_notifications, R.id.settings_subtext_vibration, R.id.settings_subtext_sound}) {
            ((TextView) view.findViewById(id)).setTextSize(subtitleSize);
        }

        // Sync dot
        int dotSize   = ScalingUtils.getScaledSize(ctx, 0.022f);
        int dotMargin = ScalingUtils.getScaledSize(ctx, 0.015f);
        syncStatusDot.getLayoutParams().width  = dotSize;
        syncStatusDot.getLayoutParams().height = dotSize;
        ViewGroup.MarginLayoutParams dotParams = (ViewGroup.MarginLayoutParams) syncStatusDot.getLayoutParams();
        dotParams.setMarginEnd(dotMargin);
        syncStatusDot.setLayoutParams(dotParams);
        syncStatusText.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.038f));

        // Sync pill padding
        int pillPadH = ScalingUtils.getScaledSize(ctx, 0.025f);
        int pillPadV = ScalingUtils.getScaledSize(ctx, 0.012f);
        syncStatusPill.setPadding(pillPadH, pillPadV, pillPadH, pillPadV);

        // Backup button padding & margin
        int backupPadH = ScalingUtils.getScaledSize(ctx, 0.03f);
        int backupPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        backupButton.setPadding(backupPadH, backupPadV, backupPadH, backupPadV);
        ScalingUtils.applyScaledLayout(backupButton, -1, -1, 0.025f, 0, 0, 0);

        // Version text
        TextView versionText = view.findViewById(R.id.settings_version_text);
        versionText.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.033f));
        ScalingUtils.applyScaledLayout(versionText, -1, -1, 0.03f, 0.01f, 0, 0);
    }

    // ================================================================
    //  CLAYMORPHISM (FIX #5: use BORDER-based clay instead of shadow-only
    //  for better visibility of card edges in LIGHT theme)
    // ================================================================
    @Override
    protected void applyClaymorphism(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        // FIX #5: use a distinctly visible card body color + a visible stroke
        // so both left and top edges are fully visible in light theme.
        int cardBodyColor   = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int shadowColor     = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int highlightColor  = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        int strokeColor     = ContextCompat.getColor(ctx, R.color.off_white_grayish);

        for (int id : new int[]{R.id.settings_card_theme, R.id.settings_card_preferences, R.id.settings_card_data_management}) {
            View card = view.findViewById(id);
            // Convex clay with a visible soft stroke for clear edges in light theme
            card.setBackground(ScalingUtils.createClayDrawable(ctx,
                    0.045f,   // cornerRadius %
                    0.010f,   // shadowOffset %
                    0.006f,   // innerInset %
                    0.003f,   // strokeWidth % (visible border for light theme)
                    cardBodyColor, shadowColor, highlightColor, strokeColor));
        }

        // Theme toggle bar: inset "trough" effect
        View themeContainer = view.findViewById(R.id.settings_theme_toggle_container);
        themeContainer.setBackground(ScalingUtils.createInsetClayDrawable(ctx, 0.035f, 0.006f, 0.004f,
                ContextCompat.getColor(ctx, R.color.off_derived),
                shadowColor, highlightColor));

        // Backup button: distinct convex button
        View backupBtn = view.findViewById(R.id.settings_button_backup);
        backupBtn.setBackground(ScalingUtils.createClayDrawable(ctx,
                0.035f, 0.008f, 0.005f, 0.003f,
                ContextCompat.getColor(ctx, R.color.off_derived),
                shadowColor, highlightColor, strokeColor));

        // Sync status pill: rounded pill
        applyPillBackground(view.findViewById(R.id.settings_sync_status_pill), ctx);

        // Re-apply theme toggle highlights now that claymorphism is set
        updateThemeToggleUI();
    }

    private void applyPillBackground(View pill, Context ctx) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(ScalingUtils.getScaledSize(ctx, 0.06f));
        bg.setColor(ContextCompat.getColor(ctx, R.color.emerald_alpha_20));
        pill.setBackground(bg);
    }
}
