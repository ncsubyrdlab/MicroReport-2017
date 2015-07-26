package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.location.places.Place;
//import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.Calendar;
import java.util.Date;

/**
 * Presents a form for the user to submit a report. The user can select "Use current location" and
 * or select a building and the coordinates will be sent on.
 * When the user clicks "Preview", they are sent to the ConfirmReport activity to submit or go back.
 * The report is "formattedreport" and sent as one POST field to the reports file.
 * Uses Google Location Services to find the user's location and display on the map. The location client
 * is started when the activity starts and stops when the activity is no longer visible.
 */
public class ReportActivity extends Activity implements
        ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    private String draftreport;

    public final static String EXTRA_DRAFT = "edu.ucsc.psyc_files.microreport.DRAFT";
    public final static String EXTRA_DATA = "edu.ucsc.psyc_files.microreport.DATA";
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int UPDATE_INTERVAL_IN_SECONDS = 30;
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private double lat;
    private double longi;
    private boolean locationEnabled;
    private MarkerOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        //check internet connection
        if (!isNetworkConnected()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }

        //check to Google Play Services is installed
        checkGooglePlay();

        //set up location client
        buildGoogleApiClient();

        //checks for location enabled
        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationEnabled = !(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        //Create a new location client, using the enclosing class to handle callbacks.
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //sets up map
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        setUpMapIfNeeded();

        //retrieve current location text and current location if just changing screen orientation
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("currentloc")) {
                TextView textView = (TextView) findViewById(R.id.currentlocationtext);
                textView.setText(savedInstanceState.getCharSequence("currentlocationtext"));
                textView.setContentDescription(savedInstanceState.getCharSequence("currentlocationtext"));
                options = savedInstanceState.getParcelable("markeroptions");
                mMap.addMarker(options);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(options.getPosition(), 18));
            }
        }
        //move camera to center of campus if no saved location
        else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.991386, -122.060872), 14));

    }

    //set up location client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //set up PlacePicker https://developers.google.com/places/android/start#api-key
    public void pickPlace(View view) {
        int PLACE_PICKER_REQUEST = 1;
        //PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        //Context context = getApplicationContext();
        //startActivityForResult(builder.build(context), PLACE_PICKER_REQUEST);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // todo: Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    /**
     * Called when the user selects the "use current location" checkbox. The location client is asked
     * for an update, then changes the text of the checkbox to the coordinates and centers the map on the user's location.
     * Unchecking the box resets it.
     * @param view The current activity
     */
    public void useCurrentLocation(View view) {
        CheckBox checkbox = (CheckBox)findViewById(R.id.currentlocationcheckbox);

        if (checkbox.isChecked()){
            if (locationEnabled) {
                //get location update and set (works without this line but might be helpful)
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);

                //cannot get location at the moment
                if (mCurrentLocation == null) {
                    checkbox.setChecked(false);
                    Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();
                    return;
                }

                //otherwise move camera to current location and add marker
                options = new MarkerOptions()
                        .position(new LatLng(lat, longi))
                        .title("Current Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                mMap.addMarker(options);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(options.getPosition(), 18));
                TextView textView = (TextView) findViewById(R.id.currentlocationtext);
                textView.setText("Current Location: " + lat + ", " + longi);
                textView.setContentDescription("Current Location: " + lat + ", " + longi);

                checkbox.setChecked(true);
            }
            else {
                checkbox.setChecked(false);
                Toast.makeText(this, "Enable location services to use current location", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            //turn off checkbox and remove marker
            checkbox.setChecked(false);
            TextView textView = (TextView)findViewById(R.id.currentlocationtext);
            textView.setText("Use current location");
            textView.setContentDescription("Use current location");
           mMap.clear();
        }

    }

    public boolean checkGooglePlay(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        }
        else {
            Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Takes each field and puts it into two reports: draft for displaying in the confirm activity,
     * and one formatted for the KML file
     * @param view The current view
     */
    public void submitReport(View view) {
        boolean self, helse;   //happened to you and/or someone else
        boolean now;    //just now (true)
        String othertime;   //user enters other time/date
        String classdescription;
        String description; //description of event
        boolean currentlocation; //use current location
        String currentlocationtext; //lat and long of current location
        String selectedbuilding; //building from spinner
        String locationtext;    //user enters location

        //coordinates for each building (matches list in string resource/spinner)
        double[][] buildings = {{0.0,0.0},
                {37.000689078482,-122.0578801632},
                {36.987514096338,-122.05866336822},
                {36.977692429956,-122.05431818962},
                {36.994601020807,-122.06105589866},
                {36.997797204754,-122.0555627346},
                {36.994277538941,-122.06595629453},
                {36.97948,-122.05152},
                {36.9982,-122.06047},
                {36.99787,-122.05685},
                {36.99177,-122.06452},
                {36.99127,-122.06485},
                {36.99158,-122.06545},
                {37.00092,-122.06182},
                {36.99721,-122.05407},
                {36.9973,-122.05354},
                {36.99733,-122.05344},
                {36.99683,-122.05304},
                {36.99587,-122.05488},
                {37.00007,-122.05482},
                {36.99985,-122.05488},
                {37.0002,-122.05441},
                {36.99985,-122.05464},
                {36.9991,-122.05392},
                {36.99981,-122.0549},
                {37.00169,-122.05377},
                {37.0017,-122.05396},
                {36.9938,-122.06033},
                {36.99795,-122.05986},
                {36.99397,-122.05296},
                {36.99455,-122.05489},
                {36.99422,-122.05503},
                {36.99427,-122.05518},
                {36.99361,-122.05447},
                {36.99442,-122.05488},
                {36.99435,-122.05441},
                {36.99421,-122.05484},
                {37.00094,-122.06341},
                {36.99335,-122.06048},
                {36.99767,-122.05251},
                {36.9942,-122.06594},
                {36.9996,-122.05775},
                {36.99805,-122.05463},
                {36.99845,-122.05475},
                {36.99837,-122.05459},
                {36.99874,-122.05982},
                {37.00028,-122.06234},
                {37.0005,-122.06294},
                {36.99793,-122.06577},
                {36.99732,-122.06649},
                {36.99727,-122.0661},
                {36.9972,-122.06549},
                {36.99797,-122.06543},
                {36.99745,-122.06695},
                {36.99718,-122.06643},
                {36.99785,-122.06685},
                {36.99871,-122.06622},
                {37.00061,-122.05466},
                {36.94945,-122.06486},
                {36.99165,-122.05115},
                {36.98807,-122.05577},
                {36.99554,-122.05944},
                {36.99523,-122.06171},
                {36.99966,-122.05303},
                {37.00013,-122.05325},
                {36.99998,-122.05371},
                {36.99946,-122.05088},
                {36.9997,-122.05226},
                {36.99859,-122.06051},
                {36.99856,-122.06105},
                {37.00224,-122.05938},
                {36.98979,-122.06277},
                {36.98878,-122.06279},
                {36.98979,-122.06278},
                {36.98888,-122.06238},
                {36.98942,-122.0635},
                {36.94966,-122.06577},
                {36.99497,-122.05408},
                {36.9938,-122.05484},
                {36.994763832352,-122.05420017242},
                {36.99685,-122.05352},
                {36.99979,-122.06172},
                {36.99408,-122.06535},
                {36.99439,-122.06605},
                {36.99445,-122.0658},
                {36.99577,-122.06583},
                {36.9965,-122.0653},
                {36.99871,-122.06162},
                {37.00203,-122.05802},
                {37.0015,-122.05868},
                {36.99726,-122.05201},
                {36.99693,-122.05236},
                {36.99687,-122.05165},
                {36.99718,-122.05146},
                {36.99778,-122.05067},
                {36.99709,-122.05078},
                {36.99464,-122.06193},
                {36.9945,-122.06175},
                {36.99455,-122.06227},
                {36.99477,-122.06263},
                {36.99536,-122.06196},
                {36.99485,-122.06212},
                {36.99495,-122.06217},
                {36.99442,-122.062},
                {36.99514,-122.06232},
                {36.99814,-122.06177},
                {36.99806,-122.06171},
                {36.99361,-122.05467},
                {36.99156,-122.06415},
                {36.99128,-122.06411}};

        //TARGET
        CheckBox checkbox4 = (CheckBox) findViewById(R.id.radio_you);
        CheckBox checkbox3 = (CheckBox) findViewById(R.id.radio_else);
        self = checkbox4.isChecked();
        helse = checkbox3.isChecked();

        //TIME
        CheckBox checkbox1 = (CheckBox) findViewById(R.id.nowcheckbox);
        now = checkbox1.isChecked();
        EditText text1 = (EditText) findViewById(R.id.othertime);
        othertime = text1.getText().toString();

        //DESCRIPTION
        EditText text4 = (EditText) findViewById(R.id.classdescription);
        classdescription = text4.getText().toString();
        EditText text2 = (EditText) findViewById(R.id.description);
        description = text2.getText().toString();

        //LOCATION
        CheckBox checkbox2 = (CheckBox) findViewById(R.id.currentlocationcheckbox);
        currentlocation = checkbox2.isChecked();
        currentlocationtext = lat+", "+longi;
        Spinner spinner = (Spinner) findViewById(R.id.location_spinner);
        selectedbuilding = spinner.getSelectedItem().toString();
        EditText text3 = (EditText) findViewById(R.id.locationtextbox);
        locationtext = text3.getText().toString();
        //set lat and longi to building if one is selected
        if (!currentlocation) {
            lat = buildings[spinner.getSelectedItemPosition()][0];
            longi = buildings[spinner.getSelectedItemPosition()][1];
        }

        //TIMESTAMP
        Date d = Calendar.getInstance().getTime();

        //Android ID (some devices have same one in 2.2 (9774d56d682e549c), not reliable prior to 2.2, changes with factory reset
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        SharedPreferences preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        String partid = preferenceSettings.getString("partID", "000000");

       // String[] formattedreport = {Installation.id(this), androidId, String.valueOf(self), String.valueOf(now), othertime, classdescription,
       //         String.valueOf(currentlocation), locationtext, selectedbuilding, String.valueOf(lat), String.valueOf(longi), partid};
        String output = "installID="+Uri.encode(Installation.id(this))+"&deviceID="+Uri.encode(androidId)
                +"&target="+Uri.encode(String.valueOf(self))+"&currentTime="+Uri.encode(String.valueOf(now))
                +"&otherTime="+Uri.encode(othertime)+"&classOrEvent="+Uri.encode(classdescription)+
                "&currentLocation="+Uri.encode(String.valueOf(currentlocation))+"&otherLocation="+Uri.encode(locationtext)
                +"&building="+Uri.encode(selectedbuilding)+"&description="+Uri.encode(description)
                +"&latitude="+Uri.encode(String.valueOf(lat))+"&longitude="+Uri.encode(String.valueOf(longi))
                +"&partID="+Uri.encode(partid);
        /**
                "\n<Placemark><name>"+d+"</name>" +
                "<ExtendedData xmlns:report=\"ucsc.edu\"><report:DeviceInfo>InstallationID: "+ Installation.id(this)+
                " AndroidID: "+ androidId+"</report:DeviceInfo><report:Timestamp>"+d+"</report:Timestamp><report:Target-self>"+self+"</report:Target-self><report:Target-else>"+helse+
                "</report:Target-else><report:CurrentTime>"+now+"</report:CurrentTime><report:OtherTime>"+othertime+
                "</report:OtherTime><report:ClassOrEvent>"+classdescription+"</report:ClassOrEvent><report:CurrentLocation>"+currentlocation+
                "</report:CurrentLocation><report:Building>"+selectedbuilding+"</report:Building><report:OtherLocation>"+locationtext+"</report:OtherLocation></ExtendedData>"+
                "<description><![CDATA["+classdescription+" - "+description+"]]></description>" +
                "<Point><coordinates>"+longi+","+lat+",0.0</coordinates></Point></Placemark>\n"; **/

       draftreport = "";
        if (self) {
            draftreport = "Happened to you\n";
        }
        if (helse) {
            draftreport = draftreport + "Happened to someone else\n";
        }

        if (now) {
            draftreport = draftreport + "\nDate/time: Just now\nClass or event: "+classdescription + "\nDescription: " + description + "\n";
        }
        if (!now) {
            draftreport = draftreport + "\nDate/time: " + othertime + "\nClass or event: "+classdescription + "\nDescription: " + description + "\n";
        }

        if (currentlocation) {
            draftreport = draftreport +currentlocationtext+"\nOther location details: " + locationtext;
        }
        if (!currentlocation && spinner.getSelectedItemPosition() == 0) { //did not choose building
            draftreport = draftreport + "\nLocation: " + locationtext;
        }
        if (!currentlocation && spinner.getSelectedItemPosition() > 0) {
            draftreport = draftreport + "\nLocation: " + selectedbuilding + " " + locationtext;
        }
        //open confirm report activity with draft report
        Intent intent = new Intent(this, ConfirmReport.class);
        intent.putExtra(EXTRA_DRAFT, draftreport);
        intent.putExtra(EXTRA_DATA, output);
        startActivity(intent);
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. Requests last location or tries to connect again
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
         if (locationEnabled) {
            if (mCurrentLocation == null) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
            } else {
                lat = mCurrentLocation.getLatitude();
                longi = mCurrentLocation.getLongitude();
             }
        }
      }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to connect", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        // necessary to implement this even if don't use it
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.home:
                NavUtils.navigateUpFromSameTask(this);
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
            case R.id.action_bulletin:
                intent = new Intent(this, BulletinBoard.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Saves checkbox and current location marker when activity is hidden
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        CheckBox checkbox = (CheckBox)findViewById(R.id.currentlocationcheckbox);
        if (checkbox.isChecked()) {
            outState.putBoolean("currentloc",true);
            outState.putParcelable("markeroptions", options);
            TextView textView = (TextView) findViewById(R.id.currentlocationtext);
            outState.putCharSequence("currentlocationtext", textView.getText());
        }
        else outState.putBoolean("currentloc", false);
        super.onSaveInstanceState(outState);
    }
}
