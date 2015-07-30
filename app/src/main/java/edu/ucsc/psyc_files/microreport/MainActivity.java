package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Base64;
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
import android.widget.Toolbar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * MicroReport 2.0
 * @author Christy M. Byrd, University of California, Santa Cruz
 * Copyright 2015
 * This Android app is used to report microaggressions and display them on a map. It displays a list of markers from a KML
 * file and allows user to post reports in the KML file. Utility pages display campus resources,
 * show a feedback form, and link to the study homepage
 * Requires Google Play Services and Android 3.0+
 * Uses Utility Library: https://github.com/googlemaps/android-maps-utils
 * and ACRA: https://github.com/ACRA/acra
 */
public class MainActivity extends Activity implements OnMapReadyCallback {

    private static MapFragment mMapFragment;  //Google map fragment
    private static ClusterManager<MyItem> mClusterManager;  //Handles rendering of markers at different zoom levels
    private ArrayAdapter<MyItem> adapter;   //handles the list of reports in landscape mode
    private ArrayList<MyItem> myItemCollection; //list of markers taken from the report (through TransformReports)
    private ArrayList<MyItem> filteredReports; //filtered reports for use in sorting and viewing own reports
    private DefaultClusterRenderer<MyItem> clusterRenderer; //my implementation of Google utility library cluster renderer
    private List<TransformReports.Report> reports;  //reports file that is transformed into myItemCollection
    private ListView nav;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Opens the main page and displays the reports on a map in clusters if necessary
     * @param savedInstanceState The map clusters are saved in an array on orientation changes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (isNetworkConnected()) {//if network is not connected, don't do anything
            //todo: errors thrown when come back from submitting report
            setContentView(R.layout.activity_main);

            //if just an orientation change or no network connection, don't download new file
            if (savedInstanceState != null)  {
                myItemCollection = savedInstanceState.getParcelableArrayList("items");
                filteredReports = savedInstanceState.getParcelableArrayList("filtereditems");
                //toastResult("using savedInstanceState");
                setUpMap();
            }
            else {
                //check if device is registered, if not, the activity will automatically close
                new checkRegistration().execute();
                //check age of cached reports file and download reports file
                File reportsFile = new File(getCacheDir(), "reports.xml");
                if (System.currentTimeMillis() - reportsFile.lastModified() > 900000) {
                    //toastResult("downloading report");
                    new downloadReports().execute();
                    //the async task will update the reports file in the cache and set up the map (calls onMapReady)
                } else {
                    //toastResult("using cached file");
                    setUpMap(); //this function just uses myItemCollection and skips the xml step
                }
            }
        } else {    //end if network is connected
            toastResult("No internet connection");
        }

        //todo: change actionbar and overflow options
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

