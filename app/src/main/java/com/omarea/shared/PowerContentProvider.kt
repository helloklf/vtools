package com.omarea.shared

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class PowerContentProvider : ContentProvider() {

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // Implement this to handle requests to delete one or more rows.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun getType(uri: Uri): String? {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // TODO: Implement this to handle requests to insert a new row.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate(): Boolean {
        // TODO: Implement this to initialize your content provider on startup.
        return false
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        // TODO: Implement this to handle query requests from clients.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        // TODO: Implement this to handle requests to update one or more rows.
        throw UnsupportedOperationException("Not yet implemented")
    }
}
