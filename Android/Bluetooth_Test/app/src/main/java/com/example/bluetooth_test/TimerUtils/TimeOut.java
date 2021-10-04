package com.example.bluetooth_test.TimerUtils;

import android.icu.text.RelativeDateTimeFormatter;
import android.util.Log;

import com.example.bluetooth_test.Procedure;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimeOut extends TimerBase{
    private static final String TAG = "TIMEOUT";

    private Timer mTimer;
    private Procedure procedure;
    private boolean scheduled;

    public TimeOut(Procedure procedure){
        super(procedure, false);
    };

    public boolean timeOut(long milliseconds){
        return this.scheduleTask(milliseconds, -1);
    }

    public boolean timeOut(Date startTime){
        return this.scheduleTask(startTime, -1);
    }

    public boolean timeOut(Duration offset){
        return this.scheduleTask(dateAfterDuration(offset), -1);
    }

}
