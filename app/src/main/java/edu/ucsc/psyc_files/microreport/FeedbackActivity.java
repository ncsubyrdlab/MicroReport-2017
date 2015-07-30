package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.acra.ReportField;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * Feedback form that posts to AFS space
 */
public class FeedbackActivity extends Activity {
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //navigation drawer
        String[] menuList = getResources().getStringArray(R.array.menu);
        nav = (ListView) findViewById(R.id.navigation_drawer);
        nav.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuList));
        nav.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.navigation);
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(R.string.app_name);
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    public void submitFeedback (View view) {
        if (isNetworkConnected()) { //only do if network is connected
        //go back to main view
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //open connection and post feedback
        EditText text = (EditText) findViewById(R.id.feedbacktext);
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String feedback = "\nTimestamp:"+ Calendar.getInstance().getTime()+"\tInstallationID:"+Installation.id(this)+"\tDeviceID:"+androidId+"\tComments:"+text.getText().toString();
        new postFeedback().execute(feedback);
        } else Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
    }

    private class postFeedback extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String urlstring = "http://people.ucsc.edu/~cmbyrd/microreport/postfeedback.php?output=";
                URL url = new URL(urlstring + Uri.encode(params[0]));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                con.setRequestProperty("Authorization", basicAuth);
                con.connect();
                String result = con.getResponseMessage();
                con.disconnect();
                return result;
            } catch (Exception ex) {
                return ex.toString();
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);
            toastResult(result);
        }
    }

    private void toastResult(String result){
        Toast.makeText(this, "Submitting feedback: " + result, Toast.LENGTH_SHORT).show();
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
