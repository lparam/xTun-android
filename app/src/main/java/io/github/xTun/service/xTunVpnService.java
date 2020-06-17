package io.github.xTun.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private String TAG = xTunVpnService.class.getSimpleName();

    private Config config = null;
    private ParcelFileDescriptor vpnInterface;

    private BroadcastReceiver closeReceiver = null;
    private Constants.State state = Constants.State.INIT;
    private int callbackCount = 0;
    private final RemoteCallbackList<IxTunServiceCallback> callbacks = new RemoteCallbackList<>();

    private Thread vpnThread;
    private Handler handler;

    private Builder builder;

    private final int FAILED = 0;
    private final int CONNECTED = 1;
    private final int STOPPED = 2;

    private IxTunService.Stub binder = new IxTunService.Stub() {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "service-vpn";
            String channelName = getString(R.string.service_vpn);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = nm.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            }
            nm.createNotificationChannel(channel);
            if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel.getId());
                startActivity(intent);
                Toast.makeText(this, getString(R.string.notify_tips), Toast.LENGTH_SHORT).show();

            } else {
                Notification nf = new Notification.Builder(this, channelId)
                        .setContentText(info)
                        .setSettingsText(getString(R.string.settings_tips))
                        .setContentTitle(getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_logo)
                        .build();

                Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);
                nf.fullScreenIntent = intent;

                nf.flags |= Notification.FLAG_AUTO_CANCEL;
                nm.notify(1, nf);
                startForeground(1, nf);
            }

        } else {
            Intent openIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, 0);
            Intent closeIntent = new Intent(Constants.Action.CLOSE);
            PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
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

    private void route(Builder builder, int rid) {
        String line;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getResources().openRawResource(rid)))) {
            while ((line = reader.readLine()) != null) {
                final String[] route = line.split("/");
                if (route.length == 2) {
                    builder.addRoute(route[0], Integer.parseInt(route[1]));
                }
            }

        } catch (final Throwable t) {
            Log.e(TAG, "", t);

        }
        // ignore
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
        boolean verbose = false;

        Log.i(TAG, "DNS Server: " + config.dns);

        builder = new Builder();
        builder.setSession(config.profileName);
        builder.setMtu(config.mtu);
        builder.addAddress(config.localIP, VPN_CIDR);
        builder.addDnsServer(config.dns);
        builder.addRoute("114.114.115.115", 32);

        try {
            if (config.isProxyApps) {
                Log.i(TAG, "Per-App Proxy");
                ProxyApp[] apps = AppManagerActivity.getProxiedApps(this, config.proxiedAppString);
                for (ProxyApp app : apps) {
                    if (config.isBypassApps) {
                        builder.addDisallowedApplication(app.getPackageName());

                    } else {
                        builder.addAllowedApplication(app.getPackageName());
                    }
                }
            }

        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, ex.getMessage());
        }

        boolean global = !config.isProxyApps;
        if (global) {
            Log.i(TAG, "Global Proxy");
        }

        builder.addRoute("0.0.0.0", 0);

        vpnInterface = builder.establish();

        String ifconf = String.format(Locale.ENGLISH, "%s/%d", config.localIP, VPN_CIDR);
        int fd = vpnInterface.getFd();
        String domainPath = createDomains();
        boolean rc = xTun.init(this, ifconf, fd, config.mtu, config.protocol, global, verbose,
                                config.password, LOCAL_DNS, domainPath);
        if (rc) {
            handler.sendEmptyMessage(CONNECTED);
            xTun.start(config.server, config.remotePort);
        } else {
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
        changeState(s, null);
    }

    private void changeState(Constants.State s, String msg) {
        Handler handler = new Handler(getBaseContext().getMainLooper());
        handler.post(() -> {
            if (state != s) {
                if (callbackCount > 0) {
                    int n = callbacks.beginBroadcast();
                    for (int i = 0; i <= n - 1; i++) {
                        try {
                            callbacks.getBroadcastItem(i).stateChanged(s.ordinal(), msg);
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
        switch (msg.what) {
            case CONNECTED:
                notifyForegroundAlert(getString(R.string.forward_success), getString(R.string.service_running, config.profileName));
                changeState(Constants.State.CONNECTED);
                break;
            case STOPPED:
                // clean up
                break;
        }

        return true;
    }

    @Override
    public void run() {
        boolean resolved = false;
        if (!Utils.isIPv4Address(config.server) && !Utils.isIPv6Address(config.server)) {
            String host = config.server;
            String addr = Utils.resolve(config.server, false);
            if (addr != null) {
                config.server = addr;
                resolved = true;
            }
            Log.i(TAG, "resolved " + host + ": " + addr);

        } else {
            resolved = true;
        }

        if (resolved) {
            startVpn();
        } else {
            changeState(Constants.State.STOPPED, getString(R.string.service_failed));
            stopRunner();
        }
    }

    public boolean protectSocket(int socket) {
        boolean rc = protect(socket);
        if (!rc) {
            Log.e(TAG, "Protect socket failed.");
        }
        return rc;
    }
}