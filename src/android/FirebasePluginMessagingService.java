package org.apache.cordova.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";
    private static final String FILE_NAME = "notificationMapping.json";
    private static final String NOTIFICATION_REPLY = "NotificationReply";
    private static final String MARK_AS_READ_REPLY = "MarkAsReadReply";

    private static final int REQUEST_CODE_HELP = 101;
    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String NOTIFY_ID = "id";

    private static final String PREVIOUS_MESSAGES = "previousMessages";
    private static final String NOTIFY_ID_FOR_UPDATING = "notifIdForUpdating";
    private static final String MESSAGE_TARGET = "messageTarget";

    private static final String AUDIO_FORMAT = "\uD83D\uDCFC Audio";
    private static final String VOICE_FORMAT = "\uD83C\uDF99 Voice Message";
    private static final String PHOTO_FORMAT = "\uD83D\uDCF7 Photo";
    private static final String LINK_FORMAT = "\uD83D\uDD17 Link";

    private static String getStringResource(Context activityOrServiceContext, String name) {
        return activityOrServiceContext.getString(
                activityOrServiceContext.getResources().getIdentifier(
                        name, "string", activityOrServiceContext.getPackageName()
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
                displayNotification(this, getApplicationContext(), "0", target, username, groupName, message, eventType, nsound, showNotification, "", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    private static void saveNotificationsIdInFile(Context activityOrServiceContext, String target, Integer nId) {
        File file = new File(activityOrServiceContext.getFilesDir(), FILE_NAME);

        FileWriter fileWriter;

        // create file if does nto exist
        if (!file.exists()) {
            try {
                file.createNewFile();
                fileWriter = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("{}");
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // read file into string
            String response = readNotificationsFile(file);
            if (response == null) {
                return;
            }

            String nIdString = String.valueOf(nId);

            // parse file content
            JSONObject messageDetails = new JSONObject(response);

            // put notification id
            Boolean isConversationExisting = messageDetails.has(target);
            if (isConversationExisting) {
                JSONArray userMessages = (JSONArray) messageDetails.get(target);

                boolean idAlreadyExists = false;
                for (int i = 0; i < userMessages.length(); i++) {
                    if (userMessages.get(i).toString().equals(nIdString)) {
                        idAlreadyExists = true;
                        break;
                    }
                }
                if (!idAlreadyExists) {
                    userMessages.put(nIdString);
                }
            } else {
                JSONArray newUserMessages = new JSONArray();
                newUserMessages.put(nIdString);
                messageDetails.put(target, newUserMessages);
            }
            // save file
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

    public static ArrayList<String> removeFromFileAndHideNotificationsForTarget(Context activityOrServiceContext, String target) {
        File file = new File(activityOrServiceContext.getFilesDir(), FILE_NAME);

        // create file if does nto exist
        if (!file.exists()) {
            return null;
        }

        ArrayList<String> nIds = null;

        try {
            // read file into string
            String response = readNotificationsFile(file);
            if (response == null) {
                return null;
            }

            // parse file content
            JSONObject messageDetails = new JSONObject(response);

            // remove notification ids
            Boolean isConversationExisting = messageDetails.has(target);
            if (isConversationExisting) {
                nIds = new ArrayList<String>();

                // collect notifications ids
                JSONArray userMessages = (JSONArray) messageDetails.get(target);
                for (int i = 0; i < userMessages.length(); i++) {
                    nIds.add(userMessages.getString(i));
                }

                // remove
                messageDetails.remove(target);

                // save file
                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fileWriter);
                bw.write(messageDetails.toString());
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return nIds;
    }

    private static String readNotificationsFile(File file) {
        String response = null;
        try {
            // read file into string
            StringBuffer output = new StringBuffer();
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line + "\n");
            }
            response = output.toString();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    public static void displayNotification(Context activityOrServiceContext, Context appContext, String id, String target, String name, String groupName, String message, String eventType, String nsound, boolean showNotification, String sound, String lights) {
        Log.i(TAG, "displayNotification: Target: " + target);
        Log.i(TAG, "displayNotification: username: " + name);
        Log.i(TAG, "displayNotification: groupName: " + groupName);
        Log.i(TAG, "displayNotification: message: " + message);
        Log.i(TAG, "displayNotification: eventType: " + eventType);
        Log.i(TAG, "displayNotification: nsound: " + nsound);
        Log.i(TAG, "displayNotification: showNotification: " + showNotification);
        Log.i(TAG, "displayNotification: sound: " + sound);
        Log.i(TAG, "displayNotification: lights: " + lights);

        if (!showNotification) {
            return;
        }

        Integer notificationId = Integer.valueOf(id);
        if (notificationId == 0) {
            notificationId = target.hashCode();
        }

        Log.i(TAG, "displayNotification: id: " + notificationId);

        String channelId = getStringResource(activityOrServiceContext, "default_notification_channel_id");
        String channelName = getStringResource(activityOrServiceContext, "default_notification_channel_name");
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (nsound.equals("mute")) {
            Log.d(TAG, "notification nsound - switching channel");
            channelId = getStringResource(activityOrServiceContext, "silent_notification_channel_id");
            channelName = getStringResource(activityOrServiceContext, "silent_notification_channel_name");
            defaultSoundUri = null;
        }

        String typeOfLink = getTypeOfLink(message);
        message = typeOfLink == null ? message : typeOfLink;

        String title;
        String text;
        if (eventType.equals("chat")) {
            title = name;
            text = message;
        } else {
            title = groupName != null && groupName.length() > 0 ? groupName : target;
            text = name;
            if (message != null && message.trim().length() > 0) {
                text = text + " : " + message;
            }
        }

        Log.d(TAG, "Notification title: " + title);

        ////////////////////////////////////////////////////////////////////////////////////
        // Find previous messages and update notification ID
        ////////////////////////////////////////////////////////////////////////////////////
        StatusBarNotification[] activeToasts = new StatusBarNotification[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            activeToasts = ((NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();
        }
        List<String> msgs = new ArrayList<String>();
        int count = 0;
        for (StatusBarNotification sbn : activeToasts) {
            count++;
            Notification curNotif = sbn.getNotification();
            Bundle bundle = curNotif.extras;
            String currentTitle = bundle.getCharSequence(Notification.EXTRA_TITLE).toString();
            String currentText = bundle.getCharSequence(Notification.EXTRA_TEXT).toString();
            String currentTarget = bundle.getString(MESSAGE_TARGET);
            List<String> previousMessages = sbn.getNotification().extras.getStringArrayList(PREVIOUS_MESSAGES);

            Log.i("vnc", "NOTIFICATION " + count + " : = " + currentTitle + " : " + currentTarget + " : "
                    + currentText + " : " + previousMessages + ". Message: " + message);

            if (currentTarget != null && currentTarget.equals(target)) {
                notificationId = sbn.getNotification().extras.getInt(NOTIFY_ID_FOR_UPDATING);
                msgs.addAll(previousMessages);
                break;
            }
        }
        if (count == 0) {
            Log.i("vnc", "no notifications in Status bar, when message: " + message);
        }
        msgs.add(text);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activityOrServiceContext, channelId);

        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(null);
        //
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
            messagingStyle.setConversationTitle(title);
        }
        //
        for (String msg : msgs) {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
                messagingStyle.addMessage(msg, System.currentTimeMillis(), null);
            }else{
                messagingStyle.addMessage(msg, System.currentTimeMillis(), title);
            }
        }

        Intent intent = new Intent(activityOrServiceContext, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_PEER_JID, target);
        bundle.putString("vncEventType", "chat");
        bundle.putInt(NOTIFY_ID, notificationId);
        intent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activityOrServiceContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (nsound.equals("mute")) {
            Log.d(TAG, "notification nsound: " + title);
            notificationBuilder
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(messagingStyle)
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
                    .setStyle(messagingStyle)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setContentIntent(pendingIntent)
                    .setSound(defaultSoundUri)
                    .setGroup(title)
                    .setPriority(NotificationCompat.PRIORITY_MAX);
        }

        // Add actions
        //
        String notificationIdString = String.valueOf(notificationId);
        String inlineReplyActionName = NOTIFICATION_REPLY + "@@" + notificationIdString + "@@" + target;
        String markAsReadActionName = MARK_AS_READ_REPLY + "@@" + notificationIdString + "@@" + target;
        //
        PendingIntent replyPendingIntent;
        PendingIntent markAsReadPendingIntent;
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i("VNC", "Create notification actions (>=N), NOTIFY_ID: " + id);

            replyPendingIntent = PendingIntent.getBroadcast(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, NotificationReceiver.class)
                            .setAction(inlineReplyActionName),
                    0);

            markAsReadPendingIntent = PendingIntent.getBroadcast(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, NotificationReceiver.class)
                            .setAction(markAsReadActionName),
                    0);
        } else {
            Log.i("VNC", "Create notification actions, NOTIFY_ID: " + id);

            replyPendingIntent = PendingIntent.getActivity(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, ReplyActivity.class)
                            .setAction(inlineReplyActionName),
                    0);

            markAsReadPendingIntent = PendingIntent.getActivity(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, ReplyActivity.class)
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

        if (target != null && target.trim().length() > 0 && target.indexOf("@") != -1) {
            notificationBuilder.addAction(actionReply);
            notificationBuilder.addAction(actionMarkAsRead);
        }

        int resID = activityOrServiceContext.getResources().getIdentifier("logo", "drawable", activityOrServiceContext.getPackageName());
        if (resID != 0) {
            notificationBuilder.setSmallIcon(resID);
        } else {
            notificationBuilder.setSmallIcon(activityOrServiceContext.getApplicationInfo().icon);
        }

        if (nsound.equals("mute")) {
            Log.d(TAG, "not setting sound");
        } else {
            if (sound != null) {
                Log.d(TAG, "sound before path is: " + sound);
                Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + activityOrServiceContext.getPackageName() + "/raw/" + sound);
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
            int accentID = activityOrServiceContext.getResources().getIdentifier("accent", "color", activityOrServiceContext.getPackageName());
            notificationBuilder.setColor(activityOrServiceContext.getResources().getColor(accentID, null));
        }

        Notification notification = notificationBuilder.build();

        notification.extras.putStringArrayList(PREVIOUS_MESSAGES, (ArrayList<String>) msgs);
        notification.extras.putInt(NOTIFY_ID_FOR_UPDATING, notificationId);
        notification.extras.putString(MESSAGE_TARGET, target);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int iconID = android.R.id.icon;
            int notiID = activityOrServiceContext.getResources().getIdentifier("icon" +
                    "", "mipmap", activityOrServiceContext.getPackageName());
            if (notification.contentView != null) {
                notification.contentView.setImageViewResource(iconID, notiID);
            }
        }
        NotificationManager notificationManager = (NotificationManager) activityOrServiceContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //  Since android Oreo notification channel is needed.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (nsound.equals("mute")) {
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

        saveNotificationsIdInFile(activityOrServiceContext, target, notificationId);
    }

    private static String getTypeOfLink(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        text = text.trim();
        if (!text.startsWith("http") && !text.startsWith("https")) {
            return null;
        }

        List<String> photoFormat = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");
        List<String> audioFormat = Arrays.asList("wav", "mp3", "wma", "webm", "ogg");

        if (text.contains("audio_recording_")) {
            return VOICE_FORMAT;
        }

        String extension = text.substring(text.lastIndexOf(".") + 1);

        if (photoFormat.indexOf(extension) != -1) {
            return PHOTO_FORMAT;
        }

        if (audioFormat.indexOf(extension) != -1) {
            return AUDIO_FORMAT;
        }

        return LINK_FORMAT;
    }


    private String getPreference(Context context, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, null);
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
}
