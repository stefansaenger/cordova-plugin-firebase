package org.apache.cordova.firebase.utils;

import android.content.Context;

import android.os.Build;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {

    public static String getStringResource(Context context, String name) {
        return context.getString(
                context.getResources().getIdentifier(
                        name, "string", context.getPackageName()
                )
        );
    }

    public static Spannable getColorizedText(Context context, String stringResName, String colorName) {
        Spannable spannable = new SpannableString(getStringResource(context, stringResName));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            spannable.setSpan(
                new ForegroundColorSpan(context.getColor(context.getResources().getIdentifier(colorName, "color", context.getPackageName()))),
                0, 
                spannable.length(), 
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        return spannable;
    }

    public static String getMD5forString(String source) {
        String result = null;

        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(source.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) {
                    h = "0" + h;
                }

                hexString.append(h);
            }

            result = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return result;
    }
}
