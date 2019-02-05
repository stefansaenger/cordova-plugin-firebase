package org.apache.cordova.firebase.utils;

import android.content.Context;

public class StringUtils {

    public static String getStringResource(Context activityOrServiceContext, String name) {
        return activityOrServiceContext.getString(
                activityOrServiceContext.getResources().getIdentifier(
                        name, "string", activityOrServiceContext.getPackageName()
                )
        );
    }
}
