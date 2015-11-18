package io.github.xTun;

import io.github.xTun.service.XtunVpnService;

public class xTun {
    static {
        java.lang.System.loadLibrary("xTun");
    }

    public static native boolean init(XtunVpnService service, int fd, int mtu,
                                      boolean isGlobalProxy, boolean verbose,
                                      String server, String password, String dns, String domainPath);
    public static native void start();
    public static native void stop();
}