package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

//https://developer.android.com/training/material/lists-cards.html#Dependencies
public class BulletinBoard extends Activity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private ArrayList<NewsItem> news;
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private static final String ns = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulletin_board);

        //load recyclerview layout
        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //get news items
        new getNews().execute();
        news = new ArrayList<NewsItem>();
        mAdapter = new MyNewsAdapter(news);
        mRecyclerView.setAdapter(mAdapter);

        //navigation drawer
        String[] menuList = getResources().getStringArray(R.array.menu);
        nav = (ListView) findViewById(R.id.navigation_drawer);
        nav.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuList));
        nav.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            // Called when a drawer has settled in a completely open state.
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.navigation);
            }

            // Called when a drawer has settled in a completely closed state.
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(R.string.app_name);
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private class getNews extends AsyncTask<Void, Void, ArrayList<NewsItem>> {
        @Override
        protected ArrayList<NewsItem> doInBackground(Void... params) {
            try {
                //URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/news.xml");
                URL url = new URL("https://groups.google.com/a/ucsc.edu/forum/feed/microreport-goodnews-group/msgs/rss.xml?num=50");
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
                                news.add(new NewsItem(title, text, link, timestamp));
                            }
                        }
                        eventType = parser.next();
                    }

                } else {
                    news.add(new NewsItem("Connection Error",con.getResponseMessage(),"link",new Date()));
                }
            } catch (XmlPullParserException ex) {
                news.add(new NewsItem("XmlPullParserException"+String.valueOf(ex.getLineNumber()),ex.toString(),"link",new Date()));
            }catch (IOException ex) {
                news.add(new NewsItem("IOException",ex.toString(),"link",new Date()));
            }

            return news;
        }

        @Override
        protected void onPostExecute(ArrayList<NewsItem> news) {
            super.onPostExecute(news);
            //reset adapter
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onCardClick(View view) {
        //go to link connected with card
        //Intent intent = new Intent(this, ReportActivity.class);
        //startActivity(intent);
        //use? http://sapandiwakar.in/recycler-view-item-click-handler/
        //http://stackoverflow.com/questions/24885223/why-doesnt-recyclerview-have-onitemclicklistener-and-how-recyclerview-is-dif
    }

    public static class NewsItem {
        public final String title;
        public final String text;
        public final String link;
        public final Date timestamp;

        public NewsItem(String title, String text, String link, Date timestamp) {
            this.title = title;
            this.text = text;
            this.link = link;
            this.timestamp = timestamp;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        public String getLink() {
            return link;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }



    private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position){
        Intent intent;
        switch (position) {
            case 0:
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent = new Intent(this, BulletinBoard.class);
                startActivity(intent);
                break;
            case 2:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                break;
            case 3:
                intent = new Intent(this, FeedbackActivity.class);
                startActivity(intent);
                break;
            case 4:
                Uri webpage = Uri.parse("http://people.ucsc.edu/~cmbyrd/microaggressionstudy.html");
                intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
            default:
                break;
        }
        nav.setItemChecked(position, true);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.closeDrawer(nav);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return false;
    }
}

