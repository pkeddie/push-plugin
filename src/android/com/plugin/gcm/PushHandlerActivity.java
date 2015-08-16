package com.plugin.gcm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;


public class PushHandlerActivity extends Activity
{
    private static String TAG = "PushHandlerActivity";

    // storing counters

    // RX ACCUMULATORS
    public static int UNSEEN_RX_COUNT = 0;
    public static  int RX_PATIENT_COUNT = 0;
    public static int RX_ITEMS_COUNT = 0;

    // INBOX ACCUMULATORS =
    public static ArrayList<String> INBOX_CONTACTS = new ArrayList<String>();
    public static ArrayList<String> INBOX_PATIENTS_CONTACTS = new ArrayList<String>();
    public static HashMap<String,ArrayList<String>> INBOX_CONTACT_MESSAGE_MAP = new HashMap<String,ArrayList<String>>();
    public static HashMap<String,ArrayList<String>> INBOX_CONTACT_MESSAGE_LINE_MAP = new HashMap<String,ArrayList<String>>();
    public static HashMap<String,ArrayList<String>> INBOX_PATIENTS_CONTACT_MESSAGE_MAP = new HashMap<String,ArrayList<String>>();
    public static HashMap<String,ArrayList<String>> INBOX_PATIENTS_CONTACT_MESSAGE_LINE_MAP = new HashMap<String,ArrayList<String>>();
    public static int UNSEEN_MESSAGE_COUNT = 0;
    public static int UNSEEN_PATIENT_MSG_COUNT = 0;



    /*
     * this activity will be started if the user touches a notification that we own.
     * We send it's data off to the push plugin for processing.
     * If needed, we boot up the main activity to kickstart the application.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        boolean isPushPluginActive = PushPlugin.isActive();
        processPushBundle(isPushPluginActive);

        finish();

        if (!isPushPluginActive) {
            forceMainActivityReload();
        }
    }

    /**
     * Takes the pushBundle extras from the intent,
     * and sends it through to the PushPlugin for processing.
     */
    private void processPushBundle(boolean isPushPluginActive)
    {
        Bundle extras = getIntent().getExtras();

        if (extras != null)	{
            PushHandlerActivity.UNSEEN_RX_COUNT = 0;
            PushHandlerActivity.RX_PATIENT_COUNT = 0;
            PushHandlerActivity.RX_ITEMS_COUNT = 0;
            PushHandlerActivity.INBOX_CONTACTS.clear();
            PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.clear();
            PushHandlerActivity.INBOX_CONTACT_MESSAGE_LINE_MAP.clear();
            PushHandlerActivity.UNSEEN_MESSAGE_COUNT = 0;
            PushHandlerActivity.INBOX_PATIENTS_CONTACTS.clear();
            PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.clear();
            PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_LINE_MAP.clear();
            PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT = 0;

            Bundle originalExtras = extras.getBundle("pushBundle");

            originalExtras.putBoolean("foreground", false);
            originalExtras.putBoolean("coldstart", !isPushPluginActive);

            PushPlugin.sendExtras(originalExtras);
        }
    }

    /**
     * Forces the main activity to re-launch if it's unloaded.
     */
    private void forceMainActivityReload()
    {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();





    }

}
