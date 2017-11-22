package com.enfle.android.camera.utils;

import static com.enfle.android.camera.utils.Constants.FACING_BACK;
import static com.enfle.android.camera.utils.Constants.FACING_FRONT;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Direction the camera faces relative to device screen. */
@IntDef({FACING_BACK, FACING_FRONT})
@Retention(RetentionPolicy.SOURCE)
public @interface Facing {
}
