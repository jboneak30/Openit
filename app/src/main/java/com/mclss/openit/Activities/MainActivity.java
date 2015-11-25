package com.mclss.openit.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.mclss.openit.R;
import com.mclss.openit.controls.McSweetAlertDialog;
import com.mclss.openit.logger.Log;
import com.mclss.openit.logger.LogFragment;
import com.mclss.openit.logger.LogWrapper;
import com.mclss.openit.logger.MessageOnlyLogFilter;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.orhanobut.dialogplus.ViewHolder;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private final String NFC_NXP_FILENAME = "/etc/libnfc-nxp.conf";
    private final String NFC_BRM_FILENAME = "/etc/libnfc-nxp.conf";
    private final String NFC_CACHE_FILENAME = "/data/data/com.mclss.openit/cache/openitcachefile.conf";

    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    private boolean mLogShown;
    private ArrayAdapter<String> mAddressName;
    private ArrayList<String> mAddr;
    private ArrayList<String> mNfcId;
    private ArrayList<String> mStrArrayFile;
    private int mKeyStrIndex;
    private String mStrMountInfo;
    private int iTick = -1;
    private McSweetAlertDialog mMcSweetAlertDialog;
    private int mMenuId;

    private Handler mNfcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == McSweetAlertDialog.MSG_NFC_GETUID) {
                Bundle bundle = msg.getData();
                final String strNfcUid = bundle.getString("NfcUid");
                Log.i(TAG, strNfcUid);
                if (mMcSweetAlertDialog != null) {
                    NfcAdapter nfc = NfcAdapter.getDefaultAdapter(MainActivity.this);
                    if (nfc != null) {
                        nfc.disableReaderMode(MainActivity.this);
                    }
                    mMcSweetAlertDialog.dismiss();
                    mMcSweetAlertDialog = null;

                    //save to list
                    final DialogPlus dialog = DialogPlus.newDialog(MainActivity.this)
                            .setContentHolder(new ViewHolder(R.layout.add_dialog))
                            .setExpanded(true)
                            .setGravity(Gravity.CENTER)
                            .setCancelable(false)
                            .setContentWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
                            .setExpanded(true)  // This will enable the expand feature, (similar to android L share dialog)
                            .create();
                    dialog.show();
                    Button btnCancel = (Button) findViewById(R.id.cancel_button);
                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });
                    Button btnSave = (Button) findViewById(R.id.save_button);
                    final EditText editText = (EditText) findViewById(R.id.apply_edit);
                    btnSave.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //
                            if (editText.getText().toString().length() == 0) {
                                Snackbar.make(view, "并没有输入什么文字。\n真的没有输入什么文字。", Snackbar.LENGTH_SHORT)
                                        .show();
                            } else {
                                String strTmp = editText.getText().toString();
                                saveAddress(strTmp, strNfcUid);
                                dialog.dismiss();
                            }
                        }
                    });
                    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                    actionId == EditorInfo.IME_ACTION_DONE ||
                                    event.getAction() == KeyEvent.ACTION_DOWN &&
                                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
