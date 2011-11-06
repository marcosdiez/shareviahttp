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

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

// file: server.java
// the real (http) serverclass
// it extends thread so the server is run in a different
// thread than the gui, that is to make it responsive.
// it's really just a macho coding thing.
public class HttpServerConnection extends Thread {

	// the constructor method
	// the parameters it takes is what port to bind to, the default tcp port
	// for a httpserver is port 80. the other parameter is a reference to
	// the gui, this is to pass messages to our nice interface
	public HttpServerConnection(Uri fileUri, Socket connectionsocket) {
		this.fileUri = fileUri;
		this.connectionsocket = connectionsocket;
		this.start();
	}

	private Socket connectionsocket;
	private Uri fileUri;
	private String ipAddress = "";

	// this is a overridden method from the Thread class we extended from
	public void run() {
		// we are now inside our own thread separated from the gui.
		InetAddress client = connectionsocket.getInetAddress();
		ipAddress = client.getHostAddress() + "/" + client.getHostName();

		InputStream theInputStream;
		try {
			theInputStream = connectionsocket.getInputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			s("Error getting inputString from connection socket.");
			e1.printStackTrace();
			return;
		}

		OutputStream theOuputStream;
		try {
			theOuputStream = connectionsocket.getOutputStream();
		} catch (IOException e1) {
			s("Error getting theOuputStream from connection socket.");
			e1.printStackTrace();
			return;
		}

		BufferedReader input = new BufferedReader(new InputStreamReader(
				theInputStream));

		DataOutputStream output = new DataOutputStream(theOuputStream);
		http_handler(input, output);

		s("Closing connection.");
	}

	// our implementation of the hypertext transfer protocol
	// its very basic and stripped down
	private void http_handler(BufferedReader input, DataOutputStream output) {
		int method = 0; // 1 get, 0 not supported
		String path = new String(); // the various things, what http v, what
									// path,
		try {
			// This is the two types of request we can handle
			// GET /index.html HTTP/1.0
			// HEAD /index.html HTTP/1.0
			String tmp = input.readLine(); // read from the stream
			String tmp2 = new String(tmp);
			tmp.toUpperCase(); // convert it to uppercase
			if (tmp.startsWith("GET")) { // compare it is it GET
				method = 1;
			} // if we set it to method 1
				// if (tmp.startsWith("HEAD")) { // same here is it HEAD
				// method = 2;
				// } // set method to 2

			if (method == 0) { // not supported
				try {
					output.writeBytes(construct_http_header(501, null));
					output.close();
					return;
				} catch (Exception e3) { // if some error happened catch it
					s("_error:" + e3.getMessage());
				} // and display error
			}
			// }

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
		} catch (Exception e) {
			s("errorr" + e.getMessage());
		} // catch any exception

		// path do now have the filename to what to the file it wants to open

		if (path == null) {
			s("path is null!!!");
		}
		if (fileUri == null) {
			s("fileUri is null");
		}

		s("Client requested: [" + path + "][" + fileUri.toString() + "]");

		if (path.equals("/favicon.ico")) {
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
			String thePath = fixPath(fileUri.getEncodedPath(), mime);

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
			// NOTE that there are several security consideration when passing
			// the untrusted string "path" to FileInputStream.
			// You can access all files the current user has read access to!!!
			// current user is the user running the javaprogram.
			// you can do this by passing "../" in the url or specify absoulute
			// path
			// or change drive (win)

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

		String outputString = construct_http_header(200, mime);

		try {
			output.writeBytes(outputString);

			// if it was a HEAD request, we don't print any BODY
			if (method == 1) { // 1 is GET 2 is head and skips the body
				byte[] b = new byte[4096];

				for (int n; (n = requestedfile.read(b)) != -1;) {
					output.write(b, 0, n);

					// out.append(new String(b, 0, n));
				}
				// return out.toString();

				/*
				 * // read the file from filestream, and print out through the
				 * // client-outputstream on a byte per byte base. int b =
				 * requestedfile.read(); if (b == -1) { break; // end of file }
				 * output.write(b);
				 */

				// clean up the files, close open handles

			}

			output.close();
			requestedfile.close();
		} catch (IOException e) {
		}
	}

	// this function adds an extention to files without extensions, according to
	// it's mime type.
	// this is usefull because of the gallery
	private String fixPath(String encodedPath, String mime) {
		if (mime == null || encodedPath.indexOf('.') >= 0)
			return encodedPath;

		// there is not "." on the name ( android gallery stuff...) , let's add
		// one according to the MIME type

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

		return encodedPath;
	}

	private void s(String s2) { // an alias to avoid typing so much!
		Log.d(Util.myLogName, "[" + ipAddress + "] " + s2);
	}

	private static String httpReturnCodeToString(int return_code) {
		switch (return_code) {
		case 200:
			return ("200 OK");
		case 302:
			return "302 Moved Temporarily";
		case 400:
			return ("400 Bad Request");
		case 403:
			return ("403 Forbidden");
		case 404:
			return ("404 Not Found");
		case 500:
			return ("500 Internal Server Error");
		case 501:
		default:
			return ("501 Not Implemented");
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
					return "";
				}
			}
			return "Content-Length: " + Long.toString(size) + "\r\n";
		} catch (Exception e) {
			return "";
		}

	}

	// this method makes the HTTP header for the response
	// the headers job is to tell the browser the result of the request
	// among if it was successful or not.
	private String construct_http_header(int return_code, String mime,
			String location) {

		StringBuilder output = new StringBuilder();
		output.append("HTTP/1.0 ");
		output.append(httpReturnCodeToString(return_code));
		output.append("\r\n"); // other header fields,
		output.append(getFileSizeHeader());
		output.append("Connection: close\r\n"); // we can't handle persistent
		// connections
		output.append("Server: " + Util.myLogName + " " + Util.getAppVersion()
				+ "\r\n"); // server
		// name
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

		// Construct the right Content-Type for the header.
		// This is so the browser knows what to do with the
		// file, you may know the browser dosen't look on the file
		// extension, it is the servers job to let the browser know
		// what kind of file is being transmitted. You may have experienced
		// if the server is miss configured it may result in
		// pictures displayed as text!
		if (mime != null) {
			output.append("Content-Type: " + mime + "\r\n");
		}
		// //so on and so on......
		output.append("\r\n"); // this marks the end of the httpheader
		// and the start of the body
		// ok return our newly created header!
		return output.toString();
	}
}