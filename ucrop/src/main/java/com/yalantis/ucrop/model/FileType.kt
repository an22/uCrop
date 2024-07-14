package com.yalantis.ucrop.model

import android.net.Uri

enum class FileType {
    REMOTE,
    CONTENT,
    FILE,
    UNKNOWN;

    companion object {
        fun from(uri: Uri): FileType {
            return when(uri.scheme) {
                "http", "https" -> REMOTE
                "content", "asset" -> CONTENT
                "file" -> FILE
                else -> UNKNOWN
            }
        }
    }
}