//                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                editText.clearFocus();
                                return true;
                            }
                            return false;
                        }
                    });
                }
            }
        }
    };

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
                //waiting for nfc card intent
                final McSweetAlertDialog pDialog = new McSweetAlertDialog(MainActivity.this, McSweetAlertDialog.PROGRESS_TYPE, mNfcHandler);
                pDialog.setTitleText("请扫描需要记录的卡片...");
                pDialog.setCancelable(false);
                Log.i(TAG, "Enabling reader mode");
                NfcAdapter nfc = NfcAdapter.getDefaultAdapter(MainActivity.this);
                if (nfc != null) {
                    nfc.enableReaderMode(MainActivity.this, pDialog, READER_FLAGS, null);
                }
                mMcSweetAlertDialog = pDialog;
                pDialog.show();

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
                Snackbar.make(getCurrentFocus(), "并没有找到文件系统挂载信息。", Snackbar.LENGTH_LONG).show();
            }
        }

        if (!initializeFile()) {
            if (getCurrentFocus() != null) {
                Snackbar.make(getCurrentFocus(), "并不能修改NFC卡片信息。", Snackbar.LENGTH_LONG).show();
            }
        }

        initializeView();

    }

    private void saveAddress(String strTmp, String strNfcUid) {
        Log.i(TAG, "Save: index=" + strTmp + " UID=" + strNfcUid);
        SharedPreferences sharedPreferences = getSharedPreferences("token",
                MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(strTmp, strNfcUid);
        editor.putString("index", addAddressIndex(strTmp));
        editor.apply();
        //
        mNfcId.add(strNfcUid);
        //
        mAddressName.add(strTmp);
        //
    }

    private String addAddressIndex(String strAddr) {
        String strIndex = "";
        int sizeofmAddr = mAddr.size();
        if (sizeofmAddr > 0) {
            strIndex += mAddr.get(0);
            for (int i = 1; i < sizeofmAddr; i++) {
                strIndex += "," + mAddr.get(i);
            }
            strIndex += "," + strAddr;
        }
        else {
            strIndex += strAddr;
            ViewAnimator output = (ViewAnimator) findViewById(R.id.log_wrapper);
            output.setDisplayedChild(1);
        }

        Log.i(TAG, "Refresh index: " + strIndex);

        return strIndex;
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
                android.util.Log.i(TAG, "mountinfo: " + mStrMountInfo);
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
                    mMenuId = output.getDisplayedChild();
                    output.setDisplayedChild(2);
                } else {
                    output.setDisplayedChild(mMenuId);
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

//        Log.i(TAG, "Adapter count=" + mInfoListView.getCount());
//        Log.i(TAG, "System mount info=" + mStrMountInfo);
    }

    private boolean initializeData() {
        mAddr = new ArrayList<String>();
        mNfcId = new ArrayList<String>();
        initializePreference();
        mStrArrayFile = new ArrayList<String>();
        mAddressName = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mAddr);

        return false;
    }

    private void initializePreference() {
        ViewAnimator output = (ViewAnimator) findViewById(R.id.log_wrapper);
        SharedPreferences sharedPreferences = getSharedPreferences("token",
                MODE_PRIVATE);

        String strIndex = sharedPreferences.getString("index", "");
//        String strIndex = "Home,office,partner,sexmate";
        android.util.Log.i(TAG, strIndex);
        if (!Objects.equals("", strIndex)) {
            String[] strAddr = strIndex.split(",");
            Collections.addAll(mAddr, strAddr);
            output.setDisplayedChild(1);
        } else {
            output.setDisplayedChild(0);
        }

        for (String strNfcId :
                mAddr) {
            mNfcId.add(sharedPreferences.getString(strNfcId, ""));
        }
//        String strNfcIndex = "01B9551B,01B9551B,01B9551B,01B9551B";
//        String[] strNfcId = strNfcIndex.split(",");
//        Collections.addAll(mNfcId, strNfcId);

    }

    private void initializeView() {
        ListView mInfoListView = (ListView) findViewById(R.id.info_listView);
        mInfoListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mInfoListView.setAdapter(mAddressName);

        mInfoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                Log.i(TAG, "Click:pos=" + position + ",id=" + id);
                String tmpAddr = "确认切换到" + mAddr.get(Long.valueOf(id).intValue()) + "吗？";
//                AlertDialog.Builder applyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
//                applyDialogBuilder.setMessage(tmpAddr);
//                applyDialogBuilder.setTitle("切换NFC卡片");
//                applyDialogBuilder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        applyChangeForNfcId(id);
//                        dialog.dismiss();
//                    }
//                });
//                applyDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
//                applyDialogBuilder.setCancelable(false);
//                applyDialogBuilder.create().show();
                new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("确认操作")
                        .setContentText(tmpAddr)
                        .setCancelText("取消")
                        .setConfirmText("嗯，走你")
                        .showCancelButton(true)
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismiss();
                                // reuse previous dialog instance, keep widget user state, reset them if you need
//                                sDialog.setTitleText("取消啦。")
//                                        .setContentText("Your imaginary file is safe :)")
//                                        .setConfirmText("OK")
//                                        .showCancelButton(false)
//                                        .setCancelClickListener(null)
//                                        .setConfirmClickListener(null)
//                                        .changeAlertType(SweetAlertDialog.ERROR_TYPE);

                                // or you can new a SweetAlertDialog to show
                               /* sDialog.dismiss();
                                new SweetAlertDialog(SampleActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Cancelled!")
                                        .setContentText("Your imaginary file is safe :)")
                                        .setConfirmText("OK")
                                        .show();*/
                            }
                        })
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                //Progress dlg
                                final SweetAlertDialog pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE)
                                        .setTitleText("切换中，请稍后...");
                                pDialog.show();
                                pDialog.setCancelable(false);
                                new CountDownTimer(500 * 4, 500) {
                                    public void onTick(long millisUntilFinished) {
                                        iTick++;
                                        switch (iTick) {
                                            case 0:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.blue_btn_bg_color));
                                                break;
                                            case 1:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.material_deep_teal_50));
                                                break;
                                            case 2:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.success_stroke_color));
                                                break;
                                            case 3:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.material_deep_teal_20));
                                                break;
                                            case 4:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.material_blue_grey_80));
                                                break;
                                            case 5:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.warning_stroke_color));
                                                break;
                                            case 6:
                                                pDialog.getProgressHelper().setBarColor(getResources().getColor(R.color.success_stroke_color));
                                                break;
                                        }
                                    }

                                    public void onFinish() {
                                        iTick = -1;
                                        pDialog.setTitleText("切换成功，Open It!")
                                                .setConfirmText("快去")
                                                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                    }
                                }.start();
                                //
                                sDialog.dismiss();
                                //
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        applyChangeForNfcId(id);
                                    }
                                });
                                thread.start();
