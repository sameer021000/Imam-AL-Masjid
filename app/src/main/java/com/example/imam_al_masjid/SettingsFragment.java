package com.example.imam_al_masjid;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
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

    private static final String PREFS_NAME = "imam_app_settings_prefs";
    private static final String KEY_NOTIF = "pref_notif";
    private static final String KEY_VIB = "pref_vib";
    private static final String KEY_SOUND = "pref_sound";

    // Data management
    private LinearLayout syncStatusPill, backupButton;
    private View syncStatusDot;
    private TextView syncStatusText;

    // --- In-Card Waqt Calculation ---
    private View calculationListRoot, calculationAsrDetailRoot;
    private View asrCardStandard, asrCardHanafi;
    private boolean isAsrDetailVisible = false;

    // Current theme selection index (0=System, 1=Light, 2=Dark)
    private int selectedThemeIndex = 0;

    @Override
    protected int getLayoutId() { return R.layout.fragment_settings; }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
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
            boolean mode = !isSettingOn(KEY_NOTIF);
            saveSetting(KEY_NOTIF, mode);
            updateNotifIcon();
        });
        toggleVibration.setOnClickListener(v -> {
            boolean mode = !isSettingOn(KEY_VIB);
            saveSetting(KEY_VIB, mode);
            updateVibIcon();
        });
        toggleSound.setOnClickListener(v -> {
            boolean mode = !isSettingOn(KEY_SOUND);
            saveSetting(KEY_SOUND, mode);
            updateSoundIcon();
        });

        // Initialize icons based on current state (persisted across theme changes)
        updateAllToggleIcons();

        // === Mock Sync Status (design) ===
        setSyncStatus();

        // Ensure status bar color matches Settings theme (overwriting dashboard accent)
        updateStatusBarColor();

        // === Backup button (Tactile feedback matching Login dropdown) ===
        backupButton.setOnClickListener(v -> {
            // Placeholder for backup logic
        });
        applyTactileTouch(backupButton);

        // === Waqt Calculation Rows (In-Card Transition) ===
        calculationListRoot = view.findViewById(R.id.settings_calculation_list_root);
        calculationAsrDetailRoot = view.findViewById(R.id.settings_calculation_asr_detail_root);
        asrCardStandard = view.findViewById(R.id.settings_card_asr_standard);
        asrCardHanafi = view.findViewById(R.id.settings_card_asr_hanafi);
        
        View asrBackBtn = view.findViewById(R.id.settings_button_asr_back);
        asrBackBtn.setOnClickListener(v -> toggleAsrDetail(false));
        applyTactileTouch(asrBackBtn);

        View asrRow = view.findViewById(R.id.settings_row_asr_madhab);
        asrRow.setOnClickListener(v -> toggleAsrDetail(true));
        applyTactileTouch(asrRow);

        asrCardStandard.setOnClickListener(v -> handleInCardAsrSelection(false));
        asrCardHanafi.setOnClickListener(v -> handleInCardAsrSelection(true));

        applyTactileTouch(asrCardStandard);
        applyTactileTouch(asrCardHanafi);

        updateAsrSubtitle();

        applyTactileTouch(view.findViewById(R.id.settings_row_methods));
        applyTactileTouch(view.findViewById(R.id.settings_row_hijri));
        applyTactileTouch(view.findViewById(R.id.settings_row_country));
    }

    private void toggleAsrDetail(boolean show) {
        if (show == isAsrDetailVisible) return;
        isAsrDetailVisible = show;

        if (getView() == null) return;
        android.transition.TransitionManager.beginDelayedTransition(getView().findViewById(R.id.settings_card_calculation));

        getView().findViewById(R.id.settings_label_calculation).setVisibility(show ? View.GONE : View.VISIBLE);
        calculationListRoot.setVisibility(show ? View.GONE : View.VISIBLE);
        calculationAsrDetailRoot.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            // Entrance animation for sub-cards
            asrCardStandard.setAlpha(0f);
            asrCardStandard.setTranslationX(50f);
            asrCardHanafi.setAlpha(0f);
            asrCardHanafi.setTranslationX(50f);

            asrCardStandard.animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(100).start();
            asrCardHanafi.animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(200).start();
        }
    }

    private void handleInCardAsrSelection(boolean hanafi) {
        if (getContext() == null) return;
        
        // Persist selection
        getContext().getSharedPreferences("PrayerSettings", Context.MODE_PRIVATE)
                .edit().putBoolean("is_asr_hanafi", hanafi).apply();

        updateAsrSubtitle();
        applyClaymorphism(getView()); // Refresh highlights

        // Delay slightly for tactile feedback then slide back
        calculationAsrDetailRoot.postDelayed(() -> toggleAsrDetail(false), 300);
    }

    private void updateAsrSubtitle() {
        if (getView() == null || getContext() == null) return;
        TextView sub = getView().findViewById(R.id.settings_subtext_asr_madhab);
        if (sub == null) return;

        boolean isHanafi = getContext().getSharedPreferences("PrayerSettings", Context.MODE_PRIVATE)
                .getBoolean("is_asr_hanafi", false);
        
        sub.setText(isHanafi ? getString(R.string.asr_selector_hanafi_title) : getString(R.string.asr_selector_standard_title));
    }

    private void applyTactileTouch(View v) {
        if (v == null) return;
        v.setOnTouchListener((v1, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v1.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.85f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v1.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(100).start();
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v1.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    // ================================================================
    //  THEME SELECTION  (Do NOT call setDefaultNightMode if recreate
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

    private void updateStatusBarColor() {
        if (getActivity() != null && getActivity().getWindow() != null && getContext() != null) {
            // Settings uses the default window background color (off_white_primary)
            // Note: off_white_primary is overridden to dark_background in values-night
            int color = ContextCompat.getColor(getContext(), R.color.off_white_primary);
            getActivity().getWindow().setStatusBarColor(color);

            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            androidx.core.view.WindowInsetsControllerCompat controller =
                    androidx.core.view.WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());

            controller.setAppearanceLightStatusBars(!isNightMode);
        }
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
    //  PERSISTENCE HELPERS
    // ================================================================
    private boolean isSettingOn(String key) {
        if (getContext() == null) return true;
        return getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(key, true);
    }

    private void saveSetting(String key, boolean value) {
        if (getContext() == null) return;
        getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    // ================================================================
    //  NOTIFICATION ICON
    // ================================================================
    private void updateNotifIcon() {
        toggleNotifications.setImageResource(isSettingOn(KEY_NOTIF)
                ? R.drawable.ic_settings_notifications
                : R.drawable.ic_settings_notifications_off);
    }

    private void updateVibIcon() {
        toggleVibration.setImageResource(isSettingOn(KEY_VIB)
                ? R.drawable.ic_settings_vibration_on
                : R.drawable.ic_settings_vibration_off);
    }

    private void updateSoundIcon() {
        toggleSound.setImageResource(isSettingOn(KEY_SOUND)
                ? R.drawable.ic_settings_sound_on
                : R.drawable.ic_settings_sound_off);
    }

    private void updateAllToggleIcons() {
        updateNotifIcon();
        updateVibIcon();
        updateSoundIcon();
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
        int[] cardIds = {R.id.settings_card_theme, R.id.settings_card_calculation, 
                        R.id.settings_card_preferences, R.id.settings_card_data_management};
        for (int id : cardIds) {
            View card = view.findViewById(id);
            if (card == null) continue;
            card.setPadding(cardHorizPad, cardVertPad, cardHorizPad, cardVertPad);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
            p.setMargins(0, cardMarginV, 0, cardMarginV);
            card.setLayoutParams(p);
        }

        // Section labels
        float sectionLabelSize = ScalingUtils.getScaledTextSize(ctx, 0.034f);
        for (int id : new int[]{R.id.settings_label_theme, R.id.settings_label_calculation,
                R.id.settings_label_preferences, R.id.settings_label_data}) {
            TextView t = view.findViewById(id);
            t.setTextSize(sectionLabelSize);
            ScalingUtils.applyScaledLayout(t, -1, -1, 0, 0.015f, 0, 0);
        }

        // Theme toggle container: fixed height so tabs are vertically centered
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

        for (int rowId : new int[]{R.id.settings_row_notifications, R.id.settings_row_vibration, R.id.settings_row_sound,
                R.id.settings_row_asr_madhab, R.id.settings_row_methods, R.id.settings_row_hijri, R.id.settings_row_country}) {
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
                R.id.settings_text_sound, R.id.settings_text_sync, R.id.settings_text_backup,
                R.id.settings_text_asr_madhab, R.id.settings_text_methods, R.id.settings_text_hijri, R.id.settings_text_country}) {
            ((TextView) view.findViewById(id)).setTextSize(titleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_notifications, R.id.settings_subtext_vibration, R.id.settings_subtext_sound,
                R.id.settings_subtext_asr_madhab, R.id.settings_subtext_methods, R.id.settings_subtext_hijri, R.id.settings_subtext_country}) {
            ((TextView) view.findViewById(id)).setTextSize(subtitleSize);
        }

        // New selection icons (chevrons)
        for (int id : new int[]{R.id.settings_icon_asr_madhab, R.id.settings_icon_methods, R.id.settings_icon_hijri, R.id.settings_icon_country}) {
            ImageView icon = view.findViewById(id);
            icon.getLayoutParams().width = iconSize;
            icon.getLayoutParams().height = iconSize;
            icon.setLayoutParams(icon.getLayoutParams());
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

        // --- In-Card Detail Scaling ---
        // Assuming tagStandard and tagHanafi are TextViews and are defined/found elsewhere
        // or are meant to be found here. Since they are not in the original code,
        // I will define them as TextViews found by ID, assuming they exist in the layout.
        TextView tagStandard = view.findViewById(R.id.settings_subtext_asr_standard); // Assuming this is the ID for the tag
        TextView tagHanafi = view.findViewById(R.id.settings_subtext_asr_hanafi); // Assuming this is the ID for the tag

        if (tagStandard != null) ScalingUtils.applyScaledLayout(tagStandard, -1, -1, 0.005f, 0, 0, 0);
        if (tagHanafi != null) ScalingUtils.applyScaledLayout(tagHanafi, -1, -1, 0.005f, 0, 0, 0);

        int detailCardPadH = ScalingUtils.getScaledSize(ctx, 0.045f);
        int detailCardPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        int detailCardMarginV = ScalingUtils.getScaledSize(ctx, 0.02f);
        float detailTitleSize = ScalingUtils.getScaledTextSize(ctx, 0.04f);
        float detailTagSize   = ScalingUtils.getScaledTextSize(ctx, 0.03f);

        for (View v : new View[]{asrCardStandard, asrCardHanafi}) {
            v.setPadding(detailCardPadH, detailCardPadV, detailCardPadH, detailCardPadV);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.setMargins(0, detailCardMarginV, 0, detailCardMarginV);
            v.setLayoutParams(lp);
        }
        
        ((TextView)view.findViewById(R.id.settings_text_asr_standard)).setTextSize(detailTitleSize);
        ((TextView)view.findViewById(R.id.settings_text_asr_hanafi)).setTextSize(detailTitleSize);
        ((TextView)view.findViewById(R.id.settings_subtext_asr_standard)).setTextSize(detailTagSize);
        ((TextView)view.findViewById(R.id.settings_subtext_asr_hanafi)).setTextSize(detailTagSize);

        // --- Asr Header Scaling ---
        View asrBackBtn = view.findViewById(R.id.settings_button_asr_back);
        int backIconSize = ScalingUtils.getScaledSize(ctx, 0.05f);
        asrBackBtn.getLayoutParams().width = backIconSize;
        asrBackBtn.getLayoutParams().height = backIconSize;
        ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) asrBackBtn.getLayoutParams();
        m.setMarginEnd(ScalingUtils.getScaledSize(ctx, 0.02f));
        asrBackBtn.setLayoutParams(m);

        TextView asrHeaderTitle = view.findViewById(R.id.settings_text_asr_header_title);
        asrHeaderTitle.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.034f));
        view.findViewById(R.id.settings_calculation_asr_header).setPadding(0, 0, 0, ScalingUtils.getScaledSize(ctx, 0.02f));
    }

    // ================================================================
    //  CLAYMORPHISM (use BORDER-based clay instead of shadow-only
    //  for better visibility of card edges in LIGHT theme)
    // ================================================================
    @Override
    protected void applyClaymorphism(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        // use a distinctly visible card body color + a visible stroke
        // so both left and top edges are fully visible in light theme.
        int cardBodyColor   = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int shadowColor     = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int highlightColor  = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        int strokeColor     = ContextCompat.getColor(ctx, R.color.off_white_grayish);

        int[] cards = {R.id.settings_card_theme, R.id.settings_card_calculation,
                      R.id.settings_card_preferences, R.id.settings_card_data_management};
        for (int id : cards) {
            View card = view.findViewById(id);
            if (card == null) continue;
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

        // Re-apply theme toggle highlights
        updateThemeToggleUI();

        // --- In-Card Detail Styling ---
        boolean isStandardActive = !ctx.getSharedPreferences("PrayerSettings", Context.MODE_PRIVATE)
                .getBoolean("is_asr_hanafi", false);

        int activeColor   = ContextCompat.getColor(ctx, R.color.emerald_primary);
        int detailBodyColor = ContextCompat.getColor(ctx, R.color.off_white_primary);

        asrCardStandard.setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                isStandardActive ? activeColor : detailBodyColor, shadowColor, highlightColor, 
                isStandardActive ? activeColor : strokeColor));

        asrCardHanafi.setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                !isStandardActive ? activeColor : detailBodyColor, shadowColor, highlightColor, 
                !isStandardActive ? activeColor : strokeColor));

        // Text & Subtext colors (Active vs Inactive)
        boolean isNight = (ctx.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        int activeSubColor = ContextCompat.getColor(ctx, R.color.prayer_card_active_secondary_text);
        int inactiveSubColor = ContextCompat.getColor(ctx, R.color.text_secondary);

        TextView subStandard = view.findViewById(R.id.settings_subtext_asr_standard);
        TextView subHanafi = view.findViewById(R.id.settings_subtext_asr_hanafi);

        // Titles
        ((TextView)view.findViewById(R.id.settings_text_asr_standard)).setTextColor(isStandardActive ? detailBodyColor : activeColor);
        ((TextView)view.findViewById(R.id.settings_text_asr_hanafi)).setTextColor(!isStandardActive ? detailBodyColor : activeColor);

        // Subtexts (Regional Tags) - Matching Home Screen "AZAN/JAMAT" logic
        if (isStandardActive && !isNight) {
            subStandard.setTextColor(activeSubColor);
        } else {
            subStandard.setTextColor(inactiveSubColor);
        }
        subStandard.setAlpha(0.8f);

        if (!isStandardActive && !isNight) {
            subHanafi.setTextColor(activeSubColor);
        } else {
            subHanafi.setTextColor(inactiveSubColor);
        }
        subHanafi.setAlpha(0.8f);
    }

    private void applyPillBackground(View pill, Context ctx) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(ScalingUtils.getScaledSize(ctx, 0.06f));
        bg.setColor(ContextCompat.getColor(ctx, R.color.emerald_alpha_20));
        pill.setBackground(bg);
    }
}