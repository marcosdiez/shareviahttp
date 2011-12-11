/*
 * Contatins code from https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/activity/MessageCompose.java
 * APACHE 2.0 License.
 * 
 */
package com.MarcosDiez.shareviahttp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

public class UriInterpretation {

	public long size = -1;
	public String name = null;
	public String mime;
	Uri uri;
	ContentResolver contentResolver;

	public InputStream getInputStream() throws FileNotFoundException {
		return contentResolver.openInputStream(uri);
	}

	public UriInterpretation(Uri uri) {
		this.uri = uri;

		contentResolver = Util.theContext.getContentResolver();
		Cursor metadataCursor = contentResolver.query(uri, new String[] {
				OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE }, null,
				null, null);

		if (metadataCursor != null) {
			try {
				if (metadataCursor.moveToFirst()) {
					name = metadataCursor.getString(0);
					size = metadataCursor.getInt(1);
				}
			} finally {
				metadataCursor.close();
			}
		}

		if (name == null) {
			name = uri.getLastPathSegment();
		}

		getMime(uri, contentResolver);

		getFileSize(uri);

	}

	private void getFileSize(Uri uri) {
		if (size <= 0) {
			String uriString = uri.toString();
			if (uriString.startsWith("file://")) {
				Log.v(Util.myLogName, uriString.substring("file://".length()));
				File f = new File(uriString.substring("file://".length()));
				size = f.length();
			} else {
				Log.v(Util.myLogName, "Not a file: " + uriString);
			}
		} // else {
			// Log.v(Util.myLogName, "old file size: " + size);
		// }
		// Log.v(Util.myLogName, "new file size: " + size);
	}

	private void getMime(Uri uri, ContentResolver contentResolver) {
		mime = contentResolver.getType(uri);
		if (mime == null) {
			mime = "application/octet-stream";
		}
	}
}
