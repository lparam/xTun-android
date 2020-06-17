package io.github.xTun.aidl;

import android.os.Parcel;
import android.os.Parcelable;

import io.github.xTun.utils.Constants;

public class Config implements Parcelable {

    public boolean isGlobalProxy = true;
    public boolean isProxyApps = false;
    public boolean isBypassApps = false;

    public String profileName = Constants.DefaultProfileName;
    public String localIP = Constants.DefaultIP;
    public String server = "";
    public String password = "";
    public String route = Constants.Route.ALL;
    public String proxiedAppString = "";

    public int remotePort = Constants.DefaultPort;
    public int mtu = Constants.DefaultMTU;
    public String dns = Constants.DefaultDNS;
    public int protocol = Constants.UDP;

    public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config(boolean isGlobalProxy, boolean isProxyApps, boolean isBypassApps,
                  String profileName, String localIP, String server, String password,
                  String proxiedAppString, String route, String dns, int remotePort, int mtu, int protocol) {
        this.isGlobalProxy = isGlobalProxy;
        this.isProxyApps = isProxyApps;
        this.isBypassApps = isBypassApps;
        this.profileName = profileName;
        this.localIP = localIP;
        this.server = server;
        this.password = password;
        this.proxiedAppString = proxiedAppString;
        this.route = route;
        this.remotePort = remotePort;
        this.mtu = mtu;
        this.dns = dns;
        this.protocol = protocol;
    }

    private Config(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        isGlobalProxy = in.readInt() == 1;
        isProxyApps = in.readInt() == 1;
        isBypassApps = in.readInt() == 1;
        profileName = in.readString();
        localIP = in.readString();
        server = in.readString();
        password = in.readString();
        proxiedAppString = in.readString();
        route = in.readString();
        remotePort = in.readInt();
        mtu = in.readInt();
        dns = in.readString();
        protocol = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(isGlobalProxy ? 1 : 0);
        out.writeInt(isProxyApps ? 1 : 0);
        out.writeInt(isBypassApps ? 1 : 0);
        out.writeString(profileName);
        out.writeString(localIP);
        out.writeString(server);
        out.writeString(password);
        out.writeString(proxiedAppString);
        out.writeString(route);
        out.writeInt(remotePort);
        out.writeInt(mtu);
        out.writeString(dns);
        out.writeInt(protocol);
    }
}
