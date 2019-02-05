package org.apache.cordova.firebase;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.apache.cordova.firebase.models.Payload;
import org.apache.cordova.firebase.notification.NotificationManager;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Map;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String token = SharedPrefsUtils.getString(getApplicationContext(), "auth-token");
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
                NotificationManager.displayNotification(this, getApplicationContext(), "0",
                        target, username, groupName, message, eventType, nsound, showNotification, "", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }
}
