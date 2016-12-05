package io.github.xTun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "xTun";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetInfo = cm.getActiveNetworkInfo();
        if (activeNetInfo != null) {
            Log.i(TAG, "Receiver : " + activeNetInfo);
        } else {
            Log.i(TAG, "Receiver : " + "No network");
        }

    }
}