package edu.ucsc.psyc_files.microreport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Registration screen asking for email and name, assigns participant ID, sends those along with
 * Installation ID and Device ID to the user file on the server, then sets sharedPreferences with
 * participant ID and email address. Is displayed as first screen as long as the installation is
 * not registered.
 * Based on Login Activity template.
 */
public class RegisterActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mFnameView;
    private TextView mResult;
    private View mProgressView;
    private View mLoginFormView;
    private Button mRegisterButton;
    private Button mFinishButton;
    private Button mNewButton;
    private SharedPreferences preferenceSettings;
    private SharedPreferences.Editor preferenceEditor;

    /**
     * Sets up the layout and listeners. If there is no internet connection, a message is displayed.
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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
            populateAutoComplete();

            mFnameView = (EditText) findViewById(R.id.fname);
            mFnameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
     * Uses device contacts to AutoComplete email field.
     */
    private void populateAutoComplete() {
        if (VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            getLoaderManager().initLoader(0, null, this);
        } else if (VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new SetupEmailAutoCompleteTask().execute(null, null);
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
        mFnameView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String fname = mFnameView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check if the user entered an ID
        if (TextUtils.isEmpty(fname)) {
            mFnameView.setError(getString(R.string.error_field_required));
            focusView = mFnameView;
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
            mAuthTask = new UserLoginTask(email, fname, Installation.id(this), androidId, false);
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

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
    }

    /**
     * Use an AsyncTask to fetch the user's email addresses on a background thread, and update
     * the email text field with results on the main UI thread.
     */
    class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<String>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getContentResolver();
            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    null, null, null);
            while (emailCur.moveToNext()) {
                String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract
                        .CommonDataKinds.Email.DATA));
                emailAddressCollection.add(email);
            }
            emailCur.close();

            return emailAddressCollection;
        }

        @Override
        protected void onPostExecute(List<String> emailAddressCollection) {
            addEmailsToAutoComplete(emailAddressCollection);
        }
    }

    /**
     * Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
     * @param emailAddressCollection
     */
    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(RegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
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
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mFname;
        private final String mInstallid;
        private final String mAndroidid;
        private String result;
        private boolean mNewDevice;

        UserLoginTask(String email, String fname, String installid, String androidid, boolean newDevice) {
            mEmail = email;
            mFname = fname;
            mInstallid = installid;
            mAndroidid = androidid;
            mNewDevice = newDevice;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            //send email, installation number, and Android ID to file
            //server generates participant ID and sends back
            //change so that returns response from server "Registration Complete" etc.
            //save participant ID and email to shared preferences

            try {
                URL url = new URL("http://ec2-52-26-239-139.us-west-2.compute.amazonaws.com/register.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                //String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                //con.setRequestProperty("Authorization", basicAuth);

                //attempt to register user
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("email=" + Uri.encode(mEmail) + "&firstName=" + Uri.encode(mFname) + "&installationID=" + Uri.encode(mInstallid) + "&deviceID=" + Uri.encode(mAndroidid)+ "&newDevice=" + mNewDevice);
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
                    result = "Unable to open connection to URL";
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

                if (result.contains("Registration Successful")) {
                    //hide register button (and new button if applicable) and show finish button
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setText("Finish");
                    mFinishButton.setOnClickListener(finishClickHandler);
                    mNewButton = (Button) findViewById(R.id.new_device_button);
                    mNewButton.setVisibility(View.GONE);
                    //save participantID and email in sharedpreferences and ACRA
                    int i = result.lastIndexOf("ID: ");
                    String partID = result.substring(i+4, result.length());
                    preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                    preferenceEditor = preferenceSettings.edit();
                    preferenceEditor.putBoolean("registered", true);
                    preferenceEditor.putString("partID", partID);
                    preferenceEditor.putString("emailAddress", mEmail);
                    preferenceEditor.apply();
                    ACRA.getErrorReporter().putCustomData("partID", partID);
                    ACRA.getErrorReporter().putCustomData("installationID", Installation.id(getBaseContext()));

                } else if (result.contains("This device is already registered")) {
                    //set shared preferences and show finish button
                    preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                    preferenceEditor = preferenceSettings.edit();
                    preferenceEditor.putBoolean("registered", true);
                    preferenceEditor.apply();
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setOnClickListener(finishClickHandler);
                    mNewButton = (Button) findViewById(R.id.new_device_button);
                    mNewButton.setVisibility(View.GONE);

                } else if (result.contains("That email address is already registered")) {
                    //show button to register new device and finish button, hide register button
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mNewButton = (Button) findViewById(R.id.new_device_button);
                    mNewButton.setVisibility(View.VISIBLE);
                    mNewButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //runs script again with "new device flag"
                            registerNewDevice(mEmail);
                        }
                    });
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setText("Cancel");
                    mFinishButton.setOnClickListener(finishClickHandler);


                } else if (result.contains("Unable to Complete Registration")) {

                }

            } else {
                mFnameView.setError("There was an error");
                mFnameView.requestFocus();
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
        return (cm.getActiveNetworkInfo() != null);
    }

    /**
     * When email address is already registered, runs the AsyncTask again with the email address
     * provided and the newDevice flag set to true;
     * @param email
     */
    private void registerNewDevice(String email) {
        showProgress(true);
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        mAuthTask = new UserLoginTask(email, null, Installation.id(this), androidId, true);
        mAuthTask.execute((Void) null);
    }

    /**Checks if the device is registered. */
    private boolean Registered(){
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        return preferenceSettings.getBoolean("registered", false);
    }

}



