package io.github.xTun.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import io.github.xTun.R;
import io.github.xTun.aidl.Config;
import io.github.xTun.aidl.IxTunService;
import io.github.xTun.aidl.IxTunServiceCallback;
import io.github.xTun.model.ProxyApp;
import io.github.xTun.ui.AppManagerActivity;
import io.github.xTun.ui.MainActivity;
import io.github.xTun.ui.xTunRunnerActivity;
import io.github.xTun.utils.Constants;
import io.github.xTun.utils.Utils;
import io.github.xTun.xTun;

public class xTunVpnService extends VpnService implements Handler.Callback, Runnable {
    private final String TAG = xTunVpnService.class.getSimpleName();

    private Config config = null;
    private ParcelFileDescriptor vpnInterface;

    private BroadcastReceiver closeReceiver = null;
    private Constants.State state = Constants.State.INIT;
    private int callbackCount = 0;
    private final RemoteCallbackList<IxTunServiceCallback> callbacks = new RemoteCallbackList<>();

    private Thread vpnThread;
    private Handler handler;

    // private final int FAILED = 0;
    private final int CONNECTED = 1;
    // private final int STOPPED = 2;

    private final IxTunService.Stub binder = new IxTunService.Stub() {
        @Override
        public int getState() {
            return state.ordinal();
        }

        @Override
        public void registerCallback(IxTunServiceCallback cb) {
            if (cb != null) {
                callbacks.register(cb);
                callbackCount += 1;
            }
        }

        @Override
        public void unregisterCallback(IxTunServiceCallback cb) {
            if (cb != null ) {
                callbacks.unregister(cb);
                callbackCount -= 1;
            }
            if (callbackCount == 0
                    && state != Constants.State.CONNECTING
                    && state != Constants.State.CONNECTED)
            {
                stopSelf();
            }
        }

        @Override
        public void start(Config config) {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                startRunner(config);
            }
        }

        @Override
        public void stop() {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                stopRunner();
            }
        }
    };

    private void notifyForegroundAlert(String title, String info) {
        String channelId = "service-vpn";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent fullScreenIntent = new Intent(this, MainActivity.class);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                    fullScreenIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification nf = new NotificationCompat.Builder(this, channelId)
                    .setContentText(info)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_logo)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(fullScreenPendingIntent)
                    .build();

            startForeground(1, nf);

        } else {
            Intent openIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
            Intent closeIntent = new Intent(Constants.Action.CLOSE);
            PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
            builder.setWhen(0)
                    .setTicker(title)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(info)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_logo)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                            getString(R.string.stop), actionIntent);

            builder.setPriority(NotificationCompat.PRIORITY_MIN);

            startForeground(1, builder.build());
        }
    }

    @Override
    public void onCreate() {
        if (handler == null) {
            handler = new Handler(this);
        }
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (VpnService.SERVICE_INTERFACE.equals(action)) {
            return super.onBind(intent);
        } else if (Constants.Action.SERVICE.equals(action)) {
            return binder;
        }
        return null;
    }

    @Override
    public void onRevoke() {
        stopRunner();
    }

    private String createDomains() {
        InputStream in;
        OutputStream out;

        in = this.getResources().openRawResource(R.raw.dns_black_list);

        try {
            out = new FileOutputStream(Constants.Path.BASE + "dns_black_list");
            Utils.copyFile(in, out);
            in.close();
            out.flush();
            out.close();

        } catch (IOException e) {
            Log.e(TAG, "Copy file error: " + e.getMessage());
            return null;
        }

        return Constants.Path.BASE + "dns_black_list";
    }

    private void startVpn() {
        String LOCAL_DNS = "114.114.114.114";
        int VPN_CIDR = 24;

        Log.i(TAG, "DNS Server: " + config.dns);

        Builder builder = new Builder();
        builder.setSession(config.profileName);
        builder.setMtu(config.mtu);
        builder.addAddress(config.localIP, VPN_CIDR);
        builder.addDnsServer(config.dns);
        builder.addRoute("114.114.115.115", 32);

        if (config.isProxyApps) {
            Log.i(TAG, "Per-App Proxy");
            ProxyApp[] apps = AppManagerActivity.getProxiedApps(this, config.proxiedAppString);
            for (ProxyApp app : apps) {
                try {
                    if (config.isBypassApps) {
                        builder.addDisallowedApplication(app.getPackageName());

                    } else {
                        builder.addAllowedApplication(app.getPackageName());
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Package not available: " + app.getPackageName(), e);
                }
            }
        }

        boolean global = !config.isProxyApps;
        if (global) {
            Log.i(TAG, "Global Proxy");
        }

        builder.addRoute("0.0.0.0", 0);

        vpnInterface = builder.establish();

        String ifconf = String.format(Locale.ENGLISH, "%s/%d", config.localIP, VPN_CIDR);
        int fd = vpnInterface.getFd();
        Log.d(TAG, "Vpn fd: " + fd);
        String domainPath = createDomains();
        boolean rc = xTun.init(this, ifconf, fd, config.mtu, config.protocol, global, false,
                               config.password, LOCAL_DNS, domainPath);
        if (rc) {
            handler.sendEmptyMessage(CONNECTED);
            xTun.start(config.server, config.remotePort);
        } else {
            int FAILED = 0;
            handler.sendEmptyMessage(FAILED);
        }
    }

    private void startRunner(Config config) {
        this.config = config;

        // register close closeReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Constants.Action.CLOSE);
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, R.string.stopping, Toast.LENGTH_SHORT).show();
                stopRunner();
            }
        };
        registerReceiver(closeReceiver, filter);

        // ensure the VPNService is prepared
        if (VpnService.prepare(this) != null) {
            Intent i = new Intent(this, xTunRunnerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }

        changeState(Constants.State.CONNECTING);

        if (config != null) {
            vpnThread = new Thread(this, "xTun VpnService");
            vpnThread.start();
        }
    }

    private void stopRunner() {
        if (vpnThread != null) {
            xTun.stop();
            Log.i(TAG, "vpn thread interrupt");
            vpnThread.interrupt();
            vpnThread = null;
        }

        stopForeground(true);

        changeState(Constants.State.STOPPING);

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                // close failed
            }
        }

        // stop the service if no callback registered
        if (callbackCount == 0) {
            stopSelf();
        }

        // clean up receiver
        if (closeReceiver != null) {
            unregisterReceiver(closeReceiver);
            closeReceiver = null;
        }

        changeState(Constants.State.STOPPED);
    }

    private void changeState(Constants.State s) {
        Handler handler = new Handler(getBaseContext().getMainLooper());
        handler.post(() -> {
            if (state != s) {
                if (callbackCount > 0) {
                    int n = callbacks.beginBroadcast();
                    for (int i = 0; i <= n - 1; i++) {
                        try {
                            callbacks.getBroadcastItem(i).stateChanged(s.ordinal(), null);
                        } catch (RemoteException e) {
                            // Ignore
                        }
                    }
                    callbacks.finishBroadcast();
                }
                state = s;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == CONNECTED) {
            notifyForegroundAlert(getString(R.string.forward_success), getString(R.string.service_running, config.profileName));
            changeState(Constants.State.CONNECTED);
        }
        return true;
    }

    @Override
    public void run() {
        startVpn();
    }

    public boolean protectSocket(int socket) {
        boolean rc = protect(socket);
        if (!rc) {
            Log.e(TAG, "Protect socket failed.");
        }
        return rc;
    }
}