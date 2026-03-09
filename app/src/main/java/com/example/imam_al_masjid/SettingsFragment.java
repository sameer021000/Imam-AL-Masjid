package com.example.imam_al_masjid;

import android.view.View;
import android.widget.TextView;

public class SettingsFragment extends BaseFragment {
    @Override
    protected int getLayoutId() { return R.layout.fragment_settings; }

    @Override
    protected void initViews(View view) {}

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        
        View root = view.findViewById(R.id.settings_root);
        int padding = ScalingUtils.getScaledSize(getContext(), 0.05f); // ~5% padding
        root.setPadding(padding, padding, padding, padding);

        TextView txtTitle = view.findViewById(R.id.txt_settings_title);
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.055f));
    }

    @Override
    protected void applyClaymorphism(View view) {}
}
