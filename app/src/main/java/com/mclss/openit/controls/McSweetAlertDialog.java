package com.mclss.openit.controls;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mclss.openit.logger.Log;

import java.util.Arrays;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by itc on 2015/11/23.
 */
public class McSweetAlertDialog extends SweetAlertDialog implements NfcAdapter.ReaderCallback{

    private final String TAG = "McSweetAlertDialog";
    public static final int MSG_NFC_GETUID = 0x3389;
    Handler mHandler;

    public McSweetAlertDialog(Context context, int alertType, Handler handler) {
        super(context, alertType);
        mHandler = handler;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "New tag discovered");
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_NFC_GETUID;
        String strNfcUid = bytesToHexString(tag.getId());
        if (strNfcUid != null) {
            strNfcUid = strNfcUid.toUpperCase();
        }
        Bundle bundle = new Bundle();
        bundle.putString("NfcUid", strNfcUid);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}


