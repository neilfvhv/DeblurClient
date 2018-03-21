package com.neilfvhv.www.deblurclient.Activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.neilfvhv.www.deblurclient.R;
import com.neilfvhv.www.deblurclient.Util.PermissionUtil;
import com.neilfvhv.www.deblurclient.Util.UriUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView originalImage;
    private ImageView processedImage;
    private Button submitButton;
    private ImageButton chooseButton;
    private ImageButton takeButton;
    private Uri mUri;

    private PermissionUtil.RequestResult requestResult = new PermissionUtil.RequestResult() {
        @Override
        public void onResult() {
            initialize();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionUtil.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestResult);
    }

    private void initialize() {

        originalImage = findViewById(R.id.original);

        processedImage = findViewById(R.id.processed);

        submitButton = findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUri != null) {
                    String path = UriUtil.getPathFromUri(MainActivity.this,
                            mUri, UriUtil.IMAGE);
                    uploadImage(path);
                } else {
                    Toast.makeText(
                            getApplicationContext(), "no image", Toast.LENGTH_LONG).show();
                }
            }
        });

        chooseButton = findViewById(R.id.choose);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, CHOOSE_PICTURE);
                Toast.makeText(
                        getApplicationContext(), "choose picture", Toast.LENGTH_SHORT).show();
            }
        });

        takeButton = findViewById(R.id.take);
        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                        MainActivity.this, "com.neilfvhv.www.deblurclient",
                        new File(pictureRootPath + "/temp.png")));
                startActivityForResult(intent, TAKE_PICTURE);
                Toast.makeText(
                        getApplicationContext(), "take picture", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void uploadImage(String path) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "file not exists");
            return;
        }
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("original_image", file.getName(), fileBody)
                .build();
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();
        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = httpBuilder
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "upload fail");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                Log.e(TAG, "upload success");
                InputStream is;
                ByteArrayOutputStream bos;
                InputStream decodeIs;
                try {
                    is = response.body().byteStream();
                    bos = new ByteArrayOutputStream();
                    byte[] bytes = new byte[512];
                    int len;
                    while ((len = is.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                    }
                    bos.flush();
                    decodeIs = new ByteArrayInputStream(bos.toByteArray());
                    BitmapFactory.Options ops = new BitmapFactory.Options();
                    ops.inJustDecodeBounds = false;
                    final Bitmap bitmap = BitmapFactory.decodeStream(decodeIs, null, ops);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            processedImage.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_PICTURE) {
                Uri uri = data.getData();
                mUri = uri;
                originalImage.setImageURI(uri);
            } else if (requestCode == TAKE_PICTURE) {
                Uri uri = Uri.parse(pictureRootPath + "/temp.png");
                mUri = uri;
                originalImage.setImageURI(uri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.result(requestCode, requestResult);
    }

}

