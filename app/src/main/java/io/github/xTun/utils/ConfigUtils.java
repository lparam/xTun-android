package io.github.xTun.utils;

import android.content.SharedPreferences;

import io.github.xTun.aidl.Config;

public class ConfigUtils {

    public static Config load(SharedPreferences settings) {
        boolean isGlobalProxy = settings.getBoolean(Constants.Key.isGlobalProxy, false);
        boolean isProxyApps = settings.getBoolean(Constants.Key.isProxyApps, true);
        boolean isBypassApps = settings.getBoolean(Constants.Key.isBypassApps, false);

        String profileName = settings.getString(Constants.Key.profileName, "Default");
        String localIP = settings.getString(Constants.Key.localIP, "");
        String server = settings.getString(Constants.Key.server, "");
        String password = settings.getString(Constants.Key.password, "");
        String route = settings.getString(Constants.Key.route, "all");

        int remotePort = Integer.parseInt(settings.getString(Constants.Key.remotePort, Integer.toString(Constants.DefaultPort)));
        int mtu = Integer.parseInt(settings.getString(Constants.Key.mtu, Integer.toString(Constants.DefaultMTU)));
        String dns = settings.getString(Constants.Key.dns, Constants.DefaultDNS);
        int protocol = Integer.parseInt(settings.getString(Constants.Key.protocol, Integer.toString(Constants.UDP)), 16);

        String proxiedAppString = settings.getString(Constants.Key.proxied, "");

        return new Config(isGlobalProxy, isProxyApps, isBypassApps, profileName, localIP, server,
                           password, proxiedAppString, route, dns, remotePort, mtu, protocol);
    }

}
