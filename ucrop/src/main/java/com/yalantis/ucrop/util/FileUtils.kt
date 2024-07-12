/*
 * Copyright (C) 2007-2008 OpenIntents.org
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
package com.yalantis.ucrop.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel

/**
 * @author Peli
 * @author paulburke (ipaulpro)
 * @version 2013-12-11
 */
object FileUtils {
    /**
     * TAG for log messages.
     */
    private const val TAG = "FileUtils"

    /**
     * Copies one file into the other with the given paths.
     * In the event that the paths are the same, trying to copy one file to the other
     * will cause both files to become null.
     * Simply skipping this step if the paths are identical.
     *
     * @param pathFrom Represents the source file
     * @param pathTo Represents the destination file
     */
    @Throws(IOException::class)
    fun copyFile(pathFrom: String, pathTo: String) {
        if (pathFrom.equals(pathTo, ignoreCase = true)) {
            return
        }
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(File(pathFrom)).channel
            outputChannel = FileOutputStream(File(pathTo)).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }

    /**
     * Copies one file into the other with the given Uris.
     * In the event that the Uris are the same, trying to copy one file to the other
     * will cause both files to become null.
     * Simply skipping this step if the paths are identical.
     *
     * @param context The context from which to require the [android.content.ContentResolver]
     * @param uriFrom Represents the source file
     * @param uriTo Represents the destination file
     */
    @Throws(IOException::class)
    fun copyFile(context: Context, uriFrom: Uri, uriTo: Uri) {
        if (uriFrom == uriTo) {
            return
        }
        var isFrom: InputStream? = null
        var osTo: OutputStream? = null
        try {
            isFrom = context.contentResolver.openInputStream(uriFrom)
            osTo = context.contentResolver.openOutputStream(uriTo)
            if (isFrom is FileInputStream && osTo is FileOutputStream) {
                val inputChannel = isFrom.channel
                val outputChannel = osTo.channel
                inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            } else {
                throw IllegalArgumentException(
                    "The input or output URI don't represent a file. " +
                            "uCrop requires then to represent files in order to work properly."
                )
            }
        } finally {
            isFrom?.close()
            osTo?.close()
        }
    }
}
