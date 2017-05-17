package edu.ucsc.sites.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
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
import java.util.HashSet;
import java.util.Set;

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
 * 3.0
 * centers map on current location or home zipcode
 * map updated: https://developers.google.com/maps/documentation/android-api/map-with-marker
 *
 */
public class MainActivity extends Activity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private MapFragment mMapFragment;  //Google map fragment
    private static MyClusterManager<Report> mClusterManager;  //Handles rendering of markers at different zoom levels
    private ReportAdapter adapter;   //handles the list of reports in landscape mode
    private DefaultClusterRenderer<Report> clusterRenderer; //my implementation of Google utility library cluster renderer
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ArrayList<Report> reports;
    private String partID;
    private String homeZIPlatlng;
    private CameraPosition position;
    private Location mCurrentLocation;
    private GoogleApiClient mGoogleApiClient;

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
            if (partID.equals("false")) {
                //go to registration page
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                finish();
            } else {

                enableHttpResponseCache();

                //if just an orientation change or no network connection, don't download reports again
                if (savedInstanceState != null) {
                    try {
                        reports = savedInstanceState.getParcelableArrayList("reports");
                        position = savedInstanceState.getParcelable("position");
                    } catch (NullPointerException ex) {
                        position = new CameraPosition(new LatLng(36.991386, -122.060872), 14, 0, 0);
                    }
                    setUpMap();
                    //todo: preserve camera position on orientation changes
                    //mMapFragment.getMap().moveCamera(CameraUpdateFactory.newCameraPosition(position));
                } else {
                    //otherwise download reports (calls setUpMap)
                    //todo: check age of file

                    //get home zipcode
                    homeZIPlatlng = preferenceSettings.getString("homeZIPlatlng", "36.991386,-122.060872");

                    //set up location client
                    buildGoogleApiClient();

                    //Create a new location client, using the enclosing class to handle callbacks.
                    LocationRequest mLocationRequest = new LocationRequest();
                    mLocationRequest.setInterval(10000);
                    mLocationRequest.setFastestInterval(5000);
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


                    try {
                        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (mCurrentLocation == null) {
                            Toast.makeText(this, "location: null", Toast.LENGTH_SHORT).show();
                            //try again
                            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                                    mGoogleApiClient);}
                        Toast.makeText(this, "getting location: "+mCurrentLocation, Toast.LENGTH_SHORT).show();

                        if (mCurrentLocation == null) {
                            // move map to center of homeZIP
                            String[] latlong = homeZIPlatlng.split(",");
                            double latitude = Double.parseDouble(latlong[0]);
                            double longitude = Double.parseDouble(latlong[1]);
                            position = new CameraPosition(new LatLng(latitude, longitude), 14, 0, 0);
                            Toast.makeText(this, "location: center of zip "+position, Toast.LENGTH_SHORT).show();
                        } else {
                            position = new CameraPosition(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 14, 0, 0);
                            Toast.makeText(this, "location: " + position, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception ex) {
                        position = new CameraPosition(new LatLng(36.991386, -122.060872), 14, 0, 0);
                        Toast.makeText(this, "exception: "+ex, Toast.LENGTH_LONG).show();
                    }
                    reports = new ArrayList<Report>();
                    new getReports().execute();
                }
            }   //end check registration
            }else{    //end if network is connected
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }

            //filter
            //populate spinner
            if (findViewById(R.id.reports) != null) {
                Spinner spinner = (Spinner) findViewById(R.id.filter_spinner);
                ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(this, R.array.filter_options, android.R.layout.simple_spinner_item);
                //this, android.R.layout.simple_spinner_item, R.array.filter_options);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerArrayAdapter);
                spinner.setOnItemSelectedListener(this);
            }

            //navigation drawer
            String[] menuList = getResources().getStringArray(R.array.menu);
            SharedPreferences preferenceSettings;
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            String points = preferenceSettings.getString("points", "temporarily unavailable");
            menuList[4] = menuList[4] + points;
            nav = (ListView) findViewById(R.id.navigation_drawer);
            nav.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuList));
            nav.setOnItemClickListener(new DrawerItemClickListener());
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    R.string.drawer_open, R.string.drawer_close) {

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    getActionBar().setTitle(R.string.navigation);
                }

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    getActionBar().setTitle(R.string.app_name);
                }
            };
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ListView list = (ListView) findViewById(R.id.reports);
        ArrayList<Report> new_list = new ArrayList<Report>();
        //filter reports

                switch (position) {
                case 1: //race
                    for (Report i : reports) {
                        if (i.isRace()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                case 2: //culture
                    for (Report i : reports) {
                        if (i.isCulture()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                case 3: //gender
                    for (Report i : reports) {
                        if (i.isGender()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                case 4: //sexual orientaiton
                    for (Report i : reports) {
                        if (i.isSex()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                case 5: //other
                    for (Report i : reports) {
                        if (i.isOther()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                case 6: //my reports
                    for (Report i : reports) {
                        if (i.isUser_report()) {
                            new_list.add(i);
                        }
                    }
                    adapter = new ReportAdapter(this, new_list);
                    break;
                    case 7: //last week
                        long week = System.currentTimeMillis() - 604800000;
                        Log.d("week timestamp", String.valueOf(week));
                        for (Report i : reports) {
                            if ((Long.parseLong(i.getRawTimestamp())*1000 - week) > 0) {
                                new_list.add(i);
                            }
                        }
                        adapter = new ReportAdapter(this, new_list);
                        break;
                default:
                    //no list
                    if (reports.isEmpty()) {
                        //do nothing
                    }
                    else {
                        adapter = new ReportAdapter(this, reports);
                    }
                    break;

            }

            list.setAdapter(adapter);
            adapter.notifyDataSetChanged();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
                Uri webpage = Uri.parse("http://microreport.sites.ucsc.edu");
                intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
                break;
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
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position)); //moves camera to campus center
        mMap.setInfoWindowAdapter(new MyInfoWindow());  //uses my version of info window

        // Initialize the cluster manager and renderer and set up listeners
        mClusterManager = new MyClusterManager<Report>(this, mMap);
        clusterRenderer = new MyClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(clusterRenderer);
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        try {
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
        catch (NullPointerException ex) {
            //don't show anything
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

    /**Save markers to an arraylist so don't have to re-download file on screen orientation changes*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("reports", reports);
        //Parcelable position = null;
        try {
           position = mMapFragment.getMap().getCameraPosition();
        }
        catch (NullPointerException ex ){
           position = new CameraPosition(new LatLng(36.991386, -122.060872), 14, 0, 0);
        }
        outState.putParcelable("position", position);
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
     * Refreshes the map. Clears the cache and reloads the activity.
     */
    public void updateMap() {
         if (isNetworkConnected()) {
             Toast.makeText(this, "Updating map...", Toast.LENGTH_SHORT).show();
             //clear cache
             File cacheDir = getCacheDir();
              File[] files = cacheDir.listFiles();
              if (files != null) {
              for (File file : files)
              file.delete();
              }

             //check for and try to post saved reports
             SharedPreferences preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
             Set<String> savedReports = new HashSet();
             savedReports.addAll(preferenceSettings.getStringSet("savedReports", savedReports));
            //loop through and get output
             //todo: check for errors
             for (String r : savedReports) {
                 new postSavedReport().execute(r);
             }
             //clear array and preferences
             savedReports.clear();
             SharedPreferences.Editor preferenceEditor = preferenceSettings.edit();
             preferenceEditor.remove("savedReports");
             preferenceEditor.commit();

             //reload activity
             Intent intent = new Intent(this, MainActivity.class);
             startActivity(intent);
             finish();
         }
        else {
             Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
         }
    }

    static class MyClusterManager<Report> extends ClusterManager {
        private GoogleMap mMap;
        float compareZoom;
        final float maxZoom;

        public MyClusterManager(Context context, GoogleMap map) {
            super(context, map);
            mMap = map;
            maxZoom = mMap.getMaxZoomLevel();
        }

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            float zoom = cameraPosition.zoom;
            compareZoom = maxZoom - zoom;
            super.onCameraChange(cameraPosition);
        }
    }

    /**My own version of cluster renderer (from clustering utility library)
     * shows info window with title and snippet, and blue markers (slightly darker blue for user's
     * reports. */
     static class MyClusterRenderer extends DefaultClusterRenderer<Report> {
        private static final int MIN_CLUSTER_SIZE = 4;
        private MyClusterManager clusterManager;

        public MyClusterRenderer(Context context, GoogleMap map,
                                 MyClusterManager<Report> clusterManager) {
            super(context, map, clusterManager);
            this.clusterManager = clusterManager;
        }

        /**
         * Shows whether markers should cluster. Have to be more than 4 markers and not at the maximum
         * zoom level
         * @param cluster
         * @return
         */
        @Override
        protected boolean shouldRenderAsCluster(Cluster<Report> cluster) {

            if (clusterManager.compareZoom == 0 ) {
                return false;
            }
            return cluster.getSize() > MIN_CLUSTER_SIZE;
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
            //show marker info window
            View v = getLayoutInflater().inflate(R.layout.infowindow_layout, null);
            TextView markerTitle = (TextView) v.findViewById(R.id.marker_title);
            TextView markerDescription = (TextView) v.findViewById(R.id.marker_description);
            markerTitle.setText(marker.getTitle());
            markerDescription.setText(marker.getSnippet());
            v.setContentDescription(marker.getSnippet() + marker.getTitle());
            //don't show info window for clusters in portrait view
            if (findViewById(R.id.reports) == null && marker.getTitle() == null) {
                return null;
            }
            if (findViewById(R.id.reports) != null) {   //landscape view always show info window
                //highlight item in listview based on whether filtered (landscape only)
                Report item = clusterRenderer.getClusterItem(marker);
                ListView list = (ListView) findViewById(R.id.reports);
                list.setSelection(reports.indexOf(item));
            }
            return v;
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
                        date1 = Long.parseLong(report1.getRawTimestamp());
                        date2 = Long.parseLong(report2.getRawTimestamp());
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
                    date1 = Long.parseLong(report1.getRawTimestamp());
                    date2 = Long.parseLong(report2.getRawTimestamp());
                } catch (NumberFormatException ex) {
                    date1 = (long) 0;
                    date2 = (long) 0;
                }
                return (date1 > date2 ? -1 : date1 < date2 ? 1 : 0);
            }
        });
        adapter.notifyDataSetChanged();

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
                return reports;
            }
            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/getreports.php");
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
                    return reports;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return reports;
            }

            return reports;
        }

        @Override
        protected void onPostExecute(ArrayList<Report> reports) {
            super.onPostExecute(reports);
            if (!reports.isEmpty()) {
                setUpMap();
            }
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
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        String partID = preferenceSettings.getString("partID", "none");
        String[] result;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
        //check that the input is actually something
        if (line.contains("Error: ")) {
            reports.add(new Report(new Date().toString(), "Error"+line, "36.991386", "-122.060872", null, false,
                    false, false, false, false, false));
            return reports;
        }
        try {
            do {    //take each line and read parts into report object
                result = line.split("%delim%", 12);
                reports.add(new Report(result[0], result[1]+" ("+result[10]+")", result[2], result[3], result[4], result[4].equals(partID),
                        Boolean.parseBoolean(result[5]), Boolean.parseBoolean(result[6]), Boolean.parseBoolean(result[7]),
                        Boolean.parseBoolean(result[8]), Boolean.parseBoolean(result[9])));
                line = br.readLine();
            } while (!line.equals("%end%")); //add %end% delimiter to php code?
        }
        catch (ArrayIndexOutOfBoundsException ex) { //not sure why this was necessary, it was fine before!
            line = br.readLine();
        }
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
        public final boolean race;
        public final boolean culture;
        public final boolean gender;
        public final boolean sex;
        public final boolean other;


        public Report(String timestamp, String description, String locationLat, String locationLong, String partID, boolean user_report,
                      boolean race, boolean culture, boolean gender, boolean sex, boolean other) {
            this.timestamp = timestamp;
            this.description = description;
            this.locationLat = locationLat;
            this.locationLong = locationLong;
            this.partID = partID;
            this.user_report = user_report;
            this.race = race;
            this.culture = culture;
            this.gender = gender;
            this.sex = sex;
            this.other = other;
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
            Date time;
            try {
                time = new Date(Long.parseLong(timestamp) * 1000);   //convert seconds to milliseconds
            } catch (NumberFormatException ex) {
                time = new Date();
            }
            return sdf.format(time);
        }

        public String getRawTimestamp() {
            return timestamp;
        }

        public boolean isUser_report() {
            return user_report;
        }

        public boolean isRace() {
            return race;
        }

        public boolean isCulture() {
            return culture;
        }

        public boolean isGender() {
            return gender;
        }

        public boolean isOther() {
            return other;
        }

        public boolean isSex() {
            return sex;
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
            out.writeString(getRawTimestamp());
            out.writeString(getDescription());
            out.writeString(getLocationLat());
            out.writeString(getLocationLong());
            out.writeString(getPartID());
            out.writeString(String.valueOf(isUser_report()));
            out.writeString(String.valueOf(isRace()));
            out.writeString(String.valueOf(isCulture()));
            out.writeString(String.valueOf(isGender()));
            out.writeString(String.valueOf(isSex()));
            out.writeString(String.valueOf(isOther()));
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
                return new Report(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(), Boolean.parseBoolean(in.readString()),
                        Boolean.parseBoolean(in.readString()),Boolean.parseBoolean(in.readString()),Boolean.parseBoolean(in.readString()),Boolean.parseBoolean(in.readString()),Boolean.parseBoolean(in.readString()));

            }

            public Report[] newArray(int size) {
                return new Report[size];
            }
        };


    }


    /**
     * Copied from ReportActivity
     * Tries to submit the report to the reports table. The php file echoes the result.
     */
    public class postSavedReport extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (!isNetworkConnected()) {
                return "No network connection";
            }

            String result;
            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/report.php");
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
            toastResult(result);
        }
    }

    private void toastResult (String result) {
        Toast.makeText(this, "Saved Report: "+ result.trim(), Toast.LENGTH_SHORT).show();
    }


    /**set up location client*/
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. Requests last location or tries to connect again
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        }
    }
    @Override
    public void onConnectionSuspended(int i) {

    }
    /*
        * Called by Location Services if the attempt to
        * Location Services fails.
        */
//    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to connect", Toast.LENGTH_SHORT).show();
    }

    /**
     * // necessary to implement this even if don't use it
     * @param location
     */
    //@Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }


}


