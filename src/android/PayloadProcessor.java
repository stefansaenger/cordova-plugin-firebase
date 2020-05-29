package org.apache.cordova.firebase;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import com.google.gson.Gson;
import org.apache.cordova.firebase.models.PayloadTalk;
import org.apache.cordova.firebase.models.PayloadTask;
import org.apache.cordova.firebase.models.PayloadMail;
import org.apache.cordova.firebase.utils.WidgetNotifier;
import org.apache.cordova.firebase.utils.FcmLoggerUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Context;
import android.util.Log;
import android.text.TextUtils;
import android.os.Bundle;

import org.apache.cordova.firebase.notification.NotificationManager;


public class PayloadProcessor {
  private static final String TAG = "FirebasePayloadProcessor";

  private ExecutorService notificationPool = Executors.newFixedThreadPool(1);

  private Context activityOrServiceContext;
  private Context appContext;

  PayloadProcessor(Context activityOrServiceContext, Context appContext) {
      this.activityOrServiceContext = activityOrServiceContext;
      this.appContext = appContext;
  }

  public void processTalkPayload(Map<String, String> payload) {
      try {
          JSONArray data = new JSONArray(payload.get("vnc"));

          if (data == null || data.length() == 0) {
              Log.d(TAG, "received empty data?");
              return;
          }

          for (int i = 0; i < data.length(); i++) {
              PayloadTalk notification = new Gson().fromJson(data.get(i).toString(), PayloadTalk.class);
              final String msgid = notification.msgid;
              final String target = notification.jid;
              final String initistor = notification.nfrom;
              final String receiver = notification.nto;
              final String username = notification.name;
              final String groupName = notification.gt;
              final String message = notification.body;
              final String eventType = notification.eType;
              final String nsound = notification.nsound;
              final String callSignal = notification.callSignal;
              final long timeStamp = notification.t;

              FcmLoggerUtils.logFcmReceived(appContext, msgid);

              if (TextUtils.isEmpty(target) || TextUtils.isEmpty(username)) {
                  Log.d(TAG, "returning due to empty 'target' or 'username' values");
                  return;
              }

              if (FirebasePlugin.inBackground()) {
                notificationPool.execute(new Runnable() {
                    public void run() {
                        if (notification.isCallNotification()) {
                            NotificationManager.displayTalkCallNotification(activityOrServiceContext, appContext, msgid,
                                eventType, target, username, groupName, message, initistor, receiver, timeStamp);
                        } else {
                            NotificationManager.displayTalkNotification(activityOrServiceContext, appContext, "0", msgid,
                                target, username, groupName, message, eventType, nsound, "", "");
                        }
                    }
                });
              } else {
                  // pass a notification to JS app in foreground
                  // so then a JS app will decide what to do and call a 'scheduleLocalNotification'
                  if (FirebasePlugin.hasNotificationsReceivedCallback()) {
                      Log.i(TAG, "onNotificationReceived callback provided");

                      Bundle dataBundle = new Bundle();
                      dataBundle.putString("msgid", msgid);
                      dataBundle.putString("target", target);
                      dataBundle.putString("nfrom", initistor);
                      dataBundle.putString("nto", receiver);
                      dataBundle.putString("username", username);
                      dataBundle.putString("groupName", groupName);
                      dataBundle.putString("message", message);
                      dataBundle.putString("eventType", eventType);
                      dataBundle.putString("nsound", nsound);
                      dataBundle.putString("mention", TextUtils.join(",", notification.mention));
                      dataBundle.putString("callSignal", callSignal);

                      FirebasePlugin.sendNotificationReceived(dataBundle);
                  } else {
                      Log.i(TAG, "no onNotificationReceived callback provided");
                  }
              }
          }
      } catch (JSONException e) {
          e.printStackTrace();
          return;
      }
  }

