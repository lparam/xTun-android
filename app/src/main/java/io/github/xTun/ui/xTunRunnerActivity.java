package io.github.xTun.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Objects;

import io.github.xTun.utils.ConfigUtils;
import io.github.xTun.aidl.IxTunService;
import io.github.xTun.service.xTunVpnService;
import io.github.xTun.utils.Constants;

public class xTunRunnerActivity extends Activity {

    private static final String TAG = "xTun";

    private SharedPreferences settings = null;
    private BroadcastReceiver receiver;
    private IxTunService bgService  = null;

    Handler handler = new Handler();

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bgService = IxTunService.Stub.asInterface(service);
            handler.postDelayed(xTunRunnerActivity.this::startBackgroundService, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bgService = null;
        }
    };

    private void startBackgroundService() {
        Intent intent = VpnService.prepare(xTunRunnerActivity.this);
        int REQUEST_CONNECT = 1;
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
        }
    }

    private void attachService() {
        if (bgService == null) {
            Intent intent = new Intent(this, xTunVpnService.class);
            intent.setAction(Constants.Action.SERVICE);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, xTunVpnService.class));
        }
    }

    private void detachService() {
        if (bgService != null) {
            unbindService(connection);
            bgService = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km.inKeyguardRestrictedInputMode();
        if (locked) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(Intent.ACTION_USER_PRESENT)) {
                        attachService();
                    }
                }
            };
            registerReceiver(receiver, filter);
        } else {
            attachService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachService();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (bgService != null) {
                if (settings == null) {
                    settings = PreferenceManager.getDefaultSharedPreferences(this);
                }
                try {
                    bgService.start(ConfigUtils.load(settings));
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to start VpnService");
                }
            }
        } else {
            Log.e(TAG, "Failed to start VpnService");
        }
        finish();
    }
}
