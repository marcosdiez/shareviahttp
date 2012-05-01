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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.net.Uri;
import android.util.Log;

public class HttpServerConnection implements Runnable {

	public HttpServerConnection(ArrayList<Uri> fileUris, Socket connectionsocket) {
		this.fileUriZ = fileUris;
		this.connectionsocket = connectionsocket;

	}

	private UriInterpretation theUriInterpretation;
	private Socket connectionsocket;
	private ArrayList<Uri> fileUriZ;
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
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		if (fileUriZ == null) {
			s("fileUri is null");
			return;
		}

		String fileUriStr = fileUriZ.size() == 1 ? fileUriZ.get(0).toString()
				: fileUriZ.toString();

		s("Client requested: [" + path + "][" + fileUriStr + "]");

		if (path.equals("/favicon.ico")) { // we have no favicon
			shareFavIcon(output);
			return;
		}

		if (fileUriStr.startsWith("http://")
				|| fileUriStr.startsWith("https://")
				|| fileUriStr.startsWith("ftp://")
				|| fileUriStr.startsWith("maito:")
				|| fileUriStr.startsWith("callto:")
				|| fileUriStr.startsWith("skype:")) {
			// we will work as a simple URL redirector
			redirectToFinalPath(output, fileUriStr);
			return;
		}

		try {
			theUriInterpretation = new UriInterpretation(fileUriZ.get(0));
		} catch (java.lang.SecurityException e) {
			e.printStackTrace();
			s("Share Via HTTP has no permition to read such file.");
			return;
		}
		if (path.equals("/")) {
			shareRootUrl(output);
			return;
		}

		shareOneFile(output, sendOnlyHeader);
	}

	private void shareOneFile(DataOutputStream output, Boolean sendOnlyHeader) {

		InputStream requestedfile = null;

		if (!theUriInterpretation.isDirectory) {
			try {
				requestedfile = theUriInterpretation.getInputStream();
			} catch (FileNotFoundException e) {
				try {
					// if you could not open the file send a 404
					s("Sending HTTP ERROR 404:" + e.getMessage());
					output.writeBytes(construct_http_header(404, null));
					return;
				} catch (IOException e2) {
					s("errorX:" + e2.getMessage());
					return;
				}
			} // print error to gui
		}
		// happy day scenario

		String outputString = construct_http_header(200,
				theUriInterpretation.mime);

		try {
			output.writeBytes(outputString);

			// if it was a HEAD request, we don't print any BODY
			if (!sendOnlyHeader) {

				if (theUriInterpretation.isDirectory || fileUriZ.size() > 1) {
					FileZipper zz = new FileZipper(output, fileUriZ);
					zz.run();
				} else {
					byte[] buffer = new byte[4096];
					for (int n; (n = requestedfile.read(buffer)) != -1;) {
						output.write(buffer, 0, n);
					}
				}

			}
			requestedfile.close();
		} catch (IOException e) {
		}
	}

	private void redirectToFinalPath(DataOutputStream output, String thePath) {

		String redirectOutput = construct_http_header(302, null, thePath);
		try {
			// if you could not open the file send a 404
			output.writeBytes(redirectOutput);
			// close the stream
		} catch (IOException e2) {
		}
	}

	private void shareRootUrl(DataOutputStream output) {
		if (theUriInterpretation.isDirectory) {
			redirectToFinalPath(output, theUriInterpretation.name + ".ZIP");
			return;
		}

		if (fileUriZ.size() == 1) {
			redirectToFinalPath(output, theUriInterpretation.name);
		} else {
			SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd_HH_mm_ss");
			redirectToFinalPath(output,
					"ShareViaHttpBundle-" + format.format(new Date()) + ".ZIP");
		}
	}

	private void shareFavIcon(DataOutputStream output) {
		try {
			// if you could not open the file send a 404
			output.writeBytes(construct_http_header(404, null));
			// close the stream			
		} catch (IOException e2) {
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
			return;
		} catch (Exception e3) { // if some error happened catch it
			s("_error:" + e3.getMessage());
		} // and display error
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
		if (theUriInterpretation == null) {
			return "";
		}
		if (fileUriZ.size() == 1 && theUriInterpretation.size > 0) {
			return "Content-Length: "
					+ Long.toString(theUriInterpretation.size) + "\r\n";
		}
		return "";
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
			if (fileUriZ.size() > 1) {
				mime = "multipart/x-zip";
			}
			output.append("Content-Type: " + mime + "\r\n");
		}
		output.append("\r\n");
		return output.toString();
	}

}