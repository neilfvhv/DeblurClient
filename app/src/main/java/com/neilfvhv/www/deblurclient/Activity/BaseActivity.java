package com.neilfvhv.www.deblurclient.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.os.Environment.getExternalStoragePublicDirectory;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    public static final String SERVER_IP = "172.19.189.119";
//    public static final String SERVER_IP = "210.30.97.233";
    public static final String SERVER_PORT = "5000";
    public static final String ALGORITHM_TYPE = "deblur";
    public static final String ALGORITHM_VERSION = "v1";

    public static final String UPLOAD_URL = "http://" + SERVER_IP + ":" + SERVER_PORT +
                                    "/upload/" + ALGORITHM_TYPE + "/" + ALGORITHM_VERSION;

    public static final int CHOOSE_PICTURE = 0;
    public static final int TAKE_PICTURE = 1;

    public static String pictureRootPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove top bar
        removeTopBar();
        // get path to store pictures
        getPictureRootPath();

    }

    /**
     * Remove Top Bar
     */
    private void removeTopBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Get Root Path to Store Pictures
     */
    private void getPictureRootPath() {
        File file = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (file != null) {
            pictureRootPath = file.getPath();
        }
    }

    /**
     * Save Bitmap to Image File (JPEG Format) / Save Image File to Gallery
     *
     * @param bitmap bitmap to save as image file
     * @param path   path to save
     */
    protected void saveImage(Bitmap bitmap, String path) {
        // create new file or override old file
        File file = new File(path);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            saveImageToGallery(path);
            Toast.makeText(this, "success to save image", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "fail to save image", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Save Image File to Gallery
     * @param path path to image file
     */
    protected void saveImageToGallery(String path) {
        sendBroadcast(new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
    }

    /**
     * Compress Image File to Target Size with Specific Rate
     * @param path path to source image file
     * @param size target size of, KB
     * @param rate rate to compress for every iteration, rate%
     * @return compressed image file
     */
    public static File compressImage(String path, int size, int rate) {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        FileOutputStream fos = null;
        try {
            // get source bitmap
            fis = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            // get output stream for source bitmap
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            // compress with rate% until bitmap less or equal than target size
            int quality = 100;
            while (baos.toByteArray().length / 1024 > size) {
                baos.reset();
                quality -= rate;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }
            // write to file
            fos = new FileOutputStream(path);
            fos.write(baos.toByteArray());
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new File(path);
    }

}