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
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.bluetooth_test.Data.Buffer;
import com.example.bluetooth_test.Data.DataManager;
import com.example.bluetooth_test.R;

import static com.example.bluetooth_test.Data.DataManager.ACTION_DATA_RESET;
import static com.example.bluetooth_test.Data.DataManager.ACTION_NEW_MESSAGE;
import static com.example.bluetooth_test.Data.DataManager.MESSAGE_KEY;
import static com.example.bluetooth_test.Data.DataManager.RESET_INSTRUCTION_KEY;

public class MessageFragment extends Fragment {
    private final static String bufferLenKey = "BUFFER_LEN_KEY";
    public static MessageFragment newInstance(int bufferLength) {
        Bundle arguments = new Bundle();
        arguments.putInt(bufferLenKey, bufferLength);
        MessageFragment fragment = new MessageFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private TextView messageText;
    private Buffer<String> messages;
    int bufferLength;
    private boolean paused;

    private DataManager mDataManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEW_MESSAGE:
                        addMessage(intent.getStringExtra(MESSAGE_KEY));
                        break;
                    case ACTION_DATA_RESET:
                        if(intent.getBooleanExtra(RESET_INSTRUCTION_KEY, false))
                            reset();
                        break;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments()==null || !getArguments().containsKey(bufferLenKey)) bufferLength = 10;
        else bufferLength = getArguments().getInt(bufferLenKey);
        messages = new Buffer<>(bufferLength);
        paused = false;
        mDataManager = new DataManager(getContext());
    }

    public void reset(){
        this.messages = new Buffer<>(bufferLength);
        this.messageText.setText(messages.toString());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.message_frag, container, false);
        messageText = v.findViewById(R.id.messageTextView);
        ImageButton playPause = v.findViewById(R.id.message_play_pause);
        playPause.setOnClickListener(view->{
            paused = !paused;
            setButtonLook(playPause);
        });
        setButtonLook(playPause);
        return v;
    }

    private void setButtonLook(ImageButton button) {
        if (paused) button.setImageDrawable(getContext().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        else {
            button.setImageDrawable(getContext().getDrawable(R.drawable.ic_pause_black_24dp));
            messageText.setText(messages.toString());
        }
    }

    public boolean addMessage(String message){
        messages.add(message);
        if (!paused) {
            if (messageText!= null) messageText.setText(messages.toString());
            return true;
        }
        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDataManager.registerReceiver(mBroadcastReceiver,
                DataManager.createFilter(true, false, true));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataManager.deregisterReceiver(mBroadcastReceiver);
    }
}
