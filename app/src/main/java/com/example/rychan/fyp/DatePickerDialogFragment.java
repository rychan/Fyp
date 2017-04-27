package com.example.rychan.fyp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.app.DatePickerDialog;
import android.widget.DatePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rychan on 17年4月17日.
 */

public class DatePickerDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private static final String ARG_DATE_STRING = "date";
    private static final String ARG_VIEW_ID = "id";

    private Date date;
    private int id;

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private DialogListener mListener;


    public static DatePickerDialogFragment newInstance(String date, int id) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        Bundle arg = new Bundle();
        arg.putString(ARG_DATE_STRING, date);
        arg.putInt(ARG_VIEW_ID, id);
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            date = parseDate(getArguments().getString(ARG_DATE_STRING));
            id = getArguments().getInt(ARG_VIEW_ID);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void getDateString(int viewId, String date);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (DialogListener) context;
        } catch (ClassCastException e) {
            // The context doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement DialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, dayOfMonth);
        mListener.getDateString(id, dateFormat.format(c.getTime()));
    }

    public static Date parseDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }
}
