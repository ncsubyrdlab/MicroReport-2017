package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * MicroReport 2.0
 * @author Christy M. Byrd, University of California, Santa Cruz, cmbyrd@ucsc.edu
 * Copyright 2015
 * This Android app is used to report microaggressions and display them on a map. Requires Google
 * Play Services and Android 3.0+
 * The Main activity displays a map and a floating action button. The items are stored in an ArrayList
 * that is then added to a cluster manager so that the markers cluster at certain zoom levels.
 * Uses Utility Library for marker clustering: https://github.com/googlemaps/android-maps-utils
 *
 */
public class MainActivity extends Activity implements OnMapReadyCallback {

    private static MapFragment mMapFragment;  //Google map fragment
    private static ClusterManager<Report> mClusterManager;  //Handles rendering of markers at different zoom levels
    private ReportAdapter adapter;   //handles the list of reports in landscape mode
    private DefaultClusterRenderer<Report> clusterRenderer; //my implementation of Google utility library cluster renderer
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ArrayList<Report> reports;
    private String partID;

    /**
     * Opens the main page and displays the reports on a map in clusters based on zoom level.
     * First checks if the device is registered using SharedPreferences. If not, redirects to
     * Registration page. Connects to php file which echoes reports, which are put into an ArrayList.
     * The list is then displayed in the list adapter (landscape view) and the cluster manager
     * to show the map markers.
     * @param savedInstanceState The map clusters are saved in an array on orientation changes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isNetworkConnected()) {//if network is not connected, don't do anything
            //check registration status
            SharedPreferences preferenceSettings;
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            partID = preferenceSettings.getString("partID", "false");
            if (partID == "false") {
                //go to registration page
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
            //if just an orientation change or no network connection, don't download reports again
            if (savedInstanceState != null)  {
                reports = savedInstanceState.getParcelableArrayList("reports");
                setUpMap();
            }
            else {
                //otherwise download reports (calls setUpMap)
                //todo: check age of file
                new getReports().execute();
            }
        } else {    //end if network is connected
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }

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

    /**
     * Called from the AsyncTask in setUpMap() to display the map and add the markers from the
     * ClusterManager. Also sets up the listview in landscape mode.
     * @param mMap
     */
    public void onMapReady(GoogleMap mMap) {
        //add stuff to map
        mMap.setMyLocationEnabled(true); //shows user location
        UiSettings settings = mMap.getUiSettings();
        settings.setMapToolbarEnabled(false);  //doesn't show toolbar that links to Google Maps app
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.991386, -122.060872), 14)); //moves camera to campus center
        mMap.setInfoWindowAdapter(new MyInfoWindow());  //uses my version of info window

        // Initialize the cluster manager and renderer and set up listeners
        mClusterManager = new ClusterManager<Report>(this, mMap);
        clusterRenderer = new MyClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(clusterRenderer);
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        mClusterManager.addItems(reports);
        mClusterManager.cluster();

        //fill listview with reports from array using custom adapter (only visible in landscape mode)
        if (findViewById(R.id.reports) != null) {
            ListView list = (ListView) findViewById(R.id.reports);
            adapter = new ReportAdapter(this, reports);
            list.setAdapter(adapter);
            list.setOnItemClickListener(mItemClickedHandler);
        }
    }

    /**checks whether the device is connected to an internet network*/
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    /**Save markers to an arraylist so don't have to re-download file on screen orientation changes*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("reports",reports);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**creates the menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    public void newReport(View view){
        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**launches the appropriate action when the user clicks the menu*/
        //The action bar here includes the refresh button.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_clear_cache:
                updateMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**Listview click listener (landscape only). When the user clicks on a list item, the map zooms
     * to the marker and opens the info window.
     * https://github.com/ch8908/thor-android/blob/100ff882515be390c3e0ac7f705e1ec10c7d5d90/thor-android/src/main/java/com/osolve/thor/activity/MainActivity.java*/
    private AdapterView.OnItemClickListener mItemClickedHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView parent, View v, int position, long id) {
        v.setSelected(true);
        Report item;
            item = reports.get(position);

        v.setContentDescription(item.getDescription()+item.getTimestamp());
        mMapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(item.getPosition(), 18));
            Marker marker = clusterRenderer.getMarker(item);
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    };

    /**
     * Calls the AsyncTask that sets up the map fragment. At the end goes to onMapReady()
     */
    private void setUpMap(){
        //check to Google Play Services is installed
        if (checkGooglePlay()) {
            //set up the map fragment
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mMapFragment.getMapAsync(this);
        } else Toast.makeText(this, "Install Google Play Services", Toast.LENGTH_SHORT).show();
    }

    /**
     * Refreshes the map. Right now just reloads the activity.
     */
    public void updateMap() {
         Toast.makeText(this, "Updating map...", Toast.LENGTH_SHORT).show();
        /**File cacheDir = getCacheDir();
         File[] files = cacheDir.listFiles();
         if (files != null) {
         for (File file : files)
         file.delete();
         } **/
        //reload activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**My own version of cluster renderer (from clustering utility library)
     * shows info window with title and snippet, and blue markers (slightly darker blue for user's
     * reports. */
     static class MyClusterRenderer extends DefaultClusterRenderer<Report> {

        public MyClusterRenderer(Context context, GoogleMap map,
                                 ClusterManager<Report> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(Report item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
            markerOptions.title(item.getTimestamp());
            markerOptions.snippet(item.getDescription());
            if (item.isUser_report()){
                //colors: https://developers.google.com/android/reference/com/google/android/gms/maps/model/BitmapDescriptorFactory
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(200));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(220));
            }
        }
    }

    /**My info window design, shows the whole description, with the timestamp in small text below*/
    private class MyInfoWindow implements GoogleMap.InfoWindowAdapter{

        @Override
        public View getInfoContents(Marker marker) {
            if (marker.getTitle()!= null) { //don't show infowindow for clusters
            View v = getLayoutInflater().inflate(R.layout.infowindow_layout, null);
            TextView markerTitle = (TextView)v.findViewById(R.id.marker_title);
            TextView markerDescription = (TextView)v.findViewById(R.id.marker_description);
            markerTitle.setText(marker.getTitle());
            markerDescription.setText(marker.getSnippet());
            v.setContentDescription(marker.getSnippet()+marker.getTitle());

            //highlight item in listview based on whether filtered (landscape only)
            if (findViewById(R.id.reports) != null) {
                Report item = clusterRenderer.getClusterItem(marker);
                ListView list = (ListView) findViewById(R.id.reports);
                list.setSelection(reports.indexOf(item));

               }
            return v;
        }
            return null;
        }

        public View getInfoWindow(Marker marker){
            return null;
        }
    }

    /**enables response caching on Android 4.0 and above*/
    private void enableHttpResponseCache() {
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(getCacheDir(), "http");
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
            System.out.println("Response Cache enabled");
        } catch (Exception httpResponseCacheNotAvailable) {
            //Log.d(TAG, "HTTP response cache is unavailable.");
            System.out.println("Response Cache unavailable");
        }
    }

    /**
     * Shows the items in listview
     * Uses a cache for performance
     * https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
     */
    public class ReportAdapter extends ArrayAdapter<Report> {
        private ArrayList<Report> list;

        public ReportAdapter(Context context, ArrayList<Report> list) {
            super(context, 0, list);
            this.list = list;
        }

        public ArrayList<Report> getList() {
            return list;
        }

        private class ViewHolder {
            TextView markerTitle;
            TextView markerDescription;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Report item = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                //uses the same layout s the info window when the marker is clicked
                convertView = inflater.inflate(R.layout.infowindow_layout, parent, false);
                viewHolder.markerTitle = (TextView) convertView.findViewById(R.id.marker_title);
                viewHolder.markerDescription = (TextView) convertView.findViewById(R.id.marker_description);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //set text of views
            viewHolder.markerTitle.setText(item.getTimestamp());
            viewHolder.markerDescription.setText(item.getDescription());
            //return completed view
            return convertView;
        }

    }

    /**checks that Google Play Services are available
     * http://www.androiddesignpatterns.com/2013/01/google-play-services-setup.html*/
    private boolean checkGooglePlay(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        }
        else {
            GooglePlayServicesUtil.getErrorDialog(resultCode,this,1001).show();
            Toast.makeText(this, "Install or update Google Play Services", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Shows the reports in the list view in ascending order
     * @param view
     */

    public void sortAscending(View view){
        //get list currently in listview
        ArrayList list = adapter.getList();
        //sort by timestamp
         Collections.sort(list, new Comparator<Report>() {
                public int compare(Report report1, Report report2) {
                    Long date1, date2;
                    try {
                        date1 = Long.parseLong(report1.getRawTimetamp());
                        date2 = Long.parseLong(report2.getRawTimetamp());
                    } catch (NumberFormatException ex) {
                        date1 = (long) 0;
                        date2 = (long) 0;
                    }
                    return (date1 < date2 ? -1 : date1 > date2 ? 1 : 0);
                }
            });
        adapter.notifyDataSetChanged();
    }

    /**
     * Shows the reports in the list view in descending order
     * @param view
     */
    public void sortDescending(View view) {
        //get list currently in listview
        ArrayList list = adapter.getList();
        //sort by timestamp
        Collections.sort(list, new Comparator<Report>() {
            public int compare(Report report1, Report report2) {
                Long date1, date2;
                try {
                    date1 = Long.parseLong(report1.getRawTimetamp());
                    date2 = Long.parseLong(report2.getRawTimetamp());
                } catch (NumberFormatException ex) {
                    date1 = (long) 0;
                    date2 = (long) 0;
                }
                return (date1 > date2 ? -1 : date1 < date2 ? 1 : 0);
            }
        });
        adapter.notifyDataSetChanged();

    }

    /**
     * Displays only the user's reports in the listview
     * @param view
     */
    public void viewMyReports (View view){
        //adapter.clear();
        ListView list = (ListView) findViewById(R.id.reports);
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            //copy reports into separate list
            ArrayList<Report> new_list = new ArrayList<Report>();
            //pull out list of user's reports
            for (Report i : reports) {
                if (i.isUser_report()) {
                    new_list.add(i);
                }
            }
            adapter = new ReportAdapter(this, new_list);
            list.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new ReportAdapter(this, reports);
            list.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
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

    private class getReports extends AsyncTask<String, Void, ArrayList<Report>> {
        @Override
        protected ArrayList<Report> doInBackground(String... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return null;
            }
            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/getreports.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("partID="+partID+"&installationID="+Installation.id(getBaseContext()));
                //out.write("partID="+partID+"&installationID="+Installation.id(getBaseContext())+"&search_field="+params[0]+"&search_term="+params[1]+"&sort_field="+params[2]);
                out.close();

                if (con.getResponseCode() == 200) {
                    reports = parseReports(con.getInputStream());
                } else {
                    return null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }

            return reports;
        }

        @Override
        protected void onPostExecute(ArrayList<Report> reports) {
            super.onPostExecute(reports);
            setUpMap();
        }
    }

    /**
     * Takes input stream from HttpURLConnection and reads each line into a report object.
     * @param in
     * @return
     * @throws IOException
     */
    private ArrayList<Report> parseReports(InputStream in) throws IOException {
        ArrayList<Report> reports = new ArrayList<Report>();
        //Report(String description, long locationLat, long locationLong, String partID, Date timestamp, boolean user_report) {
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        String partID = preferenceSettings.getString("partID", "none");
        String[] result;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
        //check that the input is actually something
        if (line.contains("Error: ")) {
            reports.add(new Report(new Date().toString(), "Error", "There was an error: "+line, "36.991386", "-122.060872", false));
            return reports;
        }
        do {    //take each line and read parts into report object
            result = line.split("%delim%",5);
            reports.add(new Report(result[0], result[1], result[2], result[3], result[4], result[4].equals(partID)));
            line = br.readLine();
        } while (line != null);
        return reports;
    }

    /**
     * Constructs the Report object that includes report details
     */
    public static class Report implements ClusterItem, Parcelable {
        public final String timestamp;
        public final String description;
        public final String locationLat;
        public final String locationLong;
        public final String partID;
        public final boolean user_report;

        public Report(String timestamp, String description, String locationLat, String locationLong, String partID, boolean user_report) {
            this.timestamp = timestamp;
            this.description = description;
            this.locationLat = locationLat;
            this.locationLong = locationLong;
            this.partID = partID;
            this.user_report = user_report;
        }

        public String getDescription() {
            return description;
        }

        public String getLocationLat() {
            return locationLat;
        }

        public String getLocationLong() {
            return locationLong;
        }

        public String getPartID() {
            return partID;
        }

        public String getTimestamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a zzz");
            Date time = new Date(Long.parseLong(timestamp)*1000);   //convert seconds to milliseconds
            return sdf.format(time);
        }

        public String getRawTimetamp() {
            return timestamp;
        }

        public boolean isUser_report() {
            return user_report;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * writes content of report object to parcel
         * @param out
         * @param flags
         */
        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(getTimestamp());
            out.writeString(getDescription());
            out.writeString(getLocationLat());
            out.writeString(getLocationLong());
            out.writeString(getPartID());
            out.writeString(String.valueOf(isUser_report()));
        }

        @Override
        public LatLng getPosition() {
            double lat, longi;
            try {
            lat = Double.valueOf(getLocationLat());
                longi = Double.valueOf(getLocationLong());
            } catch (NumberFormatException ex) {
                lat = 0;
                longi = 0;
            }
            return new LatLng(lat, longi);
        }

        /**
         * Reads contents of parcel into report object
         */
        public static final Parcelable.Creator<Report> CREATOR = new Parcelable.Creator<Report>() {
            public Report createFromParcel(Parcel in) {
                return new Report(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(), Boolean.parseBoolean(in.readString()));
            }

            public Report[] newArray(int size) {
                return new Report[size];
            }
        };
    }


}


