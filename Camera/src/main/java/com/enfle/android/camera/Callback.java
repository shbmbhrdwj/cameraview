package com.enfle.android.camera;

import com.enfle.android.camera.view.CameraView;

import java.io.File;

/**
 * Callback for monitoring events about {@link CameraView}.
 */
public abstract class Callback {

    /**
     * Called when camera is opened.
     *
     * @param cameraView The associated {@link CameraView}.
     */
    public void onCameraOpened(CameraView cameraView) {
    }

    /**
     * Called when camera is closed.
     *
     * @param cameraView The associated {@link CameraView}.
     */
    public void onCameraClosed(CameraView cameraView) {
    }

    /**
     * Called when a camera preview failed
     */
    public void onCameraStartFailed() {

    }

    /**
     * Called when a mPicture is taken.
     *
     * @param cameraView The associated {@link CameraView}.
     * @param data       JPEG data.
     */
    public void onPictureTaken(CameraView cameraView, String data) {
    }

    public void onPictureTakenFailed() {

    }

    /**
     * Called when a video capture session started
     *
     * @param videoFilePath The associated {@link CameraView}.
     */
    public void onVideoStarted(String videoFilePath) {

    }

    /**
     * Called when a video capture session completed
     *
     * @param videoFile The associated {@link CameraView}.
     */
    public abstract void onVideoTaken(File videoFile);

}