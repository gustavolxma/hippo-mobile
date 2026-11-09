package br.com.hippo.presentation.components

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun plainTextClipEntry(label: String, text: String): ClipEntry =
    ClipData.newPlainText(label, text).toClipEntry()
