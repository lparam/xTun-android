package io.github.xTun.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.TextView;

import io.github.xTun.R;
import io.github.xTun.utils.MovementCheck;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            Drawable upArrow;
            upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha, null);
            if (upArrow != null) {
                upArrow.setColorFilter(getResources().getColor(android.R.color.white, null), PorterDuff.Mode.SRC_IN);
            }
            ab.setHomeAsUpIndicator(upArrow);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        TextView aboutVersion = (TextView) findViewById(R.id.aboutVersion);
        try {
            PackageInfo manager = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = getResources().getString(R.string.version, manager.versionName, manager.versionCode);
            aboutVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            aboutVersion.setText(R.string.version_default);
        }

        TextView aboutDescription = (TextView) findViewById(R.id.aboutDescription);
        aboutDescription.setText(Html.fromHtml(getString(R.string.about_description_text)));
        aboutDescription.setMovementMethod(MovementCheck.getInstance());
    }

}
