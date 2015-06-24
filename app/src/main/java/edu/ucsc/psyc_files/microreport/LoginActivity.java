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
import android.os.Build.VERSION;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import 	javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;


/**
 * Registration screen asking for email and name, assigns participant ID, sends those along with
 * Installation ID and Device ID to user file; sets sharedPreferences
 * Is displayed upon first install and as first screen as long as not registered, no menu option to go to
 * Based on Login Activity template
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

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


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

        // Check for a valid email address.
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
        //TODO: Replace this with your own logic
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
        int IS_PRIMARY = 1;
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

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    /**
     * Represents an asynchronous registration task
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
                URL url = new URL("http://people.ucsc.edu/~cmbyrd/microreport/register.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                con.setRequestProperty("Authorization", basicAuth);

                //attempt to register user
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                out.write("email=" + Uri.encode(mEmail) + "&fname=" + Uri.encode(mFname) + "&installID=" + Uri.encode(mInstallid) + "&deviceID=" + Uri.encode(mAndroidid)+ "&newDevice=" + mNewDevice);
                out.close();

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = null;
                    StringBuilder stringBuilder;
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line + "\n");
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
                result = "Exception: " + ex;
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

                if (result.contains("Registration Complete")) {
                    //hide register button (and new button if applicable) and show finish button
                    mRegisterButton = (Button) findViewById(R.id.email_sign_in_button);
                    mRegisterButton.setVisibility(View.GONE);
                    mFinishButton = (Button) findViewById(R.id.finish_button);
                    mFinishButton.setVisibility(View.VISIBLE);
                    mFinishButton.setText("Finish");
                    mFinishButton.setOnClickListener(finishClickHandler);
                    mNewButton = (Button) findViewById(R.id.new_device_button);
                    mNewButton.setVisibility(View.GONE);
                    //save participantID and email in sharedpreferences
                    String partID = result.substring((result.length() - 7), result.length()-1);
                    //mFinishButton.setText(partID);
                    preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
                    preferenceEditor = preferenceSettings.edit();
                    preferenceEditor.putBoolean("registered", true);
                    preferenceEditor.putString("partID", partID);
                    preferenceEditor.putString("emailAddress", mEmail);
                    preferenceEditor.apply();

                } else if (result.contains("The device is already registered")) {
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

                } else if (result.contains("The email address is already registered")) {
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

    private void registerNewDevice(String email) {
        // Show a progress spinner, and kick off a background task to
        // perform the user registration attempt.
        showProgress(true);
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        mAuthTask = new UserLoginTask(email, null, Installation.id(this), androidId, true);
        mAuthTask.execute((Void) null);
    }

    private boolean Registered(){
        SharedPreferences preferenceSettings;
        preferenceSettings = getSharedPreferences("microreport_settings", MODE_PRIVATE);
        return preferenceSettings.getBoolean("registered", false);
    }

}



