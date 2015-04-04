package com.MarcosDiez.shareviahttp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	Boolean atLeastOneDirectory = false;

	public FileZipper(OutputStream dest, ArrayList<Uri> inputUris) {
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
			for (Uri thisUri : inputUris) {
				UriInterpretation uriFile = new UriInterpretation(thisUri);
				addFileOrDirectory(BUFFER, out, data, uriFile);
			}
			out.close();
			s("Zip Done. Checksum: " + checksum.getChecksum().getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void addFileOrDirectory(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		if (uriFile.isDirectory) {
			addDirectory(BUFFER, out, data, uriFile);
		} else {
			addFile(BUFFER, out, data, uriFile);
		}
	}

	void addDirectory(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		atLeastOneDirectory = true;
		String directoryPath = uriFile.uri.getPath();
		if (directoryPath.charAt(directoryPath.length() - 1) != File.separatorChar) {
			directoryPath += File.separatorChar;
		}
		ZipEntry entry = new ZipEntry(directoryPath.substring(1));
		out.putNextEntry(entry);

		s("Adding Directory: " + directoryPath);
		File f = new File(directoryPath);
		String[] theFiles = f.list();
		if (theFiles != null) {
			for (String aFilePath : theFiles) {
				if (!aFilePath.equals(".") && !aFilePath.equals("..")) {
					String fixedFileName = "file://" + directoryPath
							+ aFilePath;
					Uri aFileUri = Uri.parse(fixedFileName);
					UriInterpretation uriFile2 = new UriInterpretation(aFileUri);
					addFileOrDirectory(BUFFER, out, data, uriFile2);
				}
			}
		}

	}

	void addFile(int BUFFER, ZipOutputStream out, byte[] data,
			UriInterpretation uriFile) throws FileNotFoundException,
			IOException {
		BufferedInputStream origin;
		s("Adding File: " + uriFile.uri.getPath() + " -- " + uriFile.name);
		origin = new BufferedInputStream(uriFile.getInputStream(), BUFFER);

		ZipEntry entry = new ZipEntry(getFileName(uriFile));

		out.putNextEntry(entry);
		int count;
		while ((count = origin.read(data, 0, BUFFER)) != -1) {
			out.write(data, 0, count);
		}
		origin.close();
	}

	String getFileName(UriInterpretation uriFile) {
		/*	Galery Sends uri.getPath() with values like /external/images/media/16458
		 *  while urlFile.name returns IMG_20120427_120038.jpg
		 * 	
		 *  since such name has no directory info, that would break real directories
		 */
		if (atLeastOneDirectory) {
			return uriFile.uri.getPath().substring(1);
		}
		return uriFile.name;
	}

}
