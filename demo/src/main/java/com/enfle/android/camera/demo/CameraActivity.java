
package com.enfle.android.camera.demo;

import static com.enfle.android.camera.utils.Constants.FACING_BACK;
import static com.enfle.android.camera.utils.Constants.FACING_FRONT;
import static com.enfle.android.camera.utils.Constants.FLASH_AUTO;
import static com.enfle.android.camera.utils.Constants.FLASH_OFF;
import static com.enfle.android.camera.utils.Constants.FLASH_ON;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.enfle.android.camera.demo.utils.FileUtils;
import com.enfle.android.camera.utils.AspectRatio;
import com.enfle.android.camera.Callback;
import com.enfle.android.camera.view.CameraView;

import java.io.File;
import java.util.Set;

/**
 * Created by subhamtyagi on 06/09/17.
 */

public class CameraActivity extends AppCompatActivity implements AspectRatioFragment.Listener {
    public static final String KEY_ALLOW_VIDEO = "ALLOW_VIDEO";
    public static final String KEY_ALLOW_IMAGE = "ALLOW_IMAGE";

    public static final String KEY_ALLOW_FRONT_CAMERA = "ALLOW_FRONT_CAMERA";
    public static final String KEY_ALLOW_BACK_CAMERA = "ALLOW_BACK_CAMERA";

    int[] FLASH_OPTIONS = {
            FLASH_AUTO,
            FLASH_OFF,
            FLASH_ON,
    };

    int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private GestureDetectorCompat mDetector;

    private static final String TAG = "MainActivity";
    private static final String FRAGMENT_DIALOG = "dialog";

    private int mCurrentFlash;

    private CameraView mCameraView;
    private ImageView mRecordButton;
    private ImageView mCameraIndicator;
    private ImageView mVideoIndicator;
    private FrameLayout mOverlay;
    private TextView mTimerTextView;
    private ImageView mIndicator;

    private Handler mBackgroundHandler;

    private int mCameraMode = 1;
    private Long mElapsedTime = 0L;


    private Handler mHandler = new Handler();
    private boolean isVideoStarted;
    private boolean isVideoEnabled;
    private boolean isImageEnabled;
    private boolean isFrontCameraEnabled;
    private boolean isBackCameraEnabled;

    private boolean mDisableCameraSwitcher;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            if (mCameraView == null) {
                return;
            }

            if (mCameraMode == 1) {
                mCameraView.setFilePath(getImagePath());
                mCameraView.takePicture();
                return;
            }

            if (isVideoStarted) {
                mCameraView.stopRecordingVideo();
                stopTimer();
                isVideoStarted = false;
            } else {
                mCameraView.setFilePath(getVideoFilePath());
                mCameraView.startRecordingVideo();
                isVideoStarted = true;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mCameraView = (CameraView) findViewById(R.id.camera);
        mRecordButton = (ImageView) findViewById(R.id.record_button);
        mCameraIndicator = findViewById(R.id.camera_indicator);
        mVideoIndicator = findViewById(R.id.video_indicator);
        mOverlay = findViewById(R.id.overlay);
        mIndicator = findViewById(R.id.indicator);
        mTimerTextView = findViewById(R.id.timer);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        fetchData(getIntent());
        mRecordButton.setOnClickListener(mOnClickListener);

        mCameraView.setVideoQuaity(com.enfle.android.camera.utils.Constants.VIDEO_QUALITY_HIGHEST);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
    }

    private void fetchData(Intent intent) {
        // 1. Set camera face
        isBackCameraEnabled = intent.getBooleanExtra(KEY_ALLOW_BACK_CAMERA, false);
        isFrontCameraEnabled = intent.getBooleanExtra(KEY_ALLOW_FRONT_CAMERA, false);
        setCameraFace();

        // 2. Enable/ disable image/video
        isImageEnabled = intent.getBooleanExtra(KEY_ALLOW_IMAGE, false);
        isVideoEnabled = intent.getBooleanExtra(KEY_ALLOW_VIDEO, false);
        setCameraTpe();
    }

    private void setCameraTpe() {
        if (isImageEnabled && !isVideoEnabled) {
            enableImageMode();
            mVideoIndicator.setVisibility(View.GONE);
        } else if (!isImageEnabled && isVideoEnabled) {
            enableVideoMode();
            mCameraIndicator.setVisibility(View.GONE);
        } else {
            mDetector = new GestureDetectorCompat(this, mOnSwipeTouchListener);
        }
    }

    private void setCameraFace() {
        if (isFrontCameraEnabled && isBackCameraEnabled) {
            return;
        }

        mDisableCameraSwitcher = true;

        if (isFrontCameraEnabled) {
            mCameraView.setFacing(FACING_FRONT);
        }
    }

    private String getVideoFilePath() {
        File photoFile = FileUtils.createVideoNewFile(this);
        return photoFile != null ? photoFile.getAbsolutePath() : null;
    }

