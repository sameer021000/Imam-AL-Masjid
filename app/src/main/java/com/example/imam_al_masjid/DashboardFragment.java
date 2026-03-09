package com.example.imam_al_masjid;

import android.view.View;
import android.widget.TextView;

public class DashboardFragment extends BaseFragment {
    @Override
    protected int getLayoutId() { return R.layout.fragment_dashboard; }

    @Override
    protected void initViews(View view) {}

    @Override
    protected void applyDynamicScaling(View view) {
        if (getContext() == null) return;
        
        // Root padding
        View root = view.findViewById(R.id.dashboard_root);
        int padding = ScalingUtils.getScaledSize(getContext(), 0.05f); // ~5% padding
        root.setPadding(padding, padding, padding, padding);

        // Title text size
        TextView txtTitle = view.findViewById(R.id.txt_dashboard_title);
        txtTitle.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.055f));

        // Subtitle text size & margin
        TextView txtSubtitle = view.findViewById(R.id.txt_dashboard_subtitle);
        txtSubtitle.setTextSize(ScalingUtils.getScaledTextSize(getContext(), 0.035f));
        ScalingUtils.applyScaledLayout(txtSubtitle, -1, -1, 0.02f, 0, 0, 0); // 2% margin top
    }

    @Override
    protected void applyClaymorphism(View view) {}
}
