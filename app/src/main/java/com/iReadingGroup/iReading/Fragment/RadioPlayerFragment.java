package com.iReadingGroup.iReading.Fragment;


import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iReadingGroup.iReading.Activity.RadioPlayerActivity;
import com.iReadingGroup.iReading.Activity.SettingsActivity;
import com.iReadingGroup.iReading.R;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class RadioPlayerFragment extends Fragment {


    public RadioPlayerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_radio_player, container, false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                    Intent intent = new Intent(getActivity(), RadioPlayerActivity.class);
                    startActivity(intent);
        }}, 3000);
        return view;
    }
}
