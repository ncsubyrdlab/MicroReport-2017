package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Displays a feedback form that posts to log file on the server as action:feedback.
 */
public class FeedbackActivity extends Activity {
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

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
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return (isConnected);
    }

    /**
     * Uses an AsyncTask to submit the feedback to the server. Passes along the installation ID then
     * goes back to the Home screen. A toast confirms the status of the feedback (echoed from php file).
     * @param view
     */
    public void submitFeedback (View view) {
        if (isNetworkConnected()) { //only do if network is connected
        //go back to main view
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //open connection and post feedback
        EditText text = (EditText) findViewById(R.id.feedbacktext);
        //String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String output = "feedback="+Uri.encode(text.getText().toString())+"&installationID="+Uri.encode(Installation.id(this));
        new postFeedback().execute(output);
        } else Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
    }

    /**
     * Opens a connection to the server and posts feedback using the php file, which echoes the
     * status back.
     */
    private class postFeedback extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String result;
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/postfeedback.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write(params[0]);
                out.close();

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = null;
                    StringBuilder stringBuilder;
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append("\n"+line);
                    }
                    result = stringBuilder.toString();
                } else {
                    result = con.getResponseMessage();
                }
                con.disconnect();
                return result;
            } catch (Exception ex) {
                return ex.toString();
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);
            Toast.makeText(getBaseContext(), "Submitting feedback: " + result.trim(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ClickListener for navigation drawer.
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Takes clicks on navigation drawer and opens appropriate activity. Then closes the drawer.
     * @param position the position of the item clicked
     */
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
        //passes menu clicks to the navigation drawer instead of action bar
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return false;
    }
}
