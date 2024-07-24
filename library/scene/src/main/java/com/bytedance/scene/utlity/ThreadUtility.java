/*
 * Copyright (C) 2019 ByteDance Inc
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
package com.bytedance.scene.utlity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Looper;

import androidx.annotation.RestrictTo;

/**
 * Created by JiangQi on 9/5/18.
 */

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ThreadUtility {
    public static void checkUIThread() {
        ExceptionsUtility.invokeAndThrowExceptionToNextUILoop(new Runnable() {
            @Override
            public void run() {
                if (!isMainThread()) {
                    throw new IllegalStateException("This method must call on main thread");
                }
            }
        });
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper() && Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
