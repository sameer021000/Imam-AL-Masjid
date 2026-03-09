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
        initViews(view);
        applyDynamicScaling(view);
        applyClaymorphism(view);
        return view;
    }

    protected abstract int getLayoutId();
    protected abstract void initViews(View view);
    protected abstract void applyDynamicScaling(View view);
    protected abstract void applyClaymorphism(View view);
}
