package edu.ucsc.psyc_files.microreport;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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
public class ApplicationStartUp extends Application {
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

       @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-58131743-2");
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);
           tracker.set("&uid", Installation.id(this));

           //check if device is registered and set shared preferences
           new checkRegistration().execute();

           //check for notifications after 8 hours and about every 8 hours after that
           Intent notificationIntent = new Intent(this, NotificationService.class);
           PendingIntent pi = PendingIntent.getService(this, 0,notificationIntent, 0);
           AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
           //restart the alarm
           alarmManager.cancel(pi);
           //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 30000, 60000, pi);
           alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 120000, 28800000, pi);
    }

   /** private void setSharedPreferences() {
        //save registration status to sharedpreferences so registration screen will show first
        SharedPreferences preferenceSettings;
        SharedPreferences.Editor preferenceEditor;

        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        preferenceEditor = preferenceSettings.edit();
        preferenceEditor.putBoolean("registered", false);
        preferenceEditor.apply();

    }*/

    private class checkRegistration extends AsyncTask<Void, Void, String> {
        //checks device ID in user file
        @Override
        protected String doInBackground(Void... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return "No network connection";
            }

            try {
                //todo also log access here?
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/check_registration.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);

                //get partID from SharedPreferences and compare to server
                SharedPreferences preferenceSettings;
                preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                String partID = preferenceSettings.getString("partID", "false");
                if (partID == "false") {
                    return partID;
                }
                //check if ID is in user file
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("partID="+partID.trim()); //trim whitespace
                out.close();

                if (con.getResponseCode() == 200) {
                    //the php file will echo "true" or "false"
                    BufferedReader reader = null;
                    StringBuilder stringBuilder;
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line + "\n");
                    }
                    String result = stringBuilder.toString();
                    return result;
                } else {
                    return con.getResponseMessage();
                }

            } catch (Exception ex) {
                return ex.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            SharedPreferences preferenceSettings;
            SharedPreferences.Editor preferenceEditor;

            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            preferenceEditor = preferenceSettings.edit();
            if (result == "true") {
                preferenceEditor.putBoolean("registered", true);
            } else preferenceEditor.putBoolean("registered", false);
            preferenceEditor.apply();

        }


        /**checks whether the device is connected to an internet network*/
        private boolean isNetworkConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return (cm.getActiveNetworkInfo() != null);
        }
    }


}
