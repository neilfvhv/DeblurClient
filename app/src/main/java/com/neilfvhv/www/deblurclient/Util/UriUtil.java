package com.neilfvhv.www.deblurclient.Util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class UriUtil {

    public static final String AUDIO = "media_type_audio";
    public static final String IMAGE = "media_type_image";
    public static final String VIDEO = "media_type_video";

    public static String getPathFromUri(Context context, Uri uri, String mediaType) {

        String path = null, data = null, id = null;
        Uri externalUri = null;

        switch (mediaType) {
            case AUDIO:
                id = MediaStore.Audio.Media._ID;
                data = MediaStore.Audio.Media.DATA;
                externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            case IMAGE:
                id = MediaStore.Images.Media._ID;
                data = MediaStore.Images.Media.DATA;
                externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            case VIDEO:
                id = MediaStore.Video.Media._ID;
                data = MediaStore.Video.Media.DATA;
                externalUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            if (DocumentsContract.isDocumentUri(context, uri)) {

                String docId = DocumentsContract.getDocumentId(uri);

                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {

                    String selection = id + "=" + docId.split(":")[1];

                    path = getPathFromUri(context, externalUri, data, selection);

                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {

//                    Uri contentUri = ContentUris.withAppendedId(
//                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));

//                    path = getPathFromUri(context, contentUri, data,null);
                    path = docId.split(":")[1];
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                path = getPathFromUri(context, uri, data, null);
            } else {
                path = uri.toString();
            }
        } else {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                path = getPathFromUri(context, uri, data, null);
            } else {
                path = uri.toString();
            }
        }

        return path;

    }

    private static String getPathFromUri(Context context, Uri uri, String data, String selection) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null,
                selection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(data));
            }
            cursor.close();
        }
        return path;
    }

}
