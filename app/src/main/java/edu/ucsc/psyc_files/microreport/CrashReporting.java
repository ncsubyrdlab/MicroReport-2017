package edu.ucsc.psyc_files.microreport;

import android.app.Application;
import android.content.SharedPreferences;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.HashMap;

/**
 * Setup for ACRA error reporting. When an exception is thrown, the app shows a toast and tries
 * to send a report to the file. Can save reports if no internet connection, but this does not
 * happen consistently.
 * Crash reports include Installation ID, stacktrace, and device info
 * Also setup for Google Analytics and registration flag
 */
@ReportsCrashes(
        formKey = "",
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false,
        resToastText = R.string.crash_toast_text,
        formUri = "http://people.ucsc.edu/~cmbyrd/microreport/errorlogs/logerrors.php"
        ,formUriBasicAuthLogin = "MRapp",
        formUriBasicAuthPassword = "sj8719i",
        disableSSLCertValidation = true,
        customReportContent = {ReportField.DEVICE_ID, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT }
)
public class CrashReporting extends Application {
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

       @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        setSharedPreferences();
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-58131743-2");
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);
           tracker.set("&uid", Installation.id(this));
    }

    private void setSharedPreferences() {
        //save registration status to sharedpreferences so registration screen will show first
        SharedPreferences preferenceSettings;
        SharedPreferences.Editor preferenceEditor;

        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        preferenceEditor = preferenceSettings.edit();
        preferenceEditor.putBoolean("registered", false);
        preferenceEditor.apply();

    }

}
