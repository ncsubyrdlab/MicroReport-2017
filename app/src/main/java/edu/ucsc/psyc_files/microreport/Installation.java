package edu.ucsc.psyc_files.microreport;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Based on: http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
 * Creates an ID for each device that is submitted with reports and error reports
 * ID is created the first time the user runs the app after installation (but changes if they reinstall it)
 */
public class Installation {

        private static String sID = null;
        private static final String INSTALLATION = "INSTALLATION";


    public synchronized static String id(Context context) {
            if (sID == null) {
                File installation = new File(context.getFilesDir(), INSTALLATION);
                try {
                    if (!installation.exists())
                        writeInstallationFile(installation);
                    sID = readInstallationFile(installation);
                   //setSharedPreferences();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            ACRA.getErrorReporter().putCustomData("MicroReportInstallationID", sID);
            ACRA.getErrorReporter().putCustomData("InstallationType", "AndroidApp");
            return sID;
        }

        private static String readInstallationFile(File installation) throws IOException {
            RandomAccessFile f = new RandomAccessFile(installation, "r");
            byte[] bytes = new byte[(int) f.length()];
            f.readFully(bytes);
            f.close();
            return new String(bytes);
        }

        private static void writeInstallationFile(File installation) throws IOException {
            FileOutputStream out = new FileOutputStream(installation);
            String id = UUID.randomUUID().toString();
            out.write(id.getBytes());
            out.close();
        }

        private static void setSharedPreferences() {
        //save installation ID and registration status to sharedpreferences
        SharedPreferences preferenceSettings;
        SharedPreferences.Editor preferenceEditor;

        //preferenceSettings = getPreferences(PREFERENCE_MODE_PRIVATE);
        //preferenceEditor = preferenceSettings.edit();
        //preferenceEditor.putBoolean("registered", false);
    }
}
