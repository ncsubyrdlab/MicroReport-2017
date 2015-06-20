package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Shows a draft of the report; user can submit the report or go back. This activity has no menu options.
 */
public class ConfirmReport extends Activity {

    private String output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_report);

        // Get the report from the intent
        Intent intent = getIntent();
        String reportdraft = intent.getStringExtra(ReportActivity.EXTRA_DRAFT);
        output = intent.getStringExtra(ReportActivity.EXTRA_DATA);

        //set the text to the draft report
        TextView textView = (TextView)findViewById(R.id.reportdraft);
        textView.setText(reportdraft);

    }

    /**
     * Tries to submit the report via AsyncTask if the network is connected. Then deletes the cache so a new
     * report file is downloaded when it goes back to the map
     * @param view
     */
    public void okReportSubmit(View view) {
        if (isNetworkConnected()) {
        new postReport().execute();
        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files)
                file.delete();
        }
        //create an empty file so no file not found error
        new File(getCacheDir(), "reports.xml");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        } else Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    /**
     * Tries to submit the report to the report file. Toast displays "OK" or an error.
     */
    private class postReport extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String result;
            try {
                //todo: change website
                URL url = new URL("http://people.ucsc.edu/~cmbyrd/testdb/postreport.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                String basicAuth = "Basic " + new String(Base64.encode("MRapp:sj8719i".getBytes(), Base64.DEFAULT));
                con.setRequestProperty("Authorization", basicAuth);
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                //output fields created and URI encoded in ReportActivity
                out.write(output);
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

    private void toastResult(String result){
        Toast.makeText(this, ""+ result, Toast.LENGTH_SHORT).show();
    }

    public void goBack(View view) {
                finish();
    }

}
