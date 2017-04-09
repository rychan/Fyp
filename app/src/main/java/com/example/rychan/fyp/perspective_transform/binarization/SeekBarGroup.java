package com.example.rychan.fyp.perspective_transform.binarization;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.rychan.fyp.R;


/**
 * Created by rycha on 10/4/2017.
 */

public class SeekBarGroup extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    private double min;
    private double interval;
    private SeekBar seekBar;

    private TextView title;
    private TextView value;

    public SeekBarGroup(Context context) {
        super(context);
        init();
    }

    public SeekBarGroup(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public SeekBarGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.seekbar_group, this);
        title = (TextView) findViewById(R.id.title);
        value = (TextView) findViewById(R.id.value);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
    }

    public void setValue(String s, int min, int max, int interval, int init) {
        title.setText(s);
        this.min = min;
        this.interval = interval;
        seekBar.setMax((max - min) / interval);
        seekBar.setProgress((init - min) / interval);
        value.setText(String.valueOf(getDoubleProgress()));
    }

    public void setValue(String s, double min, double max, double interval, double init) {
        title.setText(s);
        this.min = min;
        this.interval = interval;
        seekBar.setMax((int) ((max - min) / interval));
        seekBar.setProgress((int) ((init - min) / interval));
        value.setText(String.valueOf(getDoubleProgress()));
    }

    public int getIntProgress() {
        return seekBar.getProgress() * (int) interval + (int) min;
    }

    public double getDoubleProgress() {
        return seekBar.getProgress() * interval + min;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        value.setText(String.valueOf(getDoubleProgress()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}