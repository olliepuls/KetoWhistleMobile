package com.example.bluetooth_test.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.bluetooth_test.Data.DataManager;
import com.example.bluetooth_test.Data.DataPoint;
import com.example.bluetooth_test.Data.DataSetInfo;
import com.example.bluetooth_test.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;

import static com.example.bluetooth_test.Data.DataManager.ACTION_DATA_RESET;
import static com.example.bluetooth_test.Data.DataManager.ACTION_NEW_DATA_POINT;
import static com.example.bluetooth_test.Data.DataManager.DATA_POINT_KEY;
import static com.example.bluetooth_test.Data.DataManager.LABEL_KEY;
import static com.example.bluetooth_test.Data.DataSetInfo.findDSIByLabel;

;

public class PlotFragment extends Fragment {
    private final static String dataSetKey = "STR_INT_DATA_SET_KEY";
    private static String TAG = "PLOT_FRAGMENT";
    private LineChart lineChart;
    private LineData lineData;
    private ArrayList<DataSetInfo> setLabelColours;

    private DataManager mDataManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action){
                    case ACTION_NEW_DATA_POINT:
                        addEntry(intent.getStringExtra(LABEL_KEY),
                                ((DataPoint) intent.getSerializableExtra(DATA_POINT_KEY)).toEntry());
                        break;
                    case ACTION_DATA_RESET:
                        resetDataSets();
                        break;
                }
            }
        }
    };

    public static PlotFragment newInstance(ArrayList<DataSetInfo> dataSetInfo) {
        PlotFragment plotFragment =  new PlotFragment();
        Bundle args = new Bundle();
        args.putSerializable(dataSetKey, dataSetInfo);
        plotFragment.setArguments(args);
        return plotFragment;
    }

    public static PlotFragment newButtonlessInstance(ArrayList<DataSetInfo> dataSetInfo) {
        PlotFragment plotFragment =  new PlotFragment();
        Bundle args = new Bundle();
        args.putSerializable(dataSetKey, dataSetInfo);
        args.putBoolean("RESET", false);
        plotFragment.setArguments(args);
        return plotFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLabelColours = (ArrayList<DataSetInfo>) getArguments().getSerializable(dataSetKey);
        Utils.init(getContext());
        mDataManager = new DataManager(getContext());
    }

    public void resetDataSets(String... labels){
        if (lineChart != null && lineData != null) lineData.clearValues();
        if(labels.length==0){
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();

            for (DataSetInfo dSI: setLabelColours) {
                LineDataSet dataSet = new LineDataSet(null, dSI.label);

                if (dSI.left) dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                else dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

                dataSet.setColor(dSI.color);
                dataSet.setCircleColor(dSI.color);
                dataSets.add(dataSet);
            }
            lineData = new LineData(dataSets);
        } else for (String s: labels) {
            DataSetInfo dSI = findDSIByLabel(s, setLabelColours);
            if(dSI==null) return;
            lineData.removeDataSet(lineData.getDataSetByLabel(s, true));
            LineDataSet dataSet = new LineDataSet(null, dSI.label);
            if (dSI.left) dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            else dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

            dataSet.setColor(dSI.color);
            dataSet.setCircleColor(dSI.color);
            lineData.addDataSet(dataSet);
        }
        lineChart.setData(lineData);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.plot_frag, container, false);
        lineChart = (v.findViewById(R.id.chart_line));
        YAxis left = lineChart.getAxisLeft();
        left.setAxisMinimum(0f);

        YAxis right = lineChart.getAxisRight();
        right.setAxisMinimum(0f);
        resetDataSets();

        Button button = v.findViewById(R.id.plot_reset_button);
        if(getArguments().getBoolean("RESET",true)) {
            button.setOnClickListener(view -> {
                mDataManager.sendReset(false);
            });
        } else {
            button.setVisibility(View.GONE);
        }
        return v;
    }


    public void addEntry(String dataLabel, Entry entry){
        try{
            lineData.addEntry(entry, getIndexByLabel(dataLabel));
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        } catch (Exception e){
            return;
        }
    }

    private int getIndexByLabel(String label) throws Exception {
        for (int i = 0; i < setLabelColours.size(); i++) {
            if(setLabelColours.get(i).label.equalsIgnoreCase(label)) return i;
        }
        throw new Exception();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDataManager.registerReceiver(mBroadcastReceiver,
                DataManager.createFilter(false, true, true));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataManager.deregisterReceiver(mBroadcastReceiver);
    }
}
