package io.github.xTun;

import android.util.Log;

import io.github.xTun.service.xTunVpnService;

public class xTun {
    static {
        java.lang.System.loadLibrary("xTun");
    }

    public static native boolean init(xTunVpnService service, String ifconfig, int fd,
                                          int mtu, int protocol, boolean isGlobalProxy,
                                          boolean verbose,
                                          String password, String dns, String domainPath);
    public static native void start(String server, int port);
    public static native void stop();
}