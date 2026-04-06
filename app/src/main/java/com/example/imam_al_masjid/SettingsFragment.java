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
    private View calculationListRoot, calculationAsrDetailRoot, calculationMethodsDetailRoot, calculationHijriDetailRoot, calculationCountryDetailRoot;
    private View asrCardStandard, asrCardHanafi;
    private View[] methodCards;  // Karachi, MWL, ISNA, UmmAlQura, Tehran, Turkey
    private View[] hijriCards;   // Astro, UmmAlQura, Local, Global, Manual
    private View[] countryCards; // SA, Saudi, UAE, Qatar, Egypt, Turkey, NA, SEA, Iran, Iraq
    private boolean isAsrDetailVisible = false;
    private boolean isMethodsDetailVisible = false;
    private boolean isHijriDetailVisible = false;
    private boolean isCountryDetailVisible = false;

    // Current theme selection index (0=System, 1=Light, 2=Dark)
    private int selectedThemeIndex = 0;

    private android.content.SharedPreferences appPrefs;
    private android.content.SharedPreferences prayerPrefs;

    @Override
    public void onAttach(@androidx.annotation.NonNull android.content.Context context) {
        super.onAttach(context);
        appPrefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prayerPrefs = context.getSharedPreferences("PrayerSettings", android.content.Context.MODE_PRIVATE);
    }

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
        updateThemeToggleUI();

        // === Theme toggle clicks ===
        if (themeSystem != null) themeSystem.setOnClickListener(v -> onThemeSelected(0));
        if (themeLight != null) themeLight.setOnClickListener( v -> onThemeSelected(1));
        if (themeDark != null) themeDark.setOnClickListener(  v -> onThemeSelected(2));

        // === Icon toggle clicks ===
        if (toggleNotifications != null) {
            toggleNotifications.setOnClickListener(v -> {
                boolean mode = !isSettingOn(KEY_NOTIF);
                saveSetting(KEY_NOTIF, mode);
                updateNotifIcon();
            });
        }
        if (toggleVibration != null) {
            toggleVibration.setOnClickListener(v -> {
                boolean mode = !isSettingOn(KEY_VIB);
                saveSetting(KEY_VIB, mode);
                updateVibIcon();
            });
        }
        if (toggleSound != null) {
            toggleSound.setOnClickListener(v -> {
                boolean mode = !isSettingOn(KEY_SOUND);
                saveSetting(KEY_SOUND, mode);
                updateSoundIcon();
            });
        }

        // Initialize icons based on current state
        updateAllToggleIcons();

        // === Mock Sync Status ===
        setSyncStatus();

        // Status bar update
        updateStatusBarColor();

        // Backup button
        if (backupButton != null) {
            backupButton.setOnClickListener(v -> {});
            applyTactileTouch(backupButton);
        }

        // --- Waqt Calculation Initialization ---
        calculationListRoot = view.findViewById(R.id.settings_calculation_list_root);
        calculationAsrDetailRoot = view.findViewById(R.id.settings_calculation_asr_detail_root);
        asrCardStandard = view.findViewById(R.id.settings_card_asr_standard);
        asrCardHanafi = view.findViewById(R.id.settings_card_asr_hanafi);

        View asrBackBtn = view.findViewById(R.id.settings_button_asr_back);
        if (asrBackBtn != null) {
            asrBackBtn.setOnClickListener(v -> toggleAsrDetail(false));
            applyTactileTouch(asrBackBtn);
        }

        View asrHeaderTitle = view.findViewById(R.id.settings_text_asr_header_title);
        if (asrHeaderTitle != null) {
            asrHeaderTitle.setOnClickListener(v -> toggleAsrDetail(false));
            applyTactileTouch(asrHeaderTitle);
        }

        View asrRow = view.findViewById(R.id.settings_row_asr_madhab);
        if (asrRow != null) {
            asrRow.setOnClickListener(v -> toggleAsrDetail(true));
            applyTactileTouch(asrRow);
        }

        if (asrCardStandard != null) {
            asrCardStandard.setOnClickListener(v -> handleInCardAsrSelection(false));
            applyTactileTouch(asrCardStandard);
        }
        if (asrCardHanafi != null) {
            asrCardHanafi.setOnClickListener(v -> handleInCardAsrSelection(true));
            applyTactileTouch(asrCardHanafi);
        }

        updateAsrSubtitle();

        // --- Methods Detail ---
        calculationMethodsDetailRoot = view.findViewById(R.id.settings_calculation_methods_detail_root);
        methodCards = new View[]{
                view.findViewById(R.id.settings_card_method_karachi),
                view.findViewById(R.id.settings_card_method_mwl),
                view.findViewById(R.id.settings_card_method_isna),
                view.findViewById(R.id.settings_card_method_ummalqura),
                view.findViewById(R.id.settings_card_method_tehran),
                view.findViewById(R.id.settings_card_method_turkey)
        };

        String[] methodIds = {"KARACHI", "MWL", "ISNA", "UMM_AL_QURA", "TEHRAN", "TURKEY"};
        for (int i = 0; i < methodCards.length; i++) {
            final String mid = methodIds[i];
            if (methodCards[i] != null) {
                methodCards[i].setOnClickListener(v -> handleInCardMethodSelection(mid));
                applyTactileTouch(methodCards[i]);
            }
        }

        View methodsBackBtn = view.findViewById(R.id.settings_button_methods_back);
        if (methodsBackBtn != null) {
            methodsBackBtn.setOnClickListener(v -> toggleMethodsDetail(false));
            applyTactileTouch(methodsBackBtn);
        }

        View methodsHeaderTitle = view.findViewById(R.id.settings_text_methods_header_title);
        if (methodsHeaderTitle != null) {
            methodsHeaderTitle.setOnClickListener(v -> toggleMethodsDetail(false));
            applyTactileTouch(methodsHeaderTitle);
        }

        View methodsRow = view.findViewById(R.id.settings_row_methods);
        if (methodsRow != null) {
            methodsRow.setOnClickListener(v -> toggleMethodsDetail(true));
            applyTactileTouch(methodsRow);
        }

        updateMethodsSubtitle();

        // --- Hijri Detail ---
        calculationHijriDetailRoot = view.findViewById(R.id.settings_calculation_hijri_detail_root);
        hijriCards = new View[]{
                view.findViewById(R.id.settings_card_hijri_astro),
                view.findViewById(R.id.settings_card_hijri_ummalqura),
                view.findViewById(R.id.settings_card_hijri_local),
                view.findViewById(R.id.settings_card_hijri_global),
                view.findViewById(R.id.settings_card_hijri_manual)
        };

        String[] hijriIds = {"ASTRONOMICAL", "UMM_AL_QURA", "LOCAL_SIGHTING", "GLOBAL_SIGHTING", "MANUAL_OFFSET"};
        for (int i = 0; i < hijriCards.length; i++) {
            final String hid = hijriIds[i];
            if (hijriCards[i] != null) {
                hijriCards[i].setOnClickListener(v -> handleInCardHijriSelection(hid));
                applyTactileTouch(hijriCards[i]);
            }
        }

        View hijriBackBtn = view.findViewById(R.id.settings_button_hijri_back);
        if (hijriBackBtn != null) {
            hijriBackBtn.setOnClickListener(v -> toggleHijriDetail(false));
            applyTactileTouch(hijriBackBtn);
        }

        View hijriHeaderTitle = view.findViewById(R.id.settings_text_hijri_header_title);
        if (hijriHeaderTitle != null) {
            hijriHeaderTitle.setOnClickListener(v -> toggleHijriDetail(false));
            applyTactileTouch(hijriHeaderTitle);
        }

        View hijriRow = view.findViewById(R.id.settings_row_hijri);
        if (hijriRow != null) {
            hijriRow.setOnClickListener(v -> toggleHijriDetail(true));
            applyTactileTouch(hijriRow);
        }

        updateHijriSubtitle();

        // --- Country Detail ---
        calculationCountryDetailRoot = view.findViewById(R.id.settings_calculation_country_detail_root);
        countryCards = new View[]{
                view.findViewById(R.id.settings_card_country_south_asia),
                view.findViewById(R.id.settings_card_country_saudi),
                view.findViewById(R.id.settings_card_country_uae),
                view.findViewById(R.id.settings_card_country_qatar),
                view.findViewById(R.id.settings_card_country_egypt),
                view.findViewById(R.id.settings_card_country_turkey),
                view.findViewById(R.id.settings_card_country_north_america),
                view.findViewById(R.id.settings_card_country_southeast_asia),
                view.findViewById(R.id.settings_card_country_iran),
                view.findViewById(R.id.settings_card_country_iraq)
        };

        String[] countryIds = {"SOUTH_ASIA", "SAUDI", "UAE", "QATAR", "EGYPT", "TURKEY", "NORTH_AMERICA", "SOUTHEAST_ASIA", "IRAN", "IRAQ"};
        for (int i = 0; i < countryCards.length; i++) {
            final String cid = countryIds[i];
            if (countryCards[i] != null) {
                countryCards[i].setOnClickListener(v -> handleInCardCountrySelection(cid));
                applyTactileTouch(countryCards[i]);
            }
        }

        View countryBackBtn = view.findViewById(R.id.settings_button_country_back);
        if (countryBackBtn != null) {
            countryBackBtn.setOnClickListener(v -> toggleCountryDetail(false));
            applyTactileTouch(countryBackBtn);
        }

        View countryHeaderTitle = view.findViewById(R.id.settings_text_country_header_title);
        if (countryHeaderTitle != null) {
            countryHeaderTitle.setOnClickListener(v -> toggleCountryDetail(false));
            applyTactileTouch(countryHeaderTitle);
        }

        View countryRow = view.findViewById(R.id.settings_row_country);
        if (countryRow != null) {
            countryRow.setOnClickListener(v -> toggleCountryDetail(true));
            applyTactileTouch(countryRow);
        }

        updateCountrySubtitle();
    }

    private void toggleAsrDetail(boolean show) {
        if (show == isAsrDetailVisible) return;
        isAsrDetailVisible = show;

        if (getView() == null) return;
        View card = getView().findViewById(R.id.settings_card_calculation);
        if (card != null) android.transition.TransitionManager.beginDelayedTransition((ViewGroup) card);

        View label = getView().findViewById(R.id.settings_label_calculation);
        if (label != null) label.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationListRoot != null) calculationListRoot.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationAsrDetailRoot != null) calculationAsrDetailRoot.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show && asrCardStandard != null && asrCardHanafi != null) {
            asrCardStandard.setAlpha(0f);
            asrCardStandard.setTranslationX(50f);
            asrCardHanafi.setAlpha(0f);
            asrCardHanafi.setTranslationX(50f);

            asrCardStandard.animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(100).start();
            asrCardHanafi.animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(200).start();
        }
    }

    private void toggleMethodsDetail(boolean show) {
        if (show == isMethodsDetailVisible) return;
        isMethodsDetailVisible = show;

        if (getView() == null) return;
        View card = getView().findViewById(R.id.settings_card_calculation);
        if (card != null) android.transition.TransitionManager.beginDelayedTransition((ViewGroup) card);

        View label = getView().findViewById(R.id.settings_label_calculation);
        if (label != null) label.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationListRoot != null) calculationListRoot.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationMethodsDetailRoot != null) calculationMethodsDetailRoot.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show && methodCards != null) {
            for (int i = 0; i < methodCards.length; i++) {
                if (methodCards[i] != null) {
                    methodCards[i].setAlpha(0f);
                    methodCards[i].setTranslationX(50f);
                    methodCards[i].animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(50L * i).start();
                }
            }
        }
    }

    private void toggleHijriDetail(boolean show) {
        if (show == isHijriDetailVisible) return;
        isHijriDetailVisible = show;

        if (getView() == null) return;
        View card = getView().findViewById(R.id.settings_card_calculation);
        if (card != null) android.transition.TransitionManager.beginDelayedTransition((ViewGroup) card);

        View label = getView().findViewById(R.id.settings_label_calculation);
        if (label != null) label.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationListRoot != null) calculationListRoot.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationHijriDetailRoot != null) calculationHijriDetailRoot.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show && hijriCards != null) {
            for (int i = 0; i < hijriCards.length; i++) {
                if (hijriCards[i] != null) {
                    hijriCards[i].setAlpha(0f);
                    hijriCards[i].setTranslationX(50f);
                    hijriCards[i].animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(50L * i).start();
                }
            }
        }
    }

    private void toggleCountryDetail(boolean show) {
        if (show == isCountryDetailVisible) return;
        isCountryDetailVisible = show;

        if (getView() == null) return;
        View card = getView().findViewById(R.id.settings_card_calculation);
        if (card != null) android.transition.TransitionManager.beginDelayedTransition((ViewGroup) card);

        View label = getView().findViewById(R.id.settings_label_calculation);
        if (label != null) label.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationListRoot != null) calculationListRoot.setVisibility(show ? View.GONE : View.VISIBLE);
        if (calculationCountryDetailRoot != null) calculationCountryDetailRoot.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show && countryCards != null) {
            for (int i = 0; i < countryCards.length; i++) {
                if (countryCards[i] != null) {
                    countryCards[i].setAlpha(0f);
                    countryCards[i].setTranslationX(50f);
                    countryCards[i].animate().alpha(1f).translationX(0f).setDuration(300).setStartDelay(50L * i).start();
                }
            }
        }
    }

    private void handleInCardAsrSelection(boolean hanafi) {
        if (prayerPrefs != null) {
            prayerPrefs.edit().putBoolean("is_asr_hanafi", hanafi).apply();
        }
        updateAsrSubtitle();
        applyClaymorphism(getView());
        if (calculationAsrDetailRoot != null) calculationAsrDetailRoot.postDelayed(() -> toggleAsrDetail(false), 300);
    }

    private void handleInCardMethodSelection(String methodId) {
        if (prayerPrefs != null) {
            prayerPrefs.edit().putString("calculation_method", methodId).apply();
        }
        updateMethodsSubtitle();
        applyClaymorphism(getView());
        if (calculationMethodsDetailRoot != null) calculationMethodsDetailRoot.postDelayed(() -> toggleMethodsDetail(false), 300);
    }

    private void handleInCardHijriSelection(String systemId) {
        if (prayerPrefs != null) {
            prayerPrefs.edit().putString("hijri_system", systemId).apply();
        }
        updateHijriSubtitle();
        applyClaymorphism(getView());
        if (calculationHijriDetailRoot != null) calculationHijriDetailRoot.postDelayed(() -> toggleHijriDetail(false), 300);
    }

    private void handleInCardCountrySelection(String countryId) {
        if (prayerPrefs != null) {
            prayerPrefs.edit().putString("selected_country", countryId).apply();
        }
        updateCountrySubtitle();
        applyClaymorphism(getView());
        if (calculationCountryDetailRoot != null) calculationCountryDetailRoot.postDelayed(() -> toggleCountryDetail(false), 300);
    }

    private void updateAsrSubtitle() {
        if (getView() == null || prayerPrefs == null) return;
        TextView sub = getView().findViewById(R.id.settings_subtext_asr_madhab);
        if (sub == null) return;
        boolean isHanafi = prayerPrefs.getBoolean("is_asr_hanafi", false);
        sub.setText(isHanafi ? getString(R.string.asr_selector_hanafi_title) : getString(R.string.asr_selector_standard_title));
    }

    private void updateMethodsSubtitle() {
        if (getView() == null || prayerPrefs == null) return;
        TextView sub = getView().findViewById(R.id.settings_subtext_methods);
        if (sub == null) return;
        String method = prayerPrefs.getString("calculation_method", "KARACHI");
        sub.setText(method.replace("_", " "));
    }

    private void updateHijriSubtitle() {
        if (getView() == null || prayerPrefs == null) return;
        TextView sub = getView().findViewById(R.id.settings_subtext_hijri);
        if (sub == null) return;
        String system = prayerPrefs.getString("hijri_system", "ASTRONOMICAL");
        sub.setText(system.replace("_", " "));
    }

    private void updateCountrySubtitle() {
        if (getView() == null || getContext() == null || prayerPrefs == null) return;
        TextView sub = getView().findViewById(R.id.settings_subtext_country);
        if (sub == null) return;
        String countryId = prayerPrefs.getString("selected_country", "SOUTH_ASIA");

        int resId;
        switch (countryId) {
            case "SOUTH_ASIA": resId = R.string.country_south_asia; break;
            case "SAUDI": resId = R.string.country_saudi; break;
            case "UAE": resId = R.string.country_uae; break;
            case "QATAR": resId = R.string.country_qatar; break;
            case "EGYPT": resId = R.string.country_egypt; break;
            case "TURKEY": resId = R.string.country_turkey; break;
            case "NORTH_AMERICA": resId = R.string.country_north_america; break;
            case "SOUTHEAST_ASIA": resId = R.string.country_southeast_asia; break;
            case "IRAN": resId = R.string.country_iran; break;
            case "IRAQ": resId = R.string.country_iraq; break;
            default: resId = 0; break;
        }

        if (resId != 0) {
            sub.setText(getString(resId));
        } else {
            sub.setText(countryId.replace("_", " "));
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
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
                    break;
            }
            return false;
        });
    }

    private void onThemeSelected(int index) {
        if (getContext() == null || index == selectedThemeIndex) return;
        selectedThemeIndex = index;
        updateThemeToggleUI();
        int mode;
        switch (index) {
            case 1:  mode = AppCompatDelegate.MODE_NIGHT_NO;            break;
            case 2:  mode = AppCompatDelegate.MODE_NIGHT_YES;           break;
            default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        ThemeManager.setTheme(getContext(), mode);
    }

    private void updateStatusBarColor() {
        if (getActivity() != null && getActivity().getWindow() != null && getContext() != null) {
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
        int activeBodyColor = ContextCompat.getColor(ctx, R.color.status_active);
        int activeTextColor = ContextCompat.getColor(ctx, R.color.text_inverse);
        int inactiveTextColor = ContextCompat.getColor(ctx, R.color.text_secondary);

        TextView[] tabs = { themeSystem, themeLight, themeDark };
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
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

    private boolean isSettingOn(String key) {
        if (appPrefs == null) return true;
        return appPrefs.getBoolean(key, true);
    }

    private void saveSetting(String key, boolean value) {
        if (appPrefs != null) {
            appPrefs.edit().putBoolean(key, value).apply();
        }
    }

    private void updateNotifIcon() {
        if (toggleNotifications != null) {
            toggleNotifications.setImageResource(isSettingOn(KEY_NOTIF)
                    ? R.drawable.ic_settings_notifications
                    : R.drawable.ic_settings_notifications_off);
        }
    }

    private void updateVibIcon() {
        if (toggleVibration != null) {
            toggleVibration.setImageResource(isSettingOn(KEY_VIB)
                    ? R.drawable.ic_settings_vibration_on
                    : R.drawable.ic_settings_vibration_off);
        }
    }

    private void updateSoundIcon() {
        if (toggleSound != null) {
            toggleSound.setImageResource(isSettingOn(KEY_SOUND)
                    ? R.drawable.ic_settings_sound_on
                    : R.drawable.ic_settings_sound_off);
        }
    }

    private void updateAllToggleIcons() {
        updateNotifIcon();
        updateVibIcon();
        updateSoundIcon();
    }

    private void setSyncStatus() {
        if (getContext() == null || syncStatusText == null || syncStatusDot == null) return;
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

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        View root = view.findViewById(R.id.settings_root);
        int topPadding  = ScalingUtils.getScaledSize(ctx, 0.04f);
        if (root != null) root.setPadding(0, topPadding, 0, 0);

        View scrollRoot = view.findViewById(R.id.settings_scroll_root);
        if (scrollRoot != null) scrollRoot.setPadding(0, 0, 0, topPadding);

        TextView screenTitle = view.findViewById(R.id.settings_screen_title);
        if (screenTitle != null) {
            screenTitle.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.065f));
            ScalingUtils.applyScaledLayout(screenTitle, -1, -1, 0, 0.01f, 0.055f, 0);
        }

        int cardHorizPad = ScalingUtils.getScaledSize(ctx, 0.04f);
        int cardVertPad  = ScalingUtils.getScaledSize(ctx, 0.03f);
        float cardMarginV  = 0.03f;
        int[] cardIds = {R.id.settings_card_theme, R.id.settings_card_calculation,
                R.id.settings_card_preferences, R.id.settings_card_data_management};
        for (int id : cardIds) {
            View card = view.findViewById(id);
            if (card == null) continue;
            card.setPadding(cardHorizPad, cardVertPad, cardHorizPad, cardVertPad);
            ScalingUtils.applyScaledLayout(card, 0.98f, -1, cardMarginV, cardMarginV, 0.01f, 0.01f);
        }

        float sectionLabelSize = ScalingUtils.getScaledTextSize(ctx, 0.034f);
        for (int id : new int[]{R.id.settings_label_theme, R.id.settings_label_calculation,
                R.id.settings_label_preferences, R.id.settings_label_data}) {
            TextView t = view.findViewById(id);
            if (t == null) continue;
            t.setTextSize(sectionLabelSize);
            ScalingUtils.applyScaledLayout(t, -1, -1, 0, 0.015f, 0, 0);
        }

        LinearLayout themeContainer = view.findViewById(R.id.settings_theme_toggle_container);
        if (themeContainer != null) {
            themeContainer.getLayoutParams().height = ScalingUtils.getScaledSize(ctx, 0.13f);
            themeContainer.requestLayout();
        }

        int tabPadH = ScalingUtils.getScaledSize(ctx, 0.02f);
        int tabPadV = ScalingUtils.getScaledSize(ctx, 0.015f);
        float tabSize = ScalingUtils.getScaledTextSize(ctx, 0.038f);
        TextView[] themeTabs = {themeSystem, themeLight, themeDark};
        for (TextView tab : themeTabs) {
            if (tab != null) {
                tab.setTextSize(tabSize);
                tab.setPadding(tabPadH, tabPadV, tabPadH, tabPadV);
            }
        }

        int rowPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        int iconSize = ScalingUtils.getScaledSize(ctx, 0.065f);
        int iconMarginRight = ScalingUtils.getScaledSize(ctx, 0.04f);
        int toggleIconSize = ScalingUtils.getScaledSize(ctx, 0.07f);

        for (int rowId : new int[]{R.id.settings_row_notifications, R.id.settings_row_vibration, R.id.settings_row_sound,
                R.id.settings_row_asr_madhab, R.id.settings_row_methods, R.id.settings_row_hijri, R.id.settings_row_country}) {
            View row = view.findViewById(rowId);
            if (row != null) row.setPadding(0, rowPadV, 0, rowPadV);
        }

        for (int iconId : new int[]{R.id.settings_icon_sync, R.id.settings_icon_backup}) {
            ImageView icon = view.findViewById(iconId);
            if (icon != null) {
                icon.getLayoutParams().width = iconSize;
                icon.getLayoutParams().height = iconSize;
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) icon.getLayoutParams();
                m.setMarginEnd(iconMarginRight);
                icon.setLayoutParams(m);
            }
        }

        for (int id : new int[]{R.id.settings_toggle_notifications, R.id.settings_toggle_vibration, R.id.settings_toggle_sound}) {
            ImageView icon = view.findViewById(id);
            if (icon != null) {
                icon.getLayoutParams().width  = toggleIconSize;
                icon.getLayoutParams().height = toggleIconSize;
                icon.setLayoutParams(icon.getLayoutParams());
            }
        }

        float titleSize    = ScalingUtils.getScaledTextSize(ctx, 0.042f);
        float subtitleSize = ScalingUtils.getScaledTextSize(ctx, 0.033f);
        for (int id : new int[]{R.id.settings_text_notifications, R.id.settings_text_vibration,
                R.id.settings_text_sound, R.id.settings_text_sync, R.id.settings_text_backup,
                R.id.settings_text_asr_madhab, R.id.settings_text_methods, R.id.settings_text_hijri, R.id.settings_text_country}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(titleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_notifications, R.id.settings_subtext_vibration, R.id.settings_subtext_sound,
                R.id.settings_subtext_asr_madhab, R.id.settings_subtext_methods, R.id.settings_subtext_hijri, R.id.settings_subtext_country}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(subtitleSize);
        }

        for (int id : new int[]{R.id.settings_icon_asr_madhab, R.id.settings_icon_methods, R.id.settings_icon_hijri, R.id.settings_icon_country}) {
            ImageView icon = view.findViewById(id);
            if (icon != null) {
                icon.getLayoutParams().width = iconSize;
                icon.getLayoutParams().height = iconSize;
                icon.setLayoutParams(icon.getLayoutParams());
            }
        }

        int dotSize   = ScalingUtils.getScaledSize(ctx, 0.022f);
        int dotMargin = ScalingUtils.getScaledSize(ctx, 0.015f);
        if (syncStatusDot != null) {
            syncStatusDot.getLayoutParams().width  = dotSize;
            syncStatusDot.getLayoutParams().height = dotSize;
            ViewGroup.MarginLayoutParams dotParams = (ViewGroup.MarginLayoutParams) syncStatusDot.getLayoutParams();
            dotParams.setMarginEnd(dotMargin);
            syncStatusDot.setLayoutParams(dotParams);
        }
        if (syncStatusText != null) syncStatusText.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.038f));

        int pillPadH = ScalingUtils.getScaledSize(ctx, 0.025f);
        int pillPadV = ScalingUtils.getScaledSize(ctx, 0.012f);
        if (syncStatusPill != null) syncStatusPill.setPadding(pillPadH, pillPadV, pillPadH, pillPadV);

        int backupPadH = ScalingUtils.getScaledSize(ctx, 0.03f);
        int backupPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        if (backupButton != null) {
            backupButton.setPadding(backupPadH, backupPadV, backupPadH, backupPadV);
            ScalingUtils.applyScaledLayout(backupButton, -1, -1, 0.025f, 0, 0, 0);
        }

        TextView versionText = view.findViewById(R.id.settings_version_text);
        if (versionText != null) {
            versionText.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.033f));
            ScalingUtils.applyScaledLayout(versionText, -1, -1, 0.03f, 0.01f, 0, 0);
        }

        int detailCardPadH = ScalingUtils.getScaledSize(ctx, 0.045f);
        int detailCardPadV = ScalingUtils.getScaledSize(ctx, 0.025f);
        int detailCardMarginV = ScalingUtils.getScaledSize(ctx, 0.02f);
        float detailTitleSize = ScalingUtils.getScaledTextSize(ctx, 0.04f);
        float detailTagSize   = ScalingUtils.getScaledTextSize(ctx, 0.03f);

        View[] asrCards = {asrCardStandard, asrCardHanafi};
        for (View v : asrCards) {
            if (v != null) {
                v.setPadding(detailCardPadH, detailCardPadV, detailCardPadH, detailCardPadV);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.setMargins(0, detailCardMarginV, 0, detailCardMarginV);
                v.setLayoutParams(lp);
            }
        }

        TextView tvAsrStandard = view.findViewById(R.id.settings_text_asr_standard);
        TextView tvAsrHanafi = view.findViewById(R.id.settings_text_asr_hanafi);
        if (tvAsrStandard != null) tvAsrStandard.setTextSize(detailTitleSize);
        if (tvAsrHanafi != null) tvAsrHanafi.setTextSize(detailTitleSize);

        TextView tvAsrSubStandard = view.findViewById(R.id.settings_subtext_asr_standard);
        TextView tvAsrSubHanafi = view.findViewById(R.id.settings_subtext_asr_hanafi);
        if (tvAsrSubStandard != null) {
            ScalingUtils.applyScaledLayout(tvAsrSubStandard, -1, -1, 0.005f, 0, 0, 0);
            tvAsrSubStandard.setTextSize(detailTagSize);
        }
        if (tvAsrSubHanafi != null) {
            ScalingUtils.applyScaledLayout(tvAsrSubHanafi, -1, -1, 0.005f, 0, 0, 0);
            tvAsrSubHanafi.setTextSize(detailTagSize);
        }

        int backIconSize = ScalingUtils.getScaledSize(ctx, 0.05f);
        int[] backBtnIds = {R.id.settings_button_asr_back, R.id.settings_button_methods_back,
                R.id.settings_button_hijri_back, R.id.settings_button_country_back};
        for (int bid : backBtnIds) {
            View btn = view.findViewById(bid);
            if (btn != null) {
                btn.getLayoutParams().width = backIconSize;
                btn.getLayoutParams().height = backIconSize;
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) btn.getLayoutParams();
                m.setMarginEnd(ScalingUtils.getScaledSize(ctx, 0.02f));
                btn.setLayoutParams(m);
            }
        }

        int[] headerTitleIds = {R.id.settings_text_asr_header_title, R.id.settings_text_methods_header_title,
                R.id.settings_text_hijri_header_title, R.id.settings_text_country_header_title};
        for (int tid : headerTitleIds) {
            TextView tv = view.findViewById(tid);
            if (tv != null) tv.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.034f));
        }

        int[] headerIds = {R.id.settings_calculation_asr_header, R.id.settings_calculation_methods_header,
                R.id.settings_calculation_hijri_header, R.id.settings_calculation_country_header};
        for (int hid : headerIds) {
            View h = view.findViewById(hid);
            if (h != null) h.setPadding(0, 0, 0, ScalingUtils.getScaledSize(ctx, 0.02f));
        }

        if (methodCards != null) {
            for (View v : methodCards) {
                if (v != null) {
                    v.setPadding(detailCardPadH, detailCardPadV, detailCardPadH, detailCardPadV);
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    lp.setMargins(0, detailCardMarginV, 0, detailCardMarginV);
                    v.setLayoutParams(lp);
                }
            }
        }

        for (int id : new int[]{R.id.settings_text_method_karachi, R.id.settings_text_method_mwl, R.id.settings_text_method_isna,
                R.id.settings_text_method_ummalqura, R.id.settings_text_method_tehran, R.id.settings_text_method_turkey}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTitleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_method_karachi_info, R.id.settings_subtext_method_mwl_info, R.id.settings_subtext_method_isna_info,
                R.id.settings_subtext_method_ummalqura_info, R.id.settings_subtext_method_tehran_info, R.id.settings_subtext_method_turkey_info,
                R.id.settings_subtext_method_karachi_regions, R.id.settings_subtext_method_mwl_regions, R.id.settings_subtext_method_isna_regions,
                R.id.settings_subtext_method_ummalqura_regions, R.id.settings_subtext_method_tehran_regions, R.id.settings_subtext_method_turkey_regions}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTagSize);
        }

        if (hijriCards != null) {
            for (View v : hijriCards) {
                if (v != null) {
                    v.setPadding(detailCardPadH, detailCardPadV, detailCardPadH, detailCardPadV);
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    lp.setMargins(0, detailCardMarginV, 0, detailCardMarginV);
                    v.setLayoutParams(lp);
                }
            }
        }
        for (int id : new int[]{R.id.settings_text_hijri_astro, R.id.settings_text_hijri_ummalqura, R.id.settings_text_hijri_local,
                R.id.settings_text_hijri_global, R.id.settings_text_hijri_manual}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTitleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_hijri_astro_tag, R.id.settings_subtext_hijri_ummalqura_tag, R.id.settings_subtext_hijri_local_tag,
                R.id.settings_subtext_hijri_global_tag, R.id.settings_subtext_hijri_manual_tag}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTagSize);
        }

        if (countryCards != null) {
            for (View v : countryCards) {
                if (v != null) {
                    v.setPadding(detailCardPadH, detailCardPadV, detailCardPadH, detailCardPadV);
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    lp.setMargins(0, detailCardMarginV, 0, detailCardMarginV);
                    v.setLayoutParams(lp);
                }
            }
        }
        for (int id : new int[]{R.id.settings_text_country_south_asia, R.id.settings_text_country_saudi, R.id.settings_text_country_uae,
                R.id.settings_text_country_qatar, R.id.settings_text_country_egypt, R.id.settings_text_country_turkey,
                R.id.settings_text_country_north_america, R.id.settings_text_country_southeast_asia, R.id.settings_text_country_iran, R.id.settings_text_country_iraq}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTitleSize);
        }
        for (int id : new int[]{R.id.settings_subtext_country_south_asia_tag, R.id.settings_subtext_country_saudi_tag, R.id.settings_subtext_country_uae_tag,
                R.id.settings_subtext_country_qatar_tag, R.id.settings_subtext_country_egypt_tag, R.id.settings_subtext_country_turkey_tag,
                R.id.settings_subtext_country_north_america_tag, R.id.settings_subtext_country_southeast_asia_tag, R.id.settings_subtext_country_iran_tag, R.id.settings_subtext_country_iraq_tag}) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setTextSize(detailTagSize);
        }
    }

    @Override
    protected void applyClaymorphism(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        int cardBodyColor   = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int shadowColor     = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int highlightColor  = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        int strokeColor     = ContextCompat.getColor(ctx, R.color.off_white_grayish);

        int[] cards = {R.id.settings_card_theme, R.id.settings_card_calculation,
                R.id.settings_card_preferences, R.id.settings_card_data_management};
        for (int id : cards) {
            View card = view.findViewById(id);
            if (card == null) continue;
            card.setBackground(ScalingUtils.createClayDrawable(ctx, 0.045f, 0.010f, 0.006f, 0.003f,
                    cardBodyColor, shadowColor, highlightColor, strokeColor));
        }

        View themeContainer = view.findViewById(R.id.settings_theme_toggle_container);
        if (themeContainer != null) {
            themeContainer.setBackground(ScalingUtils.createInsetClayDrawable(ctx, 0.035f, 0.006f, 0.004f,
                    ContextCompat.getColor(ctx, R.color.off_derived), shadowColor, highlightColor));
        }

        View backupBtn = view.findViewById(R.id.settings_button_backup);
        if (backupBtn != null) {
            backupBtn.setBackground(ScalingUtils.createClayDrawable(ctx, 0.035f, 0.008f, 0.005f, 0.003f,
                    ContextCompat.getColor(ctx, R.color.off_derived), shadowColor, highlightColor, strokeColor));
        }

        View syncPill = view.findViewById(R.id.settings_sync_status_pill);
        if (syncPill != null) applyPillBackground(syncPill, ctx);

        updateThemeToggleUI();

        if (prayerPrefs == null) return;
        boolean isStandardActive = !prayerPrefs.getBoolean("is_asr_hanafi", false);

        int activeColor   = ContextCompat.getColor(ctx, R.color.emerald_primary);
        int detailBodyColor = ContextCompat.getColor(ctx, R.color.off_white_primary);

        if (asrCardStandard != null) {
            asrCardStandard.setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                    isStandardActive ? activeColor : detailBodyColor, shadowColor, highlightColor,
                    isStandardActive ? activeColor : strokeColor));
        }
        if (asrCardHanafi != null) {
            asrCardHanafi.setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                    !isStandardActive ? activeColor : detailBodyColor, shadowColor, highlightColor,
                    !isStandardActive ? activeColor : strokeColor));
        }

        boolean isNight = (ctx.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int activeSubColor = ContextCompat.getColor(ctx, R.color.prayer_card_active_secondary_text);
        int inactiveSubColor = ContextCompat.getColor(ctx, R.color.text_secondary);

        TextView subStandard = view.findViewById(R.id.settings_subtext_asr_standard);
        TextView subHanafi = view.findViewById(R.id.settings_subtext_asr_hanafi);
        TextView tvAsrStandardTitle = view.findViewById(R.id.settings_text_asr_standard);
        TextView tvAsrHanafiTitle = view.findViewById(R.id.settings_text_asr_hanafi);

        if (tvAsrStandardTitle != null) tvAsrStandardTitle.setTextColor(isStandardActive ? detailBodyColor : activeColor);
        if (tvAsrHanafiTitle != null) tvAsrHanafiTitle.setTextColor(!isStandardActive ? detailBodyColor : activeColor);

        if (subStandard != null) {
            subStandard.setTextColor(isStandardActive && !isNight ? activeSubColor : inactiveSubColor);
            subStandard.setAlpha(0.8f);
        }
        if (subHanafi != null) {
            subHanafi.setTextColor(!isStandardActive && !isNight ? activeSubColor : inactiveSubColor);
            subHanafi.setAlpha(0.8f);
        }

        String selectedMethod = prayerPrefs.getString("calculation_method", "KARACHI");
        String[] methodIds = {"KARACHI", "MWL", "ISNA", "UMM_AL_QURA", "TEHRAN", "TURKEY"};
        if (methodCards != null) {
            for (int i = 0; i < methodCards.length; i++) {
                if (methodCards[i] == null) continue;
                boolean isActive = methodIds[i].equals(selectedMethod);
                methodCards[i].setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                        isActive ? activeColor : detailBodyColor, shadowColor, highlightColor,
                        isActive ? activeColor : strokeColor));

                int titleId = 0, infoId = 0, regId = 0;
                switch(i) {
                    case 0: titleId = R.id.settings_text_method_karachi; infoId = R.id.settings_subtext_method_karachi_info; regId = R.id.settings_subtext_method_karachi_regions; break;
                    case 1: titleId = R.id.settings_text_method_mwl; infoId = R.id.settings_subtext_method_mwl_info; regId = R.id.settings_subtext_method_mwl_regions; break;
                    case 2: titleId = R.id.settings_text_method_isna; infoId = R.id.settings_subtext_method_isna_info; regId = R.id.settings_subtext_method_isna_regions; break;
                    case 3: titleId = R.id.settings_text_method_ummalqura; infoId = R.id.settings_subtext_method_ummalqura_info; regId = R.id.settings_subtext_method_ummalqura_regions; break;
                    case 4: titleId = R.id.settings_text_method_tehran; infoId = R.id.settings_subtext_method_tehran_info; regId = R.id.settings_subtext_method_tehran_regions; break;
                    case 5: titleId = R.id.settings_text_method_turkey; infoId = R.id.settings_subtext_method_turkey_info; regId = R.id.settings_subtext_method_turkey_regions; break;
                }
                TextView tvMTitle = view.findViewById(titleId);
                if (tvMTitle != null) tvMTitle.setTextColor(isActive ? detailBodyColor : activeColor);
                TextView tvInfo = view.findViewById(infoId);
                TextView tvReg = view.findViewById(regId);
                if (tvInfo != null) {
                    tvInfo.setTextColor(isActive && !isNight ? activeSubColor : inactiveSubColor);
                    tvInfo.setAlpha(0.8f);
                }
                if (tvReg != null) {
                    tvReg.setTextColor(isActive && !isNight ? activeSubColor : inactiveSubColor);
                    tvReg.setAlpha(0.8f);
                }
            }
        }

        String selectedHijri = prayerPrefs.getString("hijri_system", "ASTRONOMICAL");
        String[] hijriIds = {"ASTRONOMICAL", "UMM_AL_QURA", "LOCAL_SIGHTING", "GLOBAL_SIGHTING", "MANUAL_OFFSET"};
        if (hijriCards != null) {
            for (int i = 0; i < hijriCards.length; i++) {
                if (hijriCards[i] == null) continue;
                boolean isActive = hijriIds[i].equals(selectedHijri);
                hijriCards[i].setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                        isActive ? activeColor : detailBodyColor, shadowColor, highlightColor,
                        isActive ? activeColor : strokeColor));
                int titleId = 0, tagId = 0;
                switch(i) {
                    case 0: titleId = R.id.settings_text_hijri_astro; tagId = R.id.settings_subtext_hijri_astro_tag; break;
                    case 1: titleId = R.id.settings_text_hijri_ummalqura; tagId = R.id.settings_subtext_hijri_ummalqura_tag; break;
                    case 2: titleId = R.id.settings_text_hijri_local; tagId = R.id.settings_subtext_hijri_local_tag; break;
                    case 3: titleId = R.id.settings_text_hijri_global; tagId = R.id.settings_subtext_hijri_global_tag; break;
                    case 4: titleId = R.id.settings_text_hijri_manual; tagId = R.id.settings_subtext_hijri_manual_tag; break;
                }
                TextView tvHTitle = view.findViewById(titleId);
                if (tvHTitle != null) tvHTitle.setTextColor(isActive ? detailBodyColor : activeColor);
                TextView tvTagH = view.findViewById(tagId);
                if (tvTagH != null) {
                    tvTagH.setTextColor(isActive && !isNight ? activeSubColor : inactiveSubColor);
                    tvTagH.setAlpha(0.8f);
                }
            }
        }

        String selectedCountry = prayerPrefs.getString("selected_country", "SOUTH_ASIA");
        String[] countryIds = {"SOUTH_ASIA", "SAUDI", "UAE", "QATAR", "EGYPT", "TURKEY", "NORTH_AMERICA", "SOUTHEAST_ASIA", "IRAN", "IRAQ"};
        if (countryCards != null) {
            for (int i = 0; i < countryCards.length; i++) {
                if (countryCards[i] == null) continue;
                boolean isActive = countryIds[i].equals(selectedCountry);
                countryCards[i].setBackground(ScalingUtils.createClayDrawable(ctx, 0.04f, 0.012f, 0.006f, 0.004f,
                        isActive ? activeColor : detailBodyColor, shadowColor, highlightColor,
                        isActive ? activeColor : strokeColor));
                int titleId = 0, tagId = 0;
                switch(i) {
                    case 0: titleId = R.id.settings_text_country_south_asia; tagId = R.id.settings_subtext_country_south_asia_tag; break;
                    case 1: titleId = R.id.settings_text_country_saudi; tagId = R.id.settings_subtext_country_saudi_tag; break;
                    case 2: titleId = R.id.settings_text_country_uae; tagId = R.id.settings_subtext_country_uae_tag; break;
                    case 3: titleId = R.id.settings_text_country_qatar; tagId = R.id.settings_subtext_country_qatar_tag; break;
                    case 4: titleId = R.id.settings_text_country_egypt; tagId = R.id.settings_subtext_country_egypt_tag; break;
                    case 5: titleId = R.id.settings_text_country_turkey; tagId = R.id.settings_subtext_country_turkey_tag; break;
                    case 6: titleId = R.id.settings_text_country_north_america; tagId = R.id.settings_subtext_country_north_america_tag; break;
                    case 7: titleId = R.id.settings_text_country_southeast_asia; tagId = R.id.settings_subtext_country_southeast_asia_tag; break;
                    case 8: titleId = R.id.settings_text_country_iran; tagId = R.id.settings_subtext_country_iran_tag; break;
                    case 9: titleId = R.id.settings_text_country_iraq; tagId = R.id.settings_subtext_country_iraq_tag; break;
                }
                TextView tvCTitle = view.findViewById(titleId);
                if (tvCTitle != null) tvCTitle.setTextColor(isActive ? detailBodyColor : activeColor);
                TextView tvTagC = view.findViewById(tagId);
                if (tvTagC != null) {
                    tvTagC.setTextColor(isActive && !isNight ? activeSubColor : inactiveSubColor);
                    tvTagC.setAlpha(0.8f);
                }
            }
        }
    }

    private void applyPillBackground(View pill, Context ctx) {
        if (pill == null || ctx == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(ScalingUtils.getScaledSize(ctx, 0.06f));
        bg.setColor(ContextCompat.getColor(ctx, R.color.emerald_alpha_20));
        pill.setBackground(bg);
    }
}