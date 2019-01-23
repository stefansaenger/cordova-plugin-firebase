package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.FirebasePlugin;
import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class MarkAsReadPost extends HttpPost {

    public MarkAsReadPost(String sender, int notificationId, Context context) {
        super(null, sender, notificationId, context, "/markConversationsRead");
    }

    @Override
    public void run() {
        super.run();
        try {
            Log.i("VNC", "Token : " + mToken);
            Log.i("VNC", "notificationId : " + notificationId);
            Log.i("VNC", "To : " + sender);
            Log.i("VNC", "Api Url : " + mApiUrl);

            JSONObject postData = new JSONObject();
            postData.put(sender, new Date().getTime() / 1000);

            Log.i("VNC", "postData : " + postData);

            URL url = new URL(mApiUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", mToken);
            urlConnection.setRequestMethod("POST");
            int resID = context.getResources().getIdentifier("logo", "drawable", context.getPackageName());
            if (resID != 0) {
                notificationBuilder.setSmallIcon(resID);
            } else {
                notificationBuilder.setSmallIcon(context.getApplicationInfo().icon);
            }

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }
            int statusCode = urlConnection.getResponseCode();
            Log.i("VNC", "Server response, statusCode: " + statusCode);
            if (statusCode != 200) {
                saveMarkAsReadOnError(context, sender);
            }
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            Log.i("VNC", e.getLocalizedMessage());
            saveMarkAsReadOnError(context, sender);
            notificationManager.cancel(notificationId);
        } finally {
            // hide all other notifications for this target
            ArrayList<String> nIds = NotificationUtils.removeFromFileAndHideNotificationsForTarget(context, sender);
            if (nIds != null) {
                for (int i = 0; i < nIds.size(); i++) {
                    notificationManager.cancel(Integer.parseInt(nIds.get(i)));
                }
            }
            Bundle data = new Bundle();
            data.putString("target", sender);
            FirebasePlugin.sendNotificationMarkAsRead(data);
        }
    }

    private void saveMarkAsReadOnError(Context context, String target) {
        Log.i("VNC", "saveMarkAsReadOnError, target: " + target);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "markAsReadFailedRequests");
        ArrayList<MarkAsRead> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<MarkAsRead>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new MarkAsRead(target));
        String json = gson.toJson(list);
        SharedPrefsUtils.putString(context, "markAsReadFailedRequests", json);
    }


    private class MarkAsRead {
        String target;
        long timestamp;

        public MarkAsRead(String target) {
            this.target = target;
            this.timestamp = new Date().getTime();
        }
    }

}
