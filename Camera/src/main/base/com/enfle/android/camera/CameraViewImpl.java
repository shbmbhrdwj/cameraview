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

import android.media.CamcorderProfile;
import android.view.View;

import java.io.File;
import java.util.Set;

abstract class CameraViewImpl {

    protected final Callback mCallback;

    protected final PreviewImpl mPreview;

    @VideoQuality
    protected int videoQuality;

    protected String filePath;

    CameraViewImpl(Callback callback, PreviewImpl preview) {
        mCallback = callback;
        mPreview = preview;
    }

    View getView() {
        return mPreview.getView();
    }

    /**
     * @return {@code true} if the implementation was able to start the camera session.
     */
    abstract boolean start();

    abstract void stop();

    abstract boolean isCameraOpened();

    abstract void setFacing(int facing);

    abstract int getFacing();

    abstract Set<AspectRatio> getSupportedAspectRatios();

    /**
     * @return {@code true} if the aspect ratio was changed.
     */
    abstract boolean setAspectRatio(AspectRatio ratio);

    abstract AspectRatio getAspectRatio();

    abstract void setAutoFocus(boolean autoFocus);

    abstract boolean getAutoFocus();

    abstract void setFlash(int flash);

    abstract int getFlash();

    abstract void takePicture();

    abstract void setDisplayOrientation(int displayOrientation);

    abstract void startVideo();

    abstract void endVideo();

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setVideoQuality(int quality) {
        this.videoQuality = quality;
    }

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data);

        void onVideoStarted(String nextVideoAbsolutePath);

        void onVideoTaken(File videoFile);

        void onCameraStartFailed();
    }

    protected CamcorderProfile getCamcorderProfile(@VideoQuality int videoQuality, int cameraId) {
        CamcorderProfile camcorderProfile = null;
        switch (videoQuality) {
            case Constants1.VIDEO_QUALITY_QVGA:
                if (hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
                    camcorderProfile = CamcorderProfile.get(cameraId,
                            CamcorderProfile.QUALITY_QVGA);
                } else {
                    return getCamcorderProfile(Constants1.VIDEO_QUALITY_LOWEST, cameraId);
                }
                break;

            case Constants1.VIDEO_QUALITY_480P:
                if (hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                    camcorderProfile = CamcorderProfile.get(cameraId,
                            CamcorderProfile.QUALITY_480P);
                } else {
                    return getCamcorderProfile(Constants1.VIDEO_QUALITY_QVGA, cameraId);
                }
                break;

            case Constants1.VIDEO_QUALITY_720P:
                if (hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
                    camcorderProfile = CamcorderProfile.get(cameraId,
                            CamcorderProfile.QUALITY_720P);
                } else {
                    return getCamcorderProfile(Constants1.VIDEO_QUALITY_480P, cameraId);
                }
                break;

            case Constants1.VIDEO_QUALITY_1080P:
                if (hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                    camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
                } else {
                    return getCamcorderProfile(Constants1.VIDEO_QUALITY_720P, cameraId);
                }
                break;

            case Constants1.VIDEO_QUALITY_2160P:
                try {
                    camcorderProfile = CamcorderProfile.get(cameraId,
                            CamcorderProfile.QUALITY_2160P);
                } catch (Exception e) {
                    return getCamcorderProfile(Constants1.VIDEO_QUALITY_HIGHEST, cameraId);
                }
                break;

            case Constants1.VIDEO_QUALITY_HIGHEST:
                camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
                break;

            case Constants1.VIDEO_QUALITY_LOWEST:
                camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
                break;
        }

        return camcorderProfile;
    }

    private boolean hasProfile(int cameraId, int quality) {
        if (cameraId > 0) {
            return CamcorderProfile.hasProfile(cameraId, quality);
        } else {
            return CamcorderProfile.hasProfile(quality);
        }
    }

}
