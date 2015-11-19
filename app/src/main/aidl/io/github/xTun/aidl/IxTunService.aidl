package io.github.xTun.aidl;

import io.github.xTun.aidl.Config;
import io.github.xTun.aidl.IxTunServiceCallback;

interface IxTunService {
    int getState();

    oneway void registerCallback(IxTunServiceCallback cb);
    oneway void unregisterCallback(IxTunServiceCallback cb);

    oneway void start(in Config config);
    oneway void stop();
}
