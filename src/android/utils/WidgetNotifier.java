package org.apache.cordova.firebase.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;


public class WidgetNotifier {

    public static void notifyMessagesListUpdated(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, "biz.vnc.mailwidget.WidgetProvider"));

            refreshListsForWidgets(context, manager, appWidgetIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void refreshListsForWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        if (appWidgetIds.length == 0) return;

        manager.notifyAppWidgetViewDataChanged(appWidgetIds, context.getResources().getIdentifier("messages_list", "id", context.getPackageName()));
    }
}