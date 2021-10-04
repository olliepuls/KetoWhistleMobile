package com.example.bluetooth_test.TimerUtils;

import com.example.bluetooth_test.Procedure;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public abstract class TimerBase {
    private final boolean continuous;
    private Timer mTimer;
    private Procedure procedure;
    private boolean running;

    public TimerBase(Procedure procedure, boolean continuous){
        this.procedure = procedure;
        this.continuous = continuous;
        running = false;
    }

    TimerTask generateTask(){
        return new TimerTask() {
            @Override
            public void run() {
                procedure.perform();
                running = continuous;
            }
        };
    }

    protected boolean scheduleTask(Date date, long interval){
        if(!running){
            mTimer = new Timer();
            running = true;
            if(interval<=0) mTimer.schedule(generateTask(), date);
            else mTimer.schedule(generateTask(), date, interval);
            return true;
        }
        return false;
    }

    protected boolean scheduleTask(long milliseconds, long interval){
        if(!running) {
            mTimer = new Timer();
            running = true;
            if(interval<=0) mTimer.schedule(generateTask(), milliseconds);
            else mTimer.schedule(generateTask(), milliseconds, interval);
            return true;
        }
        return false;
    }

    public static Date dateAfterDuration(Duration duration){
        return Date.from(ZonedDateTime.now().plus(duration).toInstant());
    }

    public void cancel(){
        if(running){
            mTimer.cancel();
            running = false;
        }
    }
}