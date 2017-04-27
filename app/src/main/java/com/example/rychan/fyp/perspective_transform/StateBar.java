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

    private OnTabChangeListener mListener;

    private int[] state = new int[5];
    private TextView[] tab = new TextView[5];
    private SparseIntArray idMap = new SparseIntArray(5);
    private final int STATE_UNCLICKABLE = -1;
    private final int STATE_UNCLICK = 0;
    private final int STATE_CLICKED = 1;
    private int clicking;

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
            mListener = (OnTabChangeListener) getContext();
        } catch (ClassCastException e) {
            // The context doesn't implement the interface, throw exception
            throw new ClassCastException(getContext().toString()
                    + " must implement OnTabChangeListener");
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
            state[i] = STATE_UNCLICK;
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

    private void setClicked(int index) {
        state[index] = STATE_CLICKED;
        tab[index].setBackgroundColor(getResources().getColor(R.color.background));
        tab[index].setTextColor(getResources().getColor(android.R.color.black));
        clicking = index;
    }

    public void setTabState(boolean isTabAllClickable, int receiptNum) {
        for (int i = 1; i < 5; ++i) {
            if (i <= receiptNum) {
                setUnclick(i);
            } else {
                setUnclickable(i);
            }
        }
        if (isTabAllClickable) {
            setClicked(0);
        } else {
            setUnclickable(0);
            setClicked(1);
        }
    }

    @Override
    public void onClick(View v) {
        int index = idMap.get(v.getId());
        if (state[index] == STATE_UNCLICK) {
            setUnclick(clicking);
            setClicked(index);
            mListener.onTabChange(clicking);
        }
    }

    public interface OnTabChangeListener {
        void onTabChange(int tab);
    }
}
