package com.example.imam_al_masjid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditTimingsFragment extends BaseFragment {

    private LinearLayout layoutDayRibbon;
    private LinearLayout layoutPrayerList;
    private TextView textEmptyState;
    private final String[] PRAYERS = {"Fajr", "Zuhr", "Asr", "Maghrib", "Isha"};
    private int selectedDayIndex = 0;
    
    // Persistence (Survives theme changes)
    private static final String PREFS_EDIT = "imam_edit_timings_prefs";
    private static final String KEY_PROP_PREFIX = "prop_preference_";

    // Session-level Mock Storage for UI demonstration
    private static final java.util.Map<String, String> sessionSavedTimings = new java.util.HashMap<>();

    public EditTimingsFragment() {
        // Required empty public constructor
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_timings;
    }

    @Override
    protected void initViews(View view) {
        layoutDayRibbon = view.findViewById(R.id.layout_day_ribbon);
        layoutPrayerList = view.findViewById(R.id.layout_prayer_list);
        textEmptyState = view.findViewById(R.id.text_empty_state_msg);
        
        setupDayRibbon();
        if (getContext() != null) {
            loadPrayerCards(LayoutInflater.from(getContext()));
        }
    }

    private void setupDayRibbon() {
        if (getContext() == null) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat numFormat = new SimpleDateFormat("dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            View dayView = inflater.inflate(R.layout.item_dashboard_edit_calendar_day, layoutDayRibbon, false);
            TextView txtDayName = dayView.findViewById(R.id.text_day_name);
            TextView txtDayNum = dayView.findViewById(R.id.text_day_number);

            txtDayName.setText(dayFormat.format(cal.getTime()).toUpperCase());
            txtDayNum.setText(numFormat.format(cal.getTime()));

            setupDayCardScaling(dayView);
            
            final int index = i;
            dayView.setOnClickListener(v -> selectDay(index));
            
            layoutDayRibbon.addView(dayView);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        selectDay(0);
    }

    private void selectDay(int index) {
        if (getContext() == null) return;
        selectedDayIndex = index;
        
        for (int i = 0; i < layoutDayRibbon.getChildCount(); i++) {
            View child = layoutDayRibbon.getChildAt(i);
            TextView txtDayName = child.findViewById(R.id.text_day_name);
            TextView txtDayNum = child.findViewById(R.id.text_day_number);
            View indicator = child.findViewById(R.id.selection_indicator);
            
            boolean isSelected = (i == index);
            indicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            if (isSelected) {
                int bodyColor = ContextCompat.getColor(getContext(), R.color.prayer_card_bg_active);
                int shadowColor = ContextCompat.getColor(getContext(), R.color.clay_dark_shadow);
                int highlightColor = ContextCompat.getColor(getContext(), R.color.clay_light_shadow);
                int strokeColor = ContextCompat.getColor(getContext(), R.color.prayer_card_border_active);
                
                child.setBackground(ScalingUtils.createClayDrawable(getContext(), 
                        0.045f, 0.010f, 0.006f, 0.003f, 
                        bodyColor, shadowColor, highlightColor, strokeColor));
                
                txtDayName.setTextColor(ContextCompat.getColor(getContext(), R.color.prayer_card_active_secondary_text));
                txtDayNum.setTextColor(ContextCompat.getColor(getContext(), R.color.prayer_card_active_primary_text));
                
                // Lift selected card slightly
                child.animate().translationZ(10f).scaleY(1.05f).scaleX(1.05f).setDuration(300).start();
            } else {
                int bodyColor = ContextCompat.getColor(getContext(), R.color.off_white_primary);
                int shadowColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_shadow);
                int highlightColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_highlight);
                int strokeColor = ContextCompat.getColor(getContext(), R.color.off_white_grayish);
                
                child.setBackground(ScalingUtils.createClayDrawable(getContext(), 
                        0.045f, 0.010f, 0.006f, 0.002f, 
                        bodyColor, shadowColor, highlightColor, strokeColor));
                
                txtDayName.setTextColor(ContextCompat.getColor(getContext(), R.color.prayer_card_sub_text));
                txtDayNum.setTextColor(ContextCompat.getColor(getContext(), R.color.prayer_card_name_text));
                
                // Reset non-selected
                child.animate().translationZ(0f).scaleY(1.0f).scaleX(1.0f).setDuration(300).start();
            }
        }
        // Reload cards to reflect context changes (IsToday vs IsFuture)
        loadPrayerCards(LayoutInflater.from(getContext()));
    }

    private void loadPrayerCards(LayoutInflater inflater) {
        Context ctx = inflater.getContext();
        layoutPrayerList.removeAllViews();
        boolean isToday = (selectedDayIndex == 0);
        int addedCount = 0;

        for (String prayer : PRAYERS) {
            // Hide card if current time is within 15min of Azan (only for Today)
            if (isToday) {
                if (isPrayerLocked(prayer)) continue;
            }

            View card = inflater.inflate(R.layout.item_dashboard_edit_prayer_session, layoutPrayerList, false);
            addedCount++;
            TextView txtName = card.findViewById(R.id.text_prayer_name);
            TextView txtAzan = card.findViewById(R.id.text_azan_time);
            TextView txtJamat = card.findViewById(R.id.text_jamat_time);
            View layoutContextual = card.findViewById(R.id.layout_contextual_action);
            TextView txtContextual = card.findViewById(R.id.text_contextual_label);
            android.widget.ImageView imgContextual = card.findViewById(R.id.img_contextual_icon);
            View btnSubmit = card.findViewById(R.id.btn_submit_prayer);

            txtName.setText(prayer);
            txtAzan.setText(getSavedOrDefault(selectedDayIndex, prayer, "azan"));
            txtJamat.setText(getSavedOrDefault(selectedDayIndex, prayer, "jamat"));
            
            updateCelestialOrbit(card, prayer, ContextCompat.getColor(ctx, R.color.prayer_card_border_active));

            // Propagate option if it's the first editable date for this prayer
            boolean showPropagate = isToday || (selectedDayIndex == 1 && isPrayerLocked(prayer));
            
            if (showPropagate) {
                txtContextual.setText(R.string.label_propagate_forward);
                imgContextual.setImageResource(R.drawable.ic_settings_sync);
                layoutContextual.setOnClickListener(v -> showPropagateMenu(v, prayer));
            } else {
                // Default for future days
                txtContextual.setText(R.string.label_copy_yesterday);
                imgContextual.setImageResource(R.drawable.ic_settings_backup);
                layoutContextual.setOnClickListener(v -> {
                    String msg = getString(R.string.toast_prayer_updated, prayer);
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                });
            }

            btnSubmit.setOnClickListener(v -> handlePrayerSubmit(card, prayer));

            card.findViewById(R.id.layout_edit_azan).setOnClickListener(v -> 
                showTimePicker(txtAzan, prayer + " Azan"));
            
            card.findViewById(R.id.layout_edit_jamat).setOnClickListener(v -> 
                showTimePicker(txtJamat, prayer + " Jamat"));

            setupPrayerCardStyling(card);
            layoutPrayerList.addView(card);
        }

        // Show empty state if all cards are filtered out
        if (textEmptyState != null) {
            textEmptyState.setVisibility(addedCount == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isPrayerLocked(String prayer) {
        try {
            String azanTime = getDefaultAzan(prayer);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(azanTime);
            if (date == null) return false;

            Calendar azanCal = Calendar.getInstance();
            azanCal.setTime(date);
            int azanMins = azanCal.get(Calendar.HOUR_OF_DAY) * 60 + azanCal.get(Calendar.MINUTE);

            Calendar now = Calendar.getInstance();
            int nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

            return nowMins >= (azanMins - 15);
        } catch (Exception e) {
            return false;
        }
    }

    private int getTimeInMinutes(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(timeStr);
            if (date == null) return 0;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        } catch (Exception e) { return 0; }
    }

    private String getSavedOrDefault(int dayIndex, String prayer, String type) {
        String key = dayIndex + "_" + prayer + "_" + type;
        if (sessionSavedTimings.containsKey(key)) {
            return sessionSavedTimings.get(key);
        }
        return type.equals("azan") ? getDefaultAzan(prayer) : getDefaultJamat(prayer);
    }

    private String getDefaultAzan(String prayer) {
        switch (prayer.toLowerCase()) {
            case "fajr": return "05:00 AM";
            case "zuhr": return "01:05 PM";
            case "asr": return "04:45 PM";
            case "maghrib": return "06:26 PM";
            case "isha": return "08:00 PM";
            default: return "00:00 AM";
        }
    }

    private String getDefaultJamat(String prayer) {
        switch (prayer.toLowerCase()) {
            case "fajr": return "05:30 AM";
            case "zuhr": return "01:20 PM";
            case "asr": return "05:00 PM";
            case "maghrib": return "06:29 PM";
            case "isha": return "08:15 PM";
            default: return "00:00 AM";
        }
    }

    private void showTimePicker(TextView targetText, String title) {
        if (getContext() == null) return;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTheme(R.style.Theme_ImamALMasjid_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText(title)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                    (picker.getHour() == 0 || picker.getHour() == 12) ? 12 : picker.getHour() % 12,
                    picker.getMinute(),
                    picker.getHour() < 12 ? "AM" : "PM");

            targetText.setText(selectedTime);
        });

        picker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    private boolean isTimeAfter(String jamatStr, String azanStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date jamat = sdf.parse(jamatStr);
            Date azan = sdf.parse(azanStr);
            if (jamat != null && azan != null) {
                // Jamat must be AT LEAST 1 minute AFTER Azan (Equal is not allowed)
                return jamat.after(azan);
            }
        } catch (Exception ignored) {}
        return true;
    }

    private boolean isAzanInFuture(String azanStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf.parse(azanStr);
            if (date == null) return true;

            Calendar azanCal = Calendar.getInstance();
            azanCal.setTime(date);
            int azanMins = azanCal.get(Calendar.HOUR_OF_DAY) * 60 + azanCal.get(Calendar.MINUTE);

            Calendar now = Calendar.getInstance();
            int nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

            // Azan must be > (now + 15)
            return azanMins > (nowMins + 15);
        } catch (Exception e) {
            return true;
        }
    }

    private void setupDayCardScaling(View card) {
        Context ctx = getContext();
        if (ctx == null) return;
        int size = ScalingUtils.getScaledSize(ctx, 0.18f); // 18% width for day box
        int margin = ScalingUtils.getScaledSize(ctx, 0.02f);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(margin, 0, margin, 0);
        card.setLayoutParams(lp);
        
        TextView name = card.findViewById(R.id.text_day_name);
        TextView num = card.findViewById(R.id.text_day_number);
        View indicator = card.findViewById(R.id.selection_indicator);

        name.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.025f));
        num.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.045f));

        // Dynamically scale inner margins and dimensions
        ScalingUtils.applyScaledLayout(num, -1, -1, 0.01f, 0, 0, 0); // 1% top margin
        ScalingUtils.applyScaledLayout(indicator, 0.04f, 0.005f, 0.015f, 0, 0, 0); // 4% width, 0.5% height, 1.5% margin
    }

    private void handlePrayerSubmit(View card, String prayer) {
        if (getContext() == null || card == null) return;
        
        TextView txtAzan = card.findViewById(R.id.text_azan_time);
        TextView txtJamat = card.findViewById(R.id.text_jamat_time);
        View btnSubmit = card.findViewById(R.id.btn_submit_prayer);

        // Real-time Lockout Check (Edge case: time crosses threshold while user is on screen)
        if (selectedDayIndex == 0) {
            if (isPrayerLocked(prayer)) {
                Toast.makeText(getContext(), R.string.error_prayer_locked_after_load, Toast.LENGTH_SHORT).show();
                loadPrayerCards(LayoutInflater.from(getContext()));
                return;
            }
            // Validate that newly entered Azan is also > (now + 15)
            if (!isAzanInFuture(txtAzan.getText().toString())) {
                Toast.makeText(getContext(), R.string.error_azan_too_early, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Final validation before update
        if (!isTimeAfter(txtJamat.getText().toString(), txtAzan.getText().toString())) {
            Toast.makeText(getContext(), R.string.error_jamat_after_azan, Toast.LENGTH_SHORT).show();
            return;
        }

        // Requirement: Validate 20-minute gap between consecutive prayers
        int prayerIdx = -1;
        for (int i = 0; i < PRAYERS.length; i++) {
            if (PRAYERS[i].equals(prayer)) {
                prayerIdx = i;
                break;
            }
        }

        int curAzanMins = getTimeInMinutes(txtAzan.getText().toString());
        int curJamatMins = getTimeInMinutes(txtJamat.getText().toString());

        // Check Previous Prayer's Jamat
        if (prayerIdx > 0) {
            String prevJamatStr = getSavedOrDefault(selectedDayIndex, PRAYERS[prayerIdx - 1], "jamat");
            int prevJamatMins = getTimeInMinutes(prevJamatStr);
            if (curAzanMins - prevJamatMins < 20) {
                Toast.makeText(getContext(), R.string.error_prayer_gap_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check Next Prayer's Azan
        if (prayerIdx < PRAYERS.length - 1) {
            String nextAzanStr = getSavedOrDefault(selectedDayIndex, PRAYERS[prayerIdx + 1], "azan");
            int nextAzanMins = getTimeInMinutes(nextAzanStr);
            if (nextAzanMins - curJamatMins < 20) {
                Toast.makeText(getContext(), R.string.error_prayer_gap_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Save to Session Storage
        String azan = txtAzan.getText().toString();
        String jamat = txtJamat.getText().toString();
        sessionSavedTimings.put(selectedDayIndex + "_" + prayer + "_azan", azan);
        sessionSavedTimings.put(selectedDayIndex + "_" + prayer + "_jamat", jamat);

        // Propagation Logic
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_EDIT, Context.MODE_PRIVATE);
        int daysToPropagate = prefs.getInt(KEY_PROP_PREFIX + prayer, -1);
        
        String msg;
        if (daysToPropagate > 0) {
            // Apply timings to future days in the range
            for (int i = 1; i < daysToPropagate; i++) {
                int targetDay = selectedDayIndex + i;
                // Constraints: Ribbon only shows 7 days
                if (targetDay < 7) {
                    sessionSavedTimings.put(targetDay + "_" + prayer + "_azan", azan);
                    sessionSavedTimings.put(targetDay + "_" + prayer + "_jamat", jamat);
                }
            }
            msg = getString(R.string.toast_propagation_success, prayer, daysToPropagate);
        } else {
            msg = getString(R.string.toast_prayer_updated, prayer);
        }

        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        
        // Success Pulse Animation
        btnSubmit.animate().alpha(0.7f).setDuration(200).withEndAction(() -> 
            btnSubmit.animate().alpha(1.0f).setDuration(200).start()).start();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void applyTactileTouch(View v) {
        if (v == null) return;
        v.setOnTouchListener((v1, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Match Settings Screen: 0.97f scale, 0.85f alpha
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

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void showPropagateMenu(View anchor, String prayer) {
        Context ctx = getContext();
        if (ctx == null) return;

        // Create Custom Claymorphic Dropdown via PopupWindow (Match LoginActivity architecture)
        LinearLayout itemsContainer = new LinearLayout(ctx);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Exact Clay specs from LoginActivity
        int bgColor = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int shadowColor = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int highlightColor = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        itemsContainer.setBackground(ScalingUtils.createClayDrawable(ctx, 
                0.04f, 0.01f, 0.01f, 0, bgColor, shadowColor, highlightColor, 0));

        // Increase width to prevent text wrapping (Using fixed width instead of anchor width)
        int popupWidth = ScalingUtils.getScaledSize(ctx, 0.45f);
        android.widget.PopupWindow popup = new android.widget.PopupWindow(itemsContainer, 
                popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        String[] options = {
            getString(R.string.propagate_3_days),
            getString(R.string.propagate_5_days),
            getString(R.string.propagate_7_days)
        };

        int horizontalPadding = ScalingUtils.getScaledSize(ctx, 0.045f); 
        int verticalPadding = ScalingUtils.getScaledSize(ctx, 0.025f);
        float inputTextSize = 0.040f;

        // Highlighting Logic (Persistent from SharedPreferences)
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(PREFS_EDIT, Context.MODE_PRIVATE);
        int currentSelection = prefs.getInt(KEY_PROP_PREFIX + prayer, -1);
        int highlightBody = ContextCompat.getColor(ctx, R.color.emerald_alpha_20);

        for (int i = 0; i < options.length; i++) {
            final int days = (i == 0) ? 3 : (i == 1) ? 5 : 7;
            String option = options[i];
            
            TextView item = (TextView) getLayoutInflater().inflate(R.layout.dropdown_item, itemsContainer, false);
            item.setText(option);
            item.setTextSize(ScalingUtils.getScaledTextSize(ctx, inputTextSize));
            item.setTextColor(ContextCompat.getColor(ctx, R.color.emerald_primary));
            item.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            
            // Highlight if selected
            if (days == currentSelection) {
                item.setBackground(ScalingUtils.createInsetClayDrawable(ctx, 0.02f, 0.005f, 0.01f,
                        highlightBody, shadowColor, highlightColor));
            }

            // Apply Tactile Touch Animation from Login Screen
            item.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.85f).setDuration(100).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(100).start();
                        break;
                }
                return false;
            });

            item.setOnClickListener(v -> {
                // Save Preference Persistently
                prefs.edit().putInt(KEY_PROP_PREFIX + prayer, days).apply();
                
                String msg = getString(R.string.toast_propagation_success, prayer, days);
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                popup.dismiss();
            });
            
            itemsContainer.addView(item);
        }

        popup.setElevation(ScalingUtils.getScaledSize(ctx, 0.02f));
        popup.showAsDropDown(anchor);
    }

    private void setupPrayerCardStyling(View card) {
        Context ctx = getContext();
        if (ctx == null) return;
        
        // Root Claymorphism
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.prayer_card_root), 0.94f, -1, 0.025f, 0.005f, 0.03f, 0.03f);
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.layout_header_group), -1, -1, 0, 0.02f, 0.02f, 0.02f);

        // Contextual Action Scaling
        View layoutAction = card.findViewById(R.id.layout_contextual_action);
        int cpHP = ScalingUtils.getScaledSize(ctx, 0.025f);
        int cpVP = ScalingUtils.getScaledSize(ctx, 0.012f); // Slimmer vertical profile
        layoutAction.setPadding(cpHP, cpVP, cpHP, cpVP);
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.img_contextual_icon), 0.045f, 0.045f, 0, 0, 0, 0); // Larger icon
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.text_contextual_label), -1, -1, 0, 0, 0.015f, 0);
        ((TextView)card.findViewById(R.id.text_contextual_label)).setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.030f)); // Larger text

        // Submit Button Scaling & Styling
        TextView btnSubmit = card.findViewById(R.id.btn_submit_prayer);
        btnSubmit.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.035f));
        int sVP = ScalingUtils.getScaledSize(ctx, 0.02f);
        btnSubmit.setPadding(0, sVP, 0, sVP);
        ScalingUtils.applyScaledLayout(btnSubmit, -1, -1, 0.025f, 0, 0, 0);
        
        ScalingUtils.applyClaymorphism(btnSubmit, 0.045f, false, ContextCompat.getColor(ctx, R.color.emerald_primary));
        applyTactileTouch(btnSubmit);

        // Orbit and Name
        View orbit = card.findViewById(R.id.view_celestial_orbit);
        ScalingUtils.applyScaledLayout(orbit, 0.12f, 0.12f, 0, 0, 0, 0.015f);
        orbit.setScaleX(0.66f);
        orbit.setScaleY(0.66f);

        TextView name = card.findViewById(R.id.text_prayer_name);
        name.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.045f));
        ScalingUtils.applyScaledLayout(name, -1, -1, 0, 0, 0, 0.02f);

        // Edit Boxes Layout
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.layout_edit_container), -1, -1, 0.01f, 0, 0, 0);

        // Azan/Jamat Labels and Times
        TextView lblAzan = card.findViewById(R.id.lbl_edit_azan);
        TextView txtAzanTime = card.findViewById(R.id.text_azan_time);
        TextView lblJamat = card.findViewById(R.id.lbl_edit_jamat);
        TextView txtJamatTime = card.findViewById(R.id.text_jamat_time);

        float labelSize = ScalingUtils.getScaledTextSize(ctx, 0.028f);
        float timeSize = ScalingUtils.getScaledTextSize(ctx, 0.04f);

        lblAzan.setTextSize(labelSize);
        txtAzanTime.setTextSize(timeSize);
        lblJamat.setTextSize(labelSize);
        txtJamatTime.setTextSize(timeSize);

        ScalingUtils.applyScaledLayout(lblAzan, -1, -1, 0.01f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(txtAzanTime, -1, -1, 0.01f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(lblJamat, -1, -1, 0.01f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(txtJamatTime, -1, -1, 0.01f, 0, 0, 0);

        // Divider
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.edit_divider), 0.002f, 0.10f, 0, 0, 0.02f, 0.02f);

        // Restore Edit Boxes Claymorphism
        int fieldBodyColor = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int fieldShadowColor = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int fieldHighlightColor = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        int fieldStrokeColor = ContextCompat.getColor(ctx, R.color.off_white_grayish);

        android.graphics.drawable.Drawable fieldClay = ScalingUtils.createClayDrawable(ctx,
                0.035f, 0.008f, 0.005f, 0.002f, 
                fieldBodyColor, fieldShadowColor, fieldHighlightColor, fieldStrokeColor);

        card.findViewById(R.id.layout_edit_azan).setBackground(fieldClay);
        card.findViewById(R.id.layout_edit_jamat).setBackground(fieldClay);

        // Main Card Background
        card.findViewById(R.id.prayer_card_root).setBackground(ScalingUtils.createClayDrawable(ctx,
                0.045f, 0.010f, 0.006f, 0.002f, 
                ContextCompat.getColor(ctx, R.color.off_white_primary), 
                ContextCompat.getColor(ctx, R.color.off_white_surface_shadow), 
                ContextCompat.getColor(ctx, R.color.off_white_surface_highlight), 
                ContextCompat.getColor(ctx, R.color.off_white_grayish)));
        
        int rowHPad = ScalingUtils.getScaledSize(ctx, 0.04f);
        int rowVPad = ScalingUtils.getScaledSize(ctx, 0.03f);
        card.findViewById(R.id.prayer_card_root).setPadding(rowHPad, rowVPad, rowHPad, rowVPad);
    }

    private void updateCelestialOrbit(View row, String waqtKey, int color) {
        CelestialOrbitView orbit = row.findViewById(R.id.view_celestial_orbit);
        if (orbit != null) {
            float deg = 0f;
            boolean isNight = false;
            switch(waqtKey.toLowerCase()) {
                case "fajr": deg = 270f; break;
                case "zuhr": deg = 0f; break;
                case "asr": deg = 45f; break;
                case "maghrib": deg = 90f; break;
                case "isha": deg = 180f; isNight = true; break;
            }
            orbit.setOrbitState(deg, isNight, color, false);
        }
    }

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        Context ctx = getContext();

        TextView title = view.findViewById(R.id.edit_timings_title);
        TextView subtitle = view.findViewById(R.id.edit_timings_subtitle);
        
        title.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.055f));
        subtitle.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.035f));

        // Header Section Spacing
        View header = view.findViewById(R.id.edit_timings_header);
        int headerVP = ScalingUtils.getScaledSize(ctx, 0.02f);
        header.setPadding(0, 0, 0, headerVP);
        ScalingUtils.applyScaledLayout(subtitle, -1, -1, 0.01f, 0, 0, 0);

        // Match Header Section width (Full width with 0 margins)
        View ribbon = view.findViewById(R.id.scroll_day_ribbon);
        ScalingUtils.applyScaledLayout(ribbon, 1.0f, -1, 0.02f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(view.findViewById(R.id.layout_prayer_list), 1.0f, -1, 0, 0, 0, 0);

        // List Bottom Padding
        int listBottomPad = ScalingUtils.getScaledSize(ctx, 0.04f); // Reduced from 0.15f after FAB removal
        view.findViewById(R.id.layout_prayer_list).setPadding(0, 0, 0, listBottomPad);

        if (textEmptyState != null) {
            textEmptyState.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.04f));
            textEmptyState.setLineSpacing(0, 1.2f);
        }
    }

    @Override
    protected void applyClaymorphism(View view) {
        // Claymorphism is now applied per-card in setupPrayerCardStyling
    }
}