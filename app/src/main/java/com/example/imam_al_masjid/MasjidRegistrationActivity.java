package com.example.imam_al_masjid;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MasjidRegistrationActivity extends AppCompatActivity {

    private TextView txtTitle;
    private LinearLayout cardMasjid, cardImam, cardLocation;
    private EditText edtMasjidName, edtMasjidPhone, edtImamName, edtImamPhone, edtEmail;
    private EditText edtDistrict, edtPinCode, edtAddress;
    private TextView dropdownState;
    private MaterialButton btnSubmit;
    private TextView btnSignIn;
    private TextView txtVerificationNotice;
    private TextView txtPrefixMasjid, txtPrefixImam;

    private String selectedState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this, true);
        setContentView(R.layout.activity_masjid_registration);

        initViews();
        applyDynamicScaling();
        applyClaymorphism();
        setupValidation();
        setupFocusListeners();
        setupClickListeners();
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        findViewById(R.id.registration_root).post(() -> {
            applyDynamicScaling();
            applyClaymorphism();
            
            View focused = getCurrentFocus();
            if (focused != null) {
                View targetView = focused;
                if (focused == edtMasjidPhone) targetView = findViewById(R.id.layout_masjid_phone_container);
                else if (focused == edtImamPhone) targetView = findViewById(R.id.layout_imam_phone_container);
                
                targetView.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.012f, 0.018f));
                targetView.setScaleX(1.02f);
                targetView.setScaleY(1.02f);
            }
        });
    }


    private void initViews() {
        txtTitle = findViewById(R.id.txt_registration_title);
        cardMasjid = findViewById(R.id.card_masjid_info);
        cardImam = findViewById(R.id.card_imam_info);
        cardLocation = findViewById(R.id.card_location_info);
        
        edtMasjidName = findViewById(R.id.edt_masjid_name);
        edtMasjidPhone = findViewById(R.id.edt_masjid_phone);
        edtImamName = findViewById(R.id.edt_imam_name);
        edtImamPhone = findViewById(R.id.edt_imam_phone);
        edtEmail = findViewById(R.id.edt_email);
        
        dropdownState = findViewById(R.id.dropdown_state);
        edtDistrict = findViewById(R.id.edt_district);
        edtPinCode = findViewById(R.id.edt_pincode);
        edtAddress = findViewById(R.id.edt_address);
        
        btnSubmit = findViewById(R.id.btn_send_verification);
        btnSignIn = findViewById(R.id.btn_goto_signin);
        txtVerificationNotice = findViewById(R.id.txt_verification_notice);
        
        txtPrefixMasjid = findViewById(R.id.txt_prefix_masjid_phone);
        txtPrefixImam = findViewById(R.id.txt_prefix_imam_phone);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> {
            // Reset to blank state as requested
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        dropdownState.setOnClickListener(v -> showStateSearchDialog());

        btnSubmit.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.9f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(100).start();
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    break;
            }
            return true;
        });

        btnSubmit.setOnClickListener(v -> {
            if (performFullValidationPass()) {
                // Directly navigate to Home Screen for now
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finishAffinity(); // Clear task stack
            }
        });
    }

    private boolean performFullValidationPass() {
        // Shared drawables for highlighting
        Drawable correctBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f);
        Drawable errorBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f,
                ContextCompat.getColor(this, R.color.off_white_primary),
                ContextCompat.getColor(this, R.color.error_red),
                ContextCompat.getColor(this, R.color.clay_light_shadow));

        boolean masjidNameValid = Pattern.matches("^[a-zA-Z()\\s-]{1,50}$", edtMasjidName.getText().toString().trim());
        boolean imamNameValid = Pattern.matches("^[a-zA-Z()\\s-]{1,50}$", edtImamName.getText().toString().trim());
        
        String p1 = edtMasjidPhone.getText().toString().trim();
        boolean masjidPhoneValid = (p1.length() == 10);
        
        String p2 = edtImamPhone.getText().toString().trim();
        boolean imamPhoneValid = (p2.length() == 10);
        
        String email = edtEmail.getText().toString().trim();
        boolean emailValid = email.isEmpty() || (email.contains("@") && email.endsWith(".com"));
        
        boolean stateValid = selectedState != null && !selectedState.isEmpty();
        boolean pinValid = edtPinCode.getText().toString().length() == 6;
        boolean districtValid = Pattern.matches("^[a-zA-Z\\s]{1,50}$", edtDistrict.getText().toString().trim());
        boolean addressValid = !edtAddress.getText().toString().trim().isEmpty();

        // Highlight fields
        edtMasjidName.setBackground(masjidNameValid ? correctBg : errorBg);
        edtImamName.setBackground(imamNameValid ? correctBg : errorBg);
        findViewById(R.id.layout_masjid_phone_container).setBackground(masjidPhoneValid ? correctBg : errorBg);
        findViewById(R.id.layout_imam_phone_container).setBackground(imamPhoneValid ? correctBg : errorBg);
        edtEmail.setBackground(emailValid ? correctBg : errorBg);
        edtDistrict.setBackground(districtValid ? correctBg : errorBg);
        edtPinCode.setBackground(pinValid ? correctBg : errorBg);
        edtAddress.setBackground(addressValid ? correctBg : errorBg);
        
        if (!stateValid) {
            dropdownState.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else {
            dropdownState.setTextColor(ContextCompat.getColor(this, R.color.emerald_primary));
        }

        return masjidNameValid && imamNameValid && (masjidPhoneValid || imamPhoneValid) && emailValid && stateValid && pinValid && districtValid && addressValid;
    }

    private void showStateSearchDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_standard_state_search_dialog, new android.widget.FrameLayout(this), false);
        EditText edtSearch = dialogView.findViewById(R.id.edt_search_state);
        RecyclerView rvStates = dialogView.findViewById(R.id.rv_states_list);
        
        String[] statesArray = getResources().getStringArray(R.array.indian_states);
        List<String> statesList = Arrays.asList(statesArray);
        
        // Dynamic scaling for dialog elements
        TextView txtTitle = dialogView.findViewById(R.id.txt_dialog_title);
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(this, 0.050f));
        ScalingUtils.applyScaledLayout(txtTitle, -1, -1, 0.04f, 0.02f, 0, 0);

        View searchBar = dialogView.findViewById(R.id.layout_search_bar);
        int targetH = ScalingUtils.getScaledSize(this, 0.14f, true);
        ScalingUtils.applyScaledLayout(searchBar, 0.84f, 0.14f, 0.02f, 0.03f, 0, 0);
        searchBar.setMinimumHeight(targetH);
        
        View searchIcon = dialogView.findViewById(R.id.img_search_icon);
        ScalingUtils.applyScaledLayout(searchIcon, 0.075f, 0.075f, 0, 0, 0.045f, 0.02f);
        
        // Match edit text height with container to ensure full-area touch and proper vertical centering
        edtSearch.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        edtSearch.setTextSize(ScalingUtils.getScaledTextSize(this, 0.045f));
        edtSearch.setPadding(0, 0, ScalingUtils.getScaledSize(this, 0.045f), 0);
        
        // Significant dialog height for exhaustive search
        ScalingUtils.applyScaledLayout(rvStates, -1, 0.65f, 0.02f, 0, 0, 0);

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(dialogView);
        
        // Claymorphism for dialog
        dialogView.setBackground(ScalingUtils.createClayDrawable(this, 0.05f, 0.012f, 0.02f, 0));
        searchBar.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f));
        
        StateSelectionAdapter adapter = new StateSelectionAdapter(statesList, state -> {
            selectedState = state;
            dropdownState.setText(state);
            dropdownState.setTextColor(ContextCompat.getColor(this, R.color.emerald_primary));
            validateForm();
            dialog.dismiss();
        });
        
        rvStates.setLayoutManager(new LinearLayoutManager(this));
        rvStates.setAdapter(adapter);
        
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            if (dialog.getWindow().getAttributes() != null) {
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = ScalingUtils.getScaledSize(this, 0.90f);
                dialog.show();
                dialog.getWindow().setAttributes(lp);
            } else {
                dialog.show();
            }
        }
    }

    private void setupValidation() {
        // Shared error/correct drawables
        Drawable correctBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f);
        Drawable errorBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f,
                ContextCompat.getColor(this, R.color.off_white_primary),
                ContextCompat.getColor(this, R.color.error_red),
                ContextCompat.getColor(this, R.color.clay_light_shadow));


        // Input Fields (turning red rule preserved)
        // 1. Masjid Name: Max 50, special regex [A-Za-z\-\(\) ], block leading/trailing
        setupFieldValidation(edtMasjidName, s -> {
            String val = s.toString();
            return !val.isEmpty() && Pattern.matches("^[a-zA-Z()\\s-]{1,50}$", val);
        }, errorBg, correctBg);

        // 2. Imam Name: Max 25, Alpha + Single Space, auto-correct, block leading/trailing
        setupFieldValidation(edtImamName, s -> {
            String val = s.toString();
            return !val.isEmpty() && Pattern.matches("^[a-zA-Z\\s]{1,25}$", val);
        }, errorBg, correctBg);

        // 3. Phones: 10 digits numeric. One of them mandatory.
        TextWatcher phoneWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhones(correctBg, errorBg);
                validateForm();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtMasjidPhone.addTextChangedListener(phoneWatcher);
        edtImamPhone.addTextChangedListener(phoneWatcher);

        // 4. Email: Strictly .com
        setupFieldValidation(edtEmail, s -> {
            String val = s.toString().trim();
            if (val.isEmpty()) return true; // Optional
            return Pattern.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]{1,}(\\.[A-Za-z0-9-]+)*\\.com$", val);
        }, errorBg, correctBg);

        // 5. Pin Code: 6 digits mandatory
        setupFieldValidation(edtPinCode, s -> s.length() == 6, errorBg, correctBg);

        // 6. District: Max 20, Alpha + Single Space
        setupFieldValidation(edtDistrict, s -> {
            String val = s.toString();
            return !val.isEmpty() && Pattern.matches("^[a-zA-Z\\s]{1,20}$", val);
        }, errorBg, correctBg);

        // 7. Address: Mandatory, specific allowed, multi-space auto-correct
        setupFieldValidation(edtAddress, s -> {
            String val = s.toString();
            return !val.isEmpty() && Pattern.matches("^[a-zA-Z0-9()\\\\,./#\\s-]+$", val);
        }, errorBg, correctBg);
    }

    private void validatePhones(Drawable correct, Drawable error) {
        String p1 = edtMasjidPhone.getText().toString().trim();
        String p2 = edtImamPhone.getText().toString().trim();
        
        boolean p1Valid = p1.length() == 10;
        boolean p2Valid = p2.length() == 10;
        
        // Target containers for border highlighting
        findViewById(R.id.layout_masjid_phone_container).setBackground(p1Valid || p1.isEmpty() ? correct : error);
        findViewById(R.id.layout_imam_phone_container).setBackground(p2Valid || p2.isEmpty() ? correct : error);
    }

    private void setupFieldValidation(EditText edt, ValidationRule rule, Drawable errorBg, Drawable correctBg) {
        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString();
                
                // Requirement: Block leading spaces
                if (input.startsWith(" ")) {
                    edt.setText(input.trim());
                    edt.setSelection(edt.getText().length());
                    return;
                }

                // Requirement: Auto-correct multi-spaces
                if (input.contains("  ")) {
                    edt.setText(input.replace("  ", " "));
                    edt.setSelection(edt.getText().length());
                    return;
                }

                // Requirement: Real-time turning red
                boolean isValid = rule.isValid(s);
                edt.setBackground(isValid ? correctBg : errorBg);
                validateForm();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void validateForm() {
        // Button remains enabled per requirements. Real-time highlighting handles partial feedback.
    }

    private void applyDynamicScaling() {
        Context ctx = this;
        // 1. Title: Matched with Login Screen header presence
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.050f));
        ScalingUtils.applyScaledLayout(txtTitle, -1, -1, 0.06f, 0.04f, 0, 0);

        // 2. Cards: STRICT 98% width parity with Login Screen
        ScalingUtils.applyScaledLayout(cardMasjid, 0.98f, -1, 0.02f, 0.03f, 0, 0);
        ScalingUtils.applyScaledLayout(cardImam, 0.98f, -1, 0, 0.03f, 0, 0);
        ScalingUtils.applyScaledLayout(cardLocation, 0.98f, -1, 0, 0.04f, 0, 0);

        // Required: Reset weights to prevent ballooning on resume
        ((LinearLayout.LayoutParams)cardMasjid.getLayoutParams()).weight = 0;
        ((LinearLayout.LayoutParams)cardImam.getLayoutParams()).weight = 0;
        ((LinearLayout.LayoutParams)cardLocation.getLayoutParams()).weight = 0;

        // Required: Match card internal padding with Login Screen (6%)
        int formPadding = ScalingUtils.getScaledSize(this, 0.06f);
        cardMasjid.setPadding(formPadding, formPadding, formPadding, formPadding);
        cardImam.setPadding(formPadding, formPadding, formPadding, formPadding);
        cardLocation.setPadding(formPadding, formPadding, formPadding, formPadding);

        // 3. Section Labels
        float labelSize = ScalingUtils.getScaledTextSize(ctx, 0.040f);
        ((TextView)findViewById(R.id.txt_label_masjid_info)).setTextSize(labelSize);
        ((TextView)findViewById(R.id.txt_label_imam_info)).setTextSize(labelSize);
        ((TextView)findViewById(R.id.txt_label_location_info)).setTextSize(labelSize);

        // 4. Input Fields: STRICT 84% width parity with Login Screen
        float inputHeight = 0.075f; 
        float inputTextSize = 0.040f; 
        float itemMargin = 0.045f;

        applyInputScaling(edtMasjidName, inputHeight, 0.02f, inputTextSize);
        applyInputScaling(findViewById(R.id.layout_masjid_phone_container), inputHeight, itemMargin, 0);
        edtMasjidPhone.setTextSize(ScalingUtils.getScaledTextSize(ctx, inputTextSize));
        txtPrefixMasjid.setTextSize(ScalingUtils.getScaledTextSize(ctx, inputTextSize));

        applyInputScaling(edtImamName, inputHeight, itemMargin, inputTextSize);
        applyInputScaling(findViewById(R.id.layout_imam_phone_container), inputHeight, itemMargin, 0);
        edtImamPhone.setTextSize(ScalingUtils.getScaledTextSize(ctx, inputTextSize));
        txtPrefixImam.setTextSize(ScalingUtils.getScaledTextSize(ctx, inputTextSize));
        applyInputScaling(edtEmail, inputHeight, itemMargin, inputTextSize);

        applyInputScaling(dropdownState, inputHeight, itemMargin, inputTextSize);
        applyInputScaling(edtDistrict, inputHeight, itemMargin, inputTextSize);
        applyInputScaling(edtPinCode, inputHeight, itemMargin, inputTextSize);
        applyInputScaling(edtAddress, 0.12f, itemMargin, inputTextSize);

        // 5. Footer & Buttons
        txtVerificationNotice.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.032f));
        ScalingUtils.applyScaledLayout(txtVerificationNotice, -1, -1, 0.02f, 0.02f, 0.08f, 0.08f);
        
        ScalingUtils.applyScaledLayout(findViewById(R.id.btn_register_container), 0.75f, -1, 0.02f, 0.05f, 0, 0);
        btnSubmit.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.035f));
        btnSubmit.setCornerRadius(ScalingUtils.getScaledSize(ctx, 0.035f));
        btnSubmit.setEnabled(true);
        btnSubmit.setAlpha(1.0f);

        ScalingUtils.applyScaledLayout(findViewById(R.id.layout_signin_link), -1, -1, 0, 0.05f, 0, 0);
        ((TextView)findViewById(R.id.txt_already_registered)).setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.035f));
        btnSignIn.setTextSize(ScalingUtils.getScaledTextSize(ctx, 0.035f));
        
        // Dynamic horizontal padding for the link (1% of screen width)
        int linkPadding = ScalingUtils.getScaledSize(ctx, 0.01f);
        btnSignIn.setPadding(linkPadding, 0, linkPadding, 0);

        // Dynamic prefix margins (2% of screen width)
        ScalingUtils.applyScaledLayout(txtPrefixMasjid, -1, -1, 0, 0, 0, 0.02f);
        ScalingUtils.applyScaledLayout(txtPrefixImam, -1, -1, 0, 0, 0, 0.02f);

        // Root bottom padding (5% of screen height)
        findViewById(R.id.registration_root).setPadding(0, 0, 0, ScalingUtils.getScaledSize(ctx, 0.05f, true));
    }

    private void applyInputScaling(View view, float heightPercent, float marginTopPercent, float textSizePercent) {
        int paddingSide = ScalingUtils.getScaledSize(this, 0.045f);
        // Requirement: Add top padding for multi-line address to prevent border clipping
        int paddingTop = (view.getId() == R.id.edt_address) ? ScalingUtils.getScaledSize(this, 0.015f) : 0;
        view.setPadding(paddingSide, paddingTop, paddingSide, 0);
        
        ViewGroup.LayoutParams genericParams = view.getLayoutParams();
        if (genericParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) genericParams;
            params.topMargin = ScalingUtils.getScaledSize(this, marginTopPercent);
            params.width = ScalingUtils.getScaledSize(this, 0.84f); // Matched with Login Screen (Broad layout)
            params.height = ScalingUtils.getScaledSize(this, heightPercent, true);
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            params.weight = 0; // Prevent ballooning/stretching
            view.setLayoutParams(params);
        } else {
            genericParams.width = ScalingUtils.getScaledSize(this, 0.84f);
            genericParams.height = ScalingUtils.getScaledSize(this, heightPercent, true);
            view.setLayoutParams(genericParams);
        }

        if (view instanceof EditText) {
            ((EditText)view).setTextSize(ScalingUtils.getScaledTextSize(this, textSizePercent));
        } else if (view instanceof TextView) {
            ((TextView)view).setTextSize(ScalingUtils.getScaledTextSize(this, textSizePercent));
        }
    }

    private void applyClaymorphism() {
        Drawable raisedBg = ScalingUtils.createClayDrawable(this, 0.06f, 0.012f, 0.02f, 0.005f);
        Drawable insetBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f);
        
        cardMasjid.setBackground(raisedBg);
        cardImam.setBackground(raisedBg);
        cardLocation.setBackground(raisedBg);
        
        // 4. Submit Button (Convex Clay) - Matched with Login Screen
        int btnBodyColor = ContextCompat.getColor(this, R.color.emerald_primary);
        int btnShadow = ContextCompat.getColor(this, R.color.clay_dark_shadow);
        int btnHighlight = ContextCompat.getColor(this, R.color.clay_light_shadow);
        
        btnSubmit.setBackground(ScalingUtils.createClayDrawable(this, 0.035f, 0.015f, 0.01f, 0,
                btnBodyColor, btnShadow, btnHighlight, 0));
        btnSubmit.setTextColor(ContextCompat.getColor(this, R.color.off_white_primary));

        // Insets
        edtMasjidName.setBackground(insetBg);
        findViewById(R.id.layout_masjid_phone_container).setBackground(insetBg);
        edtImamName.setBackground(insetBg);
        findViewById(R.id.layout_imam_phone_container).setBackground(insetBg);
        edtEmail.setBackground(insetBg);
        dropdownState.setBackground(insetBg);
        edtDistrict.setBackground(insetBg);
        edtPinCode.setBackground(insetBg);
        edtAddress.setBackground(insetBg);
        
        // Final Button state
        btnSubmit.setCornerRadius(ScalingUtils.getScaledSize(this, 0.035f));
    }

    private void setupFocusListeners() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            View targetView = v;
            // Target containers for phone fields
            if (v == edtMasjidPhone) targetView = findViewById(R.id.layout_masjid_phone_container);
            else if (v == edtImamPhone) targetView = findViewById(R.id.layout_imam_phone_container);

            if (hasFocus) {
                // Focus State: Deeper Inset + Scaling
                targetView.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.012f, 0.018f));
                targetView.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
            } else {
                // Normal State
                targetView.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f));
                targetView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                
                // Validate silently to update error states if present, without full UI re-init
                validatePhones(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f),
                        ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f,
                                ContextCompat.getColor(this, R.color.off_white_primary),
                                ContextCompat.getColor(this, R.color.error_red),
                                ContextCompat.getColor(this, R.color.clay_light_shadow)));
                validateForm();
            }
        };

        edtMasjidName.setOnFocusChangeListener(focusListener);
        edtMasjidPhone.setOnFocusChangeListener(focusListener);
        edtImamName.setOnFocusChangeListener(focusListener);
        edtImamPhone.setOnFocusChangeListener(focusListener);
        edtEmail.setOnFocusChangeListener(focusListener);
        edtDistrict.setOnFocusChangeListener(focusListener);
        edtPinCode.setOnFocusChangeListener(focusListener);
        edtAddress.setOnFocusChangeListener(focusListener);
    }

    private interface ValidationRule {
        boolean isValid(CharSequence s);
    }

    private interface OnStateSelectedListener {
        void onSelected(String state);
    }

    private class StateSelectionAdapter extends RecyclerView.Adapter<StateSelectionAdapter.ViewHolder> {
        private final List<String> allStates;
        private final List<String> filteredList;
        private final OnStateSelectedListener listener;

        public StateSelectionAdapter(List<String> states, OnStateSelectedListener listener) {
            this.allStates = states;
            this.filteredList = new ArrayList<>(states);
            this.listener = listener;
        }

        public void filter(String query) {
            filteredList.clear();
            if (query.isEmpty()) {
                filteredList.addAll(allStates);
            } else {
                for (String s : allStates) {
                    if (s.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                        filteredList.add(s);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_state_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String state = filteredList.get(position);
            holder.text.setText(state);
            
            // Dynamic Scaling for Dropdown Item
            Context context = holder.itemView.getContext();
            int paddingH = ScalingUtils.getScaledSize(context, 0.045f);
            int paddingV = ScalingUtils.getScaledSize(context, 0.025f);
            holder.text.setPadding(paddingH, paddingV, paddingH, paddingV);
            holder.text.setTextSize(ScalingUtils.getScaledTextSize(context, 0.040f));
            
            // Highlighting concept from Login Screen
            if (state.equals(selectedState)) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.emerald_alpha_20));
            } else {
                holder.itemView.setBackgroundResource(android.R.drawable.btn_default); // Or transparent
                holder.itemView.setBackground(null); // Clear previous if recycled
            }
            
            holder.itemView.setOnClickListener(v -> listener.onSelected(state));
        }

        @Override public int getItemCount() { return filteredList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) { super(v); text = v.findViewById(R.id.txt_state_name); }
        }
    }
}