//                                sDialog.setTitleText("Deleted!")
//                                        .setContentText("Your imaginary file has been deleted!")
//                                        .setConfirmText("OK")
//                                        .showCancelButton(false)
//                                        .setCancelClickListener(null)
//                                        .setConfirmClickListener(null)
//                                        .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                            }
                        })
                        .show();
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

        // TODO: customize mount info
        String[] strArrayMountInfo = mStrMountInfo.split(" ");
        ProcessFiles(strArrayMountInfo[2] + " " + strArrayMountInfo[0],
                "/system/etc/libnfc-nxp.conf",
                "/data/data/com.mclss.openit/cache/openitcachefile.conf");
    }

    private void ProcessFiles(String mountInfo, String nfcFileName, String cacheFileName) {
        Process suProcess;
        DataOutputStream os;
        String mountCmd = "mount -o rw,remount -t " + mountInfo + " /system\n";
        String backupConfCmd = "cp -rf " + nfcFileName + " " + nfcFileName + ".bak\n";
        String overwriteConfCmd = "cp -rf " + cacheFileName + " " + nfcFileName + "\n";
        String chmodCmd = "chmod 666 " + nfcFileName + "\n";
        String restoreMountCmd = "mount -o ro,remount -t " + mountInfo + " /system\n";
        String exitCmd = "exit\n";

        Log.i(TAG, mountCmd + backupConfCmd + overwriteConfCmd + chmodCmd + restoreMountCmd + exitCmd);

        try{
            suProcess = Runtime.getRuntime().exec("su");
            os= new DataOutputStream(suProcess.getOutputStream());

            os.writeBytes(mountCmd);
            os.flush();

            os.writeBytes(backupConfCmd);
            os.flush();

            os.writeBytes(overwriteConfCmd);
            os.flush();

            os.writeBytes(chmodCmd);
            os.flush();

            os.writeBytes(restoreMountCmd);
            os.flush();

            os.writeBytes(exitCmd);
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
