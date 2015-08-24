package edu.ucsc.psyc_files.microreport;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
 * Sets up ACRA error reporting, Google Analytics, and the service to check for notifications for
 * Good News. Also checks if the installation has been registered.
 */
@ReportsCrashes(mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false,
        resToastText = R.string.crash_toast_text,
        formUri = "http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/logerrors.php",
        disableSSLCertValidation = true,
        logcatArguments = { "-t", "100", "-v", "long", "ActivityManager:I", "MicroReport:D", "*:S" },
        additionalSharedPreferences={"microreport_settings"},
        customReportContent = {ReportField.APP_VERSION_NAME, ReportField.CUSTOM_DATA, ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL, ReportField.STACK_TRACE, ReportField.LOGCAT
                }
)

public class ApplicationStartUp extends Application {
    public static GoogleAnalytics analytics;
    public static Tracker tracker;
    String partID;
    String installationID;

    /**
     * Initiates registration check, ACRA and Google Analytics set-up
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //get participant ID for registration check
        installationID = Installation.id(this);
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        partID = preferenceSettings.getString("partID", "false");

        //check if device is registered and set shared preferences
        new checkRegistration().execute();

         //set up ACRA and adds installation ID to custom fields
        ACRA.init(this);
        ACRA.getErrorReporter().putCustomData("InstallationID", installationID);
        ACRA.getErrorReporter().putCustomData("InstallationType", "AndroidApp2.0");

        //set up Google Analytics
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);
        tracker = analytics.newTracker("UA-58131743-2");
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);
        tracker.set("&uid", installationID);

        //check for notifications after 2 minutes and every 23 hours
        Intent notificationIntent = new Intent(this, NotificationService.class);
        PendingIntent pi = PendingIntent.getService(this, 0,notificationIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //restart the alarm
        alarmManager.cancel(pi);
        //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 30000, 60000, pi);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+120000, 82800000, pi);

    }

    /**
     * Contacts the server to check if the participant ID is in the user table. The php file echoes
     * "true" or "false". The participant ID is taken from SharedPreferences and is "false" if it
     * has not been set before. The SharedPreference is set after this and the registration activity
     * and will be cleared if the participant clears the app data.
     *
     */
    private class checkRegistration extends AsyncTask<Void, Void, String> {
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

                //compare PartID to server
                if (partID == "false") {
                    return partID;
                }

                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("partID="+partID.trim()+"&installationID="+installationID); //trim whitespace
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

        /**Checks whether the device is connected to an internet network. Does not necessarily ensure
         * that the internet is available (for example, if there is a login screen).
         * @return true if connected to a network, otherwise false*/
        private boolean isNetworkConnected() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnected();
            return (isConnected);
        }
    }
}
