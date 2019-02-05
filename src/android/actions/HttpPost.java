package org.apache.cordova.firebase.actions;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class HttpPost implements Runnable {
    protected String body;
    protected String sender;
    protected Context context;
    int notificationId;
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    String mToken;
    String mApiUrl;
    private static final String TOKEN_CONSTANT = "auth-token";
    private static final String API_URL = "apiUrl";

    HttpPost(String body, String sender, int notificationId, Context context, String urlSupplement) {
        this.body = body;
        this.sender = sender;
        this.context = context;
        this.notificationId = notificationId;

        chooseAndSetApiUrl(urlSupplement);
        initToken();
        initNotificationObjects();
    }

    private void chooseAndSetApiUrl(String urlSupplement) {
        mApiUrl = baseApiUrl() + urlSupplement;
    }

    private void initNotificationObjects() {
        notificationManager = NotificationUtils.getManager(context);
        notificationBuilder = NotificationUtils.getBuilder(context);
    }

    private void initToken() {
        mToken = SharedPrefsUtils.getString(this.context, TOKEN_CONSTANT);
    }

    String baseApiUrl() {
        return SharedPrefsUtils.getString(this.context, API_URL);
    }

    @Override
    public void run() {
    }

    HttpURLConnection createUrlConnection() throws IOException {
        URL url = new URL(mApiUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Authorization", mToken);
        urlConnection.setRequestMethod("POST");
        return urlConnection;
    }

}
