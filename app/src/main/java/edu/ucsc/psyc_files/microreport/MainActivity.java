package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import edu.ucsc.psyc_files.microreport.TransformReports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MicroReport 2.0
 * @author Christy M. Byrd, University of California, Santa Cruz
 * Copyright 2015
 * This Android app is used to report microaggressions and display them on a map. It displays a list of markers from a KML
 * file and allows user to post reports in the KML file. Utility pages display campus resources,
 * show a feedback form, and link to the study homepage
 * Requires Google Play Services and Android 3.0+
 * Uses Utility Library: https://github.com/googlemaps/android-maps-utils
 * SimpleKML: https://github.com/Ekito/Simple-KML
 * and ACRA: https://github.com/ACRA/acra
 */
public class MainActivity extends Activity implements OnMapReadyCallback {

    private static MapFragment mMapFragment;  //Google map fragment
    private static ClusterManager<MyItem> mClusterManager;  //Handles rendering of markers at different zoom levels
    private ArrayAdapter<MyItem> adapter;   //handles the list of reports in landscape mode
    private ArrayList<MyItem> myItemCollection; //list of markers taken from the report
    private DefaultClusterRenderer<MyItem> clusterRenderer; //my implementation of Google utility library cluster renderer
    private List<TransformReports.Report> reports;

    /**
     * Opens the main page and displays the reports on a map in clusters if necessary
     * @param savedInstanceState The map clusters are saved in an array on orientation changes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Registered()){  //check if device is registered
            startActivity(new Intent(this, LoginActivity.class)); //if not, launch registration screen
            finish();
        }else {
            setContentView(R.layout.activity_main);

            //NEW LOGIC
            //get reports file
            File reportsFile = new File(getCacheDir(), "reports.xml");
            //if just an orientation change or no network connection, don't download new file
            if (savedInstanceState != null || (!isNetworkConnected())) {
                //use file as is
                //todo: there's an error if there is no file loaded (cache has just been cleared)
                //myItemCollection is saved on outstate, so not sure if loading from there or file here
                //toastResult("orientation change or network not connected");
            } else {
                //check age of cached reports file, then download reports file
                if (System.currentTimeMillis() - reportsFile.lastModified() > 900000) {
                    new downloadReports().execute();
                }
                //otherwise use file as is
            }

            //move on to putting file into list of Report objects
            try {
                TransformReports transform = new TransformReports(reportsFile);
                reports = transform.getReports();

                //check to Google Play Services is installed
                if (checkGooglePlay()) {
                    //set up the map fragment
                    mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
                    mMapFragment.getMapAsync(this);
                }

                }
            catch (FileNotFoundException ex) {
                //not sure if this is the best solution
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            catch (Exception ex) {
                toastResult(ex.toString()+ " in onCreate");
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap mMap) {
        //add stuff to map
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.991386, -122.060872), 14));
        mMap.setInfoWindowAdapter(new MyInfoWindow());

        // Initialize the cluster manager and renderer and set up listeners
        mClusterManager = new ClusterManager<MyItem>(this, mMap);
        clusterRenderer = new MyClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(clusterRenderer);
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        //translate reports into myItemCollection
        //todo: streamline this?
        myItemCollection = new ArrayList<MyItem>();
        for (TransformReports.Report report : reports) {
            Double lat;
            Double longi;

            try {
                lat = Double.parseDouble(report.latitude);
                longi = Double.parseDouble(report.longitude);
            } catch (NumberFormatException ex) {
                lat = 0.0;
                longi = 0.0;
            }
            String snippet = report.classOrEvent + "\n" + report.description;
            myItemCollection.add(new MyItem(report.date, snippet, lat, longi));
        }
        //Add items from reports to ClusterManager and force a recluster (so the items show up)
        mClusterManager.addItems(myItemCollection);
        mClusterManager.cluster();

        //fill listview with reports from array using custom adapter (only visible in landscape mode)
        if (findViewById(R.id.reports) != null) {
            ListView list = (ListView) findViewById(R.id.reports);
            adapter = new ItemAdapter(this, myItemCollection);
            list.setAdapter(adapter);
            list.setOnItemClickListener(mItemClickedHandler);
        }
        /**
        //add points from report list
        for (TransformReports.Report report : reports){
            Double lat;
            Double longi;
            try {
                lat = Double.parseDouble(report.latitude);
                longi = Double.parseDouble(report.longitude);
            } catch (NumberFormatException ex) {
                lat = 0.0;
                longi = 0.0;
            }
            mMap.addMarker(new MarkerOptions()
                    .title(report.description)
                    .snippet(report.description)
                    .position(new LatLng(lat ,longi)));
        }**/
    }

    /**checks whether the device is connected to an internet network*/
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }


    /**save markers to an arraylist so don't have to reserialize kml file on screen orientation changes*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("items",myItemCollection);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private boolean Registered(){
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        return preferenceSettings.getBoolean("registered", false);
    }

    /**creates the menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    /**launches the appropriate action when the user clicks the menu*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_report:
                intent = new Intent(this, ReportActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_gethelp:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_feedback:
                intent = new Intent(this, FeedbackActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_webpage:
                Uri webpage = Uri.parse("http://people.ucsc.edu/~cmbyrd/microaggressionstudy.html");
                intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
                return true;
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
        MyItem item = myItemCollection.get(position);
        v.setContentDescription(item.getSnippet()+item.getTitle());
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
                    //todo: correct url
                    URL url = new URL("http://people.ucsc.edu/~cmbyrd/testdb/process_reports.php");
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
        }

    }

    private void toastResult(String result){
        Toast.makeText(this, ""+ result, Toast.LENGTH_SHORT).show();
    }


    /**creates my own cluster item type that includes the title and snippet (uses the clustering utility library)*/
    public static class MyItem implements ClusterItem, Parcelable {
        private final LatLng mPosition;
        private final String title;
        private final String snippet;

        public MyItem(String title, String snippet, double lat, double lng) {
            mPosition = new LatLng(lat, lng);
            this.title = title;
            this.snippet = snippet;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        public String getTitle() {
            return title;
        }

        public String getSnippet() {
            return snippet;
        }

        //needed for parcelable (to save in saveinstancestate)
        @Override
        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(title);
            out.writeString(snippet);
            out.writeDouble(mPosition.latitude);
            out.writeDouble(mPosition.longitude);
        }

        public static final Parcelable.Creator<MyItem> CREATOR = new Parcelable.Creator<MyItem>() {
            public MyItem createFromParcel(Parcel in) {
                String title, snippet;
                Double latitude, longitude;
                title = in.readString();
                snippet = in.readString();
                latitude = in.readDouble();
                longitude = in.readDouble();
                return new MyItem(title, snippet, latitude, longitude);
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
            markerOptions.title(item.getTitle());
            markerOptions.snippet(item.getSnippet());
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(210));
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

            //highlight item in listview (landscape only)
            if (findViewById(R.id.reports) != null) {
                MyItem item = clusterRenderer.getClusterItem(marker);
                ListView list = (ListView) findViewById(R.id.reports);
                int position = myItemCollection.indexOf(item);
                list.setSelection(position);
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
            super(context, 0 , list);
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
            viewHolder.markerTitle.setText(item.getTitle());
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
}
