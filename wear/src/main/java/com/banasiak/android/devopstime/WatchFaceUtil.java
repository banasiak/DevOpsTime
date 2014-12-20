/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2104 Richard Banasiak
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public final class WatchFaceUtil {

    private static final String TAG = WatchFaceUtil.class.getSimpleName();

    public static final String KEY_CLOCK_SIZE = "clock_size";

    public static final int KEY_CLOCK_SIZE_DEF = 50;

    public static final String KEY_CLOCK_DIM = "clock_dim";

    public static final boolean KEY_CLOCK_DIM_DEF = true;

    public static final String KEY_MARKER_SIZE = "marker_size";

    public static final int KEY_MARKER_SIZE_DEF = 16;

    public static final String KEY_MARKER_DIM = "marker_dim";

    public static final boolean KEY_MARKER_DIM_DEF = true;

    public static final String KEY_TZ_SIZE = "tz_size";

    public static final int KEY_TZ_SIZE_DEF = 16;

    public static final String KEY_TZ_DIM = "tz_dim";

    public static final boolean KEY_TZ_DIM_DEF = false;

    public static final String KEY_DATE_SIZE = "date_size";

    public static final int KEY_DATE_SIZE_DEF = 18;

    public static final String KEY_DATE_DIM = "date_dim";

    public static final boolean KEY_DATE_DIM_DEF = false;

    public static final String KEY_TIME_SIZE = "time_size";

    public static final int KEY_TIME_SIZE_DEF = 18;

    public static final String KEY_TIME_DIM = "time_dim";

    public static final boolean KEY_TIME_DIM_DEF = false;

    public static final String KEY_EPOCH_SIZE = "epoch_size";

    public static final int KEY_EPOCH_SIZE_DEF = 18;

    public static final String KEY_EPOCH_DIM = "epoch_dim";

    public static final boolean KEY_EPOCH_DIM_DEF = false;

    public static final String KEY_ALWAYS_UTC = "always_utc";

    public static final boolean KEY_ALWAYS_UTC_DEF = false;

    public static final String KEY_USE_SHORT_CARDS = "use_short_cards";

    public static final boolean KEY_USE_SHORT_CARDS_DEF = true;

    public static final String PATH_WITH_FEATURE = "/DevOpsTime";


    public static int getInt(final Context context, final String key, final int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
    }

    public static boolean getBoolean(final Context context, final String key,
            final boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static void setInt(final Context context, final String key, final int value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void setBoolean(final Context context, final String key, final boolean value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void resetAllPrefs(final Context context) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();

        editor.clear();
        editor.commit();
    }

    // Callback interface to perform an action with the current config DataMap
    public interface FetchConfigDataMapCallback {

        // Callback invoked with the current config DataMap
        void onConfigDataMapFetched(DataMap config);
    }

    // Asynchronously fetches the current config DataMap and passes it to the given callback. If
    // the current config DataItem doesn't exist, it isn't created and the callback receives an
    // empty DataMap.
    public static void fetchConfigDataMap(final GoogleApiClient client,
            final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(WatchFaceUtil.PATH_WITH_FEATURE)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    // Overwrites (or sets, if not present) the keys in the current config DataItem with the ones
    // appearing in the given DataMap. If the config DataItem doesn't exist, it's created. It is
    // allowed that only some of the keys used in the config DataItem appear in
    // configKeysToOverwrite. The rest of the keys remains unmodified in this case.
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
            final DataMap configKeysToOverwrite) {

        WatchFaceUtil.fetchConfigDataMap(googleApiClient,
                new FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        WatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                }
        );
    }


    // Overwrites the current config DataItem's DataMap with newConfig. If the config DataItem
    // doesn't exist, it's created.
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                        }
                    }
                });
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    // singleton
    private WatchFaceUtil() {
    }
}
