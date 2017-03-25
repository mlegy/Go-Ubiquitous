
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Created by ahmad on 3/18/17.
 */

public class WatchService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String SERVICE_NAME = "WatchService";
    public static final String ACTION_UPDATE_WATCHFACE = "ACTION_UPDATE_WATCHFACE";

    private static final String WEATHER_DATA_PATH = "/WEATHER_DATA_PATH";
    private static final String WEATHER_DATA_ID = "WEATHER_DATA_ID";
    private static final String WEATHER_DATA_HIGH = "WEATHER_DATA_HIGH";
    private static final String WEATHER_DATA_LOW = "WEATHER_DATA_LOW";
    private static final String LOG_TAG = "WatchService";

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
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30,
                    TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                return;
            }
            Log.i(LOG_TAG, "GoogleApiClient" + mGoogleApiClient.isConnected());
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("WatchService", "Updating the WatchFace");

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

            Log.i(LOG_TAG, "Sending weather information to android wear");

            PutDataMapRequest dataMap = PutDataMapRequest.create(WEATHER_DATA_PATH);
            dataMap.getDataMap().putString(WEATHER_DATA_HIGH, maxTemp);
            dataMap.getDataMap().putString(WEATHER_DATA_LOW, minTemp);
            dataMap.getDataMap().putLong(WEATHER_DATA_ID, weatherId);
            PutDataRequest request = dataMap.asPutDataRequest();

            Log.i(LOG_TAG, "GoogleApiClient " + mGoogleApiClient.isConnected());

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.i(LOG_TAG, "Cannot send weather information, status code: "
                                        + result.getStatus().getStatusCode());
                            } else {
                                Log.i(LOG_TAG, "Weather information was sent successfully "
                                        + result.getDataItem().getUri());
                            }
                        }
                    });
        }
        if (c != null) {
            c.close();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("onConnectionSuspended", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("onConnectionFailed", connectionResult.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
