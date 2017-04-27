package com.example.rychan.fyp.xml_preview;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by rychan on 17年4月17日.
 */

public class SelectShopDialog extends DialogFragment{

    public static final String ARG_SHOP_LIST = "shop_list";
    public static final String ARG_SELECTED_SHOP_LIST = "selected_shop_list";

    private List<String> shopList = new ArrayList<>();
    private boolean[] booleanArray;

    private DialogListener mListener;


    public static SelectShopDialog newInstance(ArrayList<String> shopList, ArrayList<String> selectedShopList) {
        SelectShopDialog fragment = new SelectShopDialog();
        Bundle arg = new Bundle();
        arg.putStringArrayList(ARG_SHOP_LIST, shopList);
        arg.putStringArrayList(ARG_SELECTED_SHOP_LIST, selectedShopList);
        fragment.setArguments(arg);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shopList.add("All");
        if (getArguments() != null) {
            shopList.addAll(getArguments().getStringArrayList(ARG_SHOP_LIST));
            List<String> selectedShopList = getArguments().getStringArrayList(ARG_SELECTED_SHOP_LIST);
            booleanArray = new boolean[shopList.size()];
            if (selectedShopList.isEmpty()) {
                Arrays.fill(booleanArray, true);
            } else {
                boolean isAll = true;
                for (int i = 1; i < shopList.size(); ++i) {
                    boolean isSelected = selectedShopList.contains(shopList.get(i));
                    booleanArray[i] = isSelected;
                    isAll = isAll && isSelected;
                }
                booleanArray[0] = isAll;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Select Shop")
                .setMultiChoiceItems(shopList.toArray(new String[shopList.size()]),
                        booleanArray, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                AlertDialog alertDialog = (AlertDialog) dialog;
                                if (which == 0) {
                                    booleanArray[0] = isChecked;
                                    for(int i = 1; i< booleanArray.length; ++i) {
                                        booleanArray[i] = isChecked;
                                        alertDialog.getListView().setItemChecked(i, isChecked);
                                    }
                                } else {
                                    booleanArray[which] = isChecked;
                                }
                            }
                        })
                .setPositiveButton("Select", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<String> selectedShop = new ArrayList<>();
                        for (int i = 1; i < shopList.size(); ++i) {
                            if (booleanArray[i]) {
                                selectedShop.add(shopList.get(i));
                            }
                        }
                        mListener.onDialogPositiveClick(selectedShop);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SelectShopDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(ArrayList<String> selectedShopList);
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
}
