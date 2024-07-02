package com.yalantis.ucrop.sample

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

public object ImageUtils {

    @JvmStatic
    public fun saveImageToGallery(
        context: Context,
        file: FileInputStream,
        imageName: String
    ): Uri? {
        try {
            val contentResolver = context.contentResolver
            val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.DESCRIPTION, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            // Insert file into MediaStore
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val galleryFileUri = contentResolver.insert(collection, values)
                ?: return null

            // Save file to uri from MediaStore
            contentResolver.openOutputStream(galleryFileUri)?.use {
                file.copyTo(it)
            }

            // Add exif data
            contentResolver.openFileDescriptor(galleryFileUri, "rw")?.use {
                // set Exif attribute so MediaStore.Images.Media.DATE_TAKEN will be set
                ExifInterface(it.fileDescriptor)
                    .apply {
                        setAttribute(
                            ExifInterface.TAG_DATETIME_ORIGINAL,
                            exifDateFormatter.format(Date())
                        )
                        saveAttributes()
                    }
            }

            // Now that we're finished, release the "pending" status, and allow other apps to view the image.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(galleryFileUri, values, null, null)
            }
            return galleryFileUri
        } catch (ex: Exception) {
            return null
        }
    }
}