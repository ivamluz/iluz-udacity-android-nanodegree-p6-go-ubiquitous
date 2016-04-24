package com.example.android.sunshine.app.wearable;

import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by iluz on 4/10/16.
 */
public class WearableWeatherService extends WearableListenerService {
    private static final String LOG_TAG = WearableWeatherService.class.getSimpleName();

//    private static final String sWeatherPath = "/weather";
    private static final String WEATHER_PATH = "/weather";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(LOG_TAG, "onDataChanged - dataEvents: " + dataEvents);

        if (null == dataEvents || dataEvents.getCount() == 0) {
            return;
        }

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                String path = dataEvent.getDataItem().getUri().getPath();
                Log.d(LOG_TAG, "Path: " + path);

                if (path.equals(WEATHER_PATH)) {
                    SunshineSyncAdapter.syncImmediately(this);
                }
            }
        }
    }
}
