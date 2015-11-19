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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private final String NFC_NXP_FILENAME = "/etc/libnfc-nxp.conf";
    private final String NFC_BRM_FILENAME = "/etc/libnfc-nxp.conf";
    private final String NFC_CACHE_FILENAME = "/data/data/com.mclss.openit/cache/openitcachefile.conf";

    private boolean mLogShown;
    private ListView mInfoListView;
    private ArrayAdapter<String> mAddressName;
    private List<String> mAddr;
    private List<String> mNfcId;
    private ArrayList<String> mStrArrayFile;
    private int mKeyStrIndex;
    private String mStrMountInfo;

    @Override
    protected void onStart() {
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

        if (!getRootAuth()) {
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "您的手机并没有获得ROOT权限。\n请找到相关人士帮助您ROOT手机。", Snackbar.LENGTH_LONG).show();
            }
        }

        if (!initializeData()) {
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "并没有找到存储的nfc卡片信息。", Snackbar.LENGTH_LONG).show();
            }
        }

        if (!initializeMountInfo()) {
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "并没有找到存储的nfc卡片信息。", Snackbar.LENGTH_LONG).show();
            }
        }

        if (!initializeFile()) {
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "并不能修改NFC卡片信息。", Snackbar.LENGTH_LONG).show();
            }
        }

        initializeView();

    }

    private boolean initializeMountInfo() {
        String tmpStr = "";

        try {
            tmpStr = execCommand("mount");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] tmpStrArray = tmpStr.split("\n");
        for (String str:tmpStrArray
                ) {
            if (str.contains("/system")) {
                mStrMountInfo = str.split(",")[0];
                return true;
            }
        }

        mStrMountInfo = "";
        return false;
    }

    private boolean initializeFile() {
        try {
            // read file content from file
            FileReader reader = new FileReader(NFC_NXP_FILENAME);
            BufferedReader br = new BufferedReader(reader);
            String tmpStr;
            int i = 0;
            while((tmpStr = br.readLine()) != null) {
                mStrArrayFile.add(tmpStr);
                if (!tmpStr.isEmpty() && tmpStr.toCharArray()[0] != '#' && tmpStr.split("=")[0].compareTo("NXP_CORE_CONF") == 0) {
                    mKeyStrIndex = i + 6;
                }
                i++;
            }

            reader.close();
            br.close();
        }

        catch (IOException e){
            e.printStackTrace();
        }

        return true;
    }

    public synchronized boolean getRootAuth()
    {
        Process process = null;
        DataOutputStream os = null;
        try
        {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0)
            {
                return true;
            } else
            {
                return false;
            }
        } catch (Exception e)
        {
            Log.d("*** DEBUG ***", "Unexpected error - Here is what I know: "
                    + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
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
        Log.i(TAG, "System mount info=" + mStrMountInfo);
    }

    private boolean initializeData() {
        initializePreference();
        mStrArrayFile = new ArrayList<String>();
        mAddressName = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mAddr);

        return false;
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
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                Log.i(TAG, "Click:pos=" + position + ",id=" + id);
                String tmpAddr = "确认切换到" + mAddr.get(Long.valueOf(id).intValue()) + "吗？";
                AlertDialog.Builder applyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                applyDialogBuilder.setMessage(tmpAddr);
                applyDialogBuilder.setTitle("切换NFC卡片");
                applyDialogBuilder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        applyChangeForNfcId(id);
                        dialog.dismiss();
                    }
                });
                applyDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                applyDialogBuilder.setCancelable(false);
                applyDialogBuilder.create().show();
            }
        });
    }

    private void applyChangeForNfcId(long id) {
//        for (String tmpStr:mStrArrayFile) {
//            Log.i(TAG, tmpStr);
//        }
        int uid = Long.valueOf(id).intValue();
        String strUID = mNfcId.get(uid);
        String tmpStr = mStrArrayFile.get(mKeyStrIndex);
        String[] tmpStrSplit = tmpStr.split(",");
        String tmpNewStr = tmpStrSplit[0] + "," + tmpStrSplit[1] + ", "
                + strUID.substring(0, 2) + ", "
                + strUID.substring(2, 4) + ", "
                + strUID.substring(4, 6) + ", "
                + strUID.substring(6, 8);

        mStrArrayFile.set(mKeyStrIndex, tmpNewStr);

        applyFileOfNfc();

//        Log.i(TAG, mStrArrayFile.get(mKeyStrIndex));
    }

    private void applyFileOfNfc() {
        try {
            FileWriter fw = new FileWriter(NFC_CACHE_FILENAME);
            BufferedWriter writer = new BufferedWriter(fw);

            for (String aMStrArrayFile : mStrArrayFile) {
                writer.write(aMStrArrayFile);
                writer.newLine();//换行
            }

            writer.flush();

            fw.close();
            writer.close();
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
        } finally {

        }

        testCmd();
    }

    private void testCmd() {
        Process suProcess;
        DataOutputStream os;
        String result = "";

        try{
            suProcess = Runtime.getRuntime().exec("su");
            os= new DataOutputStream(suProcess.getOutputStream());
            InputStream in = suProcess.getInputStream();

            os.writeBytes("mount -o rw,remount -t ext4 /dev/block/platform/msm_sdcc.1/by-name/system /system\n");
            os.flush();

            os.writeBytes("rm /system/etc/libnfc-nxp.conf.bak\n");
            os.flush();

            os.writeBytes("cp -rf /system/etc/libnfc-nxp.conf /system/etc/libnfc-nxp.conf.bak\n");
            os.flush();

            os.writeBytes("cp -rf /data/data/com.mclss.openit/cache/openitcachefile.conf /system/etc/libnfc-nxp.conf\n");
            os.flush();

            os.writeBytes("mount -o ro,remount -t ext4 /dev/block/platform/msm_sdcc.1/by-name/system /system\n");
            os.flush();

            os.writeBytes("exit\n");
            os.flush();

            try {
                suProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String execCommand(String command) throws IOException {
        Process process = new ProcessBuilder()
                .command(command)
                .redirectErrorStream(true)
                .start();
        try {
            InputStream in = process.getInputStream();
            DataOutputStream out = new DataOutputStream(process.getOutputStream());

            return readFully(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public String execCommand(String command, String param) throws IOException {
        Process process = new ProcessBuilder()
                .command(command, param)
                .redirectErrorStream(true)
                .start();
        try {
            InputStream in = process.getInputStream();
            DataOutputStream out = new DataOutputStream(process.getOutputStream());

            return readFully(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }
}
