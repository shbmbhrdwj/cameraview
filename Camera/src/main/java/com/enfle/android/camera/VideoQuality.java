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

import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_1080P;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_2160P;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_480P;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_720P;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_HIGHEST;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_LOWEST;
import static com.enfle.android.camera.Constants1.VIDEO_QUALITY_QVGA;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({VIDEO_QUALITY_QVGA, VIDEO_QUALITY_480P, VIDEO_QUALITY_720P, VIDEO_QUALITY_1080P,
        VIDEO_QUALITY_2160P, VIDEO_QUALITY_HIGHEST, VIDEO_QUALITY_LOWEST})
public @interface VideoQuality {
}