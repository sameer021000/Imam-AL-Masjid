package com.example.imam_al_masjid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.PrayerTimes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends BaseFragment {

    private View headerSection, cardPrayerTimings, panelAnnouncements;
    private View dateContainer, dividerDates;
    private View headerContainer, headerHorizonBackdrop, layoutTopAccent;
    private View dotAxisTop, dotAxisBottom, containerAxisGroup;
    private ImageView imgHeaderLocation, imgAddressLocation;
    private View layoutHeaderAddressGroup;

    // V3 Tab System
    private View tabTrack, tabPill;
    private TextView tabMasjid, tabWaqt;
    private View panelMasjidTimes, panelWaqtDetails;
    private ChronosDialView chronosDial;
    private TextView btnEditLocation;
    private View layoutAddressPapyrus;

    private TextView txtMasjidName, txtMasjidAddress, txtCurrentDate, txtHijriDate, txtDeviceAddress;
    private TextView txtAnnouncementsTitle, txtAnnouncementContent;






    // Timing Row views for the 5 prayers
    private View rowFajr, rowZuhr, rowAsr, rowMaghrib, rowIsha;
    private View activeRow;

    
    // Special Timing Tiles - Removed per user request

    // Backend Logic
    private PrayerTimes latestPrayerTimes;
    private CelestialWaqtEngine.PrecisePrayerTimes latestPreciseTimes;
    private static final String PREFS_NAME = "PrayerSettings";
    private static final String KEY_SPEAKER_PREFIX = "speaker_";


    // Location & Sound
    private FusedLocationProviderClient fusedLocationClient;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // Countdown Backend
    private android.os.Handler countdownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable countdownRunnable;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =

            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    detectCurrentLocation();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    detectCurrentLocation();
                }
            });

    @Override
    protected int getLayoutId() { return R.layout.fragment_dashboard; }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Override
    protected void initViews(View view) {
        // 1. Bind Sections
        layoutTopAccent = view.findViewById(R.id.layout_top_accent_container);
        headerSection = view.findViewById(R.id.dashboard_header_section);
        headerContainer = view.findViewById(R.id.dashboard_header_container);
        headerHorizonBackdrop = view.findViewById(R.id.header_horizon_backdrop);
        layoutHeaderAddressGroup = view.findViewById(R.id.layout_header_address_group);
        imgAddressLocation = view.findViewById(R.id.img_location_pin);

        dateContainer = view.findViewById(R.id.container_dashboard_dates);
        dividerDates = view.findViewById(R.id.divider_dates);
        panelAnnouncements = view.findViewById(R.id.panel_announcements);
        imgHeaderLocation = view.findViewById(R.id.img_header_location);
        dotAxisTop = view.findViewById(R.id.dot_axis_top);
        dotAxisBottom = view.findViewById(R.id.dot_axis_bottom);
        containerAxisGroup = view.findViewById(R.id.container_axis_group);

        // V3 Tab Binding
        tabTrack = view.findViewById(R.id.layout_tab_switcher);
        tabMasjid = view.findViewById(R.id.tab_masjid_times);
        tabWaqt = view.findViewById(R.id.tab_waqt_details);
        panelMasjidTimes = view.findViewById(R.id.panel_masjid_times);
        panelWaqtDetails = view.findViewById(R.id.panel_waqt_details);
        cardPrayerTimings = panelMasjidTimes;
        chronosDial = view.findViewById(R.id.view_chronos_dial);
        btnEditLocation = view.findViewById(R.id.btn_edit_location);
        layoutAddressPapyrus = view.findViewById(R.id.layout_address_papyrus_surface);
        if (layoutAddressPapyrus != null && getContext() != null) {
            layoutAddressPapyrus.setBackground(ScalingUtils.createLayeredPapyrusDrawable(getContext()));
        }

        // Tap-away logic to reset Dial state
        if (panelWaqtDetails != null) {
            panelWaqtDetails.setOnClickListener(v -> {
                if (chronosDial != null) chronosDial.resetTapState();
            });
        }






        // 2. Bind Text Elements
        txtMasjidName = view.findViewById(R.id.txt_dashboard_masjid_name);
        txtMasjidAddress = view.findViewById(R.id.txt_dashboard_masjid_address);
        txtCurrentDate = view.findViewById(R.id.txt_dashboard_current_date);
        txtHijriDate = view.findViewById(R.id.txt_dashboard_hijri_date);
        txtDeviceAddress = view.findViewById(R.id.txt_device_address);

        txtAnnouncementsTitle = view.findViewById(R.id.txt_announcements_title);
        txtAnnouncementContent = view.findViewById(R.id.txt_announcement_content);



        // 3. Bind Timing Rows
        rowFajr = view.findViewById(R.id.row_fajr);
        rowZuhr = view.findViewById(R.id.row_dhuhr);
        rowAsr = view.findViewById(R.id.row_asr);
        rowMaghrib = view.findViewById(R.id.row_maghrib);
        rowIsha = view.findViewById(R.id.row_isha);

        // 4. Bind Special Tiles - Removed per user request

        // 5. Set Initial Content (Placeholder Masjid Times)
        setupTimingRow(rowFajr, "FAJR", "fajr");
        setupTimingRow(rowZuhr, "ZUHR", "zuhr");
        setupTimingRow(rowAsr, "ASR", "asr");
        setupTimingRow(rowMaghrib, "MAGHRIB", "maghrib");
        setupTimingRow(rowIsha, "ISHA", "isha");





        // Special tiles setup removed


        // Match status bar color with top accent
        updateStatusBarColor();

        // Glass Hero Background Initial Styling
        if (getContext() != null) {
            startHeroPulse();
        }

        // 5.5 Location Icon Listener & Pulse Signal
        if (imgHeaderLocation != null) {
            imgHeaderLocation.setOnClickListener(v -> openMasjidMap());

            // Add a subtle, continuous scaling pulse to indicate clickability
            android.view.animation.Animation mapPulse = new android.view.animation.ScaleAnimation(
                    1.0f, 1.15f, 1.0f, 1.15f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
            mapPulse.setDuration(1200);
            mapPulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            mapPulse.setRepeatCount(android.view.animation.Animation.INFINITE);
            imgHeaderLocation.startAnimation(mapPulse);
        }

        // 6. Set Header Dates (Instant update on load, no animation)
        updateHeaderDates(false);

        // 7. Axis Pulse
        startAxisPulse();

        // 6. Request Location
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        // 8. Tab Switcher Setup
        setupTabSwitcher();

        // 9. Initialize Animations (Sets initial states of unrolled elements)
        setupAnimations();

        // 10. Finalize Dynamic Scaling (Requirement #4)
        applyGlobalScaling(view);
    }

    private void applyGlobalScaling(View view) {
        if (getContext() == null) return;

        // Top Accent Container
        ScalingUtils.applyScaledLayout(layoutTopAccent, -1, -1, 0, 0, 0, 0);
        layoutTopAccent.setPadding(0, 0, 0, ScalingUtils.getScaledSize(getContext(), 0.012f));

        // Header Inner Group
        ScalingUtils.applyScaledLayout(headerSection, -1, -1, 0, 0, 0, 0);
        headerSection.setPadding(0, 0, 0, ScalingUtils.getScaledSize(getContext(), 0.03f));
        ScalingUtils.applyScaledLayout(layoutHeaderAddressGroup, -1, -1, 0.005f, 0, 0, 0); // ~2dp
        ScalingUtils.applyScaledLayout(imgHeaderLocation, 0.06f, 0.06f, 0, 0, 0.02f, 0); // ~24dp + 8dp margin

        // Text Sizes
        txtMasjidName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.06f));
        txtMasjidAddress.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.032f));
        txtHijriDate.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.035f));
        txtCurrentDate.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.035f));
        txtAnnouncementsTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.03f));
        txtAnnouncementContent.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.033f));
        txtDeviceAddress.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.033f));

        // Date container margins & divider
        ScalingUtils.applyScaledLayout(dateContainer, -1, -1, 0.01f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(containerAxisGroup, -1, -1, 0, 0, 0.02f, 0.02f); // ~8dp margins
        ScalingUtils.applyScaledLayout(dividerDates, 0.004f, 0.06f, 0, 0, 0, 0);
        ScalingUtils.applyScaledLayout(dotAxisTop, 0.012f, 0.012f, 0, 0, 0, 0);
        ScalingUtils.applyScaledLayout(dotAxisBottom, 0.012f, 0.012f, 0, 0, 0, 0);

        // Date chips padding
        int hPad = ScalingUtils.getScaledSize(getContext(), 0.04f);
        int vPad = ScalingUtils.getScaledSize(getContext(), 0.012f);
        txtHijriDate.setPadding(hPad, vPad, hPad, vPad);
        txtCurrentDate.setPadding(hPad, vPad, hPad, vPad);
        
        // Tab Switcher
        ScalingUtils.applyScaledLayout(tabTrack, -1, 0.12f, 0, 0.03f, 0.06f, 0.06f);
        
        // Announcements Panel
        panelAnnouncements.setPadding(
            ScalingUtils.getScaledSize(getContext(), 0.06f), 
            ScalingUtils.getScaledSize(getContext(), 0.02f),
            ScalingUtils.getScaledSize(getContext(), 0.06f),
            ScalingUtils.getScaledSize(getContext(), 0.02f)
        );

        // Sub-panels
        ScalingUtils.applyScaledLayout(cardPrayerTimings, -1, -1, 0.01f, 0, 0.02f, 0.02f);
        
        // Address Bar Group
        ScalingUtils.applyScaledLayout(view.findViewById(R.id.container_waqt_address), -1, -1, 0.015f, 0, 0, 0);
        int paperShadowOffset = ScalingUtils.getScaledSize(getContext(), 0.015f); // The 6dp bottom inset in Layered Papyrus
        layoutAddressPapyrus.setPadding(
            ScalingUtils.getScaledSize(getContext(), 0.05f), 
            ScalingUtils.getScaledSize(getContext(), 0.03f), 
            ScalingUtils.getScaledSize(getContext(), 0.05f), 
            ScalingUtils.getScaledSize(getContext(), 0.03f) + paperShadowOffset); // Compensate for background shadow
        ScalingUtils.applyScaledLayout(imgAddressLocation, 0.06f, 0.06f, 0, 0, 0.015f, 0);
        ScalingUtils.applyScaledLayout(txtDeviceAddress, -1, -1, 0, 0, 0.025f, 0.10f); // ~10dp start, ~40dp end (clearing EDIT button)
        
        // Edit Button
        ScalingUtils.applyScaledLayout(btnEditLocation, 0.12f, 0.07f, 0.005f, 0, 0, 0.005f);
        btnEditLocation.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ScalingUtils.getScaledSize(getContext(), 0.025f));

        // Chronos Dial (The Centerpiece)
        ScalingUtils.applyScaledLayout(chronosDial, -1, 0.8f, 0.01f, 0, 0, 0);
    }

    private void setupTabSwitcher() {
        if (tabMasjid == null || tabWaqt == null) return;

        tabMasjid.setOnClickListener(v -> switchPanel(true));
        tabWaqt.setOnClickListener(v -> switchPanel(false));
        
        if (btnEditLocation != null) {
            btnEditLocation.setOnClickListener(v -> Toast.makeText(getContext(), "Location editing coming soon", Toast.LENGTH_SHORT).show());
        }

        // Default state
        switchPanel(true);
    }

    private void switchPanel(boolean isMasjid) {
        if (getContext() == null) return;

        // 1. Logic handled in Step 3 for precise Settings matching

        // 2. Toggle Panel Visibility with Cross-fade
        panelMasjidTimes.setVisibility(isMasjid ? View.VISIBLE : View.GONE);
        panelWaqtDetails.setVisibility(isMasjid ? View.GONE : View.VISIBLE);
        
        panelMasjidTimes.setAlpha(isMasjid ? 0f : 1f);
        panelWaqtDetails.setAlpha(isMasjid ? 1f : 0f);

        panelMasjidTimes.animate().alpha(isMasjid ? 1f : 0f).setDuration(300).start();
        panelWaqtDetails.animate().alpha(isMasjid ? 0f : 1f).setDuration(300).start();

        // 3. Update Styles (Match Settings precisely: swap backgrounds + colors)
        int activeBodyColor = ContextCompat.getColor(getContext(), R.color.status_active);
        int activeTextColor = ContextCompat.getColor(getContext(), R.color.text_inverse);
        int inactiveTextColor = ContextCompat.getColor(getContext(), R.color.text_secondary);

        // Update Masjid Tab
        if (isMasjid) {
            tabMasjid.setBackground(ScalingUtils.createClayDrawable(getContext(), 0.04f, 0.006f, 0.003f, 0f,
                    activeBodyColor,
                    ContextCompat.getColor(getContext(), R.color.clay_dark_shadow),
                    ContextCompat.getColor(getContext(), R.color.clay_light_shadow),
                    activeBodyColor));
            tabMasjid.setTextColor(activeTextColor);
            tabWaqt.setBackground(null);
            tabWaqt.setTextColor(inactiveTextColor);
        } else {
            tabWaqt.setBackground(ScalingUtils.createClayDrawable(getContext(), 0.04f, 0.006f, 0.003f, 0f,
                    activeBodyColor,
                    ContextCompat.getColor(getContext(), R.color.clay_dark_shadow),
                    ContextCompat.getColor(getContext(), R.color.clay_light_shadow),
                    activeBodyColor));
            tabWaqt.setTextColor(activeTextColor);
            tabMasjid.setBackground(null);
            tabMasjid.setTextColor(inactiveTextColor);
            // Staleness check: If timings were calculated for a different day, refresh them
            Calendar nowCal = Calendar.getInstance();
            Calendar calcCal = Calendar.getInstance();
            if (latestPreciseTimes != null && latestPreciseTimes.dhuhr != null) {
                calcCal.setTime(latestPreciseTimes.dhuhr); 
                if (nowCal.get(Calendar.DAY_OF_YEAR) != calcCal.get(Calendar.DAY_OF_YEAR)) {
                    detectCurrentLocation();
                }
            }
            
            updateChronosDial(latestPreciseTimes);
            if (chronosDial != null) chronosDial.triggerEntrance();
            
            // Concept D: Trigger the unroll reveal when tab is actually clicked
            animateAddressReveal();
        }
    }

    // Chronos Dial v4 implementation
    private void updateChronosDial(CelestialWaqtEngine.PrecisePrayerTimes prayerTimes) {
        if (prayerTimes == null || chronosDial == null) return;

        List<ChronosDialView.WaqtSegment> list = new ArrayList<>();
        
        // 1. Calculations for Specialized Waqts
        // Ishraq & Chasht (Starts ~15-20 min post sunrise)
        Calendar cal = Calendar.getInstance();
        cal.setTime(prayerTimes.sunrise);
        cal.add(Calendar.MINUTE, 15);
        Date ishraqStart = cal.getTime();
        
        // Dahwa-e-kubra (Islamic Noon: Halfway between Fajr and Sunset)
        long fajrMs = prayerTimes.fajr.getTime();
        long sunsetMs = prayerTimes.maghrib.getTime();
        Date dahwaEKubra = new Date(fajrMs + (sunsetMs - fajrMs) / 2);
        
        // 2. Build ordered segments
        addWaqt(list, "FAJR", prayerTimes.fajr, prayerTimes.sunrise);
        addWaqt(list, "SUNRISE", prayerTimes.sunrise, ishraqStart);
        addWaqt(list, "ISHRAQ & CHASHT", ishraqStart, dahwaEKubra);
        addWaqt(list, "DAHWA-E-KUBRA", dahwaEKubra, prayerTimes.dhuhr);
        addWaqt(list, "ZUHR", prayerTimes.dhuhr, prayerTimes.asr);
        addWaqt(list, "ASR", prayerTimes.asr, prayerTimes.maghrib);
        addWaqt(list, "MAGHRIB", prayerTimes.maghrib, prayerTimes.isha);
        
        // Isha Wrap (Active logic for Point 4 & 5)
        // If Isha is currently started, its end is the NEXT Fajr.
        // We can safely use prayerTimes.fajr + 1 day as the logical cycle end IF it's before midnight,
        // but since we are using a MIXED object, prayerTimes.fajr is ALREADY the correct end 
        // if Isha's start was from yesterday.
        if (prayerTimes.isha.after(prayerTimes.fajr)) {
            // Case: Pre-Midnight (Isha is today, ends at tomorrow's Fajr)
            cal.setTime(prayerTimes.fajr);
            cal.add(Calendar.DATE, 1);
            addWaqt(list, "ISHA", prayerTimes.isha, cal.getTime());
        } else {
            // Case: Post-Midnight (Isha is from yesterday, ends at today's Fajr)
            addWaqt(list, "ISHA", prayerTimes.isha, prayerTimes.fajr);
        }

        chronosDial.setSegments(list);
    }

    private void addWaqt(List<ChronosDialView.WaqtSegment> list, String name, Date start, Date end) {
        if (start == null || end == null) return;
        SimpleDateFormat sdfPrecise = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        SimpleDateFormat sdfRounded = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        // 1. Precise Strings (For Center Display)
        String preciseStart = sdfPrecise.format(start);
        String preciseEnd = sdfPrecise.format(end);

        // 2. Rounded Strings (For Border Markers)
        // Start Time: 06:18:SS --> 06:19 (Ceiling)
        cal.setTime(start);
        int secondsStart = cal.get(Calendar.SECOND);
        if (secondsStart > 0) {
            cal.add(Calendar.MINUTE, 1);
        }
        String roundedStart = sdfRounded.format(cal.getTime());

        // End Time: 06:18:SS --> 06:18 (Floor - dropping seconds)
        String roundedEnd = sdfRounded.format(end);

        // 3. Absolute Seconds (For Radial Mapping)
        cal.setTime(start);
        long startSec = cal.get(Calendar.HOUR_OF_DAY) * 3600L + cal.get(Calendar.MINUTE) * 60L + cal.get(Calendar.SECOND);
        
        cal.setTime(end);
        long endSec = cal.get(Calendar.HOUR_OF_DAY) * 3600L + cal.get(Calendar.MINUTE) * 60L + cal.get(Calendar.SECOND);
        
        list.add(new ChronosDialView.WaqtSegment(name, startSec, endSec, preciseStart, preciseEnd, roundedStart, roundedEnd));
    }

    private void updateStatusBarColor() {
        if (getActivity() != null && getActivity().getWindow() != null && getContext() != null) {
            int color = ContextCompat.getColor(getContext(), R.color.dashboard_top_accent_bg);
            getActivity().getWindow().setStatusBarColor(color);
            
            // Adjust light/dark icons based on background darkness
            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                                 == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            
            androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());
            
            if (controller != null) {
                // If it's NOT night mode, the background is light, so we need dark icons
                controller.setAppearanceLightStatusBars(!isNightMode);
            }
        }
    }


    private void updateHeaderDates() {
        updateHeaderDates(true);
    }

    private void updateHeaderDates(boolean animated) {
        if (getContext() == null) return;
        Date now = new Date();
        
        // 1. Gregorian Date (e.g., Mon 16 Mar 2026)
        SimpleDateFormat gregFormat = new SimpleDateFormat("EEE dd MMM yyyy", Locale.getDefault());
        String gDate = gregFormat.format(now);
        
        // 2. Hijri Date (e.g., 27 Ramadan 1447 AH)
        String hDate = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.icu.util.IslamicCalendar islamicCalendar = new android.icu.util.IslamicCalendar();
            
            // Advanced Backend Logic: Hijri day begins at Sunset (Maghrib)
            if (latestPreciseTimes != null && latestPreciseTimes.maghrib != null) {
                if (now.after(latestPreciseTimes.maghrib)) {
                    Calendar nextDay = Calendar.getInstance();
                    nextDay.setTime(now);
                    nextDay.add(Calendar.DAY_OF_YEAR, 1);
                    islamicCalendar.setTime(nextDay.getTime());
                } else {
                    islamicCalendar.setTime(now);
                }
            } else {
                islamicCalendar.setTime(now);
            }
            
            int day = islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH);
            int month = islamicCalendar.get(android.icu.util.Calendar.MONTH);
            int year = islamicCalendar.get(android.icu.util.Calendar.YEAR);
            
            String[] hijriMonths = {
                "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
                "Jumada al-Ula", "Jumada al-Akhira", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
            };
            
            hDate = day + " " + (month >= 0 && month < 12 ? hijriMonths[month] : month) + " " + year + " AH";
        }

        if (animated) {
            // Apply with Clockwork Roll (Animates only if value actually changes)
            applyClockworkRoll(txtCurrentDate, gDate);
            applyClockworkRoll(txtHijriDate, hDate);
        } else {
            if (txtCurrentDate != null) txtCurrentDate.setText(gDate);
            if (txtHijriDate != null) txtHijriDate.setText(hDate);
        }
    }

    private void applyClockworkRoll(final TextView textView, final String newText) {
        if (textView == null) return;
        
        // If text is same, just set it initially without roll to avoid flickering on first load
        if (textView.getText().toString().equals(newText)) return;

        textView.animate()
                .translationY(-30f)
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.setTranslationY(30f);
                    textView.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                }).start();
    }


    private void setupTimingRow(View row, String name, String prefKey) {
        TextView txtName = row.findViewById(R.id.txt_prayer_name);
        ImageView imgSound = row.findViewById(R.id.img_azan_sound_toggle);

        if (txtName != null) txtName.setText(name);

        if (imgSound != null) {
            boolean isOn = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_SPEAKER_PREFIX + prefKey, true);
            updateSpeakerIcon(imgSound, isOn, false);
            imgSound.setOnClickListener(v -> toggleAzanSound(imgSound, prefKey));
        }

    }

    private void updateSpeakerIcon(ImageView icon, boolean isOn, boolean isActive) {
        if (getContext() == null || icon == null) return;
        
        icon.setImageResource(isOn ? R.drawable.ic_settings_sound_on : R.drawable.ic_settings_sound_off);
        
        int colorRes;
        if (isActive) {
            // Group A for ON, Group B for OFF in Active state
            colorRes = isOn ? R.color.prayer_card_active_primary_text : R.color.prayer_card_active_secondary_text;
        } else {
            // Group A for ON, Group B for OFF in Inactive state
            colorRes = isOn ? R.color.prayer_card_name_text : R.color.prayer_card_sub_text;
        }
        
        int color = ContextCompat.getColor(getContext(), colorRes);
        // Using ImageViewCompat for most reliable vector tinting across all Android versions
        androidx.core.widget.ImageViewCompat.setImageTintList(icon, android.content.res.ColorStateList.valueOf(color));
        icon.setAlpha(1.0f); 
    }

    // setupSpecialTile removed

    private void startHeroPulse() {
        // Concept M Hero pulse removed per user request
        if (headerHorizonBackdrop != null) {
            android.animation.ObjectAnimator pulseHeader = android.animation.ObjectAnimator.ofFloat(headerHorizonBackdrop, "alpha", 0.6f, 0.9f);
            pulseHeader.setDuration(3500); // Slightly different duration for organic feel
            pulseHeader.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            pulseHeader.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            pulseHeader.start();
        }
    }



    private void startAxisPulse() {
        if (dotAxisTop != null && dotAxisBottom != null) {
            android.view.animation.Animation pulse = new android.view.animation.AlphaAnimation(0.4f, 1.0f);
            pulse.setDuration(1500);
            pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
            dotAxisTop.startAnimation(pulse);
            dotAxisBottom.startAnimation(pulse);
        }
    }


    private void openMasjidMap() {

        String address = getString(R.string.address_alamgeer);
        Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to any app that can handle geo URIs
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            startActivity(fallbackIntent);
        }
    }


    private void toggleAzanSound(ImageView icon, String prefKey) {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean currentState = prefs.getBoolean(KEY_SPEAKER_PREFIX + prefKey, true);
        boolean newState = !currentState;
        
        prefs.edit().putBoolean(KEY_SPEAKER_PREFIX + prefKey, newState).apply();
        
        // Detect if this specific row is the active one by comparing view references
        boolean isActive = false;
        android.view.ViewParent vp = icon.getParent();
        while (vp instanceof View) {
            if (vp == activeRow && activeRow != null) {
                isActive = true;
                break;
            }
            vp = vp.getParent();
        }

        updateSpeakerIcon(icon, newState, isActive);



        if (!newState && isPlaying) {
            // If user turned off sound while azan was playing, stop it
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isPlaying = false;
        }
    }

    private void playAzanSound() {
        if (isPlaying || getContext() == null) return;
        try {
            // Using placeholder notification sound as requested
            mediaPlayer = MediaPlayer.create(getContext(), android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                mediaPlayer.release();
                mediaPlayer = null;
            });
            mediaPlayer.start();
            isPlaying = true;
        } catch (Exception ignored) {}
    }


    private void detectCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateAddressFromLocation(location);
                calculateLocationBasedWaqts(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private String getStaticAzanTime(String waqt) {
        switch (waqt.toLowerCase()) {
            case "fajr": return "05:15 AM";
            case "zuhr": return "01:00 PM";
            case "asr": return "04:45 PM";
            case "maghrib": return "06:35 PM";
            case "isha": return "08:30 PM";
            default: return "--:--";
        }
    }

    private String getStaticJamatTime(String waqt) {
        switch (waqt.toLowerCase()) {
            case "fajr": return "05:30 AM";
            case "zuhr": return "01:20 PM";
            case "asr": return "05:00 PM";
            case "maghrib": return "06:38 PM";
            case "isha": return "08:45 PM";
            default: return "--:--";
        }
    }

    private void calculateLocationBasedWaqts(double lat, double lon) {
        Coordinates coordinates = new Coordinates(lat, lon);
        Calendar calToday = Calendar.getInstance();
        Date now = new Date();
        calToday.setTime(now);
        
        // 1. Calculate timings for TODAY (The default calendar day)
        CalculationParameters parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.getParameters();
        CelestialWaqtEngine.PrecisePrayerTimes today = CelestialWaqtEngine.calculate(lat, lon, calToday.getTime(), 
            parameters.fajrAngle, parameters.ishaAngle);

        // 2. Calculate timings for YESTERDAY (Required for the Isha midnight wrap-around)
        Calendar calYesterday = (Calendar) calToday.clone();
        calYesterday.add(Calendar.DATE, -1);
        CelestialWaqtEngine.PrecisePrayerTimes yesterday = CelestialWaqtEngine.calculate(lat, lon, calYesterday.getTime(), 
            parameters.fajrAngle, parameters.ishaAngle);

        // 3. MIXED STATE LOGIC: Points 3 and 4 of the approved plan
        // At 12:00 AM, update every waqt's start & end time with current date (today), 
        // except the Isha's start time (as Isha didn't complete yet).
        if (now.before(today.fajr)) {
            // "Isha is the current waqt and time is > 12:00 AM" or "today's Fajr has not started yet"
            // We use Today's object as the base but swap Isha with Yesterday's version.
            today.isha = yesterday.isha;
            // The endsAt for this Isha is today's Fajr (which today.fajr already is).
        }

        // 4. Persistence for logic and UI
        latestPreciseTimes = today;
        
        // Use Adhan library object for parts of the legacy UI/logic that still need it
        Calendar calComp = Calendar.getInstance();
        calComp.setTime(today.dhuhr); // Use the current day as reference
        DateComponents dc = new DateComponents(calComp.get(Calendar.YEAR), calComp.get(Calendar.MONTH) + 1, calComp.get(Calendar.DAY_OF_MONTH));
        latestPrayerTimes = new PrayerTimes(coordinates, dc, parameters);

        // Update Static Labels (always for today)
        // Update Static Labels
        updateRowWaqtTimes(rowFajr, getStaticAzanTime("fajr"), getStaticJamatTime("fajr"));
        updateRowWaqtTimes(rowZuhr, getStaticAzanTime("zuhr"), getStaticJamatTime("zuhr"));
        updateRowWaqtTimes(rowAsr, getStaticAzanTime("asr"), getStaticJamatTime("asr"));
        updateRowWaqtTimes(rowMaghrib, getStaticAzanTime("maghrib"), getStaticJamatTime("maghrib"));
        
        // Point 1: Except Isha's start/end time (until it completes at Fajr today)
        if (now.before(today.fajr)) {
             // For the static text row labels, we can continue showing yesterday's times? 
             // Or actually, the plan says "Don't update Isha's start/end". 
             // I'll keep the static labels matching the religious logic.
             updateRowWaqtTimes(rowIsha, getStaticAzanTime("isha"), getStaticJamatTime("isha"));
        } else {
             updateRowWaqtTimes(rowIsha, getStaticAzanTime("isha"), getStaticJamatTime("isha"));
        }

        highlightActiveWaqt(latestPreciseTimes, coordinates);
        updateHeaderDates(false);
    }



    private void highlightActiveWaqt(CelestialWaqtEngine.PrecisePrayerTimes prayerTimes, Coordinates coordinates) {
        if (prayerTimes == null) return;
        
        // Use high-precision timings for the dial
        updateChronosDial(prayerTimes);
        
        // For highlighting the status row, we can still use the library's Enum or our own logic
        // But let's just use the current time compared to our precise dates
        Date now = new Date();
        com.batoulapps.adhan.Prayer current = com.batoulapps.adhan.Prayer.NONE;
        
        if (now.after(prayerTimes.fajr) && now.before(prayerTimes.sunrise)) current = com.batoulapps.adhan.Prayer.FAJR;
        else if (now.after(prayerTimes.sunrise) && now.before(prayerTimes.dhuhr)) current = com.batoulapps.adhan.Prayer.SUNRISE;
        else if (now.after(prayerTimes.dhuhr) && now.before(prayerTimes.asr)) current = com.batoulapps.adhan.Prayer.DHUHR;
        else if (now.after(prayerTimes.asr) && now.before(prayerTimes.maghrib)) current = com.batoulapps.adhan.Prayer.ASR;
        else if (now.after(prayerTimes.maghrib) && now.before(prayerTimes.isha)) current = com.batoulapps.adhan.Prayer.MAGHRIB;
        else if (now.after(prayerTimes.isha)) current = com.batoulapps.adhan.Prayer.ISHA;
        
        // Reset all rows
        resetRowHighlight(rowFajr, "FAJR", "fajr");
        resetRowHighlight(rowZuhr, "ZUHR", "zuhr");
        resetRowHighlight(rowAsr, "ASR", "asr");
        resetRowHighlight(rowMaghrib, "MAGHRIB", "maghrib");
        resetRowHighlight(rowIsha, "ISHA", "isha");
        activeRow = null;

        View targetRow = null;

        String currentWaqtName = "";
        Date waqtEndsAt = null;

        // Reuse 'now' variable
        now = new Date();

        if (current == com.batoulapps.adhan.Prayer.NONE) {
            if (now.before(prayerTimes.fajr)) {
                current = com.batoulapps.adhan.Prayer.ISHA;
            } else {
                current = com.batoulapps.adhan.Prayer.ISHA; // Passed Isha
            }
        }

        switch (current) {
            case FAJR: 
                targetRow = rowFajr; 
                currentWaqtName = "FAJR";
                waqtEndsAt = prayerTimes.sunrise;
                break;
            case SUNRISE:
                currentWaqtName = "SUNRISE";
                waqtEndsAt = prayerTimes.dhuhr;
                break;
            case DHUHR: 
                targetRow = rowZuhr; 
                currentWaqtName = "ZUHR";
                waqtEndsAt = prayerTimes.asr;
                break;


            case ASR: 
                targetRow = rowAsr; 
                currentWaqtName = "ASR";
                waqtEndsAt = prayerTimes.maghrib;
                break;
            case MAGHRIB: 
                targetRow = rowMaghrib; 
                currentWaqtName = "MAGHRIB";
                waqtEndsAt = prayerTimes.isha;
                break;
            case ISHA: 
                targetRow = rowIsha; 
                currentWaqtName = "ISHA";
                
                // If it's early morning (before Fajr), Isha ends at today's Fajr.
                // If it's late night (after Isha starts), Isha ends at tomorrow's Fajr.
                if (now.before(prayerTimes.fajr)) {
                    waqtEndsAt = prayerTimes.fajr;
                } else {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.add(java.util.Calendar.DATE, 1);
                    
                    // Use Precise Engine for the next day's Fajr to maintain second-level accuracy
                    CalculationParameters params = CalculationMethod.MUSLIM_WORLD_LEAGUE.getParameters();
                    CelestialWaqtEngine.PrecisePrayerTimes tomorrow = CelestialWaqtEngine.calculate(
                        coordinates.latitude, coordinates.longitude, cal.getTime(), 
                        params.fajrAngle, params.ishaAngle);
                        
                    waqtEndsAt = tomorrow.fajr;
                }
                break;
        }


        if (targetRow != null && getContext() != null) {
            // Theme-aware Dynamic Colors
            int activeCardBg = ContextCompat.getColor(getContext(), R.color.prayer_card_bg_active);
            int crystalBorder = ContextCompat.getColor(getContext(), R.color.prayer_card_border_active);
            int primaryTextColor = ContextCompat.getColor(getContext(), R.color.prayer_card_active_primary_text);
            int secondaryTextColor = ContextCompat.getColor(getContext(), R.color.prayer_card_active_secondary_text);
            
            // Apply Premium Claymorphism to Active Card
            targetRow.setBackground(ScalingUtils.createClayDrawable(getContext(),
                    0.05f,   // cornerRadius %
                    0.012f,  // shadowOffset %
                    0.008f,  // innerInset %
                    0.005f,  // strokeWidth % (Thicker for active)
                    activeCardBg, 
                    ContextCompat.getColor(getContext(), R.color.clay_dark_shadow), 
                    ContextCompat.getColor(getContext(), R.color.clay_light_shadow), 
                    crystalBorder));
            
            // Physical Lift
            targetRow.animate().translationZ(15f).scaleY(1.05f).setDuration(400).start();

            
            // Group A: Prayer Name & Jamat Time
            TextView txtName = targetRow.findViewById(R.id.txt_prayer_name);
            TextView txtJamatValue = targetRow.findViewById(R.id.txt_jamat_time);
            if (txtName != null) txtName.setTextColor(primaryTextColor);
            if (txtJamatValue != null) txtJamatValue.setTextColor(primaryTextColor);

            // Group B: Azan Label, Azan Time, Jamat Label
            TextView lblAzan = targetRow.findViewById(R.id.lbl_azan);
            TextView txtAzanValue = targetRow.findViewById(R.id.txt_azan_time);
            TextView lblJamat = targetRow.findViewById(R.id.lbl_jamat);
            if (lblAzan != null) lblAzan.setTextColor(secondaryTextColor);
            if (txtAzanValue != null) txtAzanValue.setTextColor(secondaryTextColor);
            if (lblJamat != null) lblJamat.setTextColor(secondaryTextColor);

            // Speaker Inset (Claymorphic)
            View speakerInset = targetRow.findViewById(R.id.view_speaker_inset_bg);
            if (speakerInset != null) {
                // Inset (Concave) effect for the speaker area using active colors
                ScalingUtils.applyClaymorphism(speakerInset, 0.5f, true, activeCardBg);
            }


            // Update Speaker Icon Color for Active State
            ImageView imgSound = targetRow.findViewById(R.id.img_azan_sound_toggle);
            if (imgSound != null) {
                boolean isOn = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean(KEY_SPEAKER_PREFIX + currentWaqtName.toLowerCase(), true);
                updateSpeakerIcon(imgSound, isOn, true);
            }

            // Update Celestial Orbit Dial (matches border color)
            updateCelestialOrbit(targetRow, currentWaqtName, crystalBorder, true);

            activeRow = targetRow;
        }











        // Start Countdown Timer
        startWaqtCountdown(waqtEndsAt);
    }

    private void resetRowHighlight(View row, String name, String prefKey) {
        if (getContext() == null) return;
        
        row.animate().translationZ(0f).scaleY(1.0f).setDuration(300).start();
        row.clearAnimation(); // Stop any pulsing
        
        // Theme-aware Inactive Colors (Matching Settings Cards)
        int cardBodyColor = ContextCompat.getColor(getContext(), R.color.off_white_primary);
        int shadowColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_shadow);
        int highlightColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_highlight);
        int strokeColor = ContextCompat.getColor(getContext(), R.color.off_white_grayish);
        int nameTextColor = ContextCompat.getColor(getContext(), R.color.prayer_card_name_text);
        int subTextColor = ContextCompat.getColor(getContext(), R.color.prayer_card_sub_text);

        // Apply same claymorphic style as Settings cards
        row.setBackground(ScalingUtils.createClayDrawable(getContext(),
                0.045f,   // cornerRadius %
                0.010f,   // shadowOffset %
                0.006f,   // innerInset %
                0.003f,   // strokeWidth %
                cardBodyColor, shadowColor, highlightColor, strokeColor));


        TextView txtName = row.findViewById(R.id.txt_prayer_name);
        if (txtName != null) txtName.setTextColor(nameTextColor);

        TextView txtJamatValue = row.findViewById(R.id.txt_jamat_time);
        if (txtJamatValue != null) txtJamatValue.setTextColor(nameTextColor);

        TextView txtAzanValue = row.findViewById(R.id.txt_azan_time);
        if (txtAzanValue != null) txtAzanValue.setTextColor(subTextColor);

        TextView lblAzan = row.findViewById(R.id.lbl_azan);
        TextView lblJamat = row.findViewById(R.id.lbl_jamat);
        if (lblAzan != null) lblAzan.setTextColor(subTextColor);
        if (lblJamat != null) lblJamat.setTextColor(subTextColor);




        View speakerInset = row.findViewById(R.id.view_speaker_inset_bg);
        if (speakerInset != null) {
            int bodyColor = ContextCompat.getColor(getContext(), R.color.off_white_primary);
            ScalingUtils.applyClaymorphism(speakerInset, 0.5f, true, bodyColor);
        }


        // Re-apply speaker state icons with Inactive Color
        ImageView imgSound = row.findViewById(R.id.img_azan_sound_toggle);
        if (imgSound != null) {
            boolean isOn = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_SPEAKER_PREFIX + prefKey, true);
            updateSpeakerIcon(imgSound, isOn, false);
        }

        // Reset Celestial Orbit Dial to match Active Card's border color
        int activeBorderColor = ContextCompat.getColor(getContext(), R.color.prayer_card_border_active);
        updateCelestialOrbit(row, prefKey, activeBorderColor, false);

    }







    private void updateCelestialOrbit(View row, String waqtKey, int color, boolean isActive) {
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
            orbit.setOrbitState(deg, isNight, color, isActive);
        }
    }


    private void updateRowWaqtTimes(View row, String azan, String jamat) {

        TextView txtAzan = row.findViewById(R.id.txt_azan_time);
        TextView txtJamat = row.findViewById(R.id.txt_jamat_time);
        if (txtAzan != null) txtAzan.setText(azan);
        if (txtJamat != null) txtJamat.setText(jamat);
        
        // Also update hidden views for backend consistency if they exist
        TextView txtStart = row.findViewById(R.id.txt_start_time);
        TextView txtEnd = row.findViewById(R.id.txt_end_time);
        if (txtStart != null) txtStart.setText(azan);
        if (txtEnd != null) txtEnd.setText(jamat);
    }





    private void startWaqtCountdown(Date endsAt) {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }

        if (endsAt == null) {
            if (chronosDial != null) chronosDial.setCenterCountdown("00:00:00");
            return;
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long nowMs = new Date().getTime();
                long diff = endsAt.getTime() - nowMs;
                
                // Auto-Azan Logic: Check if it's exactly azan time
                checkAndTriggerAzan(nowMs);
                
                // Active Card Pulse Logic (From Azan to Jamat)
                updateActivePulse(nowMs);

                if (diff <= 0) {


                    // if (txtNextCountdown != null) txtNextCountdown.setText("00:00:00");
                    // Optionally, trigger a recalculation here when the waqt ends
                    if (fusedLocationClient != null) {
                        try {
                            detectCurrentLocation();
                        } catch (Exception ignored) {}
                    }
                    return;
                }

                long diffSeconds = diff / 1000 % 60;
                long diffMinutes = diff / (60 * 1000) % 60;
                long diffHours = diff / (60 * 60 * 1000);

                String countdownText = String.format(Locale.getDefault(), "%02d:%02d:%02d", diffHours, diffMinutes, diffSeconds);
                if (chronosDial != null) {
                    chronosDial.setCenterCountdown(countdownText);
                }

                // Check for Date Rollover every minute
                if (diffSeconds == 0) {
                    updateHeaderDates();
                }

                countdownHandler.postDelayed(this, 1000);
            }
        };

        countdownHandler.post(countdownRunnable);
    }

    private void updateAddressFromLocation(Location location) {

        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                txtDeviceAddress.setText(fullAddress);
            }
        } catch (Exception e) {
            txtDeviceAddress.setText("Location found (address error)");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }


    private void checkAndTriggerAzan(long nowMs) {
        if (latestPrayerTimes == null || getContext() == null) return;
        
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long tolerance = 2000; 

        // Use the relative date from location-based prayer times as reference for static times
        checkSingleAzan(nowMs, parseTimeToMs(getStaticAzanTime("fajr"), latestPrayerTimes.fajr), "fajr", prefs, tolerance);
        checkSingleAzan(nowMs, parseTimeToMs(getStaticAzanTime("zuhr"), latestPrayerTimes.dhuhr), "zuhr", prefs, tolerance);
        checkSingleAzan(nowMs, parseTimeToMs(getStaticAzanTime("asr"), latestPrayerTimes.asr), "asr", prefs, tolerance);
        checkSingleAzan(nowMs, parseTimeToMs(getStaticAzanTime("maghrib"), latestPrayerTimes.maghrib), "maghrib", prefs, tolerance);
        checkSingleAzan(nowMs, parseTimeToMs(getStaticAzanTime("isha"), latestPrayerTimes.isha), "isha", prefs, tolerance);
    }


    private void updateActivePulse(long nowMs) {
        if (latestPrayerTimes == null || getContext() == null) return;
        
        com.batoulapps.adhan.Prayer current = latestPrayerTimes.currentPrayer();
        View target = null;
        String waqtKey = "";
        Date refDate = null;

        if (current == com.batoulapps.adhan.Prayer.FAJR) { target = rowFajr; waqtKey = "fajr"; refDate = latestPrayerTimes.fajr; }
        else if (current == com.batoulapps.adhan.Prayer.DHUHR) { target = rowZuhr; waqtKey = "zuhr"; refDate = latestPrayerTimes.dhuhr; }
        else if (current == com.batoulapps.adhan.Prayer.ASR) { target = rowAsr; waqtKey = "asr"; refDate = latestPrayerTimes.asr; }
        else if (current == com.batoulapps.adhan.Prayer.MAGHRIB) { target = rowMaghrib; waqtKey = "maghrib"; refDate = latestPrayerTimes.maghrib; }
        else if (current == com.batoulapps.adhan.Prayer.ISHA) { target = rowIsha; waqtKey = "isha"; refDate = latestPrayerTimes.isha; }

        if (target != null && refDate != null) {
            long azanMs = parseTimeToMs(getStaticAzanTime(waqtKey), refDate);
            long jamatMs = parseTimeToMs(getStaticJamatTime(waqtKey), refDate);
            
            CelestialOrbitView orbit = target.findViewById(R.id.view_celestial_orbit);
            
            // Breathing animation ONLY between static Azan and static Jamat
            if (nowMs >= azanMs && nowMs < jamatMs) {
                startHeartbeat(target);
                // Soft pulse for the orbit node glow too
                if (orbit != null) {
                    float pulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 400.0));
                    orbit.setPulse(pulse);
                }
            } else {
                target.clearAnimation();
                if (orbit != null) orbit.setPulse(0.0f); // Solid node
            }
        }
    }



    private long parseTimeToMs(String timeStr, Date refDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date d = sdf.parse(timeStr);
            java.util.Calendar ref = java.util.Calendar.getInstance();
            ref.setTime(refDate);
            java.util.Calendar val = java.util.Calendar.getInstance();
            val.setTime(d);
            
            ref.set(java.util.Calendar.HOUR_OF_DAY, val.get(java.util.Calendar.HOUR_OF_DAY));
            ref.set(java.util.Calendar.MINUTE, val.get(java.util.Calendar.MINUTE));
            return ref.getTimeInMillis();
        } catch (Exception e) {
            return refDate.getTime() + (15 * 60 * 1000); // Fallback 15 mins
        }
    }

    private void startHeartbeat(View v) {
        if (v.getAnimation() != null) return; // Already animating
        // Scale slightly "inward" to "outward" to prevent hitting screen edges (0.98 -> 1.02)
        android.view.animation.Animation pulse = new android.view.animation.ScaleAnimation(
            0.98f, 1.02f, 0.98f, 1.02f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        );

        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        pulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        v.startAnimation(pulse);
    }

    private void checkSingleAzan(long nowMs, long azanMs, String key, android.content.SharedPreferences prefs, long tolerance) {
        // If we are exactly at the minute (or within a few seconds) and speaker is enabled
        if (Math.abs(nowMs - azanMs) < tolerance) {
            boolean isEnabled = prefs.getBoolean(KEY_SPEAKER_PREFIX + key, true);
            if (isEnabled && !isPlaying) {
                playAzanSound();
            }
        }
    }

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;

        
        // Content container should have no padding to allow edge-to-edge header
        view.findViewById(R.id.dashboard_content_container).setPadding(0, 0, 0, 0);
        int sectionPadding = ScalingUtils.getScaledSize(getContext(), 0.06f);


        // 1. Header Scaling
        txtMasjidName.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.065f));
        txtMasjidAddress.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.032f));
        txtCurrentDate.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.045f));
        txtHijriDate.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.040f));
        txtDeviceAddress.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.032f));
        
        int headerHeight = ScalingUtils.getScaledSize(getContext(), 0.18f, true);
        ScalingUtils.applyScaledLayout(headerContainer, 0.98f, 0, 0, 0, 0, 0);
        headerContainer.getLayoutParams().height = headerHeight;
        
        int headerPaddingTop = ScalingUtils.getScaledSize(getContext(), 0.05f);
        int headerPaddingBottom = ScalingUtils.getScaledSize(getContext(), 0.015f); 
        headerSection.setPadding(sectionPadding, headerPaddingTop, sectionPadding, headerPaddingBottom);
        
        // Zero out legacy padding in parent to ensure predictable scaling
        if (layoutTopAccent != null) {
            layoutTopAccent.setPadding(0, 0, 0, 0);
        }
        
        // Masjid Info Scaling
        ScalingUtils.applyScaledLayout(txtMasjidAddress, 0.68f, -1, 0.005f, 0, 0, 0);
        ScalingUtils.applyScaledLayout(imgHeaderLocation, 0.10f, 0.10f, 0.005f, 0, 0.03f, 0);












        
        // 1.5 Date Container Scaling (Celestial Axis)
        txtHijriDate.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.035f));
        txtCurrentDate.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.035f));
        ScalingUtils.applyScaledLayout(dateContainer, -1, -1, 0.015f, 0.02f, 0, 0); // Balanced internal spacing
        ScalingUtils.applyScaledLayout(dividerDates, 0.003f, 0.05f, 0, 0, 0, 0);
        
        float chipRadius = ScalingUtils.getScaledSize(getContext(), 0.05f);
        if (txtHijriDate.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable) txtHijriDate.getBackground().mutate()).setCornerRadius(chipRadius);
        }
        if (txtCurrentDate.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable) txtCurrentDate.getBackground().mutate()).setCornerRadius(chipRadius);
        }
        
        ScalingUtils.applyScaledLayout(dotAxisTop, 0.015f, 0.015f, 0, 0, 0, 0);
        ScalingUtils.applyScaledLayout(dotAxisBottom, 0.015f, 0.015f, 0, 0, 0, 0);

        // V3 Tab Scaling (Stand-alone now, matching Settings Theme Toggle height)
        ScalingUtils.applyScaledLayout(tabTrack, 0.94f, -1, 0.015f, 0.005f, 0, 0); // Standardized Top Margin
        tabTrack.getLayoutParams().height = ScalingUtils.getScaledSize(getContext(), 0.13f);
        tabMasjid.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.038f));
        tabWaqt.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.038f));

        // Waqt Details Content Scaling

        View addressContainer = view.findViewById(R.id.container_waqt_address);
        if (addressContainer != null) {
            // Match header at 98% width and center it
            ScalingUtils.applyScaledLayout(addressContainer, 0.98f, -1, 0.02f, 0, 0.01f, 0.01f);
        }

        if (panelWaqtDetails != null) {
            // Scaling for the address strip
            ScalingUtils.applyScaledLayout(view.findViewById(R.id.img_location_pin), 0.05f, 0.05f, 0, 0, 0, 0.015f);
            if (btnEditLocation != null) {
                btnEditLocation.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.028f));
                ScalingUtils.applyScaledLayout(btnEditLocation, 0.12f, 0.07f, 0, 0, 0, 0);
            }
        }

        // 2. Next Prayer Card Scaling - Removed per user request

        // 3. Timings Card Scaling (Increased to Match Header/Backdrop width)
        ScalingUtils.applyScaledLayout(cardPrayerTimings, 1.0f, -1, -0.01f, 0.04f, 0, 0); // Slight negative margin to pull closer to switcher

        int cardPaddingVertical = ScalingUtils.getScaledSize(getContext(), 0.02f); // Reduced top/bottom padding inside card
        int cardPaddingHorizontal = ScalingUtils.getScaledSize(getContext(), 0.03f); 
        cardPrayerTimings.setPadding(cardPaddingHorizontal, cardPaddingVertical, cardPaddingHorizontal, cardPaddingVertical);






        // Scale individual rows inside the card
        float rowTextSize = ScalingUtils.getScaledTextSize(getContext(), 0.040f);
        float rowMarginTop = 0.025f;
        scaleRow(rowFajr, rowTextSize, 0);
        scaleRow(rowZuhr, rowTextSize, rowMarginTop);
        scaleRow(rowAsr, rowTextSize, rowMarginTop);
        scaleRow(rowMaghrib, rowTextSize, rowMarginTop);
        scaleRow(rowIsha, rowTextSize, rowMarginTop);


        // Special Times Scaling - Removed per user request

        // 4. Announcements Panel Scaling
        txtAnnouncementsTitle.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.042f));
        txtAnnouncementContent.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.038f));
        // Gap Above: 0.03f (matching bottom gap)
        ScalingUtils.applyScaledLayout(panelAnnouncements, 0.94f, -1, 0.03f, 0.015f, 0, 0); 
        int panelPadding = ScalingUtils.getScaledSize(getContext(), 0.045f);
        panelAnnouncements.setPadding(panelPadding, panelPadding, panelPadding, panelPadding);
        ScalingUtils.applyScaledLayout(txtAnnouncementContent, -1, -1, 0.015f, 0, 0, 0);
    }


    private void scaleRow(View row, float textSize, float marginTop) {
        TextView txtName = row.findViewById(R.id.txt_prayer_name);
        TextView txtAzan = row.findViewById(R.id.txt_azan_time);
        TextView txtJamat = row.findViewById(R.id.txt_jamat_time);
        View speakerContainer = row.findViewById(R.id.container_speaker_toggle);

        if (txtName != null) txtName.setTextSize(textSize * 1.1f); // Prominent name
        if (txtAzan != null) txtAzan.setTextSize(textSize * 0.8f);
        if (txtJamat != null) txtJamat.setTextSize(textSize);
        
        if (speakerContainer != null) ScalingUtils.applyScaledLayout(speakerContainer, 0.10f, 0.10f, 0, 0, 0, 0);
        
        ScalingUtils.applyScaledLayout(row, -1, -1, marginTop, 0, 0, 0);
    }



    // scaleSpecialTile removed



    @Override
    protected void applyClaymorphism(View view) {
        if (getContext() == null) return;
        int bodyColor = ContextCompat.getColor(getContext(), R.color.emerald_glow_highlight);
        
        // Concept 2: Individual Pillars (no single container background needed, each row is a card)
        // cardPrayerTimings is now just a container LinearLayout.
        
        // Let's re-trigger reset highlights to apply initial card state
        resetRowHighlight(rowFajr, "FAJR", "fajr");
        resetRowHighlight(rowZuhr, "ZUHR", "zuhr");
        resetRowHighlight(rowAsr, "ASR", "asr");
        resetRowHighlight(rowMaghrib, "MAGHRIB", "maghrib");
        resetRowHighlight(rowIsha, "ISHA", "isha");


        // cardSpecialTimes removal completed



        // Header Background
        applyAtmosphericBackground(headerHorizonBackdrop, new TextView[]{txtMasjidName, txtMasjidAddress}, imgHeaderLocation);


        // Special components removed from Waqt Details

        // Inset (Carved) panels with 'Luminous' branding
        int announcementBody = ContextCompat.getColor(getContext(), R.color.dashboard_top_accent_bg);
        int internalGlow = ContextCompat.getColor(getContext(), R.color.emerald_glow_highlight);
        int deepShadow = ContextCompat.getColor(getContext(), R.color.clay_dark_shadow);
        
        panelAnnouncements.setBackground(ScalingUtils.createInsetClayDrawable(getContext(), 0.02f, 0.007f, 0.004f,
                announcementBody, deepShadow, internalGlow));

        // Concept D: Convex Pill Button
        if (btnEditLocation != null) {
            btnEditLocation.setBackground(ScalingUtils.createClayDrawable(getContext(), 0.5f, 0.006f, 0.004f, 0.002f));
        }

        // V3 Tab Switcher Claymorphism (Matching Settings Theme Toggle exactly)
        if (tabTrack != null) {
            int shadowColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_shadow);
            int highlightColor = ContextCompat.getColor(getContext(), R.color.off_white_surface_highlight);

            // Track: Inset trough
            tabTrack.setBackground(ScalingUtils.createInsetClayDrawable(getContext(), 0.035f, 0.006f, 0.004f,
                    ContextCompat.getColor(getContext(), R.color.off_derived),
                    shadowColor, highlightColor));
            
            // Note: Individual tab backgrounds and colors are handled in switchPanel(true) 
            // called by setupTabSwitcher() to ensure consistency with the Settings toggle logic.
        }

        // Start entrance animations sequence
        setupAnimations();
    }

    private void applyAtmosphericBackground(View background, TextView[] texts, ImageView... icons) {
        if (background == null || getContext() == null) return;
        android.graphics.drawable.GradientDrawable glassGrad = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BR_TL,
            new int[]{
                ContextCompat.getColor(getContext(), R.color.emerald_primary),
                ContextCompat.getColor(getContext(), R.color.emerald_dark)
            }
        );
        glassGrad.setCornerRadius(ScalingUtils.getScaledSize(getContext(), 0.08f)); // More pronounced curve
        background.setBackground(glassGrad);

        
        int textColor = ContextCompat.getColor(getContext(), R.color.off_white_primary);
        for (TextView tv : texts) {
            if (tv != null) tv.setTextColor(textColor);
        }
        for (ImageView iv : icons) {
            if (iv != null) iv.setColorFilter(textColor);
        }
    }



    private void setupAnimations() {
        // Prepare initial states
        float shift = 50f;
        float dateSlideDistance = 40f; // Reduced so they start exactly at the divider

        headerSection.setAlpha(0f);
        headerSection.setTranslationY(-shift);

        
        // Dates start "tucked in" AT the divider (Scale from center)
        txtHijriDate.setAlpha(0f);
        txtHijriDate.setPivotX(ScalingUtils.getScaledSize(getContext(), 1.0f, false)); 
        txtHijriDate.setScaleX(0f);
        txtHijriDate.setTranslationY(40f); // Ready for Clockwork Roll
        
        txtCurrentDate.setAlpha(0f);
        txtCurrentDate.setPivotX(0f); 
        txtCurrentDate.setScaleX(0f);
        txtCurrentDate.setTranslationY(40f); // Ready for Clockwork Roll

        
        dividerDates.setAlpha(0f);
        dividerDates.setScaleY(0f); 

        if (dotAxisTop != null) {
            dotAxisTop.setAlpha(0f);
            dotAxisBottom.setAlpha(0f);
        }



        cardPrayerTimings.setAlpha(0f);
        cardPrayerTimings.setTranslationY(shift);

        // Removed cardSpecialTimes initialization
        panelAnnouncements.setAlpha(0f);

        // Concept D: The Unroll Initial State
        if (layoutAddressPapyrus != null) {
            layoutAddressPapyrus.setPivotX(0f);
            layoutAddressPapyrus.setScaleX(0f);
            txtDeviceAddress.setAlpha(0f);
            txtDeviceAddress.setTranslationY(20f);
            btnEditLocation.setAlpha(0f);
        }


        // Execute Sequence
        headerSection.animate().alpha(1f).translationY(0)
                .setDuration(800)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();
        
        if (headerHorizonBackdrop != null) {
            headerHorizonBackdrop.setAlpha(0f);
            headerHorizonBackdrop.animate().alpha(1f).setDuration(1000).start();
        }



        // 1.5 Date Animation (Celestial Axis Reveal)
        dividerDates.animate().alpha(1f).scaleY(1f)
                .setDuration(600)
                .setStartDelay(400)
                .start();

        if (dotAxisTop != null) {
            dotAxisTop.animate().alpha(1f).setDuration(600).setStartDelay(800).start();
            dotAxisBottom.animate().alpha(1f).setDuration(600).setStartDelay(800).start();
        }

        txtHijriDate.animate().alpha(1f).scaleX(1f).translationY(0)
                .setDuration(1000)
                .setStartDelay(700)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .start();

        txtCurrentDate.animate().alpha(1f).scaleX(1f).translationY(0)
                .setDuration(1000)
                .setStartDelay(700)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .start();




        
        // cardNextPrayer animation removed

        cardPrayerTimings.animate().alpha(1f).translationY(0)
                .setDuration(900)
                .setStartDelay(400)
                .start();

        // Staggered Row Entrances
        animateRow(rowFajr, 500);
        animateRow(rowZuhr, 600);

        animateRow(rowAsr, 700);
        animateRow(rowMaghrib, 800);
        animateRow(rowIsha, 900);




        // Removed cardSpecialTimes animation

        panelAnnouncements.animate().alpha(1f)
                .setDuration(800)
                .setStartDelay(800)
                .start();
    }

    private void animateAddressReveal() {
        // Concept D: The Unroll Execution (Triggered on Tab Switch)
        if (layoutAddressPapyrus != null) {
            // Reset to initial state before animating
            layoutAddressPapyrus.setScaleX(0f);
            txtDeviceAddress.setAlpha(0f);
            txtDeviceAddress.setTranslationY(20f);
            btnEditLocation.setAlpha(0f);

            // Execute Animation with slight delay to sync with tab transition
            layoutAddressPapyrus.animate().scaleX(1f).setDuration(800).setStartDelay(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            
            txtDeviceAddress.animate().alpha(1f).translationY(0).setDuration(600).setStartDelay(600).start();
            btnEditLocation.animate().alpha(1f).setDuration(400).setStartDelay(800).start();
        }
    }


    private void animateRow(View row, int delay) {
        row.setAlpha(0f);
        row.setTranslationX(-30f);
        row.animate().alpha(1f).translationX(0)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }
}

