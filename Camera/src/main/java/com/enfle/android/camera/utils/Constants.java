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

package com.enfle.android.camera.utils;

/**
 * Created by subhamtyagi on 05/09/17.
 */

public interface Constants {

    /** The camera device faces the opposite direction as the device's screen. */
    int FACING_BACK = 0;

    /** The camera device faces the same direction as the device's screen. */
    int FACING_FRONT = 1;


    /** Flash will not be fired. */
    int FLASH_OFF = 0;

    /** Flash will always be fired during snapshot. */
    int FLASH_ON = 1;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    int FLASH_TORCH = 2;

    /** Flash will be fired automatically when required. */
    int FLASH_AUTO = 3;

    /** Flash will be fired in red-eye reduction mode. */
    int FLASH_RED_EYE = 4;

    int LANDSCAPE_90 = 90;
    int LANDSCAPE_270 = 270;

    int FOCUS_OFF = 0;
    int FOCUS_CONTINUOUS = 1;
    int FOCUS_TAP = 2;
    int FOCUS_TAP_WITH_MARKER = 3;

    int ZOOM_OFF = 0;
    int ZOOM_PINCH = 1;

    int METHOD_STANDARD = 0;
    int METHOD_STILL = 1;
    int METHOD_SPEED = 2;

    int PERMISSIONS_STRICT = 0;
    int PERMISSIONS_LAZY = 1;
    int PERMISSIONS_PICTURE = 2;

    int VIDEO_QUALITY_480P = 0;
    int VIDEO_QUALITY_720P = 1;
    int VIDEO_QUALITY_1080P = 2;
    int VIDEO_QUALITY_2160P = 3;
    int VIDEO_QUALITY_HIGHEST = 4;
    int VIDEO_QUALITY_LOWEST = 5;
    int VIDEO_QUALITY_QVGA = 6;

    int DEFAULT_FACING = Constants.FACING_BACK;
    int DEFAULT_FLASH = Constants.FLASH_OFF;
    int DEFAULT_FOCUS = Constants.FOCUS_CONTINUOUS;
    int DEFAULT_ZOOM = Constants.ZOOM_OFF;
    int DEFAULT_METHOD = Constants.METHOD_STANDARD;
    int DEFAULT_PERMISSIONS = Constants.PERMISSIONS_STRICT;
    int DEFAULT_VIDEO_QUALITY = Constants.VIDEO_QUALITY_480P;

    int DEFAULT_JPEG_QUALITY = 100;
    boolean DEFAULT_CROP_OUTPUT = false;
    boolean DEFAULT_ADJUST_VIEW_BOUNDS = false;
    AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(16, 9);


}
