package io.github.xTun.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class Config implements Parcelable {

    public boolean isGlobalProxy = true;
    public boolean isBypassApps = false;

    public String profileName = "Untitled";
    public String localIP = "10.0.0.3";
    public String server = "";
    public String password = "";
    public String route = "all";
    public String proxiedAppString = "";

    public int remotePort = 1082;
    public int mtu = 1440;

    public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config(boolean isGlobalProxy, boolean isBypassApps,
                  String profileName, String localIP, String server, String password,
                  String proxiedAppString, String route, int remotePort, int mtu) {
        this.isGlobalProxy = isGlobalProxy;
        this.isBypassApps = isBypassApps;
        this.profileName = profileName;
        this.localIP = localIP;
        this.server = server;
        this.password = password;
        this.proxiedAppString = proxiedAppString;
        this.route = route;
        this.remotePort = remotePort;
        this.mtu = mtu;
    }

    private Config(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        isGlobalProxy = in.readInt() == 1;
        isBypassApps = in.readInt() == 1;
        profileName = in.readString();
        localIP = in.readString();
        server = in.readString();
        password = in.readString();
        proxiedAppString = in.readString();
        route = in.readString();
        remotePort = in.readInt();
        mtu = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(isGlobalProxy ? 1 : 0);
        out.writeInt(isBypassApps ? 1 : 0);
        out.writeString(profileName);
        out.writeString(localIP);
        out.writeString(server);
        out.writeString(password);
        out.writeString(proxiedAppString);
        out.writeString(route);
        out.writeInt(remotePort);
        out.writeInt(mtu);
    }
}
