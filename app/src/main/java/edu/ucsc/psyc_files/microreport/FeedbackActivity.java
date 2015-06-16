package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.acra.ReportField;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * Feedback form that posts to AFS space
 */
public class FeedbackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    public void submitFeedback (View view) {
        if (isNetworkConnected()) { //only do if network is connected
        //go back to main view
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //open connection and post feedback
        EditText text = (EditText) findViewById(R.id.feedbacktext);
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        String feedback = "\nTimestamp:"+ Calendar.getInstance().getTime()+"\tInstallationID:"+Installation.id(this)+"\tDeviceID:"+androidId+"\tComments:"+text.getText().toString();
        new postFeedback().execute(feedback);
        } else Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
    }

    private class postFeedback extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String urlstring = "http://people.ucsc.edu/~cmbyrd/microreport/postfeedback.php?output=";
                URL url = new URL(urlstring + Uri.encode(params[0]));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                con.setRequestProperty("Authorization", basicAuth);
                con.connect();
                String result = con.getResponseMessage();
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

    private void toastResult(String result){
        Toast.makeText(this, "Submitting feedback: "+result, Toast.LENGTH_SHORT).show();
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
            case R.id.action_report:
                intent = new Intent(this, ReportActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_gethelp:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_webpage:
                Uri webpage = Uri.parse("http://people.ucsc.edu/~cmbyrd/microaggressionstudy.html");
                intent = new Intent(Intent.ACTION_VIEW, webpage);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
