package org.apache.cordova.firebase.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;

import org.apache.cordova.firebase.NotificationReceiver;
import org.apache.cordova.firebase.OnNotificationOpenReceiver;
import org.apache.cordova.firebase.ReplyActivity;
import org.apache.cordova.firebase.IncomingCallActivity;
import org.apache.cordova.firebase.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationCreator {

    private static final String TAG = "Firebase.NotificationCreator";

    public static final String VNC_PEER_JID = "vncPeerJid";
    public static final String VNC_INITIATOR_JID = "vncInitiatorJid";
    private static final String VNC_TASK_TASKID = "vncTaskTaskId";
    private static final String VNC_TASK_TASKUPDATEDON = "vncTaskTaskUpdatedOn";
    private static final String OPEN_IN_BROWSER = "open_in_browser";
    public static final String NOTIFY_ID = "id";

    private static final String VNC_MAIL_MSG_ID = "vncMailMsgId";

    public static final String PREVIOUS_MESSAGES = "previousMessages";
    public static final String NOTIFY_ID_FOR_UPDATING = "notifIdForUpdating";
    public static final String MESSAGE_TARGET = "messageTarget";
    public static final String MISSED_CALL_ID = "messed_call_id";

    public static final String NOTIFICATION_REPLY = "NotificationReply";
    public static final String MARK_AS_READ_REPLY = "MarkAsReadReply";
    public static final String SNOOZE_REPLY = "SnoozeReply";

    public static final String MAIL_MARK_AS_READ = "MailMarkAsRead";
    public static final String MAIL_NOTIFICATION_REPLY = "NotificationMailReply";
    public static final String MAIL_DELETE = "MailDelete";

    public static final String TALK_CALL_DECLINE = "TalkCallDecline";
    public static final String TALK_CALL_ACCEPT = "TalkCallAccept";
    public static final String TALK_DELETE_CALL_NOTIFICATION = "TalkDeleteCallNotification";
    //
    private static final int REQUEST_CODE_HELP = 101;

    private static final String AUDIO_FORMAT = "Audio";
    private static final String VOICE_FORMAT = "Voice Message";
    private static final String PHOTO_FORMAT = "Photo";
    private static final String LINK_FORMAT = "Link";
    private final String EMODJI_FORMAT = "Emodji";

    static Integer createNotifIdIfNecessary(String id, String target) {
        Integer notificationId = Integer.valueOf(id);
        if (notificationId == 0) {
            notificationId = target.hashCode();
        }
        Log.i(TAG, "displayNotification: id: " + notificationId);
        return notificationId;
    }

    static String defineChannelId(Context activityOrServiceContext, String nsound) {
        String channelId = StringUtils.getStringResource(activityOrServiceContext, "default_notification_channel_id");
        if (nsound.equals("mute")) {
            channelId = StringUtils.getStringResource(activityOrServiceContext, "silent_notification_channel_id");
        }
        return channelId;
    }

    static String defineChannelName(Context activityOrServiceContext, String nsound) {
        String channelName = StringUtils.getStringResource(activityOrServiceContext, "default_notification_channel_name");
        if (nsound.equals("mute")) {
            channelName = StringUtils.getStringResource(activityOrServiceContext, "silent_notification_channel_name");
        }
        return channelName;
    }

    static Uri defineSoundUri(String nsound) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (nsound.equals("mute")) {
            defaultSoundUri = null;
        }
        return defaultSoundUri;
    }

    static String defineCallChannelId(Context activityOrServiceContext) {
        String channelId = "call_channel_id";

        return channelId;
    }

    static String defineCallChannelName(Context activityOrServiceContext) {
        String channelName = StringUtils.getStringResource(activityOrServiceContext, "call_notifications_channel_name");
        return channelName;
    }

    static Uri defineCallSoundUri(Context context) {
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/incoming_call");
        return soundUri;
    }

    static String defineCallNotificationTitle(String jid, String name, String groupName) {
        String title;
        if (TextUtils.isEmpty(groupName)) {
            title = name;
        } else {
            title = groupName;
        }

        if (TextUtils.isEmpty(title)) {
            title = jid;
        }

        return title;
    }

    static String defineCallNotificationText(Context context, String callType) {
        String incomingCallFormat = StringUtils.getStringResource(context, "incoming_call_format");
        return String.format(incomingCallFormat, callType);
    }

    static String defineNotificationTitle(String eventType, String target,
                                          String name, String groupName) {
        String title;
        if (eventType.equals("chat")) {
            title = name;
        } else {
            title = groupName != null && groupName.length() > 0 ? groupName : target;
        }
        return title;
    }

    static String defineNotificationText(String eventType,
                                         String name, String message) {
        String typeOfLink = getTypeOfLink(message);
        message = typeOfLink == null ? message : typeOfLink;

        String text;
        if (eventType.equals("chat")) {
            text = message;
        } else {
            text = name;
            if (message != null && message.trim().length() > 0) {
                text = text + " : " + message;
            }
        }
        return text;
    }
    static Integer findNotificationIdForTargetAndUpdateContent(String target, StatusBarNotification[] activeToasts, List<String> msgs) {
        Integer notificationId = -1;
        for (StatusBarNotification sbn : activeToasts) {
            Bundle bundle = sbn.getNotification().extras;

            String missedCallId = bundle.getString(MISSED_CALL_ID);
            if (!TextUtils.isEmpty(missedCallId)){
                continue;
            }

            String currentTarget = bundle.getString(MESSAGE_TARGET);
            List<String> previousMessages = sbn.getNotification().extras.getStringArrayList(PREVIOUS_MESSAGES);

            if (currentTarget != null && currentTarget.equals(target)) {
                msgs.addAll(previousMessages);
                notificationId = sbn.getNotification().extras.getInt(NOTIFY_ID_FOR_UPDATING);
                break;
            }
        }
        return notificationId;
    }

    static Integer getExistingNotifIdByTarget(String target, StatusBarNotification[] statusBarNotifications) {
        for (StatusBarNotification sbn : statusBarNotifications) {
            Bundle bundle = sbn.getNotification().extras;
            String currentTarget = bundle.getString(MESSAGE_TARGET);

            if (currentTarget != null && currentTarget.equals(target)) {
                return sbn.getNotification().extras.getInt(NOTIFY_ID_FOR_UPDATING);
            }
        }
        return 0;
    }

    static NotificationCompat.MessagingStyle defineMessagingStyle(String title, List<String> msgs) {
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(title);
        //
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
            messagingStyle.setConversationTitle(title);
        }
        //
        for (String msg : msgs) {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
                messagingStyle.addMessage(msg, System.currentTimeMillis(), new Person.Builder().setName(title).build());
            } else {
                messagingStyle.addMessage(msg, System.currentTimeMillis(), title);
            }
        }
        return messagingStyle;
    }

    static PendingIntent createNotifPendingIntentTalk(Context activityOrServiceContext, String target,
                                                  Integer notificationId, String vncEventType, String vncEventValue, String vncInitiatorJid) {
        Intent intent = new Intent(activityOrServiceContext, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_PEER_JID, target);
        bundle.putString(VNC_INITIATOR_JID, vncInitiatorJid);
        bundle.putString(vncEventType, vncEventValue);
        bundle.putInt(NOTIFY_ID, notificationId);
        intent.putExtras(bundle);
        return PendingIntent.getBroadcast(activityOrServiceContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent createNotifPendingIntentTask(Context activityOrServiceContext, String taskId,
                                                  Integer notificationId, String vncEventType, String taskUpdatedOn,
                                                  String vncEventValue, String open_in_browser) {
        Intent intent = new Intent(activityOrServiceContext, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_TASK_TASKID, taskId);
        bundle.putString(vncEventType, vncEventValue);
        bundle.putString(OPEN_IN_BROWSER, open_in_browser);
        bundle.putInt(NOTIFY_ID, notificationId);
        if (taskUpdatedOn != null) {
            bundle.putString(VNC_TASK_TASKUPDATEDON, taskUpdatedOn);
        }
        intent.putExtras(bundle);
        return PendingIntent.getBroadcast(activityOrServiceContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent createNotifPendingIntentMail(Context activityOrServiceContext, String msgId, Integer notificationId, String type, String folderId, String cId) {
        Intent intent = new Intent(activityOrServiceContext, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_MAIL_MSG_ID, msgId);
        bundle.putString("mid", msgId);
        bundle.putString("cid", cId);
        bundle.putString("type", type);
        bundle.putString("folderId", folderId);
        bundle.putInt(NOTIFY_ID, notificationId);

        intent.putExtras(bundle);
        return PendingIntent.getBroadcast(activityOrServiceContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static NotificationCompat.Builder createNotification(Context activityOrServiceContext, String channelId, String nsound, String title, String text, NotificationCompat.Style style, PendingIntent pendingIntent, Uri defaultSoundUri) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activityOrServiceContext, channelId);
        notificationBuilder
                .setDefaults(nsound.equals("mute") ? NotificationCompat.DEFAULT_VIBRATE : NotificationCompat.DEFAULT_ALL)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .setSound(nsound.equals("mute") ? null : defaultSoundUri)
                .setGroup(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (style != null) {
          notificationBuilder.setStyle(style);
        } else {
          // ... do we have a default style ?
        }

        return notificationBuilder;

    }

    static NotificationCompat.Builder createCallNotification(Context activityOrServiceContext, String channelId, String title, String text, PendingIntent pendingIntent, Uri defaultSoundUri) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activityOrServiceContext, channelId);
        notificationBuilder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setTimeoutAfter(60000);

        return notificationBuilder;
    }

    public static void setNotificationSmallIcon(Context activityOrServiceContext, NotificationCompat.Builder notificationBuilder) {
        int resID = activityOrServiceContext.getResources().getIdentifier("notification_icon", "drawable", activityOrServiceContext.getPackageName());
        if (resID != 0) {
            notificationBuilder.setSmallIcon(resID);
        } else {
            notificationBuilder.setSmallIcon(activityOrServiceContext.getApplicationInfo().icon);
        }
    }

    static void setNotificationSound(Context activityOrServiceContext, NotificationCompat.Builder notificationBuilder,
                                     String nsound, String sound) {
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
    }

    static void setNotificationLights(NotificationCompat.Builder notificationBuilder, String lights) {
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
    }


    static void setNotificationColor(Context activityOrServiceContext, NotificationCompat.Builder notificationBuilder) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int accentID = activityOrServiceContext.getResources().getIdentifier("notification_accent", "color", activityOrServiceContext.getPackageName());
            notificationBuilder.setColor(activityOrServiceContext.getResources().getColor(accentID, null));
        }
    }

    static void setNotificationImageRes(Context activityOrServiceContext, Notification notification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int iconID = android.R.id.icon;
            int notifID = activityOrServiceContext.getResources().getIdentifier("icon" +
                    "", "mipmap", activityOrServiceContext.getPackageName());
            if (notification.contentView != null) {
                notification.contentView.setImageViewResource(iconID, notifID);
            }
        }
    }


    static void createNotificationChannel(NotificationManager notificationManager, String channelId, String channelName, String nsound) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            if (nsound.equals("mute")) {
                Log.d(TAG, "pushing to silentchannel");
                channel.setSound(null, null);
            }
            notificationManager.createNotificationChannel(channel);
            //
        }
    }

    static void createCallNotificationChannel(NotificationManager notificationManager, String channelId, String channelName, Uri sound) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(sound, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build());
            notificationManager.createNotificationChannel(channel);
        }
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

    public static void addReplyAndMarkAsReadActions(Context activityOrServiceContext, Context appContext, Integer notificationId, NotificationCompat.Builder notificationBuilder, String target) {
        String notificationIdString = String.valueOf(notificationId);
        String inlineReplyActionName = NOTIFICATION_REPLY + "@@" + notificationIdString + "@@" + target;
        String markAsReadActionName = MARK_AS_READ_REPLY + "@@" + notificationIdString + "@@" + target;
        //
        PendingIntent replyPendingIntent;
        PendingIntent markAsReadPendingIntent;
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i(TAG, "addReplyAndMarkAsReadActions (>=N)");

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
            Log.i(TAG, "addReplyAndMarkAsReadActions");

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
    }

    public static void addReplyMailAction(Context activityOrServiceContext, Context appContext, Integer notificationId, NotificationCompat.Builder notificationBuilder, String msgId, String subject, String fromAddress, String fromDisplay) {
       String notificationIdString = String.valueOf(notificationId);
       String inlineReplyActionName = MAIL_NOTIFICATION_REPLY
           + "@@" + notificationIdString
           + "@@" + msgId
           + "@@" + subject
           + "@@" + fromAddress
           + "@@" + fromDisplay;

      //
       PendingIntent replyPendingIntent;
       //
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
           Log.i(TAG, "addReplyAndMarkAsReadActions (>=N)");

          replyPendingIntent = PendingIntent.getBroadcast(
               appContext,
               notificationId,
               new Intent(activityOrServiceContext, NotificationReceiver.class)
                   .setAction(inlineReplyActionName),
               PendingIntent.FLAG_UPDATE_CURRENT);
       } else {
           Log.i(TAG, "addReplyAndMarkAsReadActions");

           replyPendingIntent = PendingIntent.getActivity(
               appContext,
               notificationId,
               new Intent(activityOrServiceContext, ReplyActivity.class)
                   .setAction(inlineReplyActionName),
               PendingIntent.FLAG_UPDATE_CURRENT);
       }

       NotificationCompat.Action actionReply = new NotificationCompat.Action.Builder(
               android.R.drawable.ic_menu_revert, "Reply", replyPendingIntent)
               .addRemoteInput(new RemoteInput.Builder("Reply")
                       .setLabel("Type your message").build())
               .setAllowGeneratedReplies(true)
               .build();

       notificationBuilder.addAction(actionReply);
    }


    public static void addSnoozeAction(Context activityOrServiceContext, Context appContext, Integer notificationId, NotificationCompat.Builder notificationBuilder, String taskId) {
        String notificationIdString = String.valueOf(notificationId);

        String snoozeActionName = SNOOZE_REPLY + "@@" + notificationIdString + "@@" + taskId;

        Log.i(TAG, "addSnoozeAction, snoozeActionName: " + snoozeActionName);

        PendingIntent snoozePendingIntent;
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i(TAG, "addSnoozeAction (>=N)");

            snoozePendingIntent = PendingIntent.getBroadcast(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, NotificationReceiver.class)
                            .setAction(snoozeActionName),
                    0);
        } else {
            Log.i(TAG, "addSnoozeAction");

            snoozePendingIntent = PendingIntent.getActivity(appContext,
                    REQUEST_CODE_HELP,
                    new Intent(activityOrServiceContext, ReplyActivity.class)
                            .setAction(snoozeActionName),
                    0);
        }

        NotificationCompat.Action actionSnooze = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_revert, "Snooze for 1h", snoozePendingIntent)
                .build();

        notificationBuilder.addAction(actionSnooze);
    }

    public static void addMarkMailAsReadAction(Context activityOrServiceContext, Context appContext, Integer notificationId, NotificationCompat.Builder notificationBuilder, String msgId) {
        String notificationIdString = String.valueOf(notificationId);

        String markAsReadActionName = MAIL_MARK_AS_READ + "@@" + notificationIdString + "@@" + msgId;

        Log.i(TAG, "addMarkMailAsReadAction, markAsReadActionName: " + markAsReadActionName);

        PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    Integer.parseInt(msgId),
                    new Intent(activityOrServiceContext, NotificationReceiver.class)
                            .setAction(markAsReadActionName),
                    0);

        NotificationCompat.Action actionMarkAsRead = new NotificationCompat.Action.Builder(
                0, "Mark as read", markAsReadPendingIntent)
                .build();

        notificationBuilder.addAction(actionMarkAsRead);
    }

    public static void addDeleteMailAction(Context activityOrServiceContext, Context appContext, Integer notificationId, NotificationCompat.Builder notificationBuilder, String msgId) {
        String notificationIdString = String.valueOf(notificationId);

        String deleteActionParams = MAIL_DELETE + "@@" + notificationIdString + "@@" + msgId;

        Log.i(TAG, "addMarkMailAsReadAction, deleteActionParams: " + deleteActionParams);

        PendingIntent markAsReadPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    Integer.parseInt(msgId),
                    new Intent(activityOrServiceContext, NotificationReceiver.class)
                            .setAction(deleteActionParams),
                    0);

        NotificationCompat.Action actionDelete = new NotificationCompat.Action.Builder(
                0, "Delete", markAsReadPendingIntent)
                .build();

        notificationBuilder.addAction(actionDelete);
    }

    public static void addCallDeclineAction(Context activityOrServiceContext, Context appContext, NotificationCompat.Builder notificationBuilder, 
                                            String callId, String callType, String callReceiver, boolean isGroupCall) {
        String callDeclineActionName = TALK_CALL_DECLINE 
        + "@@" + callId 
        + "@@" + callType 
        + "@@" + callReceiver 
        + "@@" + String.valueOf(isGroupCall);

        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                appContext,
                callId.hashCode(),
                new Intent(activityOrServiceContext, NotificationReceiver.class)
                    .setAction(callDeclineActionName),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action declineAction = new NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            StringUtils.getColorizedText(activityOrServiceContext, "call_action_decline", "decline_call_btn"),
            declinePendingIntent)
        .build();
 
        notificationBuilder.addAction(declineAction);
    }

    public static void addCallAcceptAction(Context activityOrServiceContext, Context appContext, NotificationCompat.Builder notificationBuilder, 
                                            String callId, String callType, String callInitiator) {
        String callAcceptActionName = TALK_CALL_ACCEPT 
        + "@@" + callId 
        + "@@" + callType
        + "@@" + callInitiator;
 
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
            appContext,
            callId.hashCode(),
            new Intent(activityOrServiceContext, NotificationReceiver.class)
                .setAction(callAcceptActionName),
            PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action acceptAction = new NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            StringUtils.getColorizedText(activityOrServiceContext, "call_action_accept", "accept_call_btn"),
            acceptPendingIntent)
        .build();
 
        notificationBuilder.addAction(acceptAction);
    }

    public static void addCallFullScreenIntent(Context appContext, NotificationCompat.Builder notificationBuilder, 
                                                String callId, String callType, String callInitiator, String callReceiver,
                                                String callTitle, String callSubTitle, boolean isGroupCall) {


        Intent callFullScreenIntent = IncomingCallActivity.createStartIntent(appContext, callId, callType, callInitiator, callReceiver, 
                                                                            callTitle, callSubTitle, isGroupCall);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            appContext, 
            callId.hashCode(),
            callFullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        
        notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true);
    }

    public static void addDeleteCallNotificationIntent(Context appContext, NotificationCompat.Builder notificationBuilder, String callId, String name, String groupName, String callType) {
        String deleteCallNotificationIntent = TALK_DELETE_CALL_NOTIFICATION 
            + "@@" + callId
            + "@@" + name
            + "@@" + groupName
            + "@@" + callType;
 
        Log.i(TAG, "addDeleteCallNotificationIntent, deleteCallNotificationIntent: " + deleteCallNotificationIntent);

        PendingIntent deleteCallNotificationPendingIntent = PendingIntent.getBroadcast(
            appContext,
            callId.hashCode(),
            new Intent(appContext, NotificationReceiver.class)
                .setAction(deleteCallNotificationIntent),
            PendingIntent.FLAG_UPDATE_CURRENT);
        
        notificationBuilder.setDeleteIntent(deleteCallNotificationPendingIntent);
    }
}
