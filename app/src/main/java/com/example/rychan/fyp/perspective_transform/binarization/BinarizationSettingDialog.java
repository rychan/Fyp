package com.example.rychan.fyp.perspective_transform.binarization;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.example.rychan.fyp.R;

/**
 * Created by rycha on 10/4/2017.
 */

public class BinarizationSettingDialog extends DialogFragment {
    // Use this instance of the interface to deliver action events
    private DialogListener mListener;

    public SeekBarGroup blurBlockSize;
    public SeekBarGroup thresholdBlockSize;
    public SeekBarGroup thresholdConstant;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_binarization_setting, null);

        blurBlockSize = (SeekBarGroup) v.findViewById(R.id.blurBlockSize);
        blurBlockSize.setValue("Blur Block Size", 3, 11, 2, mListener.getBlurBlockSize());

        thresholdBlockSize = (SeekBarGroup) v.findViewById(R.id.thresholdBlockSize);
        thresholdBlockSize.setValue("Adaptive Thresholding Block Size", 11, 199, 2, mListener.getThresholdBlockSize());

        thresholdConstant = (SeekBarGroup) v.findViewById(R.id.thresholdConstant);
        thresholdConstant.setValue("Adaptive Thresholding Constant", 0, 8, 0.1, mListener.getThresholdConstant());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Binarization Setting")
                .setView(v)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(BinarizationSettingDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        BinarizationSettingDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(BinarizationSettingDialog dialog);
        int getBlurBlockSize();
        int getThresholdBlockSize();
        double getThresholdConstant();
    }

    // Override the Fragment.onAttach() method to instantiate the DialogListener
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DialogListener so we can send events to the host
            mListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement DialogListener");
        }
    }
}
