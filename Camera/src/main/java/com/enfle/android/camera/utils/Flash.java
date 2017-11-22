
package com.enfle.android.camera.utils;

import static com.enfle.android.camera.utils.Constants.FLASH_AUTO;
import static com.enfle.android.camera.utils.Constants.FLASH_OFF;
import static com.enfle.android.camera.utils.Constants.FLASH_ON;
import static com.enfle.android.camera.utils.Constants.FLASH_RED_EYE;
import static com.enfle.android.camera.utils.Constants.FLASH_TORCH;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** The mode for for the camera device's flash control */
@IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
@Retention(RetentionPolicy.SOURCE)
public @interface Flash {
}
