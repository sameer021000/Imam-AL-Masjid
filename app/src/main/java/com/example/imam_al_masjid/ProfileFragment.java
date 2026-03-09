package com.example.imam_al_masjid;

import android.view.View;
import android.widget.TextView;

public class ProfileFragment extends BaseFragment {
    @Override
    protected int getLayoutId() { return R.layout.fragment_profile; }

    @Override
    protected void initViews(View view) {}

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        
        View root = view.findViewById(R.id.profile_root);
        int padding = ScalingUtils.getScaledSize(getContext(), 0.05f); // ~5% padding
        root.setPadding(padding, padding, padding, padding);

        TextView txtTitle = view.findViewById(R.id.txt_profile_title);
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.055f));
    }

    @Override
    protected void applyClaymorphism(View view) {}
}
