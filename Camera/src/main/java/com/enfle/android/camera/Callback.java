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

package com.enfle.android.camera;

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
     * Called when a picture is taken.
     *
     * @param cameraView The associated {@link CameraView}.
     * @param data       JPEG data.
     */
    public void onPictureTaken(CameraView cameraView, String data) {
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

    /**
     * Called when a camera preview failed
     */
    public void onCameraStartFailed() {

    }
}