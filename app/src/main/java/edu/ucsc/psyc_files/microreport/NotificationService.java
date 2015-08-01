package edu.ucsc.psyc_files.microreport;

import android.app.IntentService;
import android.content.Intent;
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

public class NotificationService extends IntentService {
    //http://developer.android.com/guide/components/services.html
    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //check for new news items
        new getNews().execute();
    }

    private class getNews extends AsyncTask<Void, Void, BulletinBoard.NewsItem> {
        BulletinBoard.NewsItem news;
        @Override
        protected BulletinBoard.NewsItem doInBackground(Void... params) {
            try {
                //just get the latest item
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
                    //https://androidcookbook.com/Recipe.seam?recipeId=2217
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
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
                                //EEE, dd MMM yyyy hh:mm:ss a
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
            Date date = new Date(System.currentTimeMillis() - (8 * 60 * 60 * 1000));
            if (result.getTimestamp().after(date)) {
                NewsNotification.notify(getBaseContext(), result.getTitle(), result.getText(), 1);
            }
            stopSelf();
        }
    }
}
