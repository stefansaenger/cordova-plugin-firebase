package org.apache.cordova.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import org.apache.cordova.firebase.actions.HttpPost;
import org.apache.cordova.firebase.actions.InlineReplyPost;
import org.apache.cordova.firebase.actions.MarkAsReadPost;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String NOTIFICATION_REPLY = "NotificationReply";
    private static final String MARK_AS_READ_REPLY = "MarkAsReadReply";
    private static final String VNC_PEER_JID = "vncPeerJid";
    private static final String NOTIFY_ID = "id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains(NOTIFICATION_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];
            CharSequence message = getReplyMessage(intent);

            Log.i("VNC", "NotificationReceiver onReceive NotificationReply, notificationId: " + notificationId + ", target: " + target + ", message: " + message);

            if (message != null && message.length() > 0) {
                Thread thread = new Thread(new InlineReplyPost(message.toString(), target, notificationId, context));
                thread.start();
            }
        } else if (intent.getAction().contains(MARK_AS_READ_REPLY)) {
            String[] actionParts = intent.getAction().split("@@");
            int notificationId = Integer.parseInt(actionParts[1]);
            String target = actionParts[2];

            Log.i("VNC", "NotificationReceiver onReceive MarkAsReadReply, notificationId: " + notificationId + ", target: " + target);

            Thread thread = new Thread(new MarkAsReadPost(target, notificationId, context));
            thread.start();
        }
    }

    private CharSequence getReplyMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("Reply");
        }
        return null;
    }
}