    public void onMapReady(GoogleMap mMap) {
        //this is called from the async task in setUpMap()
        //add stuff to map
        mMap.setMyLocationEnabled(true); //shows user location
        UiSettings settings = mMap.getUiSettings();
        settings.setMapToolbarEnabled(false);  //doesn't show toolbar that links to Google Maps app
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.991386, -122.060872), 14)); //moves camera to campus center
        mMap.setInfoWindowAdapter(new MyInfoWindow());  //uses my version of info window

        // Initialize the cluster manager and renderer and set up listeners
        mClusterManager = new ClusterManager<MyItem>(this, mMap);
        clusterRenderer = new MyClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(clusterRenderer);
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        //finally add and display items
        //Add items from reports to ClusterManager and force a recluster (so the items show up)
        //final check if myItemCollection is null for some reason
        if (myItemCollection == null) {
            new downloadReports().execute();
            return;
        }
        mClusterManager.addItems(myItemCollection);
        mClusterManager.cluster();

        //fill listview with reports from array using custom adapter (only visible in landscape mode)
        if (findViewById(R.id.reports) != null) {
            ListView list = (ListView) findViewById(R.id.reports);
            adapter = new ItemAdapter(this, myItemCollection);
            list.setAdapter(adapter);
            list.setOnItemClickListener(mItemClickedHandler);
        }

    }


    /**checks whether the device is connected to an internet network*/
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }


    /**save markers to an arraylist so don't have to re-download file on screen orientation changes*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("items",myItemCollection);
        outState.putParcelableArrayList("filtereditems",filteredReports);
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
        //ReportDialogFragment newFragment = new ReportDialogFragment();
        //newFragment.show(getFragmentManager(), "New Report");

        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    /**launches the appropriate action when the user clicks the menu*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        Intent intent;
        switch (item.getItemId()) {
            /** now a floating action button
             * case R.id.action_report:
                intent = new Intent(this, ReportActivity.class);
                startActivity(intent);
                return true; */
            case R.id.action_clear_cache:
                Toast.makeText(this, "Updating map...", Toast.LENGTH_SHORT).show();
                //todo: this creates a "file not found error" but changing orientation is OK
                /**File cacheDir = getCacheDir();
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files)
                        file.delete();
                } **/
                //reload activity
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**When the user clicks on a list item, the map zooms to the marker and opens the info window
     * https://github.com/ch8908/thor-android/blob/100ff882515be390c3e0ac7f705e1ec10c7d5d90/thor-android/src/main/java/com/osolve/thor/activity/MainActivity.java*/
    private AdapterView.OnItemClickListener mItemClickedHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView parent, View v, int position, long id) {
        v.setSelected(true);
        MyItem item;
            //check whether to get position of item in filtered view or whole view
            ToggleButton button = (ToggleButton) findViewById(R.id.my_reports);
            if (button.isChecked()) {
                item = filteredReports.get(position);
            } else {
                item = myItemCollection.get(position);
            }

        v.setContentDescription(item.getSnippet()+item.getDate());
        mMapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(item.getPosition(), 18));
            Marker marker = clusterRenderer.getMarker(item);
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    };

    private class downloadReports extends AsyncTask<Void, Void, String> {
        //connects to PHP script that copies report file with just display data, then
        // copies the processed reports file into the cache
        @Override
        protected String doInBackground(Void... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return "No network connection";
            }
                //enables response caching on 4.0 and above
                enableHttpResponseCache();

                try {
                    URL url = new URL("http://people.ucsc.edu/~cmbyrd/microreport/process_reports.php");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                    con.setRequestProperty("Authorization", basicAuth);
                    //reads into cache
                    File reportsFile = new File(getCacheDir(), "reports.xml");

                    if (con.getResponseCode() == 200) {
                        FileWriter out = new FileWriter(reportsFile);
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String line = in.readLine();
                        do {
                            out.write(line);
                            line = in.readLine();
                        } while (line != null);
                        out.close();
                        in.close();
                        con.disconnect();

                        //todo: check registration here?

                        return "Success";
                    } else return con.getResponseMessage();
                } catch (Exception ex) {
                    return ex.toString();
                }

        }

        @Override
        protected void onPostExecute(String message) {
            super.onPostExecute(message);
            if (!message.contentEquals("Success")) { //don't report anything if successfully downloaded
                toastResult(message + " in downloadReports");
            }
            else {
                try {
                    //put reports from xml file into reports variable
                    File reportsFile = new File(getCacheDir(), "reports.xml");
                    TransformReports transform = new TransformReports(reportsFile);
                    reports = transform.getReports();
                    //translate reports into myItemCollection
                    //todo: streamline this by making myItemCollection and TransformReports the same?
                    myItemCollection = new ArrayList<MyItem>();
                    filteredReports = new ArrayList<MyItem>();

                    for (TransformReports.Report report : reports) {
                        //change uDate to readable format
                        String displayDate;
                        try {
                            //http://stackoverflow.com/questions/17432735/convert-unix-time-stamp-to-date-in-java
                            long unixSeconds = Long.parseLong(report.date);
                            Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
                            SimpleDateFormat sdf = new SimpleDateFormat("E, MMMM d hh:mm a"); // the format of your date
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT-8")); // give a timezone reference for formmating (see comment at the bottom
                            displayDate = sdf.format(date);
                        }
                        catch (NumberFormatException ex) {
                            displayDate = "No Date";
                        }

                        Double lat;
                        Double longi;

                        try {
                            lat = Double.parseDouble(report.latitude);
                            longi = Double.parseDouble(report.longitude);
                        } catch (NumberFormatException ex) {
                            lat = 0.0;
                            longi = 0.0;
                        }

                        //flag for user's reports
                        boolean flag = false;
                        SharedPreferences preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                        String userPartID = preferenceSettings.getString("partID", "");
                        if (report.partID != null && report.partID.length() >0 ) {
                            if (report.partID.compareTo(userPartID)==0) {
                                flag = true;
                            }
                        }

                        String snippet = report.classOrEvent + " - " + report.description;
                        //create new item with everything
                        MyItem item = new MyItem(displayDate, report.date, snippet, lat, longi, flag);
                        myItemCollection.add(item);
                        //add to filteredReports while at it
                        if (flag) {
                            filteredReports.add(item);
                        }
                    }
                    //if filteredReports is null at this point, add a dummy item
                    if (filteredReports == null) {
                        filteredReports.add(new MyItem("No Reports","","User has no reports",0.0,0.0,true));
                    }
                    setUpMap();
                } catch (Exception ex) {
                    toastResult(ex.toString() + " in TransformReports try");
                    ex.printStackTrace();
                }
            }
        }

    }

    private void setUpMap(){
        //check to Google Play Services is installed
        if (checkGooglePlay()) {
            //set up the map fragment
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mMapFragment.getMapAsync(this);
        } else Toast.makeText(this, "Install Google Play Services", Toast.LENGTH_SHORT).show();
    }

    private void toastResult(String result){
        Toast.makeText(this, ""+ result, Toast.LENGTH_SHORT).show();
    }


    /**creates my own cluster item type that includes the title and snippet (uses the clustering utility library)*/
    public static class MyItem implements ClusterItem, Parcelable {
        private final String displayDate;
        private final String date;
        private final String snippet;
        private final LatLng mPosition;
        private final boolean flag;

        public MyItem(String displayDate, String date, String snippet, double lat, double lng, boolean flag) {
            mPosition = new LatLng(lat, lng);
            this.date = date;
            this.snippet = snippet;
            this.displayDate = displayDate;
            this.flag = flag;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        public String getDisplayDate() {
            return displayDate;
        }

        public String getDate() {
            return date;
        }

        public String getSnippet() {
            return snippet;
        }

        public boolean getFlag() { return flag; }

        //needed for parcelable (to save in saveinstancestate)
        @Override
        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(displayDate);
            out.writeString(date);
            out.writeString(snippet);
            out.writeDouble(mPosition.latitude);
            out.writeDouble(mPosition.longitude);
            out.writeString(String.valueOf(flag));
        }

        public static final Parcelable.Creator<MyItem> CREATOR = new Parcelable.Creator<MyItem>() {
            public MyItem createFromParcel(Parcel in) {
                String displayDate, date, snippet;
                Double latitude, longitude;
                String flag;
                displayDate = in.readString();
                date = in.readString();
                snippet = in.readString();
                latitude = in.readDouble();
                longitude = in.readDouble();
                flag = in.readString();
                return new MyItem(displayDate, date, snippet, latitude, longitude, Boolean.valueOf(flag));
            }

            public MyItem[] newArray(int size) {
                return new MyItem[size];
            }
        };
    }

    /**my own version of cluster renderer (from clustering utility library)
     * shows info window with title and snippet, and blue markers*/
     static class MyClusterRenderer extends DefaultClusterRenderer<MyItem> {

        public MyClusterRenderer(Context context, GoogleMap map,
                                 ClusterManager<MyItem> clusterManager) {
            super(context, map, clusterManager);
        }



        @Override
        protected void onBeforeClusterItemRendered(MyItem item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
            markerOptions.title(item.getDisplayDate());
            markerOptions.snippet(item.getSnippet());
            if (item.getFlag()){
                //colors: https://developers.google.com/android/reference/com/google/android/gms/maps/model/BitmapDescriptorFactory
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(200));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(220));
            }
        }
    }

    /**my info window design, shows the whole description, with the timestamp in small text below*/
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
                MyItem item = clusterRenderer.getClusterItem(marker);
                ListView list = (ListView) findViewById(R.id.reports);
                ToggleButton button = (ToggleButton) findViewById(R.id.my_reports);
                if (button.isChecked()) {
                    list.setSelection(filteredReports.indexOf(item));
                } else {
                    list.setSelection(myItemCollection.indexOf(item));
                }

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
    public class ItemAdapter extends ArrayAdapter<MyItem> {
        private class ViewHolder {
            TextView markerTitle;
            TextView markerDescription;
        }

        public ItemAdapter(Context context, ArrayList<MyItem> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyItem item = getItem(position);
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
            viewHolder.markerTitle.setText(item.getDisplayDate());
            viewHolder.markerDescription.setText(item.getSnippet());
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

    //button presses
    public void sortAscending(View view){
        //check whether to sort all reports or filtered report
        ToggleButton button = (ToggleButton) findViewById(R.id.my_reports);
        //http://www.limbaniandroid.com/2013/01/sorting-arraylist-string-arraylist-and.html
        if (button.isChecked()) {
            Collections.sort(filteredReports, new Comparator<MyItem>() {
                public int compare(MyItem report1, MyItem report2) {
                    Long date1, date2;
                    try {
                        date1 = Long.parseLong(report1.getDate());
                        date2 = Long.parseLong(report2.getDate());
                    } catch (NumberFormatException ex) {
                        date1 = (long) 0;
                        date2 = (long) 0;
                    }
                    return (date1 < date2 ? -1 : date1 > date2 ? 1 : 0);
                }
            });
        } else {
            Collections.sort(myItemCollection, new Comparator<MyItem>() {
                public int compare(MyItem report1, MyItem report2) {
                    Long date1, date2;
                    try {
                        date1 = Long.parseLong(report1.getDate());
                        date2 = Long.parseLong(report2.getDate());
                    } catch (NumberFormatException ex) {
                        date1 = (long) 0;
                        date2 = (long) 0;
                    }
                    return (date1 < date2 ? -1 : date1 > date2 ? 1 : 0);
                }
            });
        }
        adapter.notifyDataSetChanged();
    }

    public void sortDescending(View view) {
        //check whether to sort all reports or filtered report
        ToggleButton button = (ToggleButton) findViewById(R.id.my_reports);
        if (button.isChecked()) {
            Collections.sort(filteredReports, new Comparator<MyItem>() {
                public int compare(MyItem report1, MyItem report2) {
                    Long date1, date2;
                    try {
                        date1 = Long.parseLong(report1.getDate());
                        date2 = Long.parseLong(report2.getDate());
                    } catch (NumberFormatException ex) {
                        date1 = (long) 0;
                        date2 = (long) 0;
                    }
                    return (date1 > date2 ? -1 : date1 < date2 ? 1 : 0);
                }
            });
        } else {
            Collections.sort(myItemCollection, new Comparator<MyItem>() {
                public int compare(MyItem report1, MyItem report2) {
                    Long date1, date2;
                    try {
                        date1 = Long.parseLong(report1.getDate());
                        date2 = Long.parseLong(report2.getDate());
                    } catch (NumberFormatException ex) {
                        date1 = (long) 0;
                        date2 = (long) 0;
                    }
                    return (date1 > date2 ? -1 : date1 < date2 ? 1 : 0);
                }
            });
        }
        adapter.notifyDataSetChanged();
    }

    public void viewMyReports (View view){
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            //set view to filtered reports
                adapter = new ItemAdapter(this, filteredReports);
                ListView list = (ListView) findViewById(R.id.reports);
                list.setAdapter(adapter);
                list.setOnItemClickListener(mItemClickedHandler);
                adapter.notifyDataSetChanged();
        } else {
            //reset list view to original list
            ListView list = (ListView) findViewById(R.id.reports);
            adapter = new ItemAdapter(this, myItemCollection);
            list.setAdapter(adapter);
            list.setOnItemClickListener(mItemClickedHandler);
            adapter.notifyDataSetChanged();
        }
    }

    private class checkRegistration extends AsyncTask<Void, Void, String> {
        //checks device ID in user file
        @Override
        protected String doInBackground(Void... params) {
            if (!isNetworkConnected()) {
                //cancel if network is not connected
                return "No network connection";
            }
            //enables response caching on 4.0 and above
            enableHttpResponseCache();

            try {
                //todo also log access here?
                URL url = new URL("http://people.ucsc.edu/~cmbyrd/microreport/check_registration.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                con.setRequestProperty("Authorization", basicAuth);

                //get partID from SharedPreferences and compare to file
                SharedPreferences preferenceSettings;
                preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                String partID = preferenceSettings.getString("partID", "Not Registered");
                if (partID == "Not Registered") {
                    return "false";
                }
                    //check if ID is in user file
                    OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                    out.write("partID="+partID.trim()); //trim whitespace
                    out.close();

                    if (con.getResponseCode() == 200) {
                    //the php file will echo "true" or "false"
                        BufferedReader reader = null;
                        StringBuilder stringBuilder;
                        reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        stringBuilder = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line + "\n");
                        }
                        String result = stringBuilder.toString();
                        return result;
                    } else {
                        return con.getResponseMessage();
                    }

            } catch (Exception ex) {
                return ex.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.contains("false")) {
            //device is not registered
                startLoginActivity();
            }
        }

    }

    private void startLoginActivity(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
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


}


