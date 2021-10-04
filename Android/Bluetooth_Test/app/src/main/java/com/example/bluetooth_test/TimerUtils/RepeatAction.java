package com.example.bluetooth_test.TimerUtils;

import com.example.bluetooth_test.Procedure;

import java.util.Date;
import java.util.Timer;

public class RepeatAction extends TimerBase{
    private static final String TAG = "REPEAT";
    private final long interval;

    public RepeatAction(Procedure procedure, long interval){
        super(procedure, true);
        if(interval <=0) throw new IllegalArgumentException("The Repeat Action's interval is strictly positive");
        this.interval = interval;
    }

    public boolean start(long millisecond){
        return scheduleTask(millisecond, interval);
    }

    public boolean start(Date date){
        return scheduleTask(date, interval);
    }

    public boolean start(){
        return scheduleTask(0, interval);
    }
}
