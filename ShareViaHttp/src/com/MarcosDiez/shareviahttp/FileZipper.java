package com.MarcosDiez.shareviahttp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.net.Uri;
import android.util.Log;

public class FileZipper implements Runnable {
	private void s(String s2) { // an alias to avoid typing so much!
		Log.d(Util.myLogName, s2);
	}

	OutputStream dest;
	ArrayList<Uri> inputUris;

	public FileZipper(OutputStream dest, ArrayList<Uri> inputUris ) {
		/*
		 * // get a list of files from current directory File f = new File(".");
		 * String inputFiles[] = f.list();
		 */
		this.dest = dest;
		this.inputUris = inputUris;
	}


	@Override
	public void run() {
		int BUFFER = 4096;

		try {
			BufferedInputStream origin = null;
			// FileOutputStream dest = new
			// FileOutputStream("c:\\zip\\myfigs.zip");
			s("Initializing ZIP");

			CheckedOutputStream checksum = new CheckedOutputStream(dest,
					new Adler32());

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					checksum));

			// out.setMethod(method);
			// out.setLevel(1) ;
			byte data[] = new byte[BUFFER];
			for( Uri thisUri : inputUris ){
				UriInterpretation uriFile = new UriInterpretation(thisUri);
				s("Adding: " + uriFile.name);
				origin = new BufferedInputStream( uriFile.getInputStream(), BUFFER);
				ZipEntry entry = new ZipEntry(filterSlashes(uriFile.name));
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}			
			out.close();
			s("Zip Done. Checksum: " + checksum.getChecksum().getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String filterSlashes(String input) {
		int pos = input.lastIndexOf("/");
		if (pos < 0)
			return input;
		return input.substring(pos + 1);
	}
}
