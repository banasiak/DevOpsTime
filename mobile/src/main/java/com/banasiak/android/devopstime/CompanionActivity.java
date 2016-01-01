/*
 * Copyright (C) 2015 Richard Banasiak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.banasiak.android.devopstime;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

public class CompanionActivity extends Activity {

    private static final String TAG = CompanionActivity.class.getSimpleName();

    private EditText clockSizeEditText;

    private EditText markerSizeEditText;

    private EditText tzSizeEditText;

    private EditText dateSizeEditText;

    private EditText timeSizeEditText;

    private EditText epochSizeEditText;

    private CheckBox clockCheckBox;

    private CheckBox markerCheckBox;

    private CheckBox tzCheckBox;

    private CheckBox dateCheckBox;

    private CheckBox timeCheckBox;

    private CheckBox epochCheckBox;

    private CheckBox useShortCardsCheckBox;

    private GoogleApiClient googleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeWearable();

        setContentView(R.layout.main);

        clockSizeEditText = (EditText) findViewById(R.id.clockSizeEditText);
        markerSizeEditText = (EditText) findViewById(R.id.markerSizeEditText);
        tzSizeEditText = (EditText) findViewById(R.id.tzSizeEditText);
        dateSizeEditText = (EditText) findViewById(R.id.dateSizeEditText);
        timeSizeEditText = (EditText) findViewById(R.id.timeSizeEditText);
        epochSizeEditText = (EditText) findViewById(R.id.epochSizeEditText);

        clockCheckBox = (CheckBox) findViewById(R.id.clockCheckBox);
        markerCheckBox = (CheckBox) findViewById(R.id.markerCheckBox);
        tzCheckBox = (CheckBox) findViewById(R.id.tzCheckBox);
        dateCheckBox = (CheckBox) findViewById(R.id.dateCheckBox);
        timeCheckBox = (CheckBox) findViewById(R.id.timeCheckBox);
        epochCheckBox = (CheckBox) findViewById(R.id.epochCheckBox);

        useShortCardsCheckBox = (CheckBox) findViewById(R.id.useShortCardsCheckBox);

        Button applyButton = (Button) findViewById(R.id.applyButton);
        Button resetButton = (Button) findViewById(R.id.resetButton);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                showNumberPicker((EditText) v);
            }
        };

        clockSizeEditText.setOnClickListener(listener);
        markerSizeEditText.setOnClickListener(listener);
        tzSizeEditText.setOnClickListener(listener);
        dateSizeEditText.setOnClickListener(listener);
        timeSizeEditText.setOnClickListener(listener);
        epochSizeEditText.setOnClickListener(listener);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                resetValues();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveValues();
                pushValuesToWearable();
            }
        });

        loadValues();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    private void showNumberPicker(final EditText view) {
        RelativeLayout layout = new RelativeLayout(this);
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(99);
        numberPicker.setValue(Integer.parseInt(view.getText().toString()));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        RelativeLayout.LayoutParams numPickerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        numPickerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        layout.setLayoutParams(params);
        layout.addView(numberPicker, numPickerParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.font_size);
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                view.setText(String.valueOf(numberPicker.getValue()));
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void saveValues() {
        CompanionSettings
                .setInt(this, CompanionSettings.KEY_CLOCK_SIZE, Integer.parseInt(
                        clockSizeEditText.getText().toString()));
        CompanionSettings.setInt(this, CompanionSettings.KEY_MARKER_SIZE,
                Integer.parseInt(markerSizeEditText.getText().toString()));
        CompanionSettings.setInt(this, CompanionSettings.KEY_TZ_SIZE,
                Integer.parseInt(tzSizeEditText.getText().toString()));
        CompanionSettings.setInt(this, CompanionSettings.KEY_DATE_SIZE,
                Integer.parseInt(dateSizeEditText.getText().toString()));
        CompanionSettings.setInt(this, CompanionSettings.KEY_TIME_SIZE,
                Integer.parseInt(timeSizeEditText.getText().toString()));
        CompanionSettings.setInt(this, CompanionSettings.KEY_EPOCH_SIZE,
                Integer.parseInt(epochSizeEditText.getText().toString()));

        CompanionSettings.setBoolean(this, CompanionSettings.KEY_CLOCK_DIM,
                clockCheckBox.isChecked());
        CompanionSettings.setBoolean(this, CompanionSettings.KEY_MARKER_DIM,
                markerCheckBox.isChecked());
        CompanionSettings
                .setBoolean(this, CompanionSettings.KEY_TZ_DIM, tzCheckBox.isChecked());
        CompanionSettings
                .setBoolean(this, CompanionSettings.KEY_DATE_DIM, dateCheckBox.isChecked());
        CompanionSettings
                .setBoolean(this, CompanionSettings.KEY_TIME_DIM, timeCheckBox.isChecked());
        CompanionSettings
                .setBoolean(this, CompanionSettings.KEY_EPOCH_DIM, epochCheckBox.isChecked());
        CompanionSettings.setBoolean(this, CompanionSettings.KEY_USE_SHORT_CARDS,
                useShortCardsCheckBox.isChecked());
    }

    private void loadValues() {
        clockSizeEditText.setText(String.valueOf(
                CompanionSettings.getInt(this, CompanionSettings.KEY_CLOCK_SIZE,
                        CompanionSettings.KEY_CLOCK_SIZE_DEF)));
        markerSizeEditText.setText(String.valueOf(
                CompanionSettings.getInt(this, CompanionSettings.KEY_MARKER_SIZE,
                        CompanionSettings.KEY_MARKER_SIZE_DEF)));
        tzSizeEditText.setText(String.valueOf(
                CompanionSettings
                        .getInt(this, CompanionSettings.KEY_TZ_SIZE,
                                CompanionSettings.KEY_TZ_SIZE_DEF)));
        dateSizeEditText.setText(String.valueOf(
                CompanionSettings
                        .getInt(this, CompanionSettings.KEY_DATE_SIZE,
                                CompanionSettings.KEY_DATE_SIZE_DEF)));
        timeSizeEditText.setText(String.valueOf(
                CompanionSettings
                        .getInt(this, CompanionSettings.KEY_TIME_SIZE,
                                CompanionSettings.KEY_TIME_SIZE_DEF)));
        epochSizeEditText.setText(String.valueOf(
                CompanionSettings.getInt(this, CompanionSettings.KEY_EPOCH_SIZE,
                        CompanionSettings.KEY_EPOCH_SIZE_DEF)));

        clockCheckBox.setChecked(
                CompanionSettings
                        .getBoolean(this, CompanionSettings.KEY_CLOCK_DIM,
                                CompanionSettings.KEY_CLOCK_DIM_DEF));
        markerCheckBox.setChecked(
                CompanionSettings
                        .getBoolean(this, CompanionSettings.KEY_MARKER_DIM,
                                CompanionSettings.KEY_MARKER_DIM_DEF));
        tzCheckBox.setChecked(
                CompanionSettings
                        .getBoolean(this, CompanionSettings.KEY_TZ_DIM,
                                CompanionSettings.KEY_TZ_DIM_DEF));
        dateCheckBox.setChecked(
                CompanionSettings.getBoolean(this, CompanionSettings.KEY_DATE_DIM,
                        CompanionSettings.KEY_DATE_DIM_DEF));
        timeCheckBox.setChecked(
                CompanionSettings
                        .getBoolean(this, CompanionSettings.KEY_TIME_DIM,
                                CompanionSettings.KEY_TIME_DIM_DEF));
        epochCheckBox.setChecked(
                CompanionSettings
                        .getBoolean(this, CompanionSettings.KEY_EPOCH_DIM,
                                CompanionSettings.KEY_EPOCH_SHOW_DEF));
        useShortCardsCheckBox.setChecked(CompanionSettings
                .getBoolean(this, CompanionSettings.KEY_USE_SHORT_CARDS,
                        CompanionSettings.KEY_USE_SHORT_CARDS_DEF));
    }

    private void resetValues() {
        CompanionSettings.resetAllPrefs(this);
        loadValues();
    }

    private void pushValuesToWearable() {
        int clockSize = Integer.parseInt(clockSizeEditText.getText().toString());
        int markerSize = Integer.parseInt(markerSizeEditText.getText().toString());
        int tzSize = Integer.parseInt(tzSizeEditText.getText().toString());
        int dateSize = Integer.parseInt(dateSizeEditText.getText().toString());
        int timeSize = Integer.parseInt(timeSizeEditText.getText().toString());
        int epochSize = Integer.parseInt(epochSizeEditText.getText().toString());

        boolean clockDim = clockCheckBox.isChecked();
        boolean markerDim = markerCheckBox.isChecked();
        boolean tzDim = tzCheckBox.isChecked();
        boolean dateDim = dateCheckBox.isChecked();
        boolean timeDim = timeCheckBox.isChecked();
        boolean epochDim = epochCheckBox.isChecked();

        boolean useShortCards = useShortCardsCheckBox.isChecked();

        PutDataMapRequest dataMap = PutDataMapRequest.create(CompanionSettings.PATH_WITH_FEATURE);

        dataMap.getDataMap().putInt(CompanionSettings.KEY_CLOCK_SIZE, clockSize);
        dataMap.getDataMap().putInt(CompanionSettings.KEY_MARKER_SIZE, markerSize);
        dataMap.getDataMap().putInt(CompanionSettings.KEY_TZ_SIZE, tzSize);
        dataMap.getDataMap().putInt(CompanionSettings.KEY_DATE_SIZE, dateSize);
        dataMap.getDataMap().putInt(CompanionSettings.KEY_TIME_SIZE, timeSize);
        dataMap.getDataMap().putInt(CompanionSettings.KEY_EPOCH_SIZE, epochSize);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_CLOCK_DIM, clockDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_MARKER_DIM, markerDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_TZ_DIM, tzDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_DATE_DIM, dateDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_TIME_DIM, timeDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_EPOCH_DIM, epochDim);
        dataMap.getDataMap().putBoolean(CompanionSettings.KEY_USE_SHORT_CARDS, useShortCards);

        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(googleApiClient, request);
    }

    private void initializeWearable() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override public void onConnected(Bundle connectionHint) {
                            // Now you can use the data layer API
                            pushValuesToWearable();
                        }

                        @Override public void onConnectionSuspended(int cause) {

                        }
                    })
                    .addOnConnectionFailedListener(
                            new GoogleApiClient.OnConnectionFailedListener() {
                                @Override public void onConnectionFailed(
                                        @NonNull ConnectionResult result) {

                                }
                            }
                    )
                    .addApi(Wearable.API)
                    .build();
        }
    }
}
