package edu.ucsc.sites.microreport;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**Shows campus resources and a short FAQ on the app. Also shows the participant's ID number from
 * SharedPreferences*/
//v3: changed to show surveys
public class SurveyActivity extends Activity {

    private SharedPreferences preferenceSettings;
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    String partID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        TextView settings = (TextView) findViewById(R.id.settings);

        //show if installation has been registered
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        partID = preferenceSettings.getString("partID", "Device has not been registered");
        //String email = preferenceSettings.getString("emailAddress","");
        String settings_text = "Your access code: "+ partID +"\n";
        settings.setText(settings_text);
        settings.setContentDescription(settings_text);

        //load webpage
        WebView myWebView = (WebView) findViewById(R.id.surveyview);
        myWebView.loadUrl("https://microreport.sites.ucsc.edu/surveys/");
        myWebView.setWebViewClient(new WebViewClient());

        //navigation drawer
        String[] menuList = getResources().getStringArray(R.array.menu);
        String points = preferenceSettings.getString("points", "temporarily unavailable");
        menuList[4] = menuList[4] + points;
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
                intent = new Intent(this, SurveyActivity.class);
                startActivity(intent);
                break;
            case 3:
                Uri webpage = Uri.parse("http://people.ucsc.edu/~cmbyrd/microaggressionstudy.html");
                intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
            case 4:
                Uri webpage2 = Uri.parse("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/redeem_points.php?accesscode="+partID);
                intent = new Intent(Intent.ACTION_VIEW, webpage2);
                startActivity(intent);
                break;
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
