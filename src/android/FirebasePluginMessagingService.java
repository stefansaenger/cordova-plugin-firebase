package org.apache.cordova.firebase;

import android.app.NotificationChannel;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.app.Notification;
import android.text.TextUtils;
import android.content.ContentResolver;
import android.graphics.Color;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Map;
import java.util.Random;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";
    private static final String FILE_NAME = "notificationMapping.json";
    private static final String NOTIFICATION_REPLY = "NotificationReply";
    private static final String MARK_AS_READ_REPLY = "MarkAsReadReply";

    private static final int REQUEST_CODE_HELP = 101;
    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String NOTIFY_ID = "id";

    private static String getStringResource(Context context, String name) {
        return context.getString(
                context.getResources().getIdentifier(
                        name, "string", context.getPackageName()
                )
        );
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String token = getPreference(getApplicationContext(), "auth-token");
        if (token == null) {
            return;
        }

        File file = new File(this.getFilesDir(), FILE_NAME);

        FileReader fileReader = null;
        FileWriter fileWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;

        String response = null;

        if (!file.exists()) {
            try {
                file.createNewFile();
                fileWriter = new FileWriter(file.getAbsoluteFile());
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("{}");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Pass the message to the receiver manager so any registered receivers can decide to handle it
        boolean wasHandled = FirebasePluginMessageReceiverManager.onMessageReceived(remoteMessage);
        if (wasHandled) {
            Log.d(TAG, "Message was handled by a registered receiver");

            // Don't process the message in this method.
            return;
        }

        Map<String, String> payload = remoteMessage.getData();
        JSONArray data;
        try {
            data = new JSONArray(payload.get("vnc"));

            if (data == null || data.length() == 0) {
		            Log.d(TAG, "received empty data?");
                return;
            }

            for (int i = 0; i < data.length(); i++) {
                Payload notification = new Gson().fromJson(data.get(i).toString(), Payload.class);
                Random rand = new Random();
                int n = rand.nextInt(1000) + 1;
                String id = Integer.toString(n);
                String target = notification.jid;
                String username = notification.name;
                String groupName = notification.gt;
                String message = notification.body;
                String eventType = notification.eType;
                String nsound = notification.nsound;

                if (TextUtils.isEmpty(target) || TextUtils.isEmpty(username)) {
		                Log.d(TAG, "returning due to empty 'target' or 'username' values");
                    return;
                }

                boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback());
                displayNotification(this, id, target, username, groupName, message, eventType, nsound, showNotification, "", "");

                try {
                    StringBuffer output = new StringBuffer();
                    fileReader = new FileReader(file.getAbsolutePath());
                    bufferedReader = new BufferedReader(fileReader);

                    String line = "";

                    while ((line = bufferedReader.readLine()) != null) {
                        output.append(line + "\n");
                    }

                    response = output.toString();
                    bufferedReader.close();

                    JSONObject messageDetails = new JSONObject(response);
                    Boolean isUserExisting = messageDetails.has(target);

                    if (isUserExisting) {
                        JSONArray userMessages = (JSONArray) messageDetails.get(target);
                        userMessages.put(id);
                    } else {
                        JSONArray newUserMessages = new JSONArray();
                        newUserMessages.put(id);
                        messageDetails.put(target, newUserMessages);
                    }

                    fileWriter = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fileWriter);
                    bw.write(messageDetails.toString());
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    public static void displayNotification(Context context, String id, String target, String name, String groupName, String message, String eventType, String nosound, boolean showNotification, String sound, String lights) {
        Log.d(TAG, "displayNotification: id: " + id);
        Log.d(TAG, "displayNotification: Target: " + target);
        Log.d(TAG, "displayNotification: username: " + username);
        Log.d(TAG, "displayNotification: groupName: " + groupName);
        Log.d(TAG, "displayNotification: message: " + message);
        Log.d(TAG, "displayNotification: eventType: " + eventType);
        Log.d(TAG, "displayNotification: nsound: " + nsound);
        Log.d(TAG, "displayNotification: showNotification: " + showNotification);
        Log.d(TAG, "displayNotification: sound: " + sound);
        Log.d(TAG, "displayNotification: lights: " + lights);

        if (!showNotification) {
          return;
        }

        Integer notificationId = Integer.parseInt(id);

        String inlineReplyActionName = NOTIFICATION_REPLY + "@@" + id + "@@" + target;
        String markAsReadActionName = MARK_AS_READ_REPLY + "@@" + id + "@@" + target;
        //
        PendingIntent replyPendingIntent;
        PendingIntent markAsReadPendingIntent;
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i("VNC", "Create notification actions (>=N), NOTIFY_ID: " + id);

            replyPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                    REQUEST_CODE_HELP,
                    new Intent(context, NotificationReceiver.class)
                            .setAction(inlineReplyActionName),
                    0);

            markAsReadPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                    REQUEST_CODE_HELP,
                    new Intent(context, NotificationReceiver.class)
                            .setAction(markAsReadActionName),
                    0);
        } else {
            Log.i("VNC", "Create notification actions, NOTIFY_ID: " + id);

            replyPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    REQUEST_CODE_HELP,
                    new Intent(context, ReplyActivity.class)
                            .setAction(inlineReplyActionName),
                    0);

            markAsReadPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    REQUEST_CODE_HELP,
                    new Intent(context, ReplyActivity.class)
                            .setAction(markAsReadActionName),
                    0);
        }

        NotificationCompat.Action actionReply = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_revert, "Reply", replyPendingIntent)
                .addRemoteInput(new RemoteInput.Builder("Reply")
                        .setLabel("Type your message").build())
                .setAllowGeneratedReplies(true)
                .build();

        NotificationCompat.Action actionMarkAsRead = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_revert, "Mark as read", markAsReadPendingIntent)
                .build();

        Log.d(TAG, "going to show notification with " + nosound);

        Intent intent = new Intent(context, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_PEER_JID, target);
        bundle.putString("vncEventType", "chat");
        bundle.putInt(NOTIFY_ID, notificationId);
        intent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Integer.parseInt(id), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = getStringResource(context, "default_notification_channel_id");
        String channelName = getStringResource(context, "default_notification_channel_name");
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (nosound.equals("mute")) {
            Log.d(TAG, "notification nosound - switching channel");
            channelId = getStringResource(context, "silent_notification_channel_id");
            channelName = getStringResource(context, "silent_notification_channel_name");
            defaultSoundUri = null;
        }

        String title;
        String text;
        if (eventType.equals("chat")) {
            title = name;
            text = message;
        } else {
            title = groupName != null && groupName.length() > 0 ? groupName : target;
            text = name;
            if(message != null && message.trim().length() > 0) {
                text = text + " : " + message;
            }
        }

        Log.d(TAG, "Notification group name: " + title);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);

        if (nosound.equals("mute")) {
            Log.d(TAG, "notification nosound: " + title);

            notificationBuilder
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setContentIntent(pendingIntent)
                    .setSound(null)
                    .setGroup(title)
                    .setPriority(NotificationCompat.PRIORITY_MAX);

        } else {
            notificationBuilder
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setContentIntent(pendingIntent)
                    .setSound(defaultSoundUri)
                    .setGroup(title)
                    .setPriority(NotificationCompat.PRIORITY_MAX);
        }

        if (target != null && target.trim().length() > 0 && target.indexOf("@") != -1) {
            notificationBuilder.addAction(actionReply);
            notificationBuilder.addAction(actionMarkAsRead);
        }

        int resID = getResources().getIdentifier("logo", "drawable", getPackageName());
        if (resID != 0) {
            notificationBuilder.setSmallIcon(resID);
        } else {
            notificationBuilder.setSmallIcon(getApplicationInfo().icon);
        }

        if (nosound.equals("mute")) {
            Log.d(TAG, "not setting sound");
        } else {
            if (sound != null) {
                Log.d(TAG, "sound before path is: " + sound);
                Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                Log.d(TAG, "Parsed sound is: " + soundPath.toString());
                notificationBuilder.setSound(soundPath);
            } else {
                Log.d(TAG, "Sound was null ");
            }
        }

        if (lights != null) {
            try {
                String[] lightsComponents = lights.replaceAll("\\s", "").split(",");
                if (lightsComponents.length == 3) {
                    int lightArgb = Color.parseColor(lightsComponents[0]);
                    int lightOnMs = Integer.parseInt(lightsComponents[1]);
                    int lightOffMs = Integer.parseInt(lightsComponents[2]);

                    notificationBuilder.setLights(lightArgb, lightOnMs, lightOffMs);
                }
            } catch (Exception e) {
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int accentID = getResources().getIdentifier("accent", "color", getPackageName());
            notificationBuilder.setColor(getResources().getColor(accentID, null));
        }

        Notification notification = notificationBuilder.build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int iconID = android.R.id.icon;
            int notiID = getResources().getIdentifier("icon" +
                    "", "mipmap", getPackageName());
            if (notification.contentView != null) {
                notification.contentView.setImageViewResource(iconID, notiID);
            }
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //  Since android Oreo notification channel is needed.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (nosound.equals("mute")) {
                Log.d(TAG, "pushing to silentchannel");
                NotificationChannel silentchannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                silentchannel.setSound(null, null);
                notificationManager.createNotificationChannel(silentchannel);
            } else {
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
        }

        notificationManager.notify(notificationId, notification);
    }


    private String getPreference(Context context, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, null);
    }
}

class Payload {
    public String jid;
    public String name;
    public String eType;
    public String body;
    public String gt;
    public String nType;
    public String nsound;
}
