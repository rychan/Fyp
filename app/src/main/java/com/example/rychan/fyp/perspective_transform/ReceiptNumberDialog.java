package com.example.rychan.fyp.perspective_transform;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by rychan on 17年4月27日.
 */

public class ReceiptNumberDialog extends DialogFragment {

    private DialogListener mListener;

    final String[] numChoice = new String[]{"1","2","3","4"};
    private int num = 1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set the dialog title
        builder.setTitle("How many receipts in the image?")
                .setSingleChoiceItems(numChoice, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                num = which + 1;
                            }
                        })
                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onNumSelected(num);
                    }
                });

        return builder.create();
    }

    public interface DialogListener {
        void onNumSelected(int num);
    }

    // Override the Fragment.onAttach() method to instantiate the DialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host context implements the callback interface
        try {
            // Instantiate the DialogListener so we can send events to the host
            mListener = (ReceiptNumberDialog.DialogListener) context;
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
