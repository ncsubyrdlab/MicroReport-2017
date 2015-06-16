package edu.ucsc.psyc_files.microreport;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**Shows campus resources and a short FAQ on the app*/
public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getActionBar().setDisplayHomeAsUpEnabled(true);

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
            case R.id.action_feedback:
                intent = new Intent(this, FeedbackActivity.class);
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
