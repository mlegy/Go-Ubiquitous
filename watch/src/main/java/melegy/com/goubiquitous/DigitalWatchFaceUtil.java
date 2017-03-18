
package melegy.com.goubiquitous;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by ahmad on 3/18/17.
 */

class DigitalWatchFaceUtil {
    /**
     * The {@link DataMap} key for {@link SunShineWatchFace} background color name. The color name
     * must be a {@link String} recognized by {@link Color#parseColor}.
     */
    static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    /**
     * The {@link DataMap} key for {@link SunShineWatchFace} hour digits color name. The color name
     * must be a {@link String} recognized by {@link Color#parseColor}.
     */
    static final String KEY_HOURS_COLOR = "HOURS_COLOR";
    /**
     * The {@link DataMap} key for {@link SunShineWatchFace} minute digits color name. The color
     * name must be a {@link String} recognized by {@link Color#parseColor}.
     */
    static final String KEY_MINUTES_COLOR = "MINUTES_COLOR";
    /**
     * The {@link DataMap} key for {@link SunShineWatchFace} second digits color name. The color
     * name must be a {@link String} recognized by {@link Color#parseColor}.
     */
    static final String KEY_SECONDS_COLOR = "SECONDS_COLOR";
    /**
     * The path for the {@link DataItem} containing {@link SunShineWatchFace} configuration.
     */
    static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";
    private static final String TAG = "DigitalWatchFaceUtil";
    /**
     * Name of the default interactive mode background color and the ambient mode background color.
     */
    private static final String COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "Black";
    static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND = parseColor(
            COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);

    /**
     * Name of the default interactive mode hour digits color and the ambient mode hour digits
     * color.
     */
    private static final String COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS = "White";
    static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS = parseColor(
            COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS);

    /**
     * Name of the default interactive mode minute digits color and the ambient mode minute digits
     * color.
     */
    private static final String COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS = "White";
    static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS = parseColor(
            COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);

    /**
     * Name of the default interactive mode second digits color and the ambient mode second digits
     * color.
     */
    private static final String COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS = "Gray";
    static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS = parseColor(
            COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

    private DigitalWatchFaceUtil() {
    }

    private static int parseColor(String colorName) {
        return Color.parseColor(colorName.toLowerCase());
    }

    /**
     * Asynchronously fetches the current config {@link DataMap} for {@link SunShineWatchFace} and
     * passes it to the given callback.
     * <p>
     * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
    static void fetchConfigDataMap(final GoogleApiClient client,
            final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(DigitalWatchFaceUtil.PATH_WITH_FEATURE)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                });
    }

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with the
     * ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist, it's
     * created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
            final DataMap configKeysToOverwrite) {

        DigitalWatchFaceUtil.fetchConfigDataMap(googleApiClient,
                new FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        DigitalWatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                });
    }

    /**
     * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}. If
     * the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        putDataMapRequest.setUrgent();
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

    /**
     * Callback interface to perform an action with the current config {@link DataMap} for
     * {@link SunShineWatchFace}.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link DataMap} for {@link SunShineWatchFace}.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        DataItemResultCallback(FetchConfigDataMapCallback callback) {
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
}
