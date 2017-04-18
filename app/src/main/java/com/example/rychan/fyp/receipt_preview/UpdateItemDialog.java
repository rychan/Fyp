package com.example.rychan.fyp.receipt_preview;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.rychan.fyp.R;

/**
 * Created by rycha on 10/4/2017.
 */

public class UpdateItemDialog extends DialogFragment {
    // Use this instance of the interface to deliver action events
    private DialogListener mListener;

    public static final String ARG_ROW_ID = "view_id";

    private int rowId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rowId = getArguments().getInt(ARG_ROW_ID);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_update_item, null);

        ImageView imageView = (ImageView) v.findViewById(R.id.image_view);
        final EditText itemName = (EditText) v.findViewById(R.id.item_name);
        final EditText price = (EditText) v.findViewById(R.id.price);
        mListener.setView(imageView, itemName, price, rowId);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Update Item")
                .setView(v)
                .setPositiveButton("Save Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(itemName.getText().toString(),
                                Double.valueOf(price.getText().toString()), rowId);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UpdateItemDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void setView(ImageView imageView, EditText itemName, EditText price, int rowId);
        void onDialogPositiveClick(String itemName, double price, int rowId);
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
