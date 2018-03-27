package com.neilfvhv.www.deblurclient.Activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.io.File;

import static android.os.Environment.DIRECTORY_PICTURES;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    public static final String SERVER_IP = "210.30.97.233";
    public static final String SERVER_PORT = "5000";
    public static final String UPLOAD_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/upload";

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
        File file = getExternalFilesDir(DIRECTORY_PICTURES);
        if (file != null) {
            pictureRootPath = file.getPath();
        }
    }

}