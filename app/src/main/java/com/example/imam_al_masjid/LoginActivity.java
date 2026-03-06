package com.example.imam_al_masjid;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.core.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class LoginActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private TextView txtTitle;
    private MaterialCardView loginCard;
    private EditText edtFullName, edtPassword;
    private TextView dropdownMasjid;
    private MaterialButton btnSubmit;
    private MaterialCardView logoContainer;
    private RelativeLayout passwordContainer;
    private ImageView btnTogglePassword;

    private TextView txtError;
    private String selectedMasjid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        applyDynamicScaling();
        applyClaymorphism();
        setupAnimations();
        setupFocusListeners();
        setupClickListeners();
    }

    private void setupClickListeners() {
        dropdownMasjid.setOnClickListener(v -> showMasjidDropdown());
        btnSubmit.setOnClickListener(v -> performLogin());
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    private boolean isPasswordVisible = false;
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        Typeface janna_it_boldFont = ResourcesCompat.getFont(this, R.font.janna_it_bold_font);
        
        if (isPasswordVisible) {
            edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_visible);
        } else {
            edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden);
        }
        
        // RE-APPLY FONT: Changing inputType resets the typeface to system default (P1 Fix)
        if (janna_it_boldFont != null) edtPassword.setTypeface(janna_it_boldFont);
        
        // Force cursor to the end
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void showMasjidDropdown() {
        // 1. DISMISS KEYBOARD (Crucial for visibility - P1)
        hideKeyboard();

        // Create Custom Claymorphic Dropdown via PopupWindow (P6)
        LinearLayout itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        itemsContainer.setBackground(ScalingUtils.createClayDrawable(this, 0.04f, 0.01f, 0.01f, 0));

        android.widget.PopupWindow popup = new android.widget.PopupWindow(itemsContainer, 
                dropdownMasjid.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);

        String[] masjids = {"Masjid-E-Alamgeer", "Hajia Masjid"};
        int highlightBody = ContextCompat.getColor(this, R.color.emerald_alpha_20); // Soft emerald tint
        int highlightShadow = ContextCompat.getColor(this, R.color.clay_dark_shadow);
        int highlightLight = ContextCompat.getColor(this, R.color.clay_light_shadow);

        int horizontalPadding = ScalingUtils.getScaledSize(this, 0.045f); 
        int verticalPadding = ScalingUtils.getScaledSize(this, 0.025f); // Reduced height gap (P4)
        float inputTextSize = 0.040f; 
        
        for (String masjid : masjids) {
            TextView item = (TextView) getLayoutInflater().inflate(R.layout.dropdown_item, null);
            item.setText(masjid);
            item.setTextSize(ScalingUtils.getScaledTextSize(this, inputTextSize));
            item.setTextColor(ContextCompat.getColor(this, R.color.emerald_primary));
            item.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding); 
            
            // Highlight Selected Masjid Background (P6)
            if (masjid.equals(selectedMasjid)) {
                item.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.02f, 0.005f, 0.01f,
                        highlightBody, highlightShadow, highlightLight));
            }

            item.setOnClickListener(v -> {
                selectedMasjid = masjid;
                dropdownMasjid.setText(masjid);
                dropdownMasjid.setTextColor(ContextCompat.getColor(this, R.color.emerald_primary));
                popup.dismiss();
            });
            itemsContainer.addView(item);
        }

        popup.setElevation(ScalingUtils.getScaledSize(this, 0.02f));
        popup.showAsDropDown(dropdownMasjid);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void performLogin() {
        txtError.setVisibility(View.GONE);
        String name = edtFullName.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        // 1. Morph Button to Loading State
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Loading...");
        btnSubmit.setAlpha(0.7f);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (name.equals("Shaik Abuzer") && pass.equals("ABUZEr@786") && !selectedMasjid.isEmpty()) {
                // Success
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Failure
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit");
                btnSubmit.setAlpha(1.0f);
                txtError.setVisibility(View.VISIBLE);
                if (selectedMasjid.isEmpty()) txtError.setText("Please select a Masjid");
                else txtError.setText("Invalid Full Name or Password");
            }
        }, 2000);
    }

    private void setupAnimations() {
        // Entrance Animations (Requirement #2 style)
        android.view.animation.Animation slideDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_fade_in);
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_slide_up);

        logoContainer.startAnimation(slideDown);
        txtTitle.startAnimation(slideUp);
        
        // Sequential fade-in for form elements
        loginCard.setAlpha(0f);
        loginCard.animate().alpha(1f).setDuration(1000).setStartDelay(500).start();
    }

    private void setupFocusListeners() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            View targetView = (v == edtPassword) ? passwordContainer : v;

            if (hasFocus) {
                // Focus State: Deeper Inset + Scaling (Subtle)
                targetView.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.012f, 0.018f));
                targetView.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
            } else {
                // Normal State
                targetView.setBackground(ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f));
                targetView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };

        edtFullName.setOnFocusChangeListener(focusListener);
        edtPassword.setOnFocusChangeListener(focusListener);
    }

    private void initViews() {
        imgLogo = findViewById(R.id.img_logo);
        txtTitle = findViewById(R.id.txt_login_title);
        loginCard = findViewById(R.id.login_card);
        edtFullName = findViewById(R.id.edt_full_name);
        edtPassword = findViewById(R.id.edt_password);
        dropdownMasjid = findViewById(R.id.dropdown_masjid);
        btnSubmit = findViewById(R.id.btn_submit);
        logoContainer = findViewById(R.id.logo_container);
        txtError = findViewById(R.id.txt_error_message);
        passwordContainer = findViewById(R.id.password_container);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        
        // Satisfy the 'decreased height' feedback by adding internal form padding
        // Ensure all inputs have vertical centering (P4 uniformity)
        int verticalGravity = android.view.Gravity.CENTER_VERTICAL;
        edtFullName.setGravity(verticalGravity);
        dropdownMasjid.setGravity(verticalGravity);
        
        // Fix Password Centering: Parent container handles the height, 
        // EditText must fill height and center text vertically.
        edtPassword.setPadding(0, 0, 0, 0);
        edtPassword.setGravity(verticalGravity);
        RelativeLayout.LayoutParams passParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        passParams.addRule(RelativeLayout.CENTER_VERTICAL);
        edtPassword.setLayoutParams(passParams);

        // Expand Login Box Height: Add generous internal padding (Requirement #4)
        LinearLayout loginForm = findViewById(R.id.login_form);
        int formPadding = ScalingUtils.getScaledSize(this, 0.06f); 
        loginForm.setPadding(formPadding, formPadding, formPadding, formPadding);

        // Final Font Guard: Ensure password field respects the theme font
        Typeface janna_it_boldFont = ResourcesCompat.getFont(this, R.font.janna_it_bold_font);
        if (janna_it_boldFont != null) edtPassword.setTypeface(janna_it_boldFont);
    }

    private void applyDynamicScaling() {
        // 1. Logo Container - 25% size, 8% top margin (Pushed down more)
        ScalingUtils.applyScaledLayout(logoContainer, 0.30f, 0.30f, 0.1f, 0, 0, 0);

        // 2. Title - 2% gap
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(this, 0.050f));
        ScalingUtils.applyScaledLayout(txtTitle, -1, -1, 0.02f, 0, 0, 0);

        // 3. Login Card - 88% width, 4% top gap, 12% bottom gap (Pushed down more)
        ScalingUtils.applyScaledLayout(loginCard, 0.88f, -1, 0.04f, 0.12f, 0, 0);
        
        // 4. Form Spacing
        float inputPadding = 0.045f;
        float itemMargin = 0.045f; // Increased from 3.5% for better vertical presence
        float inputHeight = 0.075f; 
        float inputTextSize = 0.040f; // Decreased text size percentage (P4)

        applyInputScaling(edtFullName, inputPadding, 0.02f, inputHeight); // Added 2% top margin
        edtFullName.setTextSize(ScalingUtils.getScaledTextSize(this, inputTextSize));
        
        applyInputScaling(passwordContainer, inputPadding, itemMargin, inputHeight);
        edtPassword.setTextSize(ScalingUtils.getScaledTextSize(this, inputTextSize));
        
        applyInputScaling(dropdownMasjid, inputPadding, itemMargin, inputHeight);
        dropdownMasjid.setTextSize(ScalingUtils.getScaledTextSize(this, inputTextSize));

        // Scale Toggle Button (8% width/height)
        ScalingUtils.applyScaledLayout(btnTogglePassword, 0.08f, 0.08f, 0, 0, 0, 0.02f);
        int toggleInnerPadding = ScalingUtils.getScaledSize(this, 0.015f);
        btnTogglePassword.setPadding(toggleInnerPadding, 0, toggleInnerPadding, 0);

        // 5. Submit Button Scaling
        ScalingUtils.applyScaledLayout(btnSubmit, 0.60f, -1, 0.06f, 0.05f, 0, 0);
        btnSubmit.setTextSize(ScalingUtils.getScaledTextSize(this, 0.035f));
        btnSubmit.setCornerRadius(ScalingUtils.getScaledSize(this, 0.035f));

        // 6. Error Text Scaling
        txtError.setTextSize(ScalingUtils.getScaledTextSize(this, 0.040f));
        ScalingUtils.applyScaledLayout(txtError, -1, -1, 0.02f, 0.02f, 0, 0);
    }

    private void applyInputScaling(View view, float paddingPercent, float marginTopPercent, float heightPercent) {
        int paddingSide = ScalingUtils.getScaledSize(this, paddingPercent);
        // Vertical padding is less critical now that height is fixed, but helps centering
        view.setPadding(paddingSide, 0, paddingSide, 0);
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.topMargin = ScalingUtils.getScaledSize(this, marginTopPercent);
        params.width = ScalingUtils.getScaledSize(this, 0.70f); // Decreased width a bit (P4)
        params.height = ScalingUtils.getScaledSize(this, heightPercent, true); // Strict compliance
        view.setLayoutParams(params);
    }

    private void applyClaymorphism() {
        // 1. Logo Container - No border as per P7
        logoContainer.setBackground(ScalingUtils.createClayDrawable(this, 0.080f, 0.012f, 0.02f, 0f));

        // 2. Login Card Background (Raised Clay)
        loginCard.setBackground(ScalingUtils.createClayDrawable(this, 0.06f, 0.012f, 0.02f, 0.005f));

        // 3. Input Backgrounds (Inset Clay)
        Drawable insetBg = ScalingUtils.createInsetClayDrawable(this, 0.03f, 0.008f, 0.015f);
        edtFullName.setBackground(insetBg);
        passwordContainer.setBackground(insetBg); // Container gets the inset bg
        dropdownMasjid.setBackground(insetBg);

        // 4. Submit Button (Convex Clay - P3, P4)
        // Use Emerald for Light Mode and Dark Accent Primary for Dark Mode (handled by resources)
        int btnBodyColor = ContextCompat.getColor(this, R.color.emerald_primary);
        int btnShadow = ContextCompat.getColor(this, R.color.clay_dark_shadow);
        int btnHighlight = ContextCompat.getColor(this, R.color.clay_light_shadow);
        
        btnSubmit.setBackground(ScalingUtils.createClayDrawable(this, 0.035f, 0.015f, 0.01f, 0,
                btnBodyColor, btnShadow, btnHighlight, 0));
        btnSubmit.setTextColor(ContextCompat.getColor(this, R.color.off_white_primary));

        // 5. Password Toggle Color (P2)
        btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden);
        btnTogglePassword.setColorFilter(ContextCompat.getColor(this, R.color.emerald_primary));
    }
}
