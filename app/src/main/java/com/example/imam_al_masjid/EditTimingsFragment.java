package com.example.imam_al_masjid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private final String[] PRAYERS = {"Fajr", "Zuhr", "Asr", "Maghrib", "Isha"};

    public EditTimingsFragment() {
        // Required empty public constructor
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_edit_timings;
    }

    @Override
    protected void initViews(View view) {
        layoutPrayerList = view.findViewById(R.id.layout_prayer_list);
        layoutDayRibbon = view.findViewById(R.id.layout_day_ribbon);

        setupDayRibbon();
        if (getContext() != null) {
            loadPrayerCards(LayoutInflater.from(getContext()));
        }

        view.findViewById(R.id.card_sync_all).setOnClickListener(v -> {
            if (getContext() == null) return;
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(R.string.sync_dialog_title)
                    .setMessage(R.string.sync_dialog_message)
                    .setPositiveButton(R.string.sync_dialog_positive, (dialog, which) -> 
                        android.widget.Toast.makeText(getContext(), "Schedule replicated across 7 days", android.widget.Toast.LENGTH_SHORT).show())
                    .setNegativeButton(R.string.sync_dialog_negative, null)
                    .show();
        });
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
    }

    private void loadPrayerCards(LayoutInflater inflater) {
        Context ctx = inflater.getContext();
        layoutPrayerList.removeAllViews();
        for (String prayer : PRAYERS) {
            View card = inflater.inflate(R.layout.item_dashboard_edit_prayer_session, layoutPrayerList, false);
            TextView txtName = card.findViewById(R.id.text_prayer_name);
            TextView txtAzan = card.findViewById(R.id.text_azan_time);
            TextView txtJamat = card.findViewById(R.id.text_jamat_time);
            View btnCopy = card.findViewById(R.id.btn_copy_yesterday);

            txtName.setText(prayer);
            
            // Sets sun/moon icons based on prayer name
            // Pass the Active Border color even for inactive cards to match Dashboard's gold-ring aesthetic
            int activeBorderColor = ContextCompat.getColor(ctx, R.color.prayer_card_border_active);
            updateCelestialOrbit(card, prayer, activeBorderColor);

            card.findViewById(R.id.layout_edit_azan).setOnClickListener(v -> 
                showTimePicker(txtAzan, prayer + " Azan", null));
            
            card.findViewById(R.id.layout_edit_jamat).setOnClickListener(v -> 
                showTimePicker(txtJamat, prayer + " Jamat", txtAzan.getText().toString()));
            
            btnCopy.setOnClickListener(v -> {
                String msg = getString(R.string.toast_syncing_prayer, prayer);
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
            });

            setupPrayerCardStyling(card);
            layoutPrayerList.addView(card);
        }
    }

    private void showTimePicker(TextView targetText, String title, String azanTimeForValidation) {
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

            if (azanTimeForValidation != null && !azanTimeForValidation.equals(getString(R.string.placeholder_waqt_time))) {
                // Validate Jamat is after Azan
                if (!isTimeAfter(selectedTime, azanTimeForValidation)) {
                    android.widget.Toast.makeText(getContext(), R.string.error_jamat_after_azan, android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
            }

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
                return jamat.after(azan);
            }
        } catch (Exception ignored) {}
        return true;
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

        // Dynamically scale inner margins and dimensions to satisfy prompt Requirement #4
        ScalingUtils.applyScaledLayout(num, -1, -1, 0.01f, 0, 0, 0); // 1% top margin
        ScalingUtils.applyScaledLayout(indicator, 0.04f, 0.005f, 0.015f, 0, 0, 0); // 4% width, 0.5% height, 1.5% margin
    }

    private void setupPrayerCardStyling(View card) {
        Context ctx = getContext();
        if (ctx == null) return;
        
        // Root Claymorphism (already scaled)
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.prayer_card_root), 0.94f, -1, 0.025f, 0.005f, 0.03f, 0.03f);

        // Header Group Layout (originally 8dp margins)
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.layout_header_group), -1, -1, 0, 0.02f, 0.02f, 0.02f);

        // Copy Button Scaling
        View btnCopy = card.findViewById(R.id.btn_copy_yesterday);
        int cpHP = ScalingUtils.getScaledSize(ctx, 0.025f);
        int cpVP = ScalingUtils.getScaledSize(ctx, 0.01f);
        btnCopy.setPadding(cpHP, cpVP, cpHP, cpVP);
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.img_copy_icon), 0.03f, 0.03f, 0, 0, 0, 0);
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.text_copy_label), -1, -1, 0, 0, 0.015f, 0);
        ((TextView)card.findViewById(R.id.text_copy_label)).setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.025f));

        // Orbit View Scaling (Match Dashboard visually, but increase buffer to prevent node cut-off)
        // By setting layout size to 0.12f and scaling down to 0.66f, we get exactly 0.08f visual 
        // diameter while providing maximum internal canvas space for the sun/moon nodes.
        View orbit = card.findViewById(R.id.view_celestial_orbit);
        ScalingUtils.applyScaledLayout(orbit, 0.12f, 0.12f, 0, 0, 0, 0.035f);
        orbit.setScaleX(0.66f);
        orbit.setScaleY(0.66f);

        // Prayer Name Scaling
        TextView name = card.findViewById(R.id.text_prayer_name);
        name.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.045f));
        ScalingUtils.applyScaledLayout(name, -1, -1, 0, 0, 0, 0.02f); // Synchronized margin after scaling

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

        // Divider Scaling
        ScalingUtils.applyScaledLayout(card.findViewById(R.id.edit_divider), 0.002f, 0.10f, 0, 0, 0.02f, 0.02f);

        // 2. Claymorphic Backgrounds
        int bodyColor = ContextCompat.getColor(ctx, R.color.off_white_primary);
        int shadowColor = ContextCompat.getColor(ctx, R.color.off_white_surface_shadow);
        int highlightColor = ContextCompat.getColor(ctx, R.color.off_white_surface_highlight);
        int strokeColor = ContextCompat.getColor(ctx, R.color.off_white_grayish);

        // Applying exact 9-param claymorphism from Home Screen rows
        card.findViewById(R.id.prayer_card_root).setBackground(ScalingUtils.createClayDrawable(ctx,
                0.045f, 0.010f, 0.006f, 0.002f, 
                bodyColor, shadowColor, highlightColor, strokeColor));

        Drawable fieldClay = ScalingUtils.createClayDrawable(ctx,
                0.035f, 0.008f, 0.005f, 0.002f, 
                bodyColor, shadowColor, highlightColor, strokeColor);

        card.findViewById(R.id.layout_edit_azan).setBackground(fieldClay);
        card.findViewById(R.id.layout_edit_jamat).setBackground(fieldClay);
        
        // 3. Spacing Padding
        int hPad = ScalingUtils.getScaledSize(ctx, 0.04f);
        int vPad = ScalingUtils.getScaledSize(ctx, 0.03f);
        card.findViewById(R.id.prayer_card_root).setPadding(hPad, vPad, hPad, vPad);
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

        // List Bottom Padding (to avoid overlapping with the floating action bar)
        int listBottomPad = ScalingUtils.getScaledSize(ctx, 0.15f); // ~15% of screen height
        view.findViewById(R.id.layout_prayer_list).setPadding(0, 0, 0, listBottomPad);

        // Sync Action Bar Scaling (Refactored to single TextView for lint optimization)
        TextView syncBar = view.findViewById(R.id.card_sync_all);
        int sHP = ScalingUtils.getScaledSize(ctx, 0.06f);
        int sVP = ScalingUtils.getScaledSize(ctx, 0.03f);
        syncBar.setPadding(sHP, sVP, sHP, sVP);
        syncBar.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.04f));

        // Scale the compound drawable (icon) manually to maintain proportional design
        android.graphics.drawable.Drawable syncIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_settings_sync);
        if (syncIcon != null) {
            int iconSize = ScalingUtils.getScaledSize(ctx, 0.05f);
            syncIcon.setBounds(0, 0, iconSize, iconSize);
            syncIcon.setTint(ContextCompat.getColor(ctx, R.color.off_white_primary));
            syncBar.setCompoundDrawablesRelative(syncIcon, null, null, null);
            syncBar.setCompoundDrawablePadding(ScalingUtils.getScaledSize(ctx, 0.03f));
        }

        // Floating Position
        ScalingUtils.applyScaledLayout(syncBar, -1, -1, 0, 0.06f, 0, 0);
    }

    @Override
    protected void applyClaymorphism(View view) {
        // Apply Claymorphism to the Sync FAB inner layout to fix border mismatch
        if (getContext() != null) {
            ScalingUtils.applyClaymorphism(view.findViewById(R.id.card_sync_all), 0.045f, false, 
                    ContextCompat.getColor(getContext(), R.color.emerald_primary));
        }
    }
}
