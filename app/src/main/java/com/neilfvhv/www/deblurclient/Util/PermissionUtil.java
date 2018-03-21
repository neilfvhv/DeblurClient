package com.neilfvhv.www.deblurclient.Util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

public class PermissionUtil {

    private static final String TAG = PermissionUtil.class.getSimpleName();

    private static final int CODE_REQUEST = 9999;

    public interface RequestResult {

        void onResult();

    }

    public static void requestPermissions(final @NonNull Activity activity,
                                          final @NonNull String[] requestPermissions,
                                          RequestResult requestResult) {
        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList<String> notGrantedPermissionsList = new ArrayList<>();
            for (String permission : requestPermissions) {
                int status = ActivityCompat.checkSelfPermission(activity, permission);
                if (status != PackageManager.PERMISSION_GRANTED) {
                    notGrantedPermissionsList.add(permission);
                }
            }
            if (notGrantedPermissionsList.size() > 0) {
                ActivityCompat.requestPermissions(
                        activity,
                        notGrantedPermissionsList.toArray(
                                new String[notGrantedPermissionsList.size()]),
                        CODE_REQUEST);
            } else {
                requestResult.onResult();
            }
        } else {
            requestResult.onResult();
        }
    }

    public static void result(final @IntRange(from = 0) int requestCode,
                              RequestResult requestResult) {
        Log.e(TAG, "requested permission end");
        if (requestCode == CODE_REQUEST) {
            requestResult.onResult();
        }
    }

}
