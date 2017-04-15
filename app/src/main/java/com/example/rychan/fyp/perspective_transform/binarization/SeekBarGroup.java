package com.example.rychan.fyp.perspective_transform.binarization;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.rychan.fyp.R;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Created by rycha on 10/4/2017.
 */

public class SeekBarGroup extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    private BigDecimal min;
    private BigDecimal interval;
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

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
    }

    public void setValue(String s, int min, int max, int interval, int init) {
        title.setText(s);
        this.min = new BigDecimal(min);
        this.interval = new BigDecimal(interval);
        setSeekBarValue(new BigDecimal(max), new BigDecimal(init));
    }

    public void setValue(String s, double min, double max, double interval, double init) {
        title.setText(s);
        MathContext mathContext = new MathContext(2);
        this.min = new BigDecimal(min, mathContext);
        this.interval = new BigDecimal(interval, mathContext);
        setSeekBarValue(new BigDecimal(max, mathContext), new BigDecimal(init, mathContext));
    }

    private void setSeekBarValue(BigDecimal max, BigDecimal init) {
        seekBar.setMax(max.subtract(min).divide(interval, BigDecimal.ROUND_HALF_UP).intValue());
        seekBar.setProgress(init.subtract(min).divide(interval, BigDecimal.ROUND_HALF_UP).intValue());
        value.setText(getStringProgress());
    }

    public int getIntProgress() {
        return new BigDecimal(seekBar.getProgress()).multiply(interval).add(min).intValue();
    }

    public String getStringProgress() {
        return new BigDecimal(seekBar.getProgress()).multiply(interval).add(min).toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        value.setText(getStringProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}