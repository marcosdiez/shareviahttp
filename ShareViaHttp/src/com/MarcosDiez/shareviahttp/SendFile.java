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

package com.MarcosDiez.shareviahttp;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.main)
public class SendFile extends Activity {
	/** Called when the activity is first created. */

	static MyHttpServer theHttpServer = null;
	String preferedServerUri;
	CharSequence[] listOfServerUris;
	final Activity thisActivity = this;

	@ViewById
	TextView uriPath;

	@ViewById
	TextView link_msg;

	@ViewById
	TextView txtBarCodeScannerInfo;

	@AfterViews
	void init() {
		Util.theContext = this.getApplicationContext();
		ArrayList<Uri> myUris = getFileUris();
		if (myUris == null || myUris.size() == 0) {
			finish();
			return;
		}

		theHttpServer = new MyHttpServer(9999);
		listOfServerUris = theHttpServer.ListOfIpAddresses();
		preferedServerUri = listOfServerUris[0].toString();

		loadUrisToServer(myUris);
	}

	void loadUrisToServer(ArrayList<Uri> myUris) {
		MyHttpServer.SetFiles(myUris);
		generateBarCodeIfPossible(preferedServerUri);
		uriPath.setText("File(s): " + Uri.decode(myUris.toString()));
		formatHyperlinks();
	}

	private ArrayList<Uri> getFileUris() {
		Intent dataIntent = getIntent();
		ArrayList<Uri> theUris = new ArrayList<Uri>();

		if (Intent.ACTION_SEND_MULTIPLE.equals(dataIntent.getAction())) {
			ArrayList<Parcelable> list = dataIntent
					.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (list != null) {
				for (Parcelable parcelable : list) {
					Uri stream = (Uri) parcelable;
					if (stream != null) {
						theUris.add(stream);
					}
				}
			}
			return theUris;
		}

		Bundle extras = dataIntent.getExtras();

		Uri myUri = (Uri) extras.get(Intent.EXTRA_STREAM);

		if (myUri == null) {
			String tempString = (String) extras.get(Intent.EXTRA_TEXT);
			if (tempString == null) {
				Toast.makeText(this, "Error obtaining the file path...",
						Toast.LENGTH_LONG).show();
				return null;
			}

			myUri = Uri.parse(tempString);

			if (myUri == null) {
				Toast.makeText(this, "Error obtaining the file path",
						Toast.LENGTH_LONG).show();
				return null;
			}
		}

		theUris.add(myUri);
		return theUris;
	}

	@Click
	void button_share_containing_folder() {
		ArrayList<Uri> myUris = MyHttpServer.GetFiles();
		if (myUris == null || myUris.isEmpty()) {
			Toast.makeText(thisActivity, "Error getting file list.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		Uri theUri = myUris.get(0);
		String path = theUri.getPath();
		int pos = path.lastIndexOf(File.separator);
		if (pos <= 0) {
			Toast.makeText(thisActivity, "Error getting parent directory.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		String newPath = path.substring(0, pos);
		Log.d(Util.myLogName, newPath);
		File newFile = new File(newPath);
		if (!newFile.exists()) {
			Toast.makeText(thisActivity,
					"Error. New file [" + newPath + "] does not exist.", Toast.LENGTH_LONG)
					.show();

			return;
		}

		Uri theNewUri = Uri.parse(newPath);
		ArrayList<Uri> newUriArray = new ArrayList<Uri>();
		newUriArray.add(theNewUri);

		Toast.makeText(thisActivity, "We are now sharing [" + newPath + "]",
				Toast.LENGTH_LONG).show();
		loadUrisToServer(newUriArray);
	}

	void serverUriChanged() {
		formatHyperlinks();
		generateBarCodeIfPossible(preferedServerUri);
	}

	void formatHyperlinks() {
		link_msg.setText(preferedServerUri);
	}

	@Click
	void button_rate() {
		String theUrl = "https://market.android.com/details?id="
				+ Util.getPackageName();
		Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(theUrl));
		startActivity(browse);
	}

	@Click
	void stop_server() {
		MyHttpServer p = theHttpServer;
		theHttpServer = null;
		if (p != null) {
			p.stopServer();
		}
		Toast.makeText(thisActivity, R.string.now_sharing_anymore,
				Toast.LENGTH_SHORT).show();
	}

	@Click
	void change_ip() {
		showDialog(42);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.change_ip);
		b.setSingleChoiceItems(listOfServerUris, 0,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						preferedServerUri = listOfServerUris[whichButton]
								.toString();
						serverUriChanged();
						dialog.dismiss();
					}
				});
		AlertDialog theAlertDialog = b.create();

		return theAlertDialog;
	}

	public void generateBarCodeIfPossible(String message) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		intent.putExtra("ENCODE_DATA", message);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) { // the person has no barcode
			formatBarcodeLink();
			// scanner
			return;
		}
		Toast.makeText(
				this,
				"Please open the following address on the target computer: "
						+ message, Toast.LENGTH_SHORT).show();
	}

	void formatBarcodeLink() {
		txtBarCodeScannerInfo.setVisibility(View.VISIBLE);
		txtBarCodeScannerInfo.setMovementMethod(LinkMovementMethod
				.getInstance());
	}
}