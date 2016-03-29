package io.github.xTun;

import io.github.xTun.service.xTunVpnService;

public class xTun {
    static {
        java.lang.System.loadLibrary("xTun");
    }

    public static native boolean init(xTunVpnService service, String ifconfig, int fd,
                                          int mtu, int protocol, boolean isGlobalProxy,
                                          boolean verbose, String server, int port,
                                          String password, String dns, String domainPath);
    public static native void start();
    public static native void stop();
}