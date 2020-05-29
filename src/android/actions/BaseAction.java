package org.apache.cordova.firebase.actions;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.cordova.firebase.utils.NotificationUtils;
import org.apache.cordova.firebase.utils.SharedPrefsUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class BaseAction implements Runnable {
    protected Context context;
    int notificationId;
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;

    String mApiUrl;
    private static final String API_URL = "apiUrl";

    BaseAction(int notificationId, Context context, String urlSupplement) {
        this.context = context;
        this.notificationId = notificationId;

        chooseAndSetApiUrl(urlSupplement);
        initNotificationObjects();
    }

    private void chooseAndSetApiUrl(String urlSupplement) {
        mApiUrl = baseApiUrl() + urlSupplement;
    }

    private void initNotificationObjects() {
        notificationManager = NotificationUtils.getManager(context);
        notificationBuilder = NotificationUtils.getBuilder(context);
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
        
        setRequestHeaders(urlConnection);
        setRequestMethod(urlConnection);

        return urlConnection;
    }

    protected void setRequestHeaders(HttpURLConnection urlConnection) throws IOException {

    }

    protected void setRequestMethod(HttpURLConnection urlConnection) throws IOException {

    }
}
