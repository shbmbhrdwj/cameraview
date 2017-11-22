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

package com.enfle.android.camera.view;

import android.content.Context;
import android.os.Build;

import com.enfle.android.camera.wrapper.Camera1;
import com.enfle.android.camera.wrapper.Camera2;
import com.enfle.android.camera.wrapper.Camera2Api23;
import com.enfle.android.camera.preview.CameraViewImpl;
import com.enfle.android.camera.preview.PreviewImpl;

/**
 * Created by subhamtyagi on 03/11/17.
 */

public class CameraProvider {

    public static CameraViewImpl getCameraViewImpl(Context context, PreviewImpl preview,
            CameraView.CallbackBridge mCallbacks) {
        if (Build.VERSION.SDK_INT < 21) {
            return new Camera1(mCallbacks, preview);
        } else if (Build.VERSION.SDK_INT < 23) {
            return new Camera2(mCallbacks, preview, context);
        } else {
            return new Camera2Api23(mCallbacks, preview, context);
        }
    }
}
