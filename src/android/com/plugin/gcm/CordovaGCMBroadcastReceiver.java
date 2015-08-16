package com.plugin.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Implementation of GCMBroadcastReceiver that hard-wires the intent service to be
 * com.plugin.gcm.GcmntentService, instead of your_package.GcmIntentService
 */
public class CordovaGCMBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GcmIntentService";

    // notId for the different kinds of notifications we are expecting
    private static final int RX_NOTIFICATION = 1;
    private static final int INBOX_NOTIFICATION = 2;
    private static final int ACTIVATION_NOTIFICATION = 3;
    private static final int INBOX_PATIENT_NOTIFICATION = 4;


    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction() == "reduce"){
            // switch here on notId
            String notIdString = intent.getExtras().getString("reduceId");
            int notId = Integer.parseInt(notIdString);

            if(notId == RX_NOTIFICATION){
                // reset rx notification stuff
                PushHandlerActivity.UNSEEN_RX_COUNT = 0;
                PushHandlerActivity.RX_PATIENT_COUNT = 0;
                PushHandlerActivity.RX_ITEMS_COUNT = 0;

            }else if(notId == INBOX_NOTIFICATION){
                // reset inbox notification stuff
                PushHandlerActivity.INBOX_CONTACTS.clear();
                PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.clear();
                PushHandlerActivity.INBOX_CONTACT_MESSAGE_LINE_MAP.clear();
                PushHandlerActivity.UNSEEN_MESSAGE_COUNT = 0;
            }else if(notId == INBOX_PATIENT_NOTIFICATION){
                // reset patients inbox notification stuff
                PushHandlerActivity.INBOX_PATIENTS_CONTACTS.clear();
                PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.clear();
                PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_LINE_MAP.clear();
                PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT = 0;
            }

        }else{

            Log.d(TAG, "onHandleIntent - context: " + context);

            // Extract the payload from the message
            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            String messageType = gcm.getMessageType(intent);

            if (extras != null) {
                try {
                    if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                        JSONObject json = new JSONObject();

                        json.put("event", "error");
                        json.put("message", extras.toString());

                        PushPlugin.sendJavascript(json);
                    } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                        JSONObject json = new JSONObject();
                        json.put("event", "deleted");
                        json.put("message", extras.toString());
                        PushPlugin.sendJavascript(json);
                    } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                        // if we are in the foreground, just surface the payload, else post it to the statusbar
                        if (PushPlugin.isInForeground()) {
                            extras.putBoolean("foreground", true);

                            PushPlugin.sendExtras(extras);
                        } else {
                            extras.putBoolean("foreground", false);

                            // Send a notification if there is a message
                            if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                                createNotification(context, extras);
                            }
                        }
                    }
                } catch (JSONException exception) {
                    Log.d(TAG, "JSON Exception was had!");
                }
            }
        }
    }


    public void adjustExtras(Bundle extras){
        int notId = 0;

        try {
            notId = Integer.parseInt(extras.getString("notId"));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
        }

        if(notId == INBOX_NOTIFICATION){

            if(PushHandlerActivity.INBOX_CONTACTS.size()>1){
                String notificationData = extras.getString("notificationData");
                String modified = notificationData.replaceAll("sender","inbox");
                extras.remove("notificationData");
                extras.putString("notificationData",notificationData);
            }
        }else if(notId == INBOX_PATIENT_NOTIFICATION){
            if(PushHandlerActivity.INBOX_PATIENTS_CONTACTS.size()>1){
                String notificationData = extras.getString("notificationData");
                String modified = notificationData.replaceAll("sender","inbox");
                extras.remove("notificationData");
                extras.putString("notificationData",notificationData);
            }
        }else if(notId == RX_NOTIFICATION){
            if(PushHandlerActivity.UNSEEN_RX_COUNT>0){
                String notificationData = extras.getString("notificationData");
                String modified = notificationData.replaceAll("sender","multiple");
                extras.remove("notificationData");
                extras.putString("notificationData",notificationData);
            }
        }



    }

    public void createNotification(Context context, Bundle extras) {
        int notId = 0;

        try {
            notId = Integer.parseInt(extras.getString("notId"));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
        }
        if (notId == 0) {
            // no notId passed, so assume we want to show all notifications, so make it a random number
            notId = new Random().nextInt(100000);
            Log.d(TAG, "Generated random notId: " + notId);
        } else {
            Log.d(TAG, "Received notId: " + notId);
        }


        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName(context);

        Intent notificationIntent = new Intent(context, PushHandlerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        adjustExtras(extras);
        notificationIntent.putExtra("pushBundle", extras);

        PendingIntent contentIntent = PendingIntent.getActivity(context, notId, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent rIntent = new Intent(context, CordovaGCMBroadcastReceiver.class);
        rIntent.setAction("reduce");
        Bundle reduceExtras = new Bundle();
        reduceExtras.putString("reduceId",""+notId);
        rIntent.putExtras(reduceExtras);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, rIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        int defaults = Notification.DEFAULT_ALL;

        if (extras.getString("defaults") != null) {
            try {
                defaults = Integer.parseInt(extras.getString("defaults"));
            } catch (NumberFormatException ignore) {
            }
        }



        // building notification builder
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setDefaults(defaults)
                        .setSmallIcon(getSmallIcon(context, extras))
                        .setWhen(System.currentTimeMillis())
                        .setContentIntent(contentIntent)
                        .setColor(getColor(extras))
                        .setAutoCancel(true)
                        .setDeleteIntent(deleteIntent);
        mBuilder.setContentText("<missing message content>");


        String msgcnt = extras.getString("msgcnt");
        if (msgcnt != null) {
            mBuilder.setNumber(Integer.parseInt(msgcnt));
        }

        String soundName = extras.getString("sound");
        if (soundName != null) {
            Resources r = context.getResources();
            int resourceId = r.getIdentifier(soundName, "raw", context.getPackageName());
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resourceId);
            mBuilder.setSound(soundUri);
            defaults &= ~Notification.DEFAULT_SOUND;
            mBuilder.setDefaults(defaults);
        }

        if(notId == RX_NOTIFICATION){
            // increase rx counters
            int itemCount = 0;
            try {
                itemCount = Integer.parseInt(extras.getString("item_count"));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
            }


            PushHandlerActivity.RX_PATIENT_COUNT = PushHandlerActivity.RX_PATIENT_COUNT+1;
            PushHandlerActivity.RX_ITEMS_COUNT = PushHandlerActivity.RX_ITEMS_COUNT+itemCount;

            String title = "";
            String message = "";
            if(PushHandlerActivity.UNSEEN_RX_COUNT>0){
                // we have many rx so lets aggregage
                title = extras.getString("title_many");
                message = extras.getString("message_many");
                // replace the stuff here
                String itemCountString = ""+ PushHandlerActivity.RX_ITEMS_COUNT;
                String patientCountString =  "" + PushHandlerActivity.RX_PATIENT_COUNT;
                message = message.replaceAll("item_count",itemCountString);
                message = message.replaceAll("patient_count",patientCountString);
            }else if(PushHandlerActivity.UNSEEN_RX_COUNT == 0){
                // we have no unseen notifications

                if (itemCount > 1) {
                    // we have multiple items here
                    message = extras.getString("message_single_plural");
                    // have to substitute the number
                    message = message.replaceAll("item_count", "" + itemCount);
                    title = extras.getString("title");
                } else if(itemCount == 1){ // we have a single item
                    message = extras.getString("message");
                    title = extras.getString("title");
                }

            

            }
            PushHandlerActivity.UNSEEN_RX_COUNT = PushHandlerActivity.UNSEEN_RX_COUNT+1;
            mBuilder.setTicker(title);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(message);

        }else if(notId == INBOX_NOTIFICATION){
            String message = "";
            String title = "";
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            String senderId = extras.getString("sender_id");
            // increase inbox counters

            if(PushHandlerActivity.UNSEEN_MESSAGE_COUNT>0){
                // we have unseen messages and the options are:

                if(PushHandlerActivity.INBOX_CONTACTS.size()==1 && senderId.equals(PushHandlerActivity.INBOX_CONTACTS.get(0))){
                // One contact and a message from the same person

                    // 1) single contact many messages
                    title = extras.getString("title");
                    String message_text = extras.getString("message");


                    // message will be a summary of the contacts count
                    ArrayList<String> messages = PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.get(senderId);
                    messages.add(message_text);
                    //Buiding the expanded view thing
                    for(int i=0; i<messages.size(); i++) {
                        inboxStyle.addLine(messages.get(i));
                    }
                    PushHandlerActivity.UNSEEN_MESSAGE_COUNT = PushHandlerActivity.UNSEEN_MESSAGE_COUNT + 1;
                    message = extras.getString("single_many_message_text");
                    message = message.replaceAll("messsage_count", "" + PushHandlerActivity.UNSEEN_MESSAGE_COUNT);
                    inboxStyle.setSummaryText(message);


                }else{
                    // 2) Many contacts many messages
                    title = extras.getString("many_title");
                    String message_text = extras.getString("message");
                    String message_line = extras.getString("message_line");

                    if(PushHandlerActivity.INBOX_CONTACTS.contains(senderId)){
                        // this sender has already been seen
                        ArrayList<String> messages = PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.get(senderId);
                        ArrayList<String> messages_lines = PushHandlerActivity.INBOX_CONTACT_MESSAGE_LINE_MAP.get(senderId);
                        messages.add(message_text);
                        messages_lines.add(message_line);
                    }else{
                        // this sender has not already been seen
                        ArrayList<String> messages = new ArrayList<String>();
                        ArrayList<String> messages_lines = new ArrayList<String>();
                        messages.add(message);
                        messages_lines.add(message_line);
                        PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.put(senderId, messages);
                        PushHandlerActivity.INBOX_CONTACT_MESSAGE_LINE_MAP.put(senderId,messages_lines);
                        PushHandlerActivity.INBOX_CONTACTS.add(senderId);
                    }


                    // big for statement to get all the messages for all the users and message count
                    for(int i=0; i<PushHandlerActivity.INBOX_CONTACTS.size(); i++){
                        String contact_id = PushHandlerActivity.INBOX_CONTACTS.get(i);
                        ArrayList<String> contactMessages = PushHandlerActivity.INBOX_CONTACT_MESSAGE_LINE_MAP.get(contact_id);
                        for(int y=0; y<contactMessages.size(); y++) {
                            inboxStyle.addLine(contactMessages.get(y));
                        }
                    }
                    PushHandlerActivity.UNSEEN_MESSAGE_COUNT = PushHandlerActivity.UNSEEN_MESSAGE_COUNT + 1;
                    message = extras.getString("many_many_message_text");
                    message = message.replaceAll("messsage_count", "" + PushHandlerActivity.UNSEEN_MESSAGE_COUNT);
                    message = message.replaceAll("contact_count", ""+PushHandlerActivity.INBOX_CONTACTS.size());
                    inboxStyle.setSummaryText(message);

                }

                mBuilder.setStyle(inboxStyle);


            }else{
                // we don't have any unseen messages
                message = extras.getString("message");
                title = extras.getString("title");
                String message_line = extras.getString("message_line");


                // still we need to save the message and the contacts to the right collections
                inboxStyle.addLine(message);
                PushHandlerActivity.UNSEEN_MESSAGE_COUNT = PushHandlerActivity.UNSEEN_MESSAGE_COUNT + 1;
                // add to the maps and lists
                ArrayList<String> messages = new ArrayList<String>();
                ArrayList<String> messages_lines = new ArrayList<String>();
                messages.add(message);
                messages_lines.add(message_line);
                PushHandlerActivity.INBOX_CONTACT_MESSAGE_MAP.put(senderId, messages);
                PushHandlerActivity.INBOX_CONTACTS.add(senderId);
                mBuilder.setStyle(inboxStyle);
            }

            mBuilder.setTicker(title);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(message);



        }else if(notId == INBOX_PATIENT_NOTIFICATION){
            String message = "";
            String title = "";
            String senderId = extras.getString("sender_id");


            if(PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT>0){

                if(PushHandlerActivity.INBOX_PATIENTS_CONTACTS.size() == 1 && senderId.equals(PushHandlerActivity.INBOX_PATIENTS_CONTACTS.get(0))) {
                // One contact and a message from the same person
                    String message_text = extras.getString("message");
                    ArrayList<String> messages = PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.get(senderId);
                    messages.add(message_text);

                    title = extras.getString("patient_single_many_message_title");
                    message = extras.getString("patient_single_many_message_text");
                    // replace message count
                    PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT = PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT + 1;
                    message = message.replaceAll("messsage_count", ""+PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT);
                }else{
                // many contacts
                    title = extras.getString("patient_many_many_message_title");
                    String message_text = extras.getString("message");

                    if(PushHandlerActivity.INBOX_PATIENTS_CONTACTS.contains(senderId)){
                        // this sender has already been seen
                        ArrayList<String> messages = PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.get(senderId);
                        messages.add(message_text);
                    }else{
                        // this sender has not already been seen
                        ArrayList<String> messages = new ArrayList<String>();
                        messages.add(message);
                        PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.put(senderId, messages);
                        PushHandlerActivity.INBOX_PATIENTS_CONTACTS.add(senderId);
                    }

                    PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT = PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT + 1;
                    message = extras.getString("patient_many_many_message_text");
                    message = message.replaceAll("messsage_count", "" + PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT);
                    message = message.replaceAll("contact_count", ""+PushHandlerActivity.INBOX_PATIENTS_CONTACTS.size());


                }

                }else{
                // we don't have any unseen messages
                message = extras.getString("message");
                title = extras.getString("title");
                PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT = PushHandlerActivity.UNSEEN_PATIENT_MSG_COUNT + 1;

                // add to the maps and lists
                ArrayList<String> messages = new ArrayList<String>();
                messages.add(message);
                PushHandlerActivity.INBOX_PATIENTS_CONTACT_MESSAGE_MAP.put(senderId, messages);
                PushHandlerActivity.INBOX_PATIENTS_CONTACTS.add(senderId);
            }

            mBuilder.setTicker(title);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(message);


        }else if (notId == ACTIVATION_NOTIFICATION){
            // settled. Not much to see here so move on
            mBuilder.setTicker(extras.getString("title"));
            mBuilder.setContentTitle(extras.getString("title"));
            String message = extras.getString("message");
            mBuilder.setContentText(message);

        }

        final Notification notification = mBuilder.build();
        final int largeIcon = getLargeIcon(context, extras);
        if (largeIcon > -1) {
            notification.contentView.setImageViewResource(android.R.id.icon, largeIcon);
        }

        mNotificationManager.notify(appName, notId, notification);
    }

    private static String getAppName(Context context) {
        CharSequence appName =
                context
                        .getPackageManager()
                        .getApplicationLabel(context.getApplicationInfo());

        return (String) appName;
    }

    private int getColor(Bundle extras) {
        int theColor = 0; // default, transparent
        final String passedColor = extras.getString("color"); // something like "#FFFF0000", or "red"
        if (passedColor != null) {
            try {
                theColor = Color.parseColor(passedColor);
            } catch (IllegalArgumentException ignore) {}
        }
        return theColor;
    }

    private int getSmallIcon(Context context, Bundle extras) {

        int icon = -1;

        // first try an iconname possible passed in the server payload
        final String iconNameFromServer = extras.getString("smallIcon");
        if (iconNameFromServer != null) {
            icon = getIconValue(context.getPackageName(), iconNameFromServer);
        }

        // try a custom included icon in our bundle named ic_stat_notify(.png)
        if (icon == -1) {
            icon = getIconValue(context.getPackageName(), "ic_stat_notify");
        }

        // fall back to the regular app icon
        if (icon == -1) {
            icon = context.getApplicationInfo().icon;
        }

        return icon;
    }

    private int getLargeIcon(Context context, Bundle extras) {

        int icon = -1;

        // first try an iconname possible passed in the server payload
        final String iconNameFromServer = extras.getString("largeIcon");
        if (iconNameFromServer != null) {
            icon = getIconValue(context.getPackageName(), iconNameFromServer);
        }

        // try a custom included icon in our bundle named ic_stat_notify(.png)
        if (icon == -1) {
            icon = getIconValue(context.getPackageName(), "ic_notify");
        }

        // fall back to the regular app icon
        if (icon == -1) {
            icon = context.getApplicationInfo().icon;
        }

        return icon;
    }

    private int getIconValue(String className, String iconName) {
        try {
            Class<?> clazz  = Class.forName(className + ".R$drawable");
            return (Integer) clazz.getDeclaredField(iconName).get(Integer.class);
        } catch (Exception ignore) {}
        return -1;
    }
}
