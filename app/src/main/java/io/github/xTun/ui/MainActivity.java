package io.github.xTun.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hu.akarnokd.rxjava2.async.AsyncFlowable;
import io.github.xTun.R;
import io.github.xTun.aidl.IxTunService;
import io.github.xTun.aidl.IxTunServiceCallback;
import io.github.xTun.model.Profile;
import io.github.xTun.service.xTunVpnService;
import io.github.xTun.store.ProfileManager;
import io.github.xTun.utils.ConfigUtils;
import io.github.xTun.utils.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private Switch switchButton;
    private ProgressDialog progressDialog = null;
    private PrefsFragment prefsFragment;
    private Drawer drawer;

    private Handler handler;

    private Profile currentProfile;
    private ProfileManager profileManager;
    private SharedPreferences settings;
    private Constants.State state = Constants.State.INIT;

    public enum Action {
        ADD,
        RECOVERY,
        ABOUT,
    }

    class PreferenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentProfile = profileManager.save();
            updateAdapter();
        }
    }
    private PreferenceBroadcastReceiver preferenceReceiver = new PreferenceBroadcastReceiver();

    private IxTunService vpnService;

    IxTunServiceCallback.Stub callback = new IxTunServiceCallback.Stub() {
        @Override
        public void stateChanged(int state, String msg) {
            onStateChanged(state, msg);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            vpnService = IxTunService.Stub.asInterface(service);
            try {
                vpnService.registerCallback(callback);

                if (switchButton != null) {
                    switchButton.setEnabled(true);
                }

                if (Constants.State.isAvailable(vpnService.getState())) {
                    prefsFragment.setPreferenceEnabled(true);

                } else {
                    changeSwitch(true);
                    prefsFragment.setPreferenceEnabled(false);
                }

                state = Constants.State.values()[vpnService.getState()];

            } catch (RemoteException e) {
                // ignore
            }
            if (switchButton != null) {
                switchButton.setOnCheckedChangeListener(MainActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (switchButton != null) switchButton.setEnabled(false);
            try {
                if (vpnService != null) vpnService.unregisterCallback(callback);
            } catch (RemoteException e) {
                // ignore
            }
            vpnService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        profileManager = new ProfileManager(this, settings);
        Profile profile = profileManager.getProfile(settings.getInt(Constants.Key.profileId, -1));
        currentProfile = profile != null ? profile : new Profile();

        SharedPreferences status = getSharedPreferences(Constants.Key.status, Context.MODE_PRIVATE);
        if (!status.getBoolean(getVersionName(), false)) {
            progressDialog = ProgressDialog.show(this, "", getString(R.string.initializing), true, false);
            status.edit().putBoolean(getVersionName(), true).apply();
            AsyncFlowable.toAsync(this::reset).call()
                    .observeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(o -> clearDialog());
            currentProfile = profileManager.firstCreate();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.ic_logo);

        switchButton = toolbar.findViewById(R.id.switchButton);
        switchButton.setOnCheckedChangeListener(this);

        TextView title  = toolbar.findViewById(R.id.title);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Iceland.ttf");
        if (tf != null) title.setTypeface(tf);

        ArrayList<IDrawerItem> items = getDrawerItems();

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withDrawerItems(items)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem != null) {
                        if (drawerItem instanceof ProfileDrawerItem) {
                            ProfileDrawerItem item = (ProfileDrawerItem) drawerItem;
                            int profileId = item.getProfileId();
                            updateProfile(profileId);
                            return false;
                        } else {
                            int identity = (int) drawerItem.getIdentifier();
                            if (identity == -1) return false;
                            Action action = Action.values()[identity];
                            switch (action) {
                                case ADD:
                                    addProfile();
                                    break;
                                case RECOVERY:
                                    recovery();
                                    break;
                                case ABOUT:
                                    showAbout();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    return false;
                })
                .withOnDrawerItemLongClickListener((view, position, drawerItem) -> {
                    if (drawerItem instanceof ProfileDrawerItem) {
                        ProfileDrawerItem item = (ProfileDrawerItem) drawerItem;
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(String.format(Locale.ENGLISH, getString(R.string.remove_profile), item.getName()))
                                .setCancelable(false)
                                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                                .setPositiveButton(R.string.yes, (dialog, which) -> {
                                    delProfile(item.getProfileId());
                                    dialog.dismiss();
                                })
                                .create()
                                .show();
                        return true;
                    }
                    return false;
                })
                .build();

        prefsFragment = new PrefsFragment();
        getFragmentManager().beginTransaction().replace(R.id.content, prefsFragment).commit();

        registerReceiver(preferenceReceiver, new IntentFilter(Constants.Action.UPDATE_PREFS));

        AsyncFlowable.runAsync(Schedulers.newThread(), (observer, subscription) -> attachService());
        //attachService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        detachService();
        unregisterReceiver(preferenceReceiver);
        new BackupManager(this).dataChanged();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        if (vpnService != null) {
            try {
                state = Constants.State.values()[vpnService.getState()];
                switch (state) {
                    case CONNECTED:
                        changeSwitch(true);
                        break;
                    case CONNECTING:
                        changeSwitch(true);
                        break;
                    default:
                        changeSwitch(false);
                        break;
                }
            } catch (RemoteException e) {
                // ignore
            }
            switchButton.setOnCheckedChangeListener(MainActivity.this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (switchButton != null) {
            switchButton.setOnCheckedChangeListener(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearDialog();
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (compoundButton.equals(switchButton)) {
            if (checked) {
                if (isReady()) {
                    prepareStartService();
                } else {
                    changeSwitch(false);
                }
            } else {
                serviceStop();
            }
            if (switchButton.isEnabled()) {
                switchButton.setEnabled(false);
                handler.postDelayed(() -> switchButton.setEnabled(true), 1000);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            serviceStart();
        } else {
            cancelStart();
            String TAG = "xTun";
            Log.e(TAG, "Failed to start VpnService");
        }
    }

    private void onStateChanged(int s, String m) {
        handler.post(() -> {
            if (state.ordinal() != s) {
                state = Constants.State.values()[s];
                switch (state) {
                    case CONNECTING:
                        if (progressDialog == null) {
                            progressDialog = ProgressDialog
                                    .show(MainActivity.this, "", getString(R.string.connecting), true, true);
                        }
                        prefsFragment.setPreferenceEnabled(false);
                        break;
                    case CONNECTED:
                        clearDialog();
                        changeSwitch(true);
                        prefsFragment.setPreferenceEnabled(false);
                        break;
                    case STOPPING:
                        if (progressDialog == null) {
                            progressDialog = ProgressDialog
                                    .show(MainActivity.this, "", getString(R.string.stopping), true, true);
                        }
                        break;
                    case STOPPED:
                        clearDialog();
                        changeSwitch(false);
                        if (m != null) {
                            Snackbar.make(drawer.getDrawerLayout(),
                                    String.format(getString(R.string.vpn_error), m),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        prefsFragment.setPreferenceEnabled(true);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void attachService() {
        if (vpnService == null) {
            Class s = xTunVpnService.class;
            Intent intent = new Intent(this, s);
            intent.setAction(Constants.Action.SERVICE);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, s));
        }
    }

    private void detachService() {
        if (vpnService != null) {
            try {
                vpnService.unregisterCallback(callback);
            } catch (RemoteException e) {
                // ignore
            }
            vpnService = null;
            unbindService(connection);
        }
    }

    private ArrayList<IDrawerItem> getProfileList() {
        ArrayList<IDrawerItem> items = new ArrayList<>();
        List<Profile> profiles = profileManager.getAllProfile();
        if (profiles != null) {
            for (Profile profile : profiles) {
                ProfileDrawerItem item = new ProfileDrawerItem().withName(profile.getName())
                        .withProfileId(profile.getId())
                        .withIcon(GoogleMaterial.Icon.gmd_computer)
                        .withSelectable(false);
                items.add(item);
            }
        }
        return items;
    }

    private ArrayList<IDrawerItem> getDrawerItems() {
        SectionDrawerItem profileItem = new SectionDrawerItem().withName(R.string.profiles)
                .withTextColorRes(R.color.accentColor)
                .withDivider(false);

        PrimaryDrawerItem addItem = new PrimaryDrawerItem().withName(R.string.add_profile)
                .withIdentifier(Action.ADD.ordinal())
                .withIcon(GoogleMaterial.Icon.gmd_add_circle_outline)
                .withSelectable(false);

        SectionDrawerItem settingItem = new SectionDrawerItem().withName(R.string.settings)
                .withTextColorRes(R.color.accentColor);

        PrimaryDrawerItem recoveryItem = new PrimaryDrawerItem().withName(R.string.recovery)
                .withIdentifier(Action.RECOVERY.ordinal())
                .withIcon(GoogleMaterial.Icon.gmd_restore)
                .withSelectable(false);

        PrimaryDrawerItem aboutItem = new PrimaryDrawerItem().withName(R.string.about)
                .withIdentifier(Action.ABOUT.ordinal())
                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                .withSelectable(false);

        ArrayList<IDrawerItem> items = new ArrayList<>();
        items.add(profileItem);
        items.addAll(getProfileList());
        items.add(addItem);
        items.add(settingItem);
        items.add(recoveryItem);
        items.add(aboutItem);
        return items;
    }

    private void updateAdapter() {
        ArrayList<IDrawerItem> items = getDrawerItems();
        drawer.setItems(items);
    }

    private void addProfile() {
        showProgress(getString(R.string.loading));
        Handler h = createDialogHandler();
        handler.postDelayed(() -> {
            profileManager.save();
            currentProfile = profileManager.create();
            updateAdapter();
            prefsFragment.updatePreferenceScreen(currentProfile);
            h.sendEmptyMessage(0);
        }, 600);
    }

    private void delProfile(int id) {
        profileManager.delProfile(id);
        List<Profile> profiles = profileManager.getAllProfile();
        int profileId = profiles != null && profiles.size() > 0 ? profiles.get(0).getId() : -1;
        currentProfile = profileManager.load(profileId);
        updateAdapter();
        prefsFragment.updatePreferenceScreen(currentProfile);
    }

    private void updateProfile(int id) {
        showProgress(getString(R.string.loading));
        Handler h = createDialogHandler();
        handler.postDelayed(() -> {
            currentProfile = profileManager.reload(id);
            prefsFragment.updatePreferenceScreen(currentProfile);
            h.sendEmptyMessage(0);
        }, 600);
    }

    private void showAbout() {
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        MainActivity.this.startActivity(intent);
/*        WebView web = new WebView(MainActivity.this);
        web.loadUrl("file:///android_asset/pages/about.html");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.toString());
                return true;
            }
        });
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(String.format(Locale.ENGLISH, getString(R.string.about_title), BuildConfig.VERSION_NAME))
                .setNegativeButton(getString(android.R.string.ok), null)
                .setView(web)
                .create()
                .show();*/
    }

    private Handler createDialogHandler() {
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                clearDialog();
            }
        };
    }

    private void clearDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void showProgress(String msg) {
        progressDialog = ProgressDialog.show(this, "", msg, true, false);
    }

    private void prepareStartService() {
        showProgress(getString(R.string.connecting));
        int REQUEST_CONNECT = 1;
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
        }
    }

    private void serviceStart() {
        try {
            vpnService.start(ConfigUtils.load(settings));
        } catch (RemoteException e) {
            // ignore
        }
        changeSwitch(false);
    }

    private void serviceStop() {
        if (vpnService != null) {
            try {
                vpnService.stop();
            } catch (RemoteException e) {
                // ignore
            }
        }
    }

    private void cancelStart() {
        clearDialog();
        changeSwitch(false);
    }

    private void recovery() {

    }

    private void reset() {

    }

    private String getVersionName() {
        String version;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "Package name not found";
        }
        return version;
    }

    private boolean isTextEmpty(String s, String msg) {
        if (s == null || s.length() <= 0) {
            Snackbar.make(drawer.getDrawerLayout(), msg, Snackbar.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private boolean checkText(String key) {
        String text = settings.getString(key, "");
        switch (key) {
            case Constants.Key.localIP:
                return !isTextEmpty(text, getString(R.string.localIP_empty));

            case Constants.Key.server:
                return !isTextEmpty(text, getString(R.string.server_empty));

            case Constants.Key.password:
                return !isTextEmpty(text, getString(R.string.password_empty));
        }
        return false;
    }

    private boolean checkNumber(String key) {
        String text = settings.getString(key, "");
        switch (key) {
            case Constants.Key.mtu:
                return !isTextEmpty(text, getString(R.string.localIP_empty));

            case Constants.Key.remotePort:
                return !isTextEmpty(text, getString(R.string.port_empty));
        }
        return false;
    }

    private boolean isReady() {
        return checkText(Constants.Key.localIP) &&
                checkText(Constants.Key.server) &&
                checkText(Constants.Key.password) &&
                checkNumber(Constants.Key.remotePort) &&
                checkNumber(Constants.Key.mtu) &&
                vpnService != null;
    }

    private void changeSwitch(Boolean checked) {
        switchButton.setOnCheckedChangeListener(null);
        switchButton.setChecked(checked);
        if (switchButton.isEnabled()) {
            switchButton.setEnabled(false);
            handler.postDelayed(() -> switchButton.setEnabled(true), 1000);
        }
        switchButton.setOnCheckedChangeListener(this);
    }
}
