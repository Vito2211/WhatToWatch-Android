package com.vp.whattowatch.ui.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.vp.whattowatch.R
import com.vp.whattowatch.data.sharing.ShareResult
import com.vp.whattowatch.data.sharing.SharingService

object SharingHelper {

    suspend fun shareContentItem(
        context: Context,
        sharingService: SharingService,
        contentId: String,
        onNotice: (String) -> Unit = {}
    ) {
        val payload = sharingService.createSingleItemSharePayload(contentId)
        if (payload == null) {
            Toast.makeText(context, context.getString(R.string.content_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        when (val result = sharingService.generateShareResult(payload)) {
            is ShareResult.DeepLink -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, result.url)
                    putExtra(Intent.EXTRA_TITLE, result.title)
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                context.startActivity(chooser)
            }

            is ShareResult.FileShare -> {
                onNotice(context.getString(R.string.share_fallback_file_notice))
                val uri = Uri.parse(result.fileUriString)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, result.title)
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    type = result.mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }

            is ShareResult.Error -> {
                val msg = context.getString(R.string.share_error, result.message)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun shareCustomList(
        context: Context,
        sharingService: SharingService,
        listId: String,
        onNotice: (String) -> Unit = {}
    ) {
        val payload = sharingService.createCustomListSharePayload(listId)
        if (payload == null) {
            Toast.makeText(context, context.getString(R.string.empty_list_items_title), Toast.LENGTH_SHORT).show()
            return
        }

        when (val result = sharingService.generateShareResult(payload)) {
            is ShareResult.DeepLink -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, result.url)
                    putExtra(Intent.EXTRA_TITLE, result.title)
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                context.startActivity(chooser)
            }

            is ShareResult.FileShare -> {
                onNotice(context.getString(R.string.share_fallback_file_notice))
                val uri = Uri.parse(result.fileUriString)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, result.title)
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    type = result.mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }

            is ShareResult.Error -> {
                val msg = context.getString(R.string.share_error, result.message)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
