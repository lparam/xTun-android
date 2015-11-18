package io.github.xTun.aidl;

import io.github.xTun.aidl.Config;
import io.github.xTun.aidl.IXtunServiceCallback;

interface IXtunService {
    int getState();

    oneway void registerCallback(IXtunServiceCallback cb);
    oneway void unregisterCallback(IXtunServiceCallback cb);

    oneway void start(in Config config);
    oneway void stop();
}