  public void processTaskPayload(Map<String, String> payload) {
      try {
          JSONObject data = new JSONObject(payload.get("vnctask"));

          // {vnctask={"task_id":46568,"type":"assignment","body":"Task \"Kskdk\" has been assigned to you by Luna Beier","username":"Luna Beier"}}
          // {vnctask={"task_id":46568,"type":"task_update","body":"Task has been updated","username":null}}
          // {vnctask={"task_id":46568,"type":"reminder","body":"A task \"Kskdk\" has a due date 03\/29\/2019","username":null}}

          if (data == null) {
              Log.d(TAG, "received empty data?");
              return;
          }

          PayloadTask notification = new Gson().fromJson(data.toString(), PayloadTask.class);
          final String body = notification.body;
          final String username = notification.username;
          final String taskId = notification.task_id;
          final String taskUpdatedOn = notification.task_updated_on;
          final String type = notification.type;
          final String sound = notification.sound;
          final String open_in_browser = notification.open_in_browser;
          final String language = notification.language;

          if (FirebasePlugin.inBackground()) {
            notificationPool.execute(new Runnable() {
                public void run() {
                    NotificationManager.displayTaskNotification(activityOrServiceContext, appContext,
                            body, username, taskId, taskUpdatedOn, type, sound, open_in_browser, language);
                }
            });
          } else {
            // pass a notification to JS app in foreground
            // so then a JS app will decide what to do and call a 'scheduleLocalNotification'
            if (FirebasePlugin.hasNotificationsReceivedCallback()) {
                Log.i(TAG, "onNotificationReceived callback provided");

                Bundle dataBundle = new Bundle();
                dataBundle.putString("body", body);
                dataBundle.putString("username", username);
                dataBundle.putString("task_id", taskId);
                dataBundle.putString("task_updated_on", taskUpdatedOn);
                dataBundle.putString("type", type);
                dataBundle.putString("sound", sound);
                dataBundle.putString("open_in_browser", open_in_browser);
                dataBundle.putString("language", language);
                FirebasePlugin.sendNotificationReceived(dataBundle);
            } else {
                Log.i(TAG, "no onNotificationReceived callback provided");
            }
          }
      } catch (JSONException e) {
          e.printStackTrace();
          return;
      }
  }

    public void processMailPayload(Map<String, String> payload) {
        try {
            JSONObject data = new JSONObject(payload);

            if (data == null || data.length() == 0) {
                Log.w(TAG, "received empty data?");
                return;
            }

            // notify widget data set changed
            WidgetNotifier.notifyMessagesListUpdated(appContext);

            PayloadMail notification = new Gson().fromJson(data.toString(), PayloadMail.class);
            final String fromAddress = notification.fromAddress;
            final String subject = notification.subject;
            final String fromDisplay = notification.fromDisplay;
            final String mid = notification.mid;
            final String cid = notification.cid;
            final String type = notification.type;
            final String folderId = notification.folderId;
            final String title = notification.title;
            final String body = notification.body;

            if (FirebasePlugin.inBackground()) {
                notificationPool.execute(new Runnable() {
                    public void run() {
                        NotificationManager.displayMailNotification(
                            activityOrServiceContext,
                            appContext,
                            subject,
                            body,
                            fromDisplay,
                            mid,
                            type,
                            folderId,
                            "",
                            fromAddress,
                            cid);
                    }
                });
            } else {
                // pass a notification to JS app in foreground
                // so then a JS app will decide what to do and call a 'scheduleLocalNotification'
                if (FirebasePlugin.hasNotificationsReceivedCallback()) {
                    Log.i(TAG, "onNotificationReceived callback provided");

                    Bundle dataBundle = new Bundle();
                    dataBundle.putString("mid", mid);
                    dataBundle.putString("cid", cid);
                    dataBundle.putString("ntype", type);
                    dataBundle.putString("fromAddress", fromAddress);
                    dataBundle.putString("subject", subject);
                    dataBundle.putString("fromDisplay", fromDisplay);
                    dataBundle.putString("folderId", folderId);
                    dataBundle.putString("body", body);

                    FirebasePlugin.sendNotificationReceived(dataBundle);
                } else {
                    Log.i(TAG, "no onNotificationReceived callback provided");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
