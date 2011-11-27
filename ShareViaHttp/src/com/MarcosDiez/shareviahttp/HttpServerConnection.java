/*
Copyright (c) 2011, Marcos Diez --  marcos AT unitron.com.br
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.   
 * Neither the name of  Marcos Diez nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * 
 * Title: A simple Webserver Tutorial NO warranty, NO guarantee, MAY DO damage
 * to FILES, SOFTWARE, HARDWARE!! Description: This is a simple tutorial on
 * making a webserver posted on http://turtlemeat.com . Go there to read the
 * tutorial! This program and sourcecode is free for all, and you can copy and
 * modify it as you like, but you should give credit and maybe a link to
 * turtlemeat.com, you know R-E-S-P-E-C-T. You gotta respect the work that has
 * been put down.
 * 
 * Copyright: Copyright (c) 2002 Company: TurtleMeat
 * 
 * @author: Jon Berg <jon.berg[on_server]turtlemeat.com
 * @version 1.0
 */

package com.MarcosDiez.shareviahttp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class HttpServerConnection extends Thread {

	public HttpServerConnection(Uri fileUri, Socket connectionsocket) {
		this.fileUri = fileUri;
		this.connectionsocket = connectionsocket;
		this.start();
	}

	private Socket connectionsocket;
	private Uri fileUri;
	private String ipAddress = "";

	public void run() {
		ipAddress = getClientIpAddress();

		InputStream theInputStream;
		try {
			theInputStream = connectionsocket.getInputStream();
		} catch (IOException e1) {
			s("Error getting the InputString from connection socket.");
			e1.printStackTrace();
			return;
		}

		OutputStream theOuputStream;
		try {
			theOuputStream = connectionsocket.getOutputStream();
		} catch (IOException e1) {
			s("Error getting the OuputStream from connection socket.");
			e1.printStackTrace();
			return;
		}

		BufferedReader input = new BufferedReader(new InputStreamReader(
				theInputStream));

		DataOutputStream output = new DataOutputStream(theOuputStream);
		http_handler(input, output);
		s("Closing connection.");
	}

	private String getClientIpAddress() {
		InetAddress client = connectionsocket.getInetAddress();
		return client.getHostAddress() + "/" + client.getHostName();
	}

	// our implementation of the hypertext transfer protocol
	// its very basic and stripped down
	private void http_handler(BufferedReader input, DataOutputStream output) {
		String header;
		try {
			header = input.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		String upperCaseHeader = header.toUpperCase();
		Boolean sendOnlyHeader = false;

		if (upperCaseHeader.startsWith("HEAD")) {
			sendOnlyHeader = true;
		} else {
			if (!upperCaseHeader.startsWith("GET")) {
				dealWithUnsuporthedMethod(output);
				return;
			}
		}

		String path = getRequestedFilePath(header);

		if (path == null || path == "") {
			s("path is null!!!");
			return;
		}
		if (fileUri == null) {
			s("fileUri is null");
			return;
		}

		s("Client requested: [" + path + "][" + fileUri.toString() + "]");

		if (path.equals("/favicon.ico")) { // we have now favicon
			try {
				// if you could not open the file send a 404
				output.writeBytes(construct_http_header(404, null));
				// close the stream
				output.close();
			} catch (Exception e2) {
			}
			return;
		}

		ContentResolver cr = Util.theContext.getContentResolver();
		String mime = cr.getType(fileUri);

		if (path.equals("/")) {
			String thePath = addExtensionToUriIfNecessary(
					fileUri.getEncodedPath(), mime);

			String redirectOutput = construct_http_header(302, null, thePath);
			try {
				// if you could not open the file send a 404
				output.writeBytes(redirectOutput);
				// close the stream
				output.close();
			} catch (IOException e2) {
			}
			return;
		}

		InputStream requestedfile = null;

		try {

			// try to open the file,
			requestedfile = cr.openInputStream(fileUri);

		} catch (FileNotFoundException e) {
			try {
				// if you could not open the file send a 404
				output.writeBytes(construct_http_header(404, null));
				// close the stream
				output.close();
			} catch (Exception e2) {
				s("errorX:" + e2.getMessage());
			}
			;

			s("error" + e.getMessage());
		} // print error to gui

		// happy day scenario

		String outputString = construct_http_header(200,
				getMimeFromUri(fileUri, mime));

		try {
			output.writeBytes(outputString);

			// if it was a HEAD request, we don't print any BODY
			if (!sendOnlyHeader) {
				byte[] buffer = new byte[4096];
				for (int n; (n = requestedfile.read(buffer)) != -1;) {
					output.write(buffer, 0, n);
				}
			}

			output.close();
			requestedfile.close();
		} catch (IOException e) {
		}
	}

	private String getRequestedFilePath(String inputHeader) {
		String path;
		String tmp2 = new String(inputHeader);

		// tmp contains "GET /index.html HTTP/1.0 ......."
		// find first space
		// find next space
		// copy whats between minus slash, then you get "index.html"
		// it's a bit of dirty code, but bear with me...
		int start = 0;
		int end = 0;
		for (int a = 0; a < tmp2.length(); a++) {
			if (tmp2.charAt(a) == ' ' && start != 0) {
				end = a;
				break;
			}
			if (tmp2.charAt(a) == ' ' && start == 0) {
				start = a;
			}
		}
		path = tmp2.substring(start + 1, end); // fill in the path
		return path;
	}

	private void dealWithUnsuporthedMethod(DataOutputStream output) {
		try {
			output.writeBytes(construct_http_header(501, null));
			output.close();
			return;
		} catch (Exception e3) { // if some error happened catch it
			s("_error:" + e3.getMessage());
		} // and display error
	}

	// this function adds an extension to URIs without extensions, according to
	// it's mime type.
	// this is useful because of the android gallery
	private static String addExtensionToUriIfNecessary(String encodedPath,
			String mime) {
		if (mime == null || encodedPath.indexOf('.') >= 0)
			return encodedPath;

		if (mime.equals("image/jpeg"))
			return encodedPath + ".jpg";

		if (mime.equals("image/png"))
			return encodedPath + ".png";

		if (mime.equals("image/gif"))
			return encodedPath + ".gif";

		if (mime.equals("video/mp4"))
			return encodedPath + ".mp4";

		if (mime.equals("application/x-troff-msvideo")
				|| mime.equals("video/avi") || mime.equals("video/x-msvideo")
				|| mime.equals("video/msvideo"))
			return encodedPath + ".avi";

		if (mime.equals("video/quicktime") || mime.equals("video/mov"))
			return encodedPath + ".mov";

		
		if (mime.endsWith("text/x-vcard")) 
			return encodedPath + ".vcf";
		
		return encodedPath;
	}

	private void s(String s2) { // an alias to avoid typing so much!
		Log.d(Util.myLogName, "[" + ipAddress + "] " + s2);
	}

	private static String httpReturnCodeToString(int return_code) {
		switch (return_code) {
		case 200:
			return "200 OK";
		case 302:
			return "302 Moved Temporarily";
		case 400:
			return "400 Bad Request";
		case 403:
			return "403 Forbidden";
		case 404:
			return "404 Not Found";
		case 500:
			return "500 Internal Server Error";
		case 501:
		default:
			return "501 Not Implemented";
		}
	}

	private String construct_http_header(int return_code, String mime) {
		return construct_http_header(return_code, mime, null);
	}

	// it is not always possible to get the file size :(
	private String getFileSizeHeader() {
		String path = fileUri.toString();
		int pos = path.indexOf("://");
		if (pos > -1) {
			path = path.substring(pos + 3);
		}

		try {
			File f = new File(path);
			long size = f.length();
			if (size == 0) {
				String newUrl = URLDecoder.decode(path);
				f = new File(newUrl);
				size = f.length();
				if (size == 0) {
					size = getFileSizeBruteForceMethod();
					if (size == 0) {
						return "";
					}
				}
			}
			return "Content-Length: " + Long.toString(size) + "\r\n";
		} catch (Exception e) {
			return "";
		}
	}

	private long getFileSizeBruteForceMethod() {
		long maxSize = 20 * 1024 * 1024; // 20MBs
		ContentResolver cr = Util.theContext.getContentResolver();
		InputStream requestedfile = null;
		try {
			requestedfile = cr.openInputStream(fileUri);
		} catch (FileNotFoundException e) {
			s("FileNotFound getting filesize...");
			// TODO Auto-generated catch block
			return 0;
		}

		long fileSize = 0;
		byte[] buffer = new byte[4096];
		try {
			for (int n; (n = requestedfile.read(buffer)) != -1;) {
				fileSize += n;
				if (fileSize > maxSize) {
					requestedfile.close();
					return 0;
				}
			}
			requestedfile.close();
		} catch (IOException e) {
			s("IOException getting filesize...");
			return 0;
		}
		return fileSize;
	}

	// this method makes the HTTP header for the response
	// the headers job is to tell the browser the result of the request
	// among if it was successful or not.
	private String construct_http_header(int return_code, String mime,
			String location) {

		StringBuilder output = new StringBuilder();
		output.append("HTTP/1.0 ");
		output.append(httpReturnCodeToString(return_code) + "\r\n");
		output.append(getFileSizeHeader());
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss zzz");
		output.append("Date: " + format.format(new Date()) + "\r\n");

		output.append("Connection: close\r\n"); // we can't handle persistent
												// connections
		output.append("Server: " + Util.myLogName + " " + Util.getAppVersion()
				+ "\r\n");
		if (location != null) {
			// we don't want cache for the root URL
			output.append("Location: " + location + "\r\n"); // server name

			output.append("Expires: Tue, 03 Jul 2001 06:00:00 GMT\r\n");
			// output.append("Last-Modified: " . gmdate("D, d M Y H:i:s") .
			// " GMT");
			output.append("Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n");
			output.append("Cache-Control: post-check=0, pre-check=0\r\n");
			output.append("Pragma: no-cache\r\n");
		}
		if (mime != null) {
			output.append("Content-Type: " + mime + "\r\n");
		}
		output.append("\r\n");
		return output.toString();
	}

	String getMimeFromUri(Uri fileUri, String originalMime) {
		if (originalMime != null && !originalMime.equals("")) {
			return originalMime;
		}

		String mPath = fileUri.getPath();
		MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
		String extension = MimeTypeMap.getFileExtensionFromUrl(mPath);
		if (TextUtils.isEmpty(extension)) {
			// getMimeTypeFromExtension() doesn't handle spaces in filenames nor
			// can it handle
			// urlEncoded strings. Let's try one last time at finding the
			// extension.
			int dotPos = mPath.lastIndexOf('.');
			if (0 <= dotPos) {
				extension = mPath.substring(dotPos + 1);
			}
		}
		String mime = mimeTypeMap.getMimeTypeFromExtension(extension);
		return mime;

	}

}