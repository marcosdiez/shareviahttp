/*
 * Contatins code from https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/activity/MessageCompose.java
 * APACHE 2.0 License.
 * 
 */
package com.MarcosDiez.shareviahttp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLDecoder;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

public class UriInterpretation {

	public long size = -1;
	public String name = null;
	public String mime;
	public boolean isDirectory = false;
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
				File f = new File(uriString.substring("file://".length()));
				isDirectory = f.isDirectory();
				if (isDirectory) {
					// Log.v(Util.myLogName, "We are dealing with a directory.");
					size = 0;
					return;
				}
				size = f.length();
				if (size == 0) {
					uriString = URLDecoder.decode(uriString).substring(
							"file://".length());
					f = new File(uriString);
					size = f.length();
				}
				Log.v(Util.myLogName, "zzz" + size);

			} else {
				try {
					File f = new File(uriString);
					isDirectory = f.isDirectory();
					return;
				} catch (Exception e) {
					Log.v(Util.myLogName, "Not a file... " + uriString);
					e.printStackTrace();
				}
				Log.v(Util.myLogName, "Not a file: " + uriString);

			}
		}
	}

	private void getMime(Uri uri, ContentResolver contentResolver) {
		mime = contentResolver.getType(uri);
		if (mime == null) {
			mime = "application/octet-stream";
		}
		if (mime.equals("application/octet-stream")) {
			// we can do better than that
			int pos = name.lastIndexOf('.');
			if (pos < 0)
				return;
			String extension = name.substring(pos).toLowerCase();
			if (extension.equals(".jpg")) {
				mime = "image/jpeg";
				return;
			}
			if (extension.equals(".png")) {
				mime = "image/png";
				return;
			}
			if (extension.equals(".gif")) {
				mime = "image/gif";
				return;
			}
			if (extension.equals(".mp4")) {
				mime = "video/mp4";
				return;
			}
			if (extension.equals(".avi")) {
				mime = "video/avi";
				return;
			}
			if (extension.equals(".mov")) {
				mime = "video/mov";
				return;
			}
			if (extension.equals(".vcf")) {
				mime = "text/x-vcard";
				return;
			}
			if (extension.equals(".txt")) {
				mime = "text/plain";
				return;
			}
			if (extension.equals(".html")) {
				mime = "text/html";
				return;
			}
			if (extension.equals(".json")) {
				mime = "application/json";
				return;
			}

		}

	}
}
