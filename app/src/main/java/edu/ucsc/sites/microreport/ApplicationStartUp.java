package edu.ucsc.sites.microreport;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

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
 * Sets up ACRA error reporting, Google Analytics. Also checks if the installation has been registered and computes points.
 * Removed notification service and changed error reporting; check registration only looks for partID and whether banned
 */
@ReportsCrashes(mode = ReportingInteractionMode.TOAST,
        alsoReportToAndroidFramework = false,
        resToastText = R.string.crash_toast_text,
        formUri = "http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/logerrors.php",
        //disableSSLCertValidation = true, (doesn't work in updated ACRA)
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
    SharedPreferences preferenceSettings;
    SharedPreferences.Editor preferenceEditor;

    /**
     * Initiates registration check, ACRA and Google Analytics set-up
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //get participant ID for registration check
        installationID = Installation.id(this);
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        partID = preferenceSettings.getString("partID", "false");

        //check if device is registered and set shared preferences
        new checkRegistration().execute();

         //set up ACRA and adds installation ID to custom fields
        ACRA.init(this);
        ACRA.getErrorReporter().putCustomData("partID", partID);
        ACRA.getErrorReporter().putCustomData("InstallationID", installationID);
        ACRA.getErrorReporter().putCustomData("InstallationType", "AndroidApp2.0");

        //set up Google Analytics
        analytics = GoogleAnalytics.getInstance(this);
        //analytics.setLocalDispatchPeriod(1800); not sure needed?
        //tracker = analytics.newTracker("UA-58131743-2"); see getDefaultTracker below
        //tracker.enableExceptionReporting(true); causes crash
        //tracker.enableAutoActivityTracking(true); enabled in xml file
        //tracker.set("&uid", partID); causes crash - from old version

        //compute total points
        new getPoints().execute();

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
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/check_registration.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);

                //compare PartID to server
                if (partID == "false") {
                    return partID;
                }

                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("partID=" + partID.trim() + "&installationID=" + installationID); //trim whitespace
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
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            preferenceEditor = preferenceSettings.edit();
            if (result == "true") {
                preferenceEditor.putBoolean("registered", true);
            } else preferenceEditor.putBoolean("registered", false);
            preferenceEditor.apply();

        }

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


    //gets participant's reward points and saves
    private class getPoints extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return "unavailable";
            }

            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/compute_points.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);

                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("partID=" + partID.trim()); //trim whitespace
                out.close();

                if (con.getResponseCode() == 200) {
                    //the php file will echo number of points
                    BufferedReader reader = null;
                    StringBuilder stringBuilder;
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
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
            //add points to sharedpreferences
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            preferenceEditor = preferenceSettings.edit();
            preferenceEditor.putString("points", result);
            preferenceEditor.apply();

        }

    }
    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     * https://developers.google.com/analytics/devguides/collection/android/v4/
     */
    synchronized public Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (tracker == null) {
            tracker = analytics.newTracker(R.xml.global_tracker);
        }

        return tracker;
    }

}
