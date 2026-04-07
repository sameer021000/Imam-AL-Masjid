package com.example.imam_al_masjid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);
        applySystemInsetPadding(view);
        initViews(view);
        applyDynamicScaling(view);
        applyClaymorphism(view);
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            updateStatusBar();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            updateStatusBar();
        }
    }

    /**
     * Default inset handling: applies top system bar inset as padding to root view.
     * Specialized fragments can override this for custom handling (e.g. true edge-to-edge header).
     */
    protected void applySystemInsetPadding(View view) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /**
     * Each fragment should override this to set its own status bar color and icon appearance.
     * Default implementation sets it to transparent with automatic icon coloring.
     */
    protected void updateStatusBar() {
       if (getActivity() != null && getActivity().getWindow() != null) {
           getActivity().getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
           
           boolean isNight = ThemeManager.isNightMode(getContext());
           androidx.core.view.WindowInsetsControllerCompat controller = 
               new androidx.core.view.WindowInsetsControllerCompat(getActivity().getWindow(), getActivity().getWindow().getDecorView());
           controller.setAppearanceLightStatusBars(!isNight);
       }
    }

    protected abstract int getLayoutId();
    protected abstract void initViews(View view);
    protected abstract void applyDynamicScaling(View view);
    protected abstract void applyClaymorphism(View view);
}
