package io.github.xTun.model;

public class ProxyApp {
    private int uid;
    private String name;
    private String packageName;
    private boolean proxy;

    public ProxyApp(int uid, String name, String packageName, boolean proxied) {
        this.uid = uid;
        this.name = name;
        this.packageName = packageName;
        this.proxy = proxied;
    }

    public int getId() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean getProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }
}
