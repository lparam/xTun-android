package io.github.xTun.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;

import io.github.xTun.model.Profile;
import io.github.xTun.model.Profiles;
import io.github.xTun.utils.Constants;

public class ProfileManager {
    private SharedPreferences settings;
    private final Context context;
    private String TAG = "xTun";
    private Profiles profiles;
    private String filename = "profiles.json";


    public ProfileManager(final Context context, SharedPreferences settings) {
        this.context = context;
        this.settings = settings;
        profiles = reloadAll();
    }

    private Profile loadFromPreferences(Profile profile) {
        int id = settings.getInt(Constants.Key.profileId, -1);
        profile.setId(id);
        profile.setGlobal(settings.getBoolean(Constants.Key.isGlobalProxy, false));
        profile.setProxyApps(settings.getBoolean(Constants.Key.isProxyApps, false));
        profile.setBypass(settings.getBoolean(Constants.Key.isBypassApps, false));
        profile.setName(settings.getString(Constants.Key.profileName, "Default"));
        profile.setLocalIP(settings.getString(Constants.Key.localIP, ""));
        profile.setHost(settings.getString(Constants.Key.server, ""));
        profile.setPassword(settings.getString(Constants.Key.password, ""));
        profile.setRoute(settings.getString(Constants.Key.route, "all"));

        try {
            profile.setRemotePort(Integer.valueOf(settings.getString(Constants.Key.remotePort, Integer.toString(Constants.DefaultPort))));
        } catch (NumberFormatException ex) {
            profile.setRemotePort(Constants.DefaultPort);
        }

        int defaultMTU = Constants.DefaultMTU;
        try {
            profile.setMTU(Integer.valueOf(settings.getString(Constants.Key.mtu, Integer.toString(defaultMTU))));
        } catch (NumberFormatException ex) {
            profile.setMTU(defaultMTU);
        }

        int defaultProtocol = Constants.UDP;
        try {
            profile.setProtocol(Integer.valueOf(settings.getString(Constants.Key.protocol, Integer.toString(defaultProtocol))));
        } catch (NumberFormatException ex) {
            profile.setProtocol(defaultProtocol);
        }

        profile.setIndividual(settings.getString(Constants.Key.proxied, ""));

        return profile;
    }

    private void setPreferences(Profile profile) {
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(Constants.Key.isProxyApps, profile.isProxyApps());
        edit.putBoolean(Constants.Key.isBypassApps, profile.isBypass());
        edit.putString(Constants.Key.profileName, profile.getName());
        edit.putString(Constants.Key.localIP, profile.getLocalIP());
        edit.putString(Constants.Key.server, profile.getHost());
        edit.putString(Constants.Key.password, profile.getPassword());
        edit.putString(Constants.Key.remotePort, Integer.toString(profile.getRemotePort()));
        edit.putString(Constants.Key.mtu, Integer.toString(profile.getMTU()));
        edit.putString(Constants.Key.protocol, Integer.toString(profile.getProtocol()));
        edit.putString(Constants.Key.proxied, profile.getIndividual());
        edit.putInt(Constants.Key.profileId, profile.getId());
        edit.putString(Constants.Key.route, profile.getRoute());
        edit.apply();
    }

    public List<Profile> getAllProfile() {
        profiles = reloadAll();
        return profiles == null ? null : profiles.getProfiles();
    }

    private Profiles reloadAll() {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            if (json.isEmpty()) {
                return new Profiles();
            }
            Gson gson = new Gson();
            return gson.fromJson(json, Profiles.class);

        } catch (Exception e) {
            Log.e(TAG, "reload all profile from json file");
            return new Profiles();
        }
    }

    private void createProfile(Profile profile) {
        profiles.addProfile(profile);
        saveAll();
        profiles = reloadAll();
    }

    public Profile firstCreate() {
        Profile profile = new Profile();
        profile = loadFromPreferences(profile);
        int nextId = profiles == null ? 1 : profiles.getMaxId() + 1;
        if (nextId > 1) return profile;
        profile.setId(nextId);
        createProfile(profile);
        setPreferences(profile);
        return profile;
    }

    public Profile create() {
        Profile profile = new Profile();
        int nextId = profiles.getMaxId() + 1;
        profile.setId(nextId);
        createProfile(profile);
        setPreferences(profile);
        return profile;
    }

    private void saveAll() {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(profiles);
            FileOutputStream outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(json.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "save profile", e);
        }
    }

    public Profile save() {
        int id = settings.getInt(Constants.Key.profileId, -1);
        if (profiles != null) {
            Profile profile = profiles.getProfile(id);
            if (profile != null) {
                profile = loadFromPreferences(profile);
                saveAll();
            }
            return profile;
        } else {
            return null;
        }
    }

    public Profile getProfile(int id) {
        return profiles == null ? null : profiles.getProfile(id);
    }

    public void delProfile(int id) {
        try {
            profiles.remove(id);
            saveAll();
        } catch (Exception ex) {
            Log.e(TAG, "delete profile", ex);
        }
    }

    public Profile load(int id) {
        Profile profile = profiles.getProfile(id);
        if (profile == null) {
            profile = create();
        }
        setPreferences(profile);
        return profile;
    }

    public Profile reload(int id) {
        save();
        return load(id);
    }
}
