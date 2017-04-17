package com.example.rychan.fyp.perspective_transform.binarization;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

    SharedPreferences binarizationSetting;

    public SeekBarGroup blurBlockSizeSeekBar;
    public SeekBarGroup thresholdBlockSizeSeekBar;
    public SeekBarGroup thresholdConstantSeekBar;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binarizationSetting = getContext().getSharedPreferences("BINARIZATION_SETTING", Context.MODE_PRIVATE);
        int blurBlockSize = binarizationSetting.getInt("BLUR_BLOCK_SIZE", 5);
        int thresholdBlockSize = binarizationSetting.getInt("THRESHOLD_BLOCK_SIZE", 101);
        double thresholdConstant = Double.valueOf(binarizationSetting.getString("THRESHOLD_CONSTANT", "6.0"));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_binarization_setting, null);

        blurBlockSizeSeekBar = (SeekBarGroup) v.findViewById(R.id.blur_block_size_seekbar);
        blurBlockSizeSeekBar.setValue("Blur Block Size", 3, 11, 2, blurBlockSize);

        thresholdBlockSizeSeekBar = (SeekBarGroup) v.findViewById(R.id.threshold_block_size_seekbar);
        thresholdBlockSizeSeekBar.setValue("Adaptive Thresholding Block Size", 11, 201, 2, thresholdBlockSize);

        thresholdConstantSeekBar = (SeekBarGroup) v.findViewById(R.id.threshold_constant_seekbar);
        thresholdConstantSeekBar.setValue("Adaptive Thresholding Constant", 0, 10, 0.1, thresholdConstant);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Binarization Setting")
                .setView(v)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences.Editor editor = binarizationSetting.edit();
                        editor.putInt("BLUR_BLOCK_SIZE", blurBlockSizeSeekBar.getIntProgress());
                        editor.putInt("THRESHOLD_BLOCK_SIZE", thresholdBlockSizeSeekBar.getIntProgress());
                        editor.putString("THRESHOLD_CONSTANT", thresholdConstantSeekBar.getStringProgress());
                        editor.commit();
                        mListener.onDialogPositiveClick();
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
        void onDialogPositiveClick();
    }

    // Override the Fragment.onAttach() method to instantiate the DialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host context implements the callback interface
        try {
            // Instantiate the DialogListener so we can send events to the host
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
}
