/*
 * Copyright (C) 2014 Richard Banasiak
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CompanionSettings {

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

//    public static final String KEY_TIME_DIM = "time_dim";

//    public static final boolean KEY_TIME_DIM_DEF = false;

    public static final String KEY_EPOCH_SIZE = "epoch_size";

    public static final int KEY_EPOCH_SIZE_DEF = 18;

//    public static final String KEY_EPOCH_DIM = "epoch_dim";

//    public static final boolean KEY_EPOCH_SHOW_DEF = true;

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

}
