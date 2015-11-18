package io.github.xTun.aidl;

interface IXtunServiceCallback {
    oneway void stateChanged(int state, String msg);
}
