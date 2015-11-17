package com.mclss.openit.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ViewAnimator;

import com.mclss.openit.R;
import com.mclss.openit.logger.Log;
import com.mclss.openit.logger.LogFragment;
import com.mclss.openit.logger.LogWrapper;
import com.mclss.openit.logger.MessageOnlyLogFilter;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private boolean mLogShown;
    private ListView mInfoListView;
    private ArrayAdapter<String> mAddressName;
    private List<String> mAddr;
    private List<String> mNfcId;

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
                                Uri uri = Uri.parse("http://www.mclss.com");
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        }).show();
            }
        });

        FloatingActionButton add = (FloatingActionButton) findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        if (!initializeData()) {
            Snackbar.make(mInfoListView, "并没有找到存储的nfc卡片信息。", Snackbar.LENGTH_LONG).show();
        }
        initializeView();
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
                    output.setDisplayedChild(2);
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

//        File[] files = new File("/etc").listFiles();
//        for (File file : files) {
//            if(file.getName().length() >= 0){
//                Log.i(TAG, file.getPath());
//            }
//        }

        Log.i(TAG, "Adapter count=" + mInfoListView.getCount());
    }

    private boolean initializeData() {

        initializePreference();

        mAddressName = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mAddr);

        return true;
    }

    private void initializePreference() {
        SharedPreferences sharedPreferences = getSharedPreferences("token",
                Activity.MODE_PRIVATE);

        String strIndex = "Home,Office,Partner,Sex Partner";
//        String strIndex = "";
        String[] strAddr = strIndex.split(",");
        mAddr = Arrays.asList(strAddr);

        String strNfcIndex = "01B9551B,01B9551B,01B9551B,01B9551B";
        String[] strNfcId = strNfcIndex.split(",");
        mNfcId = Arrays.asList(strNfcId);

    }

    private void initializeView() {
        mInfoListView = (ListView)findViewById(R.id.info_listView);
        mInfoListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mInfoListView.setAdapter(mAddressName);

        mInfoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "Click:pos=" + position + ",id=" + id);
                String tmpAddr = "确认切换到" + mAddr.get(Long.valueOf(id).intValue()) + "吗？";
                AlertDialog.Builder applyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                applyDialogBuilder.setMessage(tmpAddr);
                applyDialogBuilder.setTitle("切换NFC卡片");
                applyDialogBuilder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                applyDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                applyDialogBuilder.create().show();
            }
        });
    }
}
