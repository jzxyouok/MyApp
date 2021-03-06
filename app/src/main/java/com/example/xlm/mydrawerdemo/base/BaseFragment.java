package com.example.xlm.mydrawerdemo.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import de.greenrobot.event.EventBus;

/**
 * Created by xlm on 2015/11/17.
 */
public class BaseFragment extends Fragment {
    protected EventBus eventBus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventBus=EventBus.getDefault();
        eventBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventBus.unregister(this);
    }
}