    private String getImagePath() {
        File photoFile = FileUtils.createImageNewFile(this);
        return photoFile != null ? photoFile.getAbsolutePath() : null;
    }

    private OnSwipeTouchListener mOnSwipeTouchListener = new OnSwipeTouchListener() {
        @Override
        public void onSwipeRight() {
            super.onSwipeLeft();
            if (mCameraMode == 1 || isVideoStarted) {
                return;
            }
            // enable image mode
            enableImageMode();
        }

        @Override
        public void onSwipeLeft() {
            if (mCameraMode == 2 || isVideoStarted) {
                return;
            }
            // enable video mode
            enableVideoMode();
        }
    };

    private void enableVideoMode() {
        mCameraMode = 2;
        mIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_videocam_32dp));
        showOverlay();
        mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.bg_circle_button_red));
        mCameraIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_dot));
        mVideoIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_videocam));
    }

    private void enableImageMode() {
        mCameraMode = 1;
        mIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_photo_camera_32dp));
        showOverlay();
        mRecordButton.setImageDrawable(
                getResources().getDrawable(R.drawable.bg_circle_button_transparent));
        mVideoIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_dot));
        mCameraIndicator.setImageDrawable(
                ActivityCompat.getDrawable(CameraActivity.this, R.drawable.ic_photo_camera));
    }

    void hideOverlay() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mOverlay.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mOverlay.setVisibility(View.GONE);
                            }
                        });
            }
        }, 500);
    }

    void showOverlay() {
        mOverlay.animate()
                .alpha(1.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mOverlay.setVisibility(View.VISIBLE);
                        hideOverlay();
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDetector != null) {
            this.mDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.switch_camera);
        if (mDisableCameraSwitcher) {
            menuItem.setVisible(false);
        } else {
            menuItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isVideoStarted) {
            return true;
        }

        if (item.getItemId() == R.id.aspect_ratio) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (mCameraView != null && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                final AspectRatio currentRatio = mCameraView.getAspectRatio();
                AspectRatioFragment.newInstance(ratios, currentRatio).show(fragmentManager,
                        FRAGMENT_DIALOG);
            }
            return true;
        }
        if (item.getItemId() == R.id.switch_flash) {
            if (mCameraView != null) {
                mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                item.setTitle(FLASH_TITLES[mCurrentFlash]);
                item.setIcon(FLASH_ICONS[mCurrentFlash]);
                mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
            }
            return true;
        }

        if (item.getItemId() == R.id.switch_camera) {
            if (mCameraView != null) {
                int facing = mCameraView.getFacing();
                mCameraView.setFacing(facing == FACING_FRONT ? FACING_BACK : FACING_FRONT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    private Callback mCallback = new Callback() {

        @Override
        public void onVideoStarted(String nextVideoAbsolutePath) {
            isVideoStarted = true;
            startTimer();
        }

        @Override
        public void onVideoTaken(File file) {
            onVideoCaptured(file);
        }

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
            hideOverlay();
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final String data) {
            onImageCaptured(data);
        }

    };

    private void onVideoCaptured(File file) {
//        Uri videoUri = Uri.fromFile(file);
//        //2. start crop activity
//        Intent cropIntent = new Intent(this, VideoEditActivity.class);
//        cropIntent.putExtra(Constants.KEY_SELECTED_MEDIA_ITEM, mediaItem);
//        startActivityForResult(cropIntent, REQUEST_ACTIVITY_CROP_PHOTO);
    }


    private void onImageCaptured(String filePath) {
//        //2. start crop activity
//        Intent cropIntent = new Intent(this, CropImageActivity.class);
//        cropIntent.putExtra(Constants.KEY_SELECTED_MEDIA_ITEM, mediaItem);
//        startActivityForResult(cropIntent, REQUEST_ACTIVITY_CROP_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED || data == null) {
            setResult(resultCode, data);
            finish();
            return;
        }

//        boolean didCancel = data.getBooleanExtra(Constants.RESULT_KEY_DID_CANCEL, false);
//        if (!didCancel) {
//            setResult(resultCode, data);
//            finish();
//        }
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            mElapsedTime = mElapsedTime + 1000;

            int seconds = (int) (mElapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            seconds = seconds % 60;

            String secondsText = " : " + seconds;
            if (seconds < 10) {
                secondsText = " : 0" + seconds;
            }

            String minutesText = " : " + minutes;
            if (minutes < 10) {
                minutesText = ": 0" + minutes;
            }

            String hourText = "0" + hours;
            if (hours >= 10) {
                hourText = String.valueOf(hours);
            }

            mTimerTextView.setText(String.format("%s %s%s", hourText, minutesText, secondsText));
            mHandler.postDelayed(mUpdateTimeTask, 1000);
        }
    };

    private void startTimer() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.post(mUpdateTimeTask);
    }

    private void stopTimer() {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }
}
