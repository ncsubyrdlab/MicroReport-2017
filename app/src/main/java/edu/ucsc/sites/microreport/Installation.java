package edu.ucsc.sites.microreport;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Creates an ID for each installation that is submitted with logs, reports, and error reports.
 * ID is created the first time the user opens the app after installation. Changes on re-installs.
 * Could be replaced with InstanceID.
 * Based on: http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
 */
public class Installation {

    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    /**
     * Returns the installation ID and creates one if necessary.
     * @param context
     * @return sID the installation ID
     */
    public synchronized static String id(Context context) {
            if (sID == null) {
                File installation = new File(context.getFilesDir(), INSTALLATION);
                try {
                    if (!installation.exists())
                        writeInstallationFile(installation);
                    sID = readInstallationFile(installation);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return sID;
        }

    /**
     * Reads the sID from the installation file.
     * @param installation
     * @return sID
     * @throws IOException
     */
        private static String readInstallationFile(File installation) throws IOException {
            RandomAccessFile f = new RandomAccessFile(installation, "r");
            byte[] bytes = new byte[(int) f.length()];
            f.readFully(bytes);
            f.close();
            return new String(bytes);
        }

    /**
     * If the installation ID has not been created, writes a new installation file containing the
     * installation ID, a random string of digits.
     * @param installation
     * @throws IOException
     */
        private static void writeInstallationFile(File installation) throws IOException {
            FileOutputStream out = new FileOutputStream(installation);
            String id = UUID.randomUUID().toString();
            out.write(id.getBytes());
            out.close();
        }
}
