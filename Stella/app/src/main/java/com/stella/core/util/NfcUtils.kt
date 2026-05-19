package com.stella.core.util

import android.nfc.Tag

object NfcUtils {
    fun tagIdFromIntent(tag: Tag?): String? =
        tag?.id?.joinToString(":") { byte -> "%02X".format(byte) }

    fun matchesEnrolled(scanned: String?, enrolled: String?): Boolean =
        !scanned.isNullOrBlank() && scanned == enrolled
}
