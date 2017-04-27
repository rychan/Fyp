package com.example.rychan.fyp.perspective_transform;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.rychan.fyp.R;

/**
 * Created by rychan on 17年4月27日.
 */

public class StateBar extends LinearLayout implements View.OnClickListener {

    private OnTabClickListener mListener;

    private int[] state = new int[5];
    private TextView[] tab = new TextView[5];
    private SparseIntArray idMap = new SparseIntArray(5);
    private final int STATE_UNCLICKABLE = -1;
    private final int STATE_UNCLICK = 0;
    private final int STATE_CLICKED = 1;
    public int onFocusTab = 0;

    public StateBar(Context context) {
        super(context);
        init();
    }

    public StateBar(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public StateBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        try {
            // Instantiate the DialogListener so we can send events to the host
            mListener = (OnTabClickListener) getContext();
        } catch (ClassCastException e) {
            // The context doesn't implement the interface, throw exception
            throw new ClassCastException(getContext().toString()
                    + " must implement OnTabClickListener");
        }

        LayoutInflater.from(getContext()).inflate(R.layout.state_bar, this);
        idMap.put(R.id.tab_all, 0);
        idMap.put(R.id.tab_1, 1);
        idMap.put(R.id.tab_2, 2);
        idMap.put(R.id.tab_3, 3);
        idMap.put(R.id.tab_4, 4);

        for (int i = 0; i < 5; ++i){
            tab[i] = (TextView) findViewById(idMap.keyAt(idMap.indexOfValue(i)));
            tab[i].setOnClickListener(this);
            setUnclickable(i);
        }
    }

    public void setUnclickable(int index) {
        state[index] = STATE_UNCLICKABLE;
        tab[index].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        tab[index].setTextColor(getResources().getColor(R.color.background));
    }

    private void setUnclick(int index) {
        state[index] = STATE_UNCLICK;
        tab[index].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        tab[index].setTextColor(getResources().getColor(android.R.color.black));
    }

    public void setClicked(int index) {
        if (state[onFocusTab] == STATE_CLICKED) {
            setUnclick(onFocusTab);
        }
        state[index] = STATE_CLICKED;
        tab[index].setBackgroundColor(getResources().getColor(R.color.background));
        tab[index].setTextColor(getResources().getColor(android.R.color.black));
        onFocusTab = index;
    }

    public void initHoughTab(int receiptNum) {
        for (int i = 1; i <= receiptNum; ++i) {
            setUnclick(i);
        }
        setClicked(0);
    }

    public void initDisplayTab(int receiptNum) {
        for (int i = 2; i <= receiptNum; ++i) {
            setUnclick(i);
        }
        setUnclickable(0);
        setClicked(1);
    }

    public int getUnclick() {
        for (int i = 0; i < 5; ++i) {
            if (state[i] == STATE_UNCLICK) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onClick(View v) {
        int index = idMap.get(v.getId());
        if (state[index] == STATE_UNCLICK) {
            setUnclick(onFocusTab);
            setClicked(index);
            mListener.onTabClick();
        }
    }

    public interface OnTabClickListener {
        void onTabClick();
    }
}
