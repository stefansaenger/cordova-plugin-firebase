package org.apache.cordova.firebase.actions;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.firebase.notification.NotificationCreator;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class InlineReplyPost extends HttpPost {

    public InlineReplyPost(String body, String sender, int notificationId, Context context) {
        super(body, sender, notificationId, context, "/xmpp-rest");
    }

    @Override
    public void run() {
        super.run();
        try {
            Log.i("VNC", "Token : " + mToken);
            Log.i("VNC", "notificationId : " + notificationId);
            Log.i("VNC", "To : " + sender);
            Log.i("VNC", "Message body : " + body);
            Log.i("VNC", "Api Url : " + mApiUrl);

            JSONObject postData = new JSONObject();
            postData.put("target", sender);
            postData.put("messagetext", body);

            Log.i("VNC", "postData : " + postData);
            HttpURLConnection urlConnection = createUrlConnection();

            NotificationCreator.setNotificationSmallIcon(context, notificationBuilder);

            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }
            int statusCode = urlConnection.getResponseCode();
            Log.i("VNC", "Server response, statusCode: " + statusCode);
            if (statusCode != 200) {
                saveInlineReplyOnError(context, sender, body);
            }
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            Log.i("VNC", e.getLocalizedMessage());
            saveInlineReplyOnError(context, sender, body);
            notificationManager.cancel(notificationId);
        } finally {

        }
    }

    private void saveInlineReplyOnError(Context context, String target, String message) {
        Log.i("VNC", "saveInlineReplyOnError, target: " + target + ", message: " + message);

        Gson gson = new Gson();
        String data = SharedPrefsUtils.getString(context, "replyMessages");
        ArrayList<Message> list = new ArrayList();
        if (data != null) {
            Type type = new TypeToken<ArrayList<Message>>() {
            }.getType();
            list = gson.fromJson(data, type);
        }
        list.add(new Message(target, message));
        String json = gson.toJson(list);

        SharedPrefsUtils.putString(context, "replyMessages", json);
    }

    private class Message {
        String target;
        String message;

        public Message(String target, String message) {
            this.target = target;
            this.message = message;
        }
    }
}
