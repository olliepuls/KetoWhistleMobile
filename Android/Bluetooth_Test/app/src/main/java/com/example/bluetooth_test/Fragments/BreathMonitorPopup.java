package com.example.bluetooth_test.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bluetooth_test.BreathMonitor;
import com.example.bluetooth_test.Data.DataSetInfo;
import com.example.bluetooth_test.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.bluetooth_test.BreathMonitor.NEW_STATE;
import static com.example.bluetooth_test.BreathMonitor.OLD_STATE;

public class BreathMonitorPopup  extends DialogFragment {
    private TextView title;
    private LinearLayout contentView;
    private LocalBroadcastManager broadcastManager;
    private Timer timer;

    private final boolean debugging = false;

    private boolean heating;
    private BreathMonitor monitor;

    public interface BreathMonitorPopupListener{
        void sendMessage(String message);
        @NonNull
        BreathMonitor setBreathMonitor();
    }

    private final PlotFragment plotFragment1 = PlotFragment.newButtonlessInstance(
            new ArrayList<>(Arrays.asList(
            new DataSetInfo("CO2", Color.BLACK, true),
                        new DataSetInfo("Humi", Color.BLUE, false)
                )));
    private final PlotFragment plotFragment2 = PlotFragment.newButtonlessInstance(
            new ArrayList<>(Arrays.asList(
            new DataSetInfo("CO2", Color.BLACK, true),
                        new DataSetInfo("Acet", Color.GREEN, false)
                )));

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BreathMonitor.States newState = (BreathMonitor.States) intent.getSerializableExtra(NEW_STATE);
            final BreathMonitor.States oldState = (BreathMonitor.States) intent.getSerializableExtra(OLD_STATE);
            Log.i("POPUP Receiver", newState.toString());
            switch (newState) {
                case STATE_SLEEP:
                    break;
                case STATE_WAITING:
                    detectingBreathLayout();
                    break;
                case STATE_BREATH:
                    switchPlot();
                    break;
                case STATE_DEEP_BREATH:
                    title.setText("Detecting Acetone Concentration");
                    break;
                case STATE_POST_FAIL:
                    title.setText("Reading Failed");
                    FragmentManager fragmentManager = BreathMonitorPopup.this.getChildFragmentManager();
                    fragmentManager.beginTransaction().remove(plotFragment1).remove(plotFragment2).commit();
                    contentView.removeAllViews();
                    TextView textView = new TextView(context);
                    contentView.addView(textView);
                    switch (oldState){
                        case STATE_SLEEP:
                            textView.setText(context.getString(R.string.detection_fail_sleep));
                            break;
                        case STATE_WAITING:
                            textView.setText(context.getString(R.string.detection_fail_waiting));
                            break;
                        case STATE_BREATH:
                            textView.setText(context.getString(R.string.detection_fail_breath));
                            break;
                        case STATE_DEEP_BREATH:
                            textView.setText(context.getString(R.string.detection_fail_deep_breath));
                            break;
                    }
                    BreathMonitorPopup.this.getView().setOnClickListener(v -> {
                        BreathMonitorPopup.this.dismiss();
                    });
                    break;
                case STATE_POST_SUCCESS:
                    title.setText("Reading Succeeded");
                    fragmentManager = BreathMonitorPopup.this.getChildFragmentManager();
                    fragmentManager.beginTransaction().remove(plotFragment1).remove(plotFragment2).commit();
                    contentView.removeAllViews();
                    TextView successTextView = new TextView(context);
                    successTextView.setText(context.getString(R.string.detection_success,
                            BreathMonitorPopup.this.monitor.getAcetoneReading().value));
                    contentView.addView(successTextView);
                    BreathMonitorPopup.this.getView().setOnClickListener(v -> {
                        BreathMonitorPopup.this.dismiss();
                    });
                    break;
            }
        }
    };

    public BreathMonitorPopup(){
        heating = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        monitor = ((BreathMonitorPopupListener) getActivity()).setBreathMonitor();
        return inflater.inflate(R.layout.activity_measure, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.broadcastManager = LocalBroadcastManager.getInstance(getContext());
        title = view.findViewById(R.id.measure_title);
        contentView = view.findViewById(R.id.measureContent);
        Button cancel = view.findViewById(R.id.button_measure_cancel);
        cancel.setOnClickListener(v -> dismiss());
        startScreen();

        view.setOnClickListener(v -> {
            switch (monitor.getState()){
                case STATE_SLEEP:
                    if(!heating) {
                        heating = true;
                        title.setText("Heating Sensors");
                        ((BreathMonitorPopupListener) getActivity()).sendMessage("HEAT");
                        contentView.removeAllViews();
                        TextView timerView = new TextView(getContext());
                        timerView.setId(timerView.hashCode());
                        contentView.addView(timerView);
//                        final int[] countdown = {20};
                        //debugging
                        final int[] countdown = {5};
                        timerView.setText(getActivity().getString(R.string.monitor_countdown, countdown[0]));
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                Log.d( "onTouch Timer's run() called with ", ""+countdown[0]);
                                countdown[0]--;
                                if (countdown[0] >= 0) {
                                    timerView.setText(getActivity().getString(R.string.monitor_countdown, countdown[0]));
                                } else {
                                    if (!monitor.startingDetection()) {
                                        ((BreathMonitorPopupListener) getActivity()).sendMessage("COOL");
                                        if(debugging) {
                                            Log.i("run: debugging:", monitor.getState().toString());
                                            monitor.debugStateTransition(BreathMonitor.States.STATE_WAITING);
                                        } else
                                           getActivity().runOnUiThread(() -> startScreen());
                                    }
                                    heating = false;
                                    timer.cancel();
                                }
                            }
                        }, 1000, 1000);
                    }
                    break;
                case STATE_WAITING:
                    if(debugging) monitor.debugStateTransition(BreathMonitor.States.STATE_BREATH);
                    break;
                case STATE_BREATH:
                    if(debugging) monitor.debugStateTransition(BreathMonitor.States.STATE_DEEP_BREATH);
                    break;
                case STATE_DEEP_BREATH:
                    break;
                case STATE_POST_FAIL:
                case STATE_POST_SUCCESS:
                    this.dismiss();
                    break;
            }
            Log.i("BMP: onViewCreated ", "Ended touch event");
        });
        broadcastManager.registerReceiver(this.broadcastReceiver, new IntentFilter(BreathMonitor.ACTION_STATE_CHANGED));
    }

    @Override
    public void onCancel(DialogInterface dialog) {}

    @Override
    public void onDismiss(DialogInterface dialog) {
        broadcastManager.unregisterReceiver(broadcastReceiver);
        timer.cancel();
        FragmentManager fragmentManager = this.getChildFragmentManager();
        fragmentManager.beginTransaction().remove(plotFragment1).remove(plotFragment2).commit();
    }

    private void startScreen(){
        title.setText("Ready to start test");
        contentView.removeAllViews();
        TextView info = new TextView(getContext());
        info.setText("Touch screen to begin reading");
        contentView.addView(info);
    }


    private void detectingBreathLayout() {
        title.setText("Detecting Breath");
        contentView.removeAllViews();
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction().add(R.id.measureContent, plotFragment1).commit();
    }

    private void switchPlot(){
        title.setText("Detecting Deep Breath");
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.measureContent, plotFragment2).commit();
    }
}