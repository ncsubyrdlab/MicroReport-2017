package edu.ucsc.psyc_files.microreport;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The notification service is called by the AlarmManager in {@ApplicationStartUp}. Connects to the
 * Google Groups page, downloads the first time, checks the date against the last time the service
 * was called, displays a notification if the item is recent, and then stops the service.
 * http://developer.android.com/guide/components/services.html
 */
public class NotificationService extends IntentService {

    public NotificationService() {
        super("NotificationService");
    }

    /**
     * Calls AsyncTask to check the last news item.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        new getNews().execute();
    }

    /**AsyncTask to get the latest item from the Google Group. Uses the same parser as {@BulletinBoard}.
     *
     */
    private class getNews extends AsyncTask<Void, Void, BulletinBoard.NewsItem> {
        BulletinBoard.NewsItem news;
        @Override
        protected BulletinBoard.NewsItem doInBackground(Void... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return null;
            }
            try {
                URL url = new URL("https://groups.google.com/a/ucsc.edu/forum/feed/microreport-goodnews-group/msgs/rss.xml?num=1");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                if (con.getResponseCode() == 200) {
                    String currentTag = null;
                    String title = null;
                    String text = null;
                    String link = null;
                    Date timestamp = null;

                    InputStream in = new BufferedInputStream(con.getInputStream());
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser  = factory.newPullParser();
                    parser.setInput(in, "UTF-8");
                    int eventType = parser.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            currentTag = parser.getName();
                        } else if (eventType == XmlPullParser.TEXT && !parser.isWhitespace()) {
                            if ("title".equals(currentTag)) {
                                title = parser.getText();
                            }
                            if ("description".equals(currentTag)) {
                                text = parser.getText();
                            }
                            if ("link".equals(currentTag)) {
                                link = parser.getText();
                            }
                            if ("pubDate".equals(currentTag)) {
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                //Sat, 01 Aug 2015 19:13:17 UTC
                                try {
                                    timestamp = sdf.parse(parser.getText());
                                }
                                catch (ParseException ex) {
                                    timestamp = new Date();
                                }
                            }
                        } else if (eventType == XmlPullParser.END_TAG) {
                            if ("item".equals(parser.getName())) {
                                news = new BulletinBoard.NewsItem(title, text, link, timestamp);
                            }
                        }
                        eventType = parser.next();
                    }

                } else {
                    return null;
                }

                return news;

            } catch (XmlPullParserException ex) {
                return null;
            }catch (IOException ex) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(BulletinBoard.NewsItem result) {
            super.onPostExecute(result);
            //get last time checked for updates
            SharedPreferences preferenceSettings;
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            long last_opened = preferenceSettings.getLong("last_checked_notifications", System.currentTimeMillis());
            long latest;
            try {
                latest = result.getTimestamp().getTime();
            }
            catch (NullPointerException ex) {
                latest = 0;
            }
            //NewsNotification.notify(getBaseContext(), String.valueOf(latest), String.valueOf(last_opened), 1);
            //if new news after last time opened
                if (latest > last_opened) {
                    NewsNotification.notify(getBaseContext(), result.getTitle(), result.getText(), 1);
                    //NewsNotification.notify(getBaseContext(), String.valueOf(result.getTimestamp().getTime()), String.valueOf(last_opened), 1);
                }

            //record new check time
            SharedPreferences.Editor preferenceEditor;
            preferenceEditor = preferenceSettings.edit();
            preferenceEditor.putLong("last_checked_notifications", System.currentTimeMillis());
            preferenceEditor.apply();
            stopSelf();
        }
    }

    /**checks whether the device is connected to an internet network*/
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return (isConnected);
    }
}
