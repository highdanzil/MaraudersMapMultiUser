package com.tylerlubeck.maraudersmapmultiuser;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tyler on 2/23/2015.
 */
public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.d("MARAUDERSMAP", "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.d("MARAUDERSMAP", "Deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                dispatchNotifications(extras.getString("msg"));
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void dispatchNotifications (String msg) {
        try {
            JSONObject object = new JSONObject(msg);
            String type = object.getString("type");
            if (type.equals("request_location")) {
                requestLocationNotification(object);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestLocationNotification(JSONObject msg) {
        try {
            String requestor_name = msg.getString("requestor");
            int requestor_id = msg.getInt("requestor_id");
            notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent replyIntent = new Intent(this, RespondToRequestActivity.class);
            replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            replyIntent.putExtra("requestor_id", requestor_id);
            replyIntent.putExtra("requestor", requestor_name);

            PendingIntent pendingReplyIntent = PendingIntent.getActivity(this,
                    0,
                    replyIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("WHERE YOU AT")
                    .setContentText(String.format("%s wants to know where you are", requestor_name))
                    .setContentIntent(pendingReplyIntent)
                    .setAutoCancel(true);
            notificationManager.notify(requestor_id, builder.build());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
