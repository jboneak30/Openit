package com.mclss.openit.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewAnimator;

import com.mclss.openit.R;
import com.mclss.openit.logger.Log;
import com.mclss.openit.logger.LogFragment;
import com.mclss.openit.logger.LogWrapper;
import com.mclss.openit.logger.MessageOnlyLogFilter;

import java.io.File;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private boolean mLogShown;

    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Author: Alfred Meng\nMailto: mclssf@gmail.com", Snackbar.LENGTH_LONG)
                        .setAction("DONATE", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Uri uri= Uri.parse("http://www.mclss.com");
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        }).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.log_wrapper) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.log_wrapper);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Create a chain of targets that will receive log data */
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");

        File[] files = new File("/etc").listFiles();
        for (File file : files) {
            if(file.getName().length() >= 0){
                Log.i(TAG, file.getPath());
            }
        }
    }
}
