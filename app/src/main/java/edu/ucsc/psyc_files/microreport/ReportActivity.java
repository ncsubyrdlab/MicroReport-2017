package edu.ucsc.psyc_files.microreport;

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
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Presents a form for the user to submit a report. DIsplayed as a dialog. The user can select
 * "Use current location" and have their location recorded or or select a building and the
 * coordinates will be sent on. All of the fields are combined and sent to a php file on the server.
 * Uses Google Location Services to find the user's location. The location client
 * is started when the activity starts and stops when the activity is no longer visible.
 */
public class ReportActivity extends Activity implements
        ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    public final static String EXTRA_DRAFT = "edu.ucsc.psyc_files.microreport.DRAFT";
    public final static String EXTRA_DATA = "edu.ucsc.psyc_files.microreport.DATA";
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private boolean locationEnabled;
    //coordinates for each building
    String[][] buildings = {{"Select a building", "9/10 Multipurpose Room",
            "Agroecology",
            "Barn Theater",
            "Basking Visual Arts",
            "Bay Tree Conference Center",
            "Bridge Gallery",
            "Cardiff House",
            "Center for Adaptive Optics",
            "Classroom Unit",
            "College Eight Academic",
            "College Eight Commons",
            "College Eight/Oakes Dining Hall",
            "Community Building",
            "Cowell Academic Building",
            "Cowell Classroom Building",
            "Cowell Community Room",
            "Cowell Dining Hall",
            "Cowell Press",
            "Crown Classroom Building",
            "Crown Computing Lab",
            "Crown Dining Hall",
            "Crown Faculty Wing",
            "Crown Provost House",
            "Crown Senior Commons Room",
            "Crown/Merrill Community Room",
            "Crown/Merrill Recreation Room",
            "Digital Arts Research Center",
            "Earth & Marine Sciences",
            "East Field",
            "East Field House",
            "East Field House Activities Room",
            "East Field House Dance Studio",
            "East Field House Gym",
            "East Field House Martial Arts Room",
            "East Field House Tennis Courts",
            "East Racquetball Court",
            "Engineering 2",
            "Gamelan Studio",
            "Hahn Art Facility",
            "Hall Gallery",
            "Health Center",
            "Humanities 1",
            "Humanities and Social Sciences Building",
            "Humanities Lecture",
            "Interdisciplinary Sciences Building",
            "Jack Baskin Auditorium",
            "Jack Baskin Engineering",
            "Krege Classroom Building",
            "Kresge Academic Building",
            "Kresge Annex A",
            "Kresge Annex B",
            "Kresge Library",
            "Kresge Lounge",
            "Kresge Provost House",
            "Kresge Recreation Center",
            "Kresge Town Hall",
            "KZSC",
            "Long Marine Lab Discovery Center",
            "Lower East Field",
            "Lower Quarry",
            "McHenry Library",
            "Media Theater",
            "Merrill Academic Building",
            "Merrill Baobab Room",
            "Merrill Cultural Center",
            "Merrill Provost House",
            "Ming Ong Computing Lab",
            "Natural Sciences 2",
            "Natural Sciences 2 Annex",
            "Nine Community Room",
            "Oakes Academic Building",
            "Oakes Garden",
            "Oakes Library",
            "Oakes Provost House",
            "Oakes Tutoring Center",
            "Ocean Health Building",
            "OPERS Conference Room",
            "OPERS Multipurpose Room",
            "OPERS Pool",
            "Page Smith Library",
            "Physical Sciences Building",
            "Porter Academic Building",
            "Porter Dining Hall",
            "Porter Hitchcock Lounge (Porter Fireside Lounge)",
            "Porter Provost House",
            "Porter/Kresge Lounge",
            "Sinsheimer Laboratories",
            "Social Sciences 1",
            "Social Sciences 2",
            "Stevenson Academic Building",
            "Stevenson Event Center (Dining Hall)",
            "Stevenson Fireside Lounge",
            "Stevenson Library",
            "Stevenson Music Practice Room",
            "Stevenson Provost House",
            "Theater Arts 2nd Stage",
            "Theater Arts 2nd Stage Annex",
            "Theater Arts Costume Shop",
            "Theater Arts Drama",
            "Theater Arts Experimental Theater",
            "Theater Arts Lecture",
            "Theater Arts Mainstage",
            "Theater Arts Offices",
            "Theater Arts Studio",
            "Thimann Laboratories",
            "Thimann Lecture Hall",
            "Wellness Center",
            "West Field House",
            "West Gym"},
            {"36.991386","37.000689078482",
                    "36.987514096338",
                    "36.977692429956",
                    "36.994601020807",
                    "36.997797204754",
                    "36.994277538941",
                    "36.97948",
                    "36.9982",
                    "36.99787",
                    "36.99177",
                    "36.99127",
                    "36.99158",
                    "37.00092",
                    "36.99721",
                    "36.9973",
                    "36.99733",
                    "36.99683",
                    "36.99587",
                    "37.00007",
                    "36.99985",
                    "37.0002",
                    "36.99985",
                    "36.9991",
                    "36.99981",
                    "37.00169",
                    "37.0017",
                    "36.9938",
                    "36.99795",
                    "36.99397",
                    "36.99455",
                    "36.99422",
                    "36.99427",
                    "36.99361",
                    "36.99442",
                    "36.99435",
                    "36.99421",
                    "37.00094",
                    "36.99335",
                    "36.99767",
                    "36.9942",
                    "36.9996",
                    "36.99805",
                    "36.99845",
                    "36.99837",
                    "36.99874",
                    "37.00028",
                    "37.0005",
                    "36.99793",
                    "36.99732",
                    "36.99727",
                    "36.9972",
                    "36.99797",
                    "36.99745",
                    "36.99718",
                    "36.99785",
                    "36.99871",
                    "37.00061",
                    "36.94945",
                    "36.99165",
                    "36.98807",
                    "36.99554",
                    "36.99523",
                    "36.99966",
                    "37.00013",
                    "36.99998",
                    "36.99946",
                    "36.9997",
                    "36.99859",
                    "36.99856",
                    "37.00224",
                    "36.98979",
                    "36.98878",
                    "36.98979",
                    "36.98888",
                    "36.98942",
                    "36.94966",
                    "36.99497",
                    "36.9938",
                    "36.994763832352",
                    "36.99685",
                    "36.99979",
                    "36.99408",
                    "36.99439",
                    "36.99445",
                    "36.99577",
                    "36.9965",
                    "36.99871",
                    "37.00203",
                    "37.0015",
                    "36.99726",
                    "36.99693",
                    "36.99687",
                    "36.99718",
                    "36.99778",
                    "36.99709",
                    "36.99464",
                    "36.9945",
                    "36.99455",
                    "36.99477",
                    "36.99536",
                    "36.99485",
                    "36.99495",
                    "36.99442",
                    "36.99514",
                    "36.99814",
                    "36.99806",
                    "36.99361",
                    "36.99156",
                    "36.99128"},
            {"-122.060872","-122.0578801632",
                    "-122.05866336822",
                    "-122.05431818962",
                    "-122.06105589866",
                    "-122.0555627346",
                    "-122.06595629453",
                    "-122.05152",
                    "-122.06047",
                    "-122.05685",
                    "-122.06452",
                    "-122.06485",
                    "-122.06545",
                    "-122.06182",
                    "-122.05407",
                    "-122.05354",
                    "-122.05344",
                    "-122.05304",
                    "-122.05488",
                    "-122.05482",
                    "-122.05488",
                    "-122.05441",
                    "-122.05464",
                    "-122.05392",
                    "-122.0549",
                    "-122.05377",
                    "-122.05396",
                    "-122.06033",
                    "-122.05986",
                    "-122.05296",
                    "-122.05489",
                    "-122.05503",
                    "-122.05518",
                    "-122.05447",
                    "-122.05488",
                    "-122.05441",
                    "-122.05484",
                    "-122.06341",
                    "-122.06048",
                    "-122.05251",
                    "-122.06594",
                    "-122.05775",
                    "-122.05463",
                    "-122.05475",
                    "-122.05459",
                    "-122.05982",
                    "-122.06234",
                    "-122.06294",
                    "-122.06577",
                    "-122.06649",
                    "-122.0661",
                    "-122.06549",
                    "-122.06543",
                    "-122.06695",
                    "-122.06643",
                    "-122.06685",
                    "-122.06622",
                    "-122.05466",
                    "-122.06486",
                    "-122.05115",
                    "-122.05577",
                    "-122.05944",
                    "-122.06171",
                    "-122.05303",
                    "-122.05325",
                    "-122.05371",
                    "-122.05088",
                    "-122.05226",
                    "-122.06051",
                    "-122.06105",
                    "-122.05938",
                    "-122.06277",
                    "-122.06279",
                    "-122.06278",
                    "-122.06238",
                    "-122.0635",
                    "-122.06577",
                    "-122.05408",
                    "-122.05484",
                    "-122.05420017242",
                    "-122.05352",
                    "-122.06172",
                    "-122.06535",
                    "-122.06605",
                    "-122.0658",
                    "-122.06583",
                    "-122.0653",
                    "-122.06162",
                    "-122.05802",
                    "-122.05868",
                    "-122.05201",
                    "-122.05236",
                    "-122.05165",
                    "-122.05146",
                    "-122.05067",
                    "-122.05078",
                    "-122.06193",
                    "-122.06175",
                    "-122.06227",
                    "-122.06263",
                    "-122.06196",
                    "-122.06212",
                    "-122.06217",
                    "-122.062",
                    "-122.06232",
                    "-122.06177",
                    "-122.06171",
                    "-122.05467",
                    "-122.06415",
                    "-122.06411"}};

    /**
     * Sets up the form and button listeners, checks Google Play Services, and starts location updates.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        setFinishOnTouchOutside(false); //don't close if touch outside dialog
        //populate spinner
        Spinner building_spinner = (Spinner) findViewById(R.id.location_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, buildings[0]); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        building_spinner.setAdapter(spinnerArrayAdapter);
        //button actions
        Button cancel_button = (Button) findViewById(R.id.cancel);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button submit_button = (Button) findViewById(R.id.submit);
        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check for internet connection
                EditText description_text = (EditText) findViewById(R.id.description);
                if (!isNetworkConnected()) {
                    description_text.setError("Internet connection required");
                  return;
                }
                //get dialog fields

                CheckBox current_location_box = (CheckBox) findViewById(R.id.currentlocationcheckbox);
                Spinner building_spinner = (Spinner) findViewById(R.id.location_spinner);
                EditText location_text = (EditText) findViewById(R.id.locationtextbox);
                CheckBox race_check = (CheckBox) findViewById(R.id.race_check);
                CheckBox culture_check = (CheckBox) findViewById(R.id.culture_check);
                CheckBox gender_check = (CheckBox) findViewById(R.id.gender_check);
                CheckBox sexual_orientation_check = (CheckBox) findViewById(R.id.sexual_orientation_check);
                CheckBox else_check = (CheckBox) findViewById(R.id.else_check);
                SeekBar bother_seek = (SeekBar) findViewById(R.id.bother_seek);

                String description = description_text.getText().toString();
                boolean currentlocation = current_location_box.isChecked();
                int building_position = building_spinner.getSelectedItemPosition();
                String lat, longi;
                if (currentlocation) {  //use device location
                    lat = String.valueOf(mCurrentLocation.getLatitude());
                    longi = String.valueOf(mCurrentLocation.getLongitude());
                } else {    //use building coordinates
                    lat = buildings[1][building_position];
                    longi = buildings[2][building_position];
                }
                String other_location = location_text.getText().toString();
                boolean race = race_check.isChecked();
                boolean culture = culture_check.isChecked();
                boolean gender = gender_check.isChecked();
                boolean sexual_orientation = sexual_orientation_check.isChecked();
                boolean else_checked = else_check.isChecked();
                int bother = bother_seek.getProgress();
                String androidId = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                SharedPreferences preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                String partID = preferenceSettings.getString("partID", "");

                //check for empty description
                description_text.setError(null);
                if (TextUtils.isEmpty(description)) {
                    description_text.setError(getString(R.string.error_field_required));
                    return;
                }

                //post report
                //Toast.makeText(getBaseContext(), "Submitting...", Toast.LENGTH_SHORT).show();
                String output = "description=" + Uri.encode(description) + "&locationLat=" + Uri.encode(lat) +
                        "&locationLong=" + Uri.encode(longi) + "&locationBuilding=" +
                        Uri.encode(building_spinner.getSelectedItem().toString()) + "&locationOther=" +
                        Uri.encode(other_location) + "&raceCheck=" + String.valueOf(race) +
                        "&cultureCheck=" + String.valueOf(culture) + "&genderCheck=" + String.valueOf(gender) +
                        "&sexCheck=" + String.valueOf(sexual_orientation) + "&otherCheck=" +
                        String.valueOf(else_checked) + "&bother=" + Uri.encode(String.valueOf(bother)) +
                        "&installationID=" + Uri.encode(Installation.id(getBaseContext())) + "&androidID=" + Uri.encode(androidId) +
                        "&partID=" + Uri.encode(partID);
                new postReport().execute(output);
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

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
    }
    /**
     * Tries to submit the report to the reports table. The php file echoes the result.
     */
    private class postReport extends AsyncTask<String, Void, String> {
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
            Toast.makeText(getBaseContext(), ""+ result.trim(), Toast.LENGTH_SHORT).show();
        }
    }

    /**set up location client*/
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        }
        else {
            Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show();
            return false;
        }
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
        Toast.makeText(this, "Unable to connect", Toast.LENGTH_SHORT).show();
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
