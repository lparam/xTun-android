package io.github.xTun.aidl;

interface IxTunServiceCallback {
    oneway void stateChanged(int state, String msg);
}
