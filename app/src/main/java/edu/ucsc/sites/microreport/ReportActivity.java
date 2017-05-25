package edu.ucsc.sites.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;


/**
 * Presents a form for the user to submit a report. DIsplayed as a dialog. The user can select
 * "Use current location" and have their location recorded or or select a building and the
 * coordinates will be sent on. All of the fields are combined and sent to a php file on the server.
 * Uses Google Location Services to find the user's location. The location client
 * is started when the activity starts and stops when the activity is no longer visible.
 * v3: changed reporting form, removed building coordinates
 */
public class ReportActivity extends Activity implements
        ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    public final static String EXTRA_DRAFT = "edu.ucsc.psyc_files.microreport.DRAFT";
    public final static String EXTRA_DATA = "edu.ucsc.psyc_files.microreport.DATA";
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private boolean locationEnabled;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Sets up the form and button listeners, checks Google Play Services, and starts location updates.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        setFinishOnTouchOutside(false); //don't close if touch outside dialog

        //check that Google Play Services is installed
        checkGooglePlay();

        //set up location client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        //checks for location enabled
        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationEnabled = !(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        //Create a new location client, using the enclosing class to handle callbacks.
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //button actions
        Button cancel_button = (Button) findViewById(R.id.cancel);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        final Button submit_button = (Button) findViewById(R.id.submit);
        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                SharedPreferences preferenceSettings;
                preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                EditText description_text = (EditText) findViewById(R.id.description);

                //get dialog fields
                String description = description_text.getText().toString();
                CheckBox current_location_box = (CheckBox) findViewById(R.id.currentlocationcheckbox);
                EditText location_text = (EditText) findViewById(R.id.locationtextbox);
                String other_location = location_text.getText().toString();
                CheckBox race_check = (CheckBox) findViewById(R.id.race_check);
                CheckBox culture_check = (CheckBox) findViewById(R.id.culture_check);
                CheckBox gender_check = (CheckBox) findViewById(R.id.gender_check);
                CheckBox sexual_orientation_check = (CheckBox) findViewById(R.id.sexual_orientation_check);
                CheckBox else_check = (CheckBox) findViewById(R.id.else_check);
                CheckBox not_sure_check = (CheckBox) findViewById(R.id.not_sure_check);
                SeekBar bother_seek = (SeekBar) findViewById(R.id.bother_seek);

                boolean currentlocation = current_location_box.isChecked();
                String lat, longi;

                if (currentlocation) {  //use device location
                    lat = String.valueOf(mCurrentLocation.getLatitude());
                    longi = String.valueOf(mCurrentLocation.getLongitude());

                } else {
                    //use home zipcode
                    String homeZIP = preferenceSettings.getString("homeZIP", "36.991386,-122.060872");
                    String[] latlong =  homeZIP.split(",");
                    lat = latlong[0];
                    longi = latlong[1];
                    description = description+" [default location]";
                }

                boolean race = race_check.isChecked();
                boolean culture = culture_check.isChecked();
                boolean gender = gender_check.isChecked();
                boolean sexual_orientation = sexual_orientation_check.isChecked();
                boolean else_checked = else_check.isChecked();
                boolean not_sure = not_sure_check.isChecked();
                int bother = bother_seek.getProgress();
                String partID = preferenceSettings.getString("partID", "no ID");

                //check for empty description
                description_text.setError(null);
                if (TextUtils.isEmpty(description)) {
                    description_text.setError(getString(R.string.error_field_required));
                    return;
                }

                //post report
                String output = "description=" + Uri.encode(description.replace("'", "\\'")) + "&latitude=" + Uri.encode(lat) +
                        "&longitude=" + Uri.encode(longi) + "&loc_description=" + Uri.encode(other_location.replace("'", "\\'")) +
                        "&notDefaultLocation=" + String.valueOf(currentlocation) +
                        "&raceCheck=" + String.valueOf(race) +
                        "&cultureCheck=" + String.valueOf(culture) + "&genderCheck=" + String.valueOf(gender) +
                        "&sexCheck=" + String.valueOf(sexual_orientation) + "&otherCheck=" +
                        String.valueOf(else_checked) + "&notsureCheck=" + String.valueOf(not_sure) +
                        "&bother=" + Uri.encode(String.valueOf(bother)) +
                        "&installationID=" + Uri.encode(Installation.id(getBaseContext())) +
                        "&ID=" + Uri.encode(partID);


                //check for internet connection
                if (!isNetworkConnected()) {
                    if (saveReport(output)) {
                        Toast.makeText(getBaseContext(), "No internet connection: Saving report.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getBaseContext(), "No internet connection. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    //post report
                    new postReport().execute(output);
                }

                //clear cache
                File cacheDir = getCacheDir();
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files)
                        file.delete();
                }
                //restart activity
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    /**
     * If there is no internet connection, tries to save the report to shared preferences to send out next time map is refreshed
     * @param output The assembled report file.
     * @return
     */
    private boolean saveReport(String output) {
        SharedPreferences preferenceSettings;
        SharedPreferences.Editor preferenceEditor;
        try {
            preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
            //copy existing reports from string set
            Set<String> savedReports = new HashSet();
            savedReports.addAll(preferenceSettings.getStringSet("savedReports", savedReports));
            //add latest report to set
            savedReports.add(output);
            preferenceEditor = preferenceSettings.edit();
            preferenceEditor.putStringSet("savedReports", savedReports);
            preferenceEditor.apply();
            return true;
        } catch (Exception ex) {
            Log.d("saveReport", ex + "output: "+ output);
            return false;
        }
    }

    /**
     * Tries to submit the report to the reports table. The php file echoes the result.
     */
    public class postReport extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (!isNetworkConnected()) {
                return "No network connection";
            }

            String result;
            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/report.php");
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
        Toast.makeText(this, ""+ result.trim(), Toast.LENGTH_SHORT).show();
    }

    /**checks if network is connected*/
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return (isConnected);
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

    /**
     * Checkbox for user to use current location. Attempts to get location, but doesn't allow the
     * box to be checked if the location is 0,0 or the location is not turned on.
     * @param view
     */
    public void useCurrentLocation(View view){
                //sets checkbox to user's current location if possible
                CheckBox checkbox = (CheckBox)findViewById(R.id.currentlocationcheckbox);
                ImageView image = (ImageView) findViewById(R.id.location_image);
                if (checkbox.isChecked()){
                    Location zero = new Location("");
                        zero.setLatitude(0.0);
                        zero.setLongitude(0.0);
                        if (mCurrentLocation == null || mCurrentLocation == zero) {
                                checkbox.setChecked(false);
                                image.setImageResource(R.drawable.ic_location_off_black_24dp);
                                Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();
                                return;
                        } else {
                            //turn on checkbox
                            image.setImageResource(R.drawable.ic_location_on_black_24dp);
                            checkbox.setChecked(true);
                                //Toast.makeText(this, "Location: "+mCurrentLocation.getLatitude()+" "+mCurrentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                        }
                }
                else {
                    //turn off checkbox
                    checkbox.setChecked(false);
                    image.setImageResource(R.drawable.ic_location_off_black_24dp);
                }
        }


    /**
     * Checks that Google Play Services are installed. Doesn't do anything if not.
     * @return
     */
    public boolean checkGooglePlay(){
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
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
            }
        }
      }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
    }

    /**
     * // necessary to implement this even if don't use it
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

    }

    /**
     * May not be necessary since checkboxes persist across orientation changes.
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


}
