package com.enfle.android.camera.demo.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Subham Tyagi
 * on 22/11/17.
 */

public class FileUtils {
    @Nullable
    public static File createImageNewFile(@NonNull final Context context) {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);

        File image = null;
        try {
            image = File.createTempFile(imageFileName,/* prefix */".jpg",/* suffix */storageDir/*
             directory */);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    @Nullable
    public static File createVideoNewFile(@NonNull final Context context) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                new Date());
        String imageFileName = "MPEG" + timeStamp + "_";
        File storageDir;

        storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);

        File image = null;
        try {
            image = File.createTempFile(imageFileName,/* prefix */".mp4",/* suffix */storageDir/*
             directory */);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
