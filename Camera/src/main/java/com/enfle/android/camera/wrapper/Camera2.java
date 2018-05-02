/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.enfle.android.camera.wrapper;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.enfle.android.camera.utils.AspectRatio;
import com.enfle.android.camera.utils.Size;
import com.enfle.android.camera.utils.SizeMap;
import com.enfle.android.camera.preview.CameraViewImpl;
import com.enfle.android.camera.preview.PreviewImpl;
import com.enfle.android.camera.utils.Constants;
import com.enfle.android.camera.utils.VideoConfigurations;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@SuppressWarnings("MissingPermission")
@TargetApi(21)
public class Camera2 extends CameraViewImpl {

    private static final String TAG = "Camera2";

    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final CameraManager mCameraManager;

    private final StateCallback mCameraDeviceCallback = new StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCamera == null) {
                        return;
                    }
                    mCaptureSession = session;
                    updateAutoFocus();
                    updateFlash();
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Failed to start camera preview.", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure capture session.");
                    if (mCallback != null) {
                        mCallback.onCameraStartFailed();
                    }
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    if (mCaptureSession != null && mCaptureSession.equals(session)) {
                        mCaptureSession = null;
                    }
                }

            };

    private PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {

        @Override
        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, null);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to run precapture sequence.", e);
            }
        }

        @Override
        public void onReady() {
            captureStillPicture();
        }

    };

    private final OnImageAvailableListener mOnImageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    mCallback.onPictureTaken(data);
                }
            }
        }
    };

    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    private CameraDevice mCamera;

    private CameraCaptureSession mCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private Size mVideoSize;

    private int mFacing;

    private AspectRatio mAspectRatio = AspectRatio.of(16, 9);

    private boolean mAutoFocus;

    private int mFlash;

    private int mDisplayOrientation;
    private Handler mBackgroundHandler;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    public Camera2(Callback callback, PreviewImpl preview, Context context) {
        super(callback, preview);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                Camera2.this.startCaptureSession();
            }
        });
    }

    @Override
    public boolean start() {
        if (!chooseCameraIdByFacing()) {
            return false;
        }
        collectCameraInfo();
        prepareImageReader();
        startOpeningCamera();
        return true;
    }

    @Override
    public void stop() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    public boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    public void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    public int getFacing() {
        return mFacing;
    }

    @Override
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mPreviewSizes.ratios();
    }

    @Override
    public boolean setAspectRatio(AspectRatio ratio) {
        if (ratio == null || ratio.equals(mAspectRatio) || !mPreviewSizes.ratios().contains(ratio)) {
            // TODO: Better error handling
            return false;
        }
        mAspectRatio = ratio;
        prepareImageReader();
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
            startCaptureSession();
        }
        return true;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        mAutoFocus = autoFocus;
        if (mPreviewRequestBuilder == null) {
            return;
        }
        updateAutoFocus();

        if (mCaptureSession == null) {
            return;
        }
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    null);
        } catch (CameraAccessException e) {
            mAutoFocus = !mAutoFocus; // Revert
        }
    }

    @Override
    public boolean getAutoFocus() {
        return mAutoFocus;
    }

    @Override
    public void setFlash(int flash) {
        if (mFlash == flash) {
            return;
        }

        int saved = mFlash;
        mFlash = flash;

        if (mPreviewRequestBuilder == null) {
            return;
        }
        updateFlash();
        if (mCaptureSession != null) {
            return;
        }
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    null);
        } catch (CameraAccessException e) {
            mFlash = saved; // Revert
        }
    }

    @Override
    public int getFlash() {
        return mFlash;
    }


    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void setUpMediaRecorder() throws IOException {

        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }

        mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

        if (TextUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("File path can not be null");
        }

        CamcorderProfile camcorderProfile = getCamcorderProfile(videoQuality, Integer.parseInt(mCameraId));

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File mVideoFile = new File(filePath);
        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(VideoConfigurations.CaptureResolution.RES_1080P.getBitrate(VideoConfigurations.CaptureQuality.HIGH));
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mMediaRecorder.setOrientationHint(sensorOrientation);
        mMediaRecorder.prepare();
    }

    @Override
    public void takePicture() {
        if (mAutoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }


    @Override
    public void startVideo() {
        if (!isCameraOpened() || !mPreview.isReady()) {
            return;
        }

        try {
            setUpMediaRecorder();
            Surface surface = mPreview.getSurface();
            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(surface);
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCamera == null) {
                        return;
                    }
                    mCaptureSession = session;
                    updatePreview();

                    mCallback.onVideoStarted(filePath);

                    // Start recording
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // TODO: show error here
                    Log.e(TAG, "Failed to configure capture session.");
                    if (mCallback != null) {
                        mCallback.onCameraStartFailed();
                    }
                }
            }, getBackgroundHandler());
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        try {
            setUpCaptureRequestBuilder(mPreviewRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    @Override
    public void endVideo() {
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mCallback.onVideoTaken(new File(filePath));
        filePath = null;
        start();
    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() {
        try {

            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            final String[] ids = mCameraManager.getCameraIdList();

            if (ids.length == 0) { // No camera
                throw new RuntimeException("No camera available.");
            }

            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }

                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (internal == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }

                if (internal == internalFacing) {
                    mCameraId = id;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }

            // Not found
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

            Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }

            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("Unexpected state: LENS_FACING null");
            }

            for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    mFacing = INTERNAL_FACINGS.keyAt(i);
                    return true;
                }
            }

            // The operation can reach here when the only camera device is an external one.
            // We treat it as facing back.
            mFacing = Constants.FACING_BACK;
            return true;
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
    }

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes}, and optionally,
     * {@link #mAspectRatio}.</p>
     */
    private void collectCameraInfo() {

        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }

        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                mPreviewSizes.add(new Size(width, height));
            }
        }

        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            if (!mPictureSizes.ratios().contains(ratio)) {
                mPreviewSizes.remove(ratio);
            }
        }

        if (!mPreviewSizes.ratios().contains(mAspectRatio)) {
            mAspectRatio = mPreviewSizes.ratios().iterator().next();
        }
    }

    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)) {
            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    private void prepareImageReader() {
        if (mImageReader != null) {
            mImageReader.close();
        }
        Size largest = mPictureSizes.sizes(mAspectRatio).last();
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /* maxImages */ 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    /**
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceCallback}.</p>
     */
    private void startOpeningCamera() {
        try {
            mMediaRecorder = new MediaRecorder();
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mCameraId, e);
        }
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
     */
    void startCaptureSession() {
        if (!isCameraOpened() || !mPreview.isReady() || mImageReader == null) {
            return;
        }
        Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = mPreview.getSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mSessionCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to start camera session");
        }
    }

    /**
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the surface size.
     *
     * @return The picked size for camera preview.
     */
    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;

        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();

        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }

        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    /**
     * we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(android.util.Size[] choices) {
        for (android.util.Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return new Size(size.getWidth(), size.getHeight());
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        android.util.Size size = choices[choices.length - 1];
        return new Size(size.getWidth(), size.getHeight());
    }

    /**
     * Updates the internal state of auto-focus to {@link #mAutoFocus}.
     */
    private void updateAutoFocus() {
        if (mAutoFocus) {
            int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    /**
     * Updates the internal state of flash to {@link #mFlash}.
     */
    protected void updateFlash() {
        switch (mFlash) {
            case Constants.FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
        }
    }

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to lock focus.", e);
        }
    }

    /**
     * Captures a still mPicture.
     */
    void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            switch (mFlash) {
                case Constants.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    break;
                case Constants.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case Constants.FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case Constants.FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case Constants.FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            // Calculate JPEG orientation.
            @SuppressWarnings("ConstantConditions")
            int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, (sensorOrientation +
                    mDisplayOrientation * (mFacing == Constants.FACING_FRONT ? 1 : -1) + 360)
                    % 360);
            // Stop preview and capture a still mPicture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(), mCameraCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot capture a still mPicture.", e);
        }
    }

    private CameraCaptureSession.CaptureCallback mCameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            unlockFocus();
        }
    };

    /**
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still mPicture.
     */
    private void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlash();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to restart camera preview.", e);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }
}
