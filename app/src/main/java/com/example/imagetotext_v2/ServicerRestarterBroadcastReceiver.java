package com.example.imagetotext_v2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// ref: https://medium.com/@raziaranisandhu/create-services-never-stop-in-android-b5dcfc5fb4b2
// Try to keep services running all the time

public class ServicerRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(ServicerRestarterBroadcastReceiver.class.getSimpleName(),"ServiceStop!! ");
        context.startService(new Intent(context,LocationService.class));
    }
}
