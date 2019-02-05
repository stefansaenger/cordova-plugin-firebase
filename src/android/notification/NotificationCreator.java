package org.apache.cordova.firebase.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import org.apache.cordova.firebase.NotificationReceiver;
import org.apache.cordova.firebase.OnNotificationOpenReceiver;
import org.apache.cordova.firebase.ReplyActivity;
import org.apache.cordova.firebase.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationCreator {

    private static final String TAG = "NotificationDisplay";

    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String NOTIFY_ID = "id";

    private static final String PREVIOUS_MESSAGES = "previousMessages";
    private static final String NOTIFY_ID_FOR_UPDATING = "notifIdForUpdating";
    private static final String MESSAGE_TARGET = "messageTarget";

    private static final String NOTIFICATION_REPLY = "NotificationReply";
    private static final String MARK_AS_READ_REPLY = "MarkAsReadReply";
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

    static Integer findPreviousMessagesAndUpdateNotifId(String target, Integer notificationId, StatusBarNotification[] activeToasts, List<String> msgs) {
        for (StatusBarNotification sbn : activeToasts) {
            Bundle bundle = sbn.getNotification().extras;
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
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(null);
        //
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
            messagingStyle.setConversationTitle(title);
        }
        //
        for (String msg : msgs) {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
                messagingStyle.addMessage(msg, System.currentTimeMillis(), null);
            } else {
                messagingStyle.addMessage(msg, System.currentTimeMillis(), title);
            }
        }
        return messagingStyle;
    }

    static PendingIntent createNotifPendingIntent(Context activityOrServiceContext, String target,
                                                  Integer notificationId, String vncEventType, String vncEventValue) {
        Intent intent = new Intent(activityOrServiceContext, OnNotificationOpenReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString(VNC_PEER_JID, target);
        bundle.putString(vncEventType, vncEventValue);
        bundle.putInt(NOTIFY_ID, notificationId);
        intent.putExtras(bundle);
        return PendingIntent.getBroadcast(activityOrServiceContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    static NotificationCompat.Builder createNotification(Context activityOrServiceContext, String channelId, String nsound, String title, String text, NotificationCompat.MessagingStyle messagingStyle, PendingIntent pendingIntent, Uri defaultSoundUri) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activityOrServiceContext, channelId);
        notificationBuilder
                .setDefaults(nsound.equals("mute") ? NotificationCompat.DEFAULT_VIBRATE : NotificationCompat.DEFAULT_ALL)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(messagingStyle)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .setSound(nsound.equals("mute") ? null : defaultSoundUri)
                .setGroup(title)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        return notificationBuilder;

    }

    public static void setNotificationSmallIcon(Context activityOrServiceContext, NotificationCompat.Builder notificationBuilder) {
        int resID = activityOrServiceContext.getResources().getIdentifier("logo", "drawable", activityOrServiceContext.getPackageName());
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
            int accentID = activityOrServiceContext.getResources().getIdentifier("accent", "color", activityOrServiceContext.getPackageName());
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
            NotificationChannel channel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
            if (nsound.equals("mute")) {
                Log.d(TAG, "pushing to silentchannel");
                channel.setSound(null, null);
            }
            notificationManager.createNotificationChannel(channel);
            //
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

    public static void addActionsForNotification(Context activityOrServiceContext, Context appContext, String id, Integer notificationId, NotificationCompat.Builder notificationBuilder, String target) {
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

    }


}
