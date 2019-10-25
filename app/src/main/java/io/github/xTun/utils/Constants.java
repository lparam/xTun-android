package io.github.xTun.utils;

public class Constants {
    public static final int DefaultMTU = 1426;
    public static final int DefaultPort = 1082;
    public static final int TCP = 1;
    public static final int UDP = 2;
    public static final String DefaultIP = "10.0.0.3";
    public static final String DefaultProfileName = "Untitled";

    public class Route {
        public static final String ALL = "all";
    }

    public enum State {
        INIT,
        CONNECTING,
        CONNECTED,
        STOPPING,
        STOPPED;

        public static boolean isAvailable(int state) {
            return state != CONNECTED.ordinal() && state != CONNECTING.ordinal();
        }
    }

    public class Path {
        public static final String BASE = "/data/data/io.github.xTun/";
    }

    public class Action {
        public static final String SERVICE = "io.github.xTun.SERVICE";
        public static final String CLOSE = "io.github.xTun.CLOSE";
        public static final String UPDATE_PREFS = "io.github.xTun.ACTION_UPDATE_PREFS";
    }

    public class Key {
        public static final String profileId = "profileId";
        public static final String profileName = "profileName";

        public static final String proxied = "Proxyed";

        public static final String status = "status";
        public static final String proxyApps = "proxyApps";
        public static final String route = "route";
        public static final String protocol = "protocol";

        public static final String isAutoConnect = "isAutoConnect";

        public static final String isGlobalProxy = "isGlobalProxy";
        public static final String isProxyApps = "isProxyApps";
        public static final String isBypassApps = "isBypassApps";

        public static final String localIP = "localIP";
        public static final String server = "server";
        public static final String password = "password";
        public static final String remotePort = "remotePort";
        public static final String mtu = "mtu";
    }

    public class Scheme {
        public static final String APP = "app://";
    }

}

