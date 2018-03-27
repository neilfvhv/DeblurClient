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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.neilfvhv.www.deblurclient.R;
import com.neilfvhv.www.deblurclient.Util.PermissionUtil;
import com.neilfvhv.www.deblurclient.Util.UriUtil;
import com.tasomaniac.android.widget.DelayedProgressDialog;

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
    private DelayedProgressDialog dialog;
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

        // check permission before initialize
        PermissionUtil.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestResult);
    }

    /**
     * Initialize Views
     */
    private void initialize() {
        // initialize ImageView - original image
        originalImage = findViewById(R.id.original);

        // initialize ImageView - processed image
        processedImage = findViewById(R.id.processed);

        // initialize Button - submit image to the server
        submitButton = findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUri != null) {
                    // get image path from uri
                    String path = UriUtil.getPathFromUri(
                            MainActivity.this, mUri, UriUtil.IMAGE);
                    // start the progress dialog
                    dialog.show();
                    // upload image
                    uploadImage(path);
                } else {
                    Toast.makeText(
                            getApplicationContext(), "no image", Toast.LENGTH_LONG).show();
                }
            }
        });

        // initialize Button - choose picture from the mobile phone
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

        // initialize Button - take picture with camera
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

        // initialize ProgressDialog - loading dialog when the server is processing image
        dialog = DelayedProgressDialog.make(
                this, "Processing", "please wait for a minute");
        // delay to show the dialog - 0.5s
        dialog.setMinDelay(500);
        // minimum time to show the dialog - 1s
        dialog.setMinShowTime(1000);

    }

    /**
     * Upload Image to the Server
     * @param path the path for the image on the mobile phone
     */
    private void uploadImage(String path) {
        // get image file
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "image not exists", Toast.LENGTH_LONG).show();
            return;
        }

        // remove the last processed image
        processedImage.setImageDrawable(null);

        // build request
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("original_image", file.getName(), fileBody)
                .build();
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();

        // build OkHttp3 client
        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = httpBuilder
                .connectTimeout(5, TimeUnit.SECONDS) // connect timeout - 5s
                .readTimeout(10, TimeUnit.MINUTES)  // read timeout - 10 min
                .writeTimeout(10, TimeUnit.MINUTES) // write timeout - 10 min
                .build();

        // communicate with the server
        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // cancel the progress dialog
                        dialog.cancel();
                        Toast.makeText(MainActivity.this,
                                "upload image fail", Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.code() == 200) { // all well done
                    InputStream is;
                    ByteArrayOutputStream bos;
                    InputStream decodeIs;
                    try {
                        // transform byte stream to byte array stream
                        is = response.body().byteStream();
                        bos = new ByteArrayOutputStream();
                        byte[] bytes = new byte[512];
                        int len;
                        while ((len = is.read(bytes)) != -1) {
                            bos.write(bytes, 0, len);
                        }
                        bos.flush();
                        decodeIs = new ByteArrayInputStream(bos.toByteArray());

                        // decode bitmap from byte array stream
                        BitmapFactory.Options ops = new BitmapFactory.Options();
                        ops.inJustDecodeBounds = false;
                        final Bitmap bitmap = BitmapFactory.decodeStream(decodeIs, null, ops);

                        // back to UI thread
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // set processed image
                                processedImage.setImageBitmap(bitmap);
                                // cancel the progress dialog
                                dialog.cancel();
                                Toast.makeText(MainActivity.this,
                                        "process image success", Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 500) { // server internal error
                    // back to UI thread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // cancel the progress dialog
                            dialog.cancel();
                            Toast.makeText(MainActivity.this,
                                    "process image fail", Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                } else {
                    // back to UI thread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // cancel the progress dialog
                            dialog.cancel();
                            Toast.makeText(MainActivity.this,
                                    "unknown error", Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_PICTURE) {
                mUri = data.getData();
            } else if (requestCode == TAKE_PICTURE) {
                mUri = Uri.parse(pictureRootPath + "/temp.png");
            }
            originalImage.setImageURI(mUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.result(requestCode, requestResult);
    }

}
