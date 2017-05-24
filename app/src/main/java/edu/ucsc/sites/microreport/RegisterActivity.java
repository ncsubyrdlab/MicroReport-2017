package edu.ucsc.sites.microreport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Registration screen asking for email and name, assigns participant ID, sends those along with
 * Installation ID and Device ID to the user file on the server, then sets sharedPreferences with
 * participant ID and email address. Is displayed as first screen as long as the installation is
 * not registered.
 * Based on Login Activity template.
 * v3: just asks for access code and confirms with users DB, removes new device flag, backend checks that install id is new, sends
 * coordinates of home zup
 */
public class RegisterActivity extends Activity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private GeoCodeTask mAuthTask2 = null; //no longer used, zip lat and long are looked up in DB

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mAccessCodeView;
    private TextView mResult;
    private View mProgressView;
    private View mLoginFormView;
    private Button mRegisterButton;
    private Button mFinishButton;
    private SharedPreferences preferenceSettings;
    private SharedPreferences.Editor preferenceEditor;

    /**
     * Sets up the layout and listeners. If there is no internet connection, a message is displayed.
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setHomeButtonEnabled(false);

        //don't allow orientation changes from portrait
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        if (Registered()) {
            mResult = (TextView) findViewById(R.id.result);
            mResult.setText("\nDevice has already been registered\n");
            mResult.setContentDescription("Device has already been registered");
        }

        //check internet connection
        if (!isNetworkConnected()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            //todo: do something here, the user will not be able to register or leave this page
            mResult = (TextView) findViewById(R.id.result);
            mResult.setText("You need an internet connection to continue. Connect to a network and then click Reload.");
            mResult.setContentDescription("You need an internet connection to continue. Connect to a network and then click Reload.");
            mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
            mRegisterButton.setVisibility(View.GONE);
            mFinishButton = (Button) findViewById(R.id.finish_button);
            mFinishButton.setVisibility(View.VISIBLE);
            mFinishButton.setText("Reload");
            mFinishButton.setOnClickListener(finishClickHandler);
        } else {

            // Set up the login form.
            mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

            mAccessCodeView = (EditText) findViewById(R.id.access_code);
            mAccessCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.id.login || id == EditorInfo.IME_NULL) {
                        attemptLogin();
                        return true;
                    }
                    return false;
                }
            });

            Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });

            mLoginFormView = findViewById(R.id.login_form);
            mProgressView = findViewById(R.id.login_progress);
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mAccessCodeView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String access_code = mAccessCodeView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check if the user entered an ID
        if (TextUtils.isEmpty(access_code)) {
            mAccessCodeView.setError(getString(R.string.error_field_required));
            focusView = mAccessCodeView;
            cancel = true;
        }

        // Check for a valid email address (contains a @).
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user registration attempt.
            showProgress(true);
            String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            mAuthTask = new UserLoginTask(email, access_code, Installation.id(this));
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * AsyncTask to connect to server and attempt registration. The php file checks if the email
     * address and installation ID are unique. If the installation ID is registered, the user can
     * click Finish and move on (this could happen if SharedPreferences as cleared but the app was
     * not uninstalled). If the email address is registered, this probably means the user has re-installed
     * the app or is using a different device. The user is invited to Register New Device (but this
     * is really just registering a new installation). The php script is run again with the newDevice
     * flag to "true".
     * <p>If the email address and installation ID are unique, a new registration is created and
     * "Registration Successful" is echoed. Otherwise, errors are displayed. </p>
     * <p>In successful registration cases, the participant ID number is echoed and saved into
     * SharedPreferences.</p>
     *
     * Android ID is collected but not checked
     * because some models do not have a unique Android ID.
     *
     * v3: confirms the access code is in the DB and matches the email address and is confirmed
     * stores the home zipcode and ID number in sharedPreferences
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mAccessCode;
        private final String mInstallid;
        private String result;
        private boolean mNewDevice;

        //v2
        UserLoginTask(String email, String access_code, String installid, boolean newDevice) {
            mEmail = email;
            mAccessCode = access_code;
            mInstallid = installid;
            mNewDevice = newDevice;
        }

        //v3
        UserLoginTask(String email, String access_code, String installid) {
            mEmail = email;
            mAccessCode = access_code;
            mInstallid = installid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            //send email, installation number, and Android ID to file
            //server generates participant ID and sends back
            //change so that returns response from server "Registration Complete" etc.
            //save participant ID and home zipcode to shared preferences

            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/v2/register_device.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                //String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                //con.setRequestProperty("Authorization", basicAuth);

                //get device info
                String OS = "Android OS: "+System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
                String API = "//API: "+android.os.Build.VERSION.SDK_INT;
                String deviceType = "//Device type: "+android.os.Build.DEVICE;
                String model = "//Model: "+android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";

                //attempt to register user
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("email=" + Uri.encode(mEmail) + "&ID=" + Uri.encode(mAccessCode) + "&installationID=" + Uri.encode(mInstallid)
                +"&deviceInfo="+Uri.encode(OS+API+deviceType+model));
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

                    con.disconnect();
                    return true;
                } else {
                    con.disconnect();
                    result = "Unable to connect to the internet";
                    return false;
                }

            } catch (Exception ex) {
                result = "Error: " + ex;
                return true;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);


            mResult = (TextView) findViewById(R.id.result);
            mResult.setText(result);
            mResult.setContentDescription(result);
            if (success) {

                //todo: make keyboard go away after submitting
                //check result
                if (result.contains("SUCCESS")) {
                    mResult.setText("Your device has been registered.");
                    mResult.setContentDescription("Your device has been registered.");

                    //hide register button and show finish button
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setText("Finish");
                    mFinishButton.setOnClickListener(finishClickHandler);

                    //save participantID and home zipcode  in sharedpreferences and ACRA
                    preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                    preferenceEditor = preferenceSettings.edit();
                    preferenceEditor.putBoolean("registered", true);
                    int i = result.lastIndexOf("ACCESSCODE=");
                    String partID = result.substring(i+11, i+19);
                    preferenceEditor.putString("partID", partID);
                    int j = result.lastIndexOf("ZIP=");
                    String homeZIP = result.substring(j+4, result.length()); //latitude, longitude
                    preferenceEditor.putString("homeZIP", homeZIP);
                    preferenceEditor.apply();
                    ACRA.getErrorReporter().putCustomData("partID", partID);
                    ACRA.getErrorReporter().putCustomData("installationID", Installation.id(getBaseContext()));


                } else if (result.contains("ERROR1")) {
                    //ID is not in db
                } else if (result.contains("ERROR2")) {
                    //emails do not match
                } else if (result.contains("ERROR3")) {
                    //need to verify email
                } else if (result.contains("ERROR4")) {
                    //device has already been registered somehow

                    mResult.setText("Your device has already been registered. Click Finish to continue.");
                    mResult.setContentDescription("Your device has already been registered. Click Finish to continue.");

                    //hide register button and show finish button
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setText("Finish");
                    mFinishButton.setOnClickListener(finishClickHandler);


                    //save ID and ZIP
                    preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                    preferenceEditor = preferenceSettings.edit();
                    preferenceEditor.putBoolean("registered", true);
                    int i = result.lastIndexOf("ACCESSCODE=");
                    String partID = result.substring(i+11, i+19);
                    preferenceEditor.putString("partID", partID);
                    int j = result.lastIndexOf("ZIP=");
                    String homeZIP = result.substring(j+4, result.length()); //latitude, longitude
                    preferenceEditor.putString("homeZIP", homeZIP);
                    preferenceEditor.apply();
                    ACRA.getErrorReporter().putCustomData("partID", partID);
                    ACRA.getErrorReporter().putCustomData("installationID", Installation.id(getBaseContext()));

                } else if (result.contains("ERROR5")) {
                    //device not added to db
                }else if (result.contains("ERROR6")) {
                    //device associated with another ID
                }else if (result.contains("ERROR7")) {
                    //user is banned
                }

            } else {
                mAccessCodeView.setError("There was an error. Please try again.");
                mAccessCodeView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }


    }



    /**
     * Click listener for the Finish button (displays Finish or Cancel depending on circumstances).
     */
    private OnClickListener finishClickHandler = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent;
            intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    };

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return (isConnected);
    }


    /**Checks if the device is registered. */
    private boolean Registered(){
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        return preferenceSettings.getBoolean("registered", false);
    }


    //not used anymore
    public class GeoCodeTask extends AsyncTask<Void, Void, Boolean> {

        private final String mHomeZIP;
        private String result2;

        GeoCodeTask(String homeZIP) {
            mHomeZIP = homeZIP;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            //use Goolgle Maps geocode service to get latlng

            try {
                URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?address="+mHomeZIP);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setChunkedStreamingMode(0);

                if (con.getResponseCode() == 200) {

                    BufferedReader reader = null;
                    StringBuilder stringBuilder;
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append("\n"+line);
                    }
                    String in = stringBuilder.toString();
                    //https://www.tutorialspoint.com/android/android_json_parser.htm
                    JSONObject reader1 = new JSONObject(in);
                    JSONObject location = reader1.getJSONObject("location");
                    String lat = location.getString("lat");
                    String lng = location.getString("long");

                    result2 = lat+","+lng;

                    con.disconnect();
                    return true;
                } else {
                    con.disconnect();
                    return false;
                }

            } catch (Exception ex) {
                result2 = "37.0105307,-122.1178261"; //santa cruz is default
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask2 = null;

           if (success) {
               preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
               preferenceEditor = preferenceSettings.edit();
               preferenceEditor.putString("homeZIPlatlng", result2);
               preferenceEditor.apply();
           }
        }

        @Override
        protected void onCancelled() {
            mAuthTask2 = null;

        }


    }



}



