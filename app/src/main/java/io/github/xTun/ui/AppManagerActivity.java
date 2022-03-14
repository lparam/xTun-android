package io.github.xTun.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import hu.akarnokd.rxjava2.async.AsyncFlowable;
import io.github.xTun.R;
import io.github.xTun.model.ProxyApp;
import io.github.xTun.utils.Constants;
import io.github.xTun.utils.Utils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class AppManagerActivity extends RxAppCompatActivity
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private boolean appsLoaded;
    private ListAdapter adapter;
    private ListView appListView;
    private ProgressDialog progressDialog;
    private final int STUB = android.R.drawable.sym_def_app_icon;
    private ProxyApp[] apps;

    private class AppIconDownloader extends BaseImageDownloader {

        AppIconDownloader(Context context, int connectTimeout, int readTimeout) {
            super(context, connectTimeout, readTimeout);
        }

        AppIconDownloader(Context context) {
            this(context, 0, 0);
        }

        @Override
        public InputStream getStreamFromOtherSource(String imageUri, Object extra) {
            String packageName = imageUri.substring(Constants.Scheme.APP.length());
            Drawable drawable = Utils.getAppIcon(getBaseContext(), packageName);
            Bitmap bitmap = Utils.drawableToBitmap(drawable);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private static class ListEntry {
        private final CheckBox box;
        private final TextView text;
        private final ImageView icon;

        ListEntry(CheckBox box, TextView text, ImageView icon) {
            this.box = box;
            this.text = text;
            this.icon = icon;
        }

        CheckBox getBox() {
            return box;
        }

        public TextView getText() {
            return text;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_ab_back_mtrl_am_alpha);
            if (upArrow != null) {
                upArrow.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
            }
            ab.setHomeAsUpIndicator(upArrow);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(this)
                        .imageDownloader(new AppIconDownloader(this))
                        .build();
        ImageLoader.getInstance().init(config);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(Constants.Key.isProxyApps, true);
        edit.apply();

        RadioGroup bypassGroup = findViewById(R.id.bypassGroup);
        bypassGroup.check(prefs.getBoolean(Constants.Key.isBypassApps, false) ? R.id.btn_bypass : R.id.btn_on);
        bypassGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.btn_off:
                    edit.putBoolean(Constants.Key.isProxyApps, false);
                    finish();
                    break;
                case R.id.btn_on:
                    edit.putBoolean(Constants.Key.isBypassApps, false);
                    break;
                case R.id.btn_bypass:
                    edit.putBoolean(Constants.Key.isBypassApps, true);
                    break;
            }
            edit.apply();
        });

        appListView = findViewById(R.id.applistview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!appsLoaded) {
            loadApps();
        }
    }

    private ProxyApp[] getApps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String proxiedAppString = prefs.getString(Constants.Key.proxied, "");
        String[] appString = new String[0];
        if (proxiedAppString != null) {
            appString = proxiedAppString.split("\\|");
        }
        Arrays.sort(appString);

        // TODO: cached apps
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> infoList = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        ArrayList<ProxyApp> appList = new ArrayList<>();

        for (PackageInfo p : infoList) {
            String  label = p.applicationInfo.loadLabel(pm).toString();
            int uid = p.applicationInfo.uid;
            if (p.requestedPermissions == null) {
                continue;
            }
            for (String perm : p.requestedPermissions) {
                if (perm.contains(Manifest.permission.INTERNET)) {
                    String userName = Integer.toString(uid);
                    int index = Arrays.binarySearch(appString, userName);
                    boolean proxied = index >= 0;
                    // trim no-break space
                    ProxyApp app = new ProxyApp(uid, label.replace("\u00A0",""), p.packageName, proxied);
                    appList.add(app);
                    break;
                }
            }
        }
        ProxyApp[] appArray = new ProxyApp[appList.size()];
        appList.toArray(appArray);

        Collator c = Collator.getInstance(Locale.CHINESE);
        Arrays.sort(appArray, (a, b) -> {
            CollationKey k1 = c.getCollationKey(a.getName());
            CollationKey k2 = c.getCollationKey(b.getName());

            if (a.getProxy()) {
                if (b.getProxy()) {
                    return k1.compareTo(k2);
                }
                return -1;

            } else if (a.getProxy() == b.getProxy()) {
                return k1.compareTo(k2);

            } else {
                return 1;
            }
        });

        return appArray;
    }

    private void loadApps() {
        progressDialog = ProgressDialog
                .show(AppManagerActivity.this, "", getString(R.string.loading), true, true);

        AsyncFlowable.toAsync(this::getApps)
                .apply(this)
                .observeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(apps -> {
                    this.apps = apps;
                    adapter = new ArrayAdapter<ProxyApp>(this, R.layout.apps_item, R.id.itemtext, apps) {
                        @NonNull
                        @Override
                        public View getView(int position, View view, @NonNull ViewGroup parent) {
                            View convertView = view;
                            ListEntry entry;
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.apps_item, parent, false);
                                TextView text = convertView.findViewById(R.id.itemtext);
                                CheckBox box = convertView.findViewById(R.id.itemcheck);
                                ImageView icon = convertView.findViewById(R.id.itemicon);
                                entry = new ListEntry(box, text, icon);
                                entry.getText().setOnClickListener(AppManagerActivity.this);
                                entry.getBox().setOnCheckedChangeListener(AppManagerActivity.this);
                                convertView.setTag(entry);

                            } else {
                                entry = (ListEntry) convertView.getTag();
                            }

                            ProxyApp app = apps[position];
                            DisplayImageOptions options =
                                    new DisplayImageOptions.Builder()
                                            .showImageOnLoading(STUB)
                                            .showImageForEmptyUri(STUB)
                                            .showImageOnFail(STUB)
                                            .resetViewBeforeLoading(true)
                                            .cacheInMemory(true)
                                            .cacheOnDisk(true)
                                            .displayer(new FadeInBitmapDisplayer(300))
                                            .build();
                            ImageLoader.getInstance().displayImage(Constants.Scheme.APP + app.getPackageName(), entry.icon, options);

                            entry.text.setText(app.getName());
                            CheckBox box = entry.getBox();
                            box.setTag(app);
                            box.setChecked(app.getProxy());
                            entry.text.setTag(box);

                            return convertView;
                        }
                    };

                    appListView.setAdapter(adapter);
                    appsLoaded = true;

                    clearDialog();
                }, ex -> {
                    Log.e("xTun", "Got error: " + ex.getMessage());
                    clearDialog();
                });
    }

    private void clearDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ProxyApp app = (ProxyApp) buttonView.getTag();
        if (app != null) {
            app.setProxy(isChecked);
        }
        saveAppSettings(this);
    }

    @Override
    public void onClick(View v) {
        CheckBox box = (CheckBox) v.getTag();
        ProxyApp app = (ProxyApp) box.getTag();
        if (app != null) {
            app.setProxy(!app.getProxy());
            box.setChecked(app.getProxy());
        }
        saveAppSettings(this);
    }

    private void saveAppSettings(Context context) {
        if (apps == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        StringBuilder proxiedApps = new StringBuilder();
        for (ProxyApp app : apps) {
            if (app.getProxy()) {
                proxiedApps.append(app.getId());
                proxiedApps.append("|");
            }
        }
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Constants.Key.proxied, proxiedApps.toString());
        edit.apply();
    }

    public static ProxyApp[] getProxiedApps(Context context, String proxiedAppString) {
        String[] proxyApps = proxiedAppString.split("\\|");
        Arrays.sort(proxyApps);

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> infoList = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        ArrayList<ProxyApp> appList = new ArrayList<>();

        for (PackageInfo p : infoList) {
            String  label = p.applicationInfo.loadLabel(pm).toString();
            int uid = p.applicationInfo.uid;
            if (p.requestedPermissions == null) {
                continue;
            }
            for (String perm : p.requestedPermissions) {
                if (perm.contains(Manifest.permission.INTERNET)) {
                    int index = Arrays.binarySearch(proxyApps, Integer.toString(uid));
                    boolean proxied = index >= 0;
                    if (proxied) {
                        ProxyApp app = new ProxyApp(uid, label, p.packageName, true);
                        appList.add(app);
                    }
                    break;
                }
            }
        }
        ProxyApp[] appArray = new ProxyApp[appList.size()];
        appList.toArray(appArray);
        return appArray;
    }

}
