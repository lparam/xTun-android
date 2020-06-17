package io.github.xTun.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import io.github.xTun.preferences.SummaryEditTextPreference;
import io.github.xTun.R;
import io.github.xTun.model.Profile;
import io.github.xTun.preferences.PasswordEditTextPreference;
import io.github.xTun.preferences.ProfileEditTextPreference;
import io.github.xTun.utils.Constants;

public class PrefsFragment extends PreferenceFragment {
    /**
     *
     */
    private SwitchPreference isProxyApps;

    public static String[] CLIENT_PREFS = {
            Constants.Key.localIP,
            Constants.Key.mtu,
    };

    public static String[] SERVER_PREFS = {
            Constants.Key.profileName,
            Constants.Key.server,
            Constants.Key.remotePort,
            Constants.Key.protocol,
            Constants.Key.password,
            Constants.Key.dns,
    };

    public static String[] FEATURE_PREFS = {
            Constants.Key.route,
            Constants.Key.isGlobalProxy,
            Constants.Key.proxyApps,
            Constants.Key.isAutoConnect
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        isProxyApps = (SwitchPreference) findPreference(Constants.Key.proxyApps);
        isProxyApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(preference.getContext(), AppManagerActivity.class);
            startActivity(intent);
            isProxyApps.setChecked(true);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        isProxyApps.setChecked(prefs.getBoolean(Constants.Key.isProxyApps, false));
    }

    public void setPreferenceEnabled(boolean enabled) {
        for (String name : CLIENT_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }
        for (String name : SERVER_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }
        for (String name : FEATURE_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                if (name.equals(Constants.Key.isGlobalProxy) || name.equals(Constants.Key.proxyApps)) {
                    pref.setEnabled(enabled);

                } else {
                    pref.setEnabled(enabled);
                }
            }
        }
    }

    public void updatePreferenceScreen(Profile profile) {
        for (String name : CLIENT_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                updatePreference(pref, name, profile);
            }
        }
        for (String name : SERVER_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                updatePreference(pref, name, profile);
            }
        }
        for (String name : FEATURE_PREFS) {
            Preference pref = findPreference(name);
            if (pref != null) {
                updatePreference(pref, name, profile);
            }
        }
    }

    private void updateListPreference(Preference pref, String value) {
        ((ListPreference)pref).setValue(value);
    }

    private void updatePasswordEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((PasswordEditTextPreference)pref).setText(value);
    }

    private void updateSummaryEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((SummaryEditTextPreference)pref).setText(value);
    }

    private void updateProfileEditTextPreference(Preference pref, String value) {
        ((ProfileEditTextPreference)pref).resetSummary(value);
        ((ProfileEditTextPreference)pref).setText(value);
    }

    private void updateCheckBoxPreference(Preference pref, boolean value) {
        ((CheckBoxPreference)pref).setChecked(value);
    }

    public void updatePreference(Preference pref, String name, Profile profile) {
        switch (name) {
            case Constants.Key.profileName: updateProfileEditTextPreference(pref, profile.getName()); break;
            case Constants.Key.localIP: updateSummaryEditTextPreference(pref, profile.getLocalIP()); break;
            case Constants.Key.server: updateSummaryEditTextPreference(pref, profile.getHost()); break;
            case Constants.Key.remotePort: updateSummaryEditTextPreference(pref, Integer.toString(profile.getRemotePort())); break;
            case Constants.Key.mtu: updateSummaryEditTextPreference(pref, Integer.toString(profile.getMTU())); break;
            case Constants.Key.password: updatePasswordEditTextPreference(pref, profile.getPassword()); break;
            case Constants.Key.dns: updateSummaryEditTextPreference(pref, profile.getDNS()); break;
            case Constants.Key.protocol: updateListPreference(pref, Integer.toString(profile.getProtocol())); break;
            case Constants.Key.route: updateListPreference(pref, profile.getRoute()); break;
            case Constants.Key.isGlobalProxy: updateCheckBoxPreference(pref, profile.isGlobal()); break;
            default: break;
        }
    }

}
