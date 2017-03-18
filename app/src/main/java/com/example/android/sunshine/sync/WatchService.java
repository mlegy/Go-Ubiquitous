
package com.example.android.sunshine.sync;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by ahmad on 3/18/17.
 */

public class WatchService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String SERVICE_NAME = "WatchService";
    public static final String ACTION_UPDATE_WATCHFACE = "ACTION_UPDATE_WATCHFACE";

    private static final String KEY_PATH = "/weather";
    private static final String KEY_WEATHER_ID = "KEY_WEATHER_ID";
    private static final String KEY_MAX_TEMP = "KEY_MAX_TEMP";
    private static final String KEY_MIN_TEMP = "KEY_MIN_TEMP";

    private GoogleApiClient mGoogleApiClient;

    /**
     * Creates an IntentService. Invoked by your subclass's constructor.
     */
    public WatchService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null
                && intent.getAction() != null
                && intent.getAction().equals(ACTION_UPDATE_WATCHFACE)) {

            mGoogleApiClient = new GoogleApiClient.Builder(WatchService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("WatchService", "Updating the WatchFace");

        Uri weatherUri = WeatherContract.WeatherEntry
                .buildWeatherUriWithDate(System.currentTimeMillis());

        // Declare the cursor to get the data to show on the WatchFace
        Cursor c = getContentResolver().query(
                weatherUri,
                new String[] {
                        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                }, null, null, null);

        // Fetch the cursor and send to the WatchFace the extracted weather by the DataApi
        if (c != null && c.moveToFirst()) {
            int weatherId = c.getInt(c.getColumnIndex(
                    WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = SunshineWeatherUtils.formatTemperature(this, c.getDouble(
                    c.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            String minTemp = SunshineWeatherUtils.formatTemperature(this, c.getDouble(
                    c.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));

            final PutDataMapRequest mapRequest = PutDataMapRequest.create(KEY_PATH);
            mapRequest.getDataMap().putInt(KEY_WEATHER_ID, weatherId);
            mapRequest.getDataMap().putString(KEY_MAX_TEMP, maxTemp);
            mapRequest.getDataMap().putString(KEY_MIN_TEMP, minTemp);

            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(mGoogleApiClient, mapRequest.asPutDataRequest());
        }
        if (c != null) {
            c.close();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
