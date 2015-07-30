package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

//https://developer.android.com/training/material/lists-cards.html#Dependencies
//todo: get list from server (xml file), add cards to reports page, add floating action button
public class BulletinBoard extends Activity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String[][] myDataset = {{"Post Your News","Click this link to send us some good news! You can post events and resources on and off-campus as well as affirmations and encouragement.", "7/27/15 4:26pm"},
            {"Heading 1","Description 1", "Timestamp"},
    {"Heading 2","Description 2","Timestamp"},
    {"Heading 3","Description 3","Timestamp"}};
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulletin_board);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MyNewsAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

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

    public void onClick(View view) {
        //go to link connected with card
        //Intent intent = new Intent(this, ReportActivity.class);
        //startActivity(intent);
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

