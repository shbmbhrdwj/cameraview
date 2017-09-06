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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.view.View;

class SavedState extends View.BaseSavedState {

    @CameraView.Facing
    int facing;

    AspectRatio ratio;

    boolean autoFocus;

    @CameraView.Flash
    int flash;

    @SuppressWarnings("WrongConstant")
    public SavedState(Parcel source, ClassLoader loader) {
        super(source);
        facing = source.readInt();
        ratio = source.readParcelable(loader);
        autoFocus = source.readByte() != 0;
        flash = source.readInt();
    }

    public SavedState(Parcelable superState) {
        super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(facing);
        out.writeParcelable(ratio, 0);
        out.writeByte((byte) (autoFocus ? 1 : 0));
        out.writeInt(flash);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
            new ParcelableCompatCreatorCallbacks<SavedState>() {

                @Override
                public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                    return new SavedState(in, loader);
                }

                @Override
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }

            });

}