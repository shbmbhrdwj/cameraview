
package com.enfle.android.camera;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class FileImageProcessor extends ImageProcessor<String> {

    public FileImageProcessor(Context context, byte[] picture) {
        super(context, picture);
    }

    @Override
    public String getResult(String filePath) {
        File file;
        if (!TextUtils.isEmpty(filePath)) {
            file = new File(filePath);
        } else {
            file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "mPicture.jpg");
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(mPicture);
            os.close();
        } catch (IOException e) {
            Log.w(TAG, "Cannot write to " + file, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return null;
    }
}