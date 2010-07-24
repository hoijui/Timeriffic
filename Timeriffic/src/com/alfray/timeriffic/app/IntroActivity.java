/*
 * Project: Timeriffic
 * Copyright (C) 2008 ralfoide gmail com,
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alfray.timeriffic.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.error.ExceptionHandlerActivity;
import com.alfray.timeriffic.prefs.PrefsValues;
import com.alfray.timeriffic.utils.AgentWrapper;

/**
 * Screen with the introduction text.
 */
public class IntroActivity extends ExceptionHandlerActivity {

    private static final boolean DEBUG = true;
    public static final String TAG = "TFC-IntroUI";

    public static final String EXTRA_NO_CONTROLS = "no-controls";

    private AgentWrapper mAgentWrapper;

    private class JSTimerifficVersion {

        private String mVersion;
        private String mIntroFile;

        public JSTimerifficVersion(String introFile) {
            mIntroFile = introFile;
        }

        public String longVersion() {
            if (mVersion == null) {
                PackageManager pm = getPackageManager();
                PackageInfo pi;
                try {
                    pi = pm.getPackageInfo(getPackageName(), 0);
                    mVersion = pi.versionName;
                    if (mVersion == null) mVersion = "";
                } catch (NameNotFoundException e) {
                    mVersion = ""; // failed, ignored
                }
            }
            return mVersion;
        }

        public String shortVersion() {
            String v = longVersion();
            if (v != null) {
                v = v.substring(0, v.lastIndexOf('.'));
            }
            return v;
        }

        @SuppressWarnings("unused")
        public String introFile() {
            return mIntroFile;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.intro);

        String introFile = selectFile("intro");
        JSTimerifficVersion jsVersion = new JSTimerifficVersion(introFile);

        String title = getString(R.string.intro_title, jsVersion.shortVersion());
        setTitle(title);

        final WebView wv = (WebView) findViewById(R.id.web);
        if (wv == null) {
            if (DEBUG) Log.d(TAG, "Missing web view");
            finish();
        }

        // Make the webview transparent (for background gradient)
        wv.setBackgroundColor(0x00000000);

        // Inject a JS method to set the version
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(jsVersion, "JSTimerifficVersion");

        loadFile(wv, introFile);
        setupProgressBar(wv);
        setupWebViewClient(wv);
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAgentWrapper = new AgentWrapper();
        mAgentWrapper.start(this);
        mAgentWrapper.event(AgentWrapper.Event.OpenIntroUI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAgentWrapper.stop(this);
    }

    private String selectFile(String baseName) {
        String file;

        // Compute which file we want to display, i.e. try to select
        // one that matches baseName-LocaleCountryName.html or default
        // to intro.html
        Locale lo = Locale.getDefault();
        String lang = lo.getLanguage();
        String country = lo.getCountry();
        if (lang != null && lang.length() > 2) {
            // There's a bug in the SDK "Locale Setup" app in Android 1.5/1.6
            // where it sets the full locale such as "en_US" in the languageCode
            // field of the Locale instead of splitting it correctly. So we do it
            // here.
            int pos = lang.indexOf('_');
            if (pos > 0 && pos < lang.length() - 1) {
                country = lang.substring(pos + 1);
                lang = lang.substring(0, pos);
            }
        }
        if (lang != null && lang.length() == 2) {
            AssetManager am = getResources().getAssets();

            // Try with both language and country, e.g. -en-US, -zh-CN
            if (country != null && country.length() == 2) {
                file = baseName + "-" + lang.toLowerCase() + "-" + country.toUpperCase() + ".html";
                if (checkFileExists(am, file)) {
                    if (DEBUG) Log.d(TAG, String.format("Locale(%s,%s) => %s", lang, country, file));
                    return file;
                }
            }

            // Try to fall back on just language, e.g. -zh, -fr
            file = baseName + "-" + lang.toLowerCase() + ".html";
            if (checkFileExists(am, file)) {
                if (DEBUG) Log.d(TAG, String.format("Locale(%s) => %s", lang, file));
                return file;
            }
        }

        if (!"en".equals(lang)) {
            if (DEBUG) Log.d(TAG, String.format("Language not found for %s-%s (Locale %s)",
                    lang, country, lo.toString()));
        }

        // This one just has to exist or we'll crash n' burn on the 101.
        return baseName + ".html";
    }

    private boolean checkFileExists(AssetManager am, String filename) {
        InputStream is = null;
        try {
            is = am.open(filename);
            return is != null;
        } catch (IOException e) {
            // pass
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // pass
                }
            }
        }
        return false;
    }

    private void loadFile(final WebView wv, String file) {
        wv.loadUrl("file:///android_asset/" + file);
        wv.setFocusable(true);
        wv.setFocusableInTouchMode(true);
        wv.requestFocus();
    }

    private void setupProgressBar(final WebView wv) {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        if (progress != null) {
            wv.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    progress.setProgress(newProgress);
                    progress.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    private void setupWebViewClient(final WebView wv) {
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith("/#new")) {
                    wv.loadUrl("javascript:location.href=\"#new\"");
                    return true;

                } else if (url.endsWith("/#known")) {
                    wv.loadUrl("javascript:location.href=\"#known\"");
                    return true;

                } else {
                    // For URLs that are not ours, including market: URLs
                    // just invoke the default view activity (e.g. Browser
                    // or Market app)
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // ignore. just means this device has no Market or
                        // Browser app... ignore it.
                    }
                    return true;
                }
            }
        });
    }

    private void setupButtons() {
        boolean hideControls = false;
        Intent i = getIntent();
        if (i != null) {
            Bundle e = i.getExtras();
            if (e != null) hideControls = e.getBoolean(EXTRA_NO_CONTROLS);
        }

        CheckBox dismiss = (CheckBox) findViewById(R.id.dismiss);
        if (dismiss != null) {
            if (hideControls) {
                dismiss.setVisibility(View.GONE);
            } else {
                final PrefsValues pv = new PrefsValues(this);
                dismiss.setChecked(pv.isIntroDismissed());

                dismiss.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        pv.setIntroDismissed(isChecked);
                    }
                });
            }
        }

        Button cont = (Button) findViewById(R.id.cont);
        if (cont != null) {
            if (hideControls) {
                cont.setVisibility(View.GONE);
            } else {
                cont.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // close activity
                        finish();
                    }
                });
            }
        }
    }
}
