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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SendFile extends Activity {
	/** Called when the activity is first created. */

	static MyHttpServer theHttpServer = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Util.theContext = this.getApplicationContext();
		Intent dataIntent = getIntent();
		if (dataIntent == null) {
			Log.d(Util.myLogName, "no data Intent");
			return;
		}

		setContentView(R.layout.main);

		Bundle extras = dataIntent.getExtras();

		Uri myUri = (Uri) extras.get(Intent.EXTRA_STREAM);

	
		if (myUri == null) {
			myUri = Uri.parse((String) extras.get(Intent.EXTRA_TEXT));

			if (myUri == null) {
				Toast.makeText(this, "Error obtaining the file path",
						Toast.LENGTH_LONG).show();
				return;
			}
		}

		// Log.d("aa", "myUri.toString():" + myUri.toString());
		// Log.d("aa", "myUri.getEncodedPath():" + myUri.getEncodedPath());
		// Log.d("myUri.getPath():" + dataIntent.getPath() );

		
		TextView t2 = (TextView) findViewById(R.id.uriPath);
		t2.setText("File: " + Uri.decode(myUri.toString()));

		// Toast.makeText(this, theFile, Toast.LENGTH_LONG).show();

		theHttpServer = new MyHttpServer(9999);
		MyHttpServer.SetFile(myUri);

		generateBarCodeIfPossible(theHttpServer.getServerUrl());

		formatHyperlinks();
		formatBarcodeLink();
		makeButtonsWork();
	}

	void formatHyperlinks(){
		((TextView) findViewById(R.id.link_msg)).setText(theHttpServer
				.getServerUrl());	
	}
	
	void makeButtonsWork() {
		Button b = (Button) findViewById(R.id.button_rate);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String theUrl = "https://market.android.com/details?id="
						+ Util.getPackageName();
				Intent browse = new Intent(Intent.ACTION_VIEW, Uri
						.parse(theUrl));
				startActivity(browse);
			}
		});

		final Activity a = this;

		Button c = (Button) findViewById(R.id.stop_server);
		c.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyHttpServer p = theHttpServer;
				theHttpServer = null;
				if (p != null) {
					p.stopServer();
				}
				Toast.makeText(a, R.string.now_sharing_anymore, Toast.LENGTH_SHORT).show();
			}
		});

	}

	void formatBarcodeLink() {
		TextView t2 = (TextView) findViewById(R.id.infoTxtCredits);
		t2.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public void generateBarCodeIfPossible(String message) {		
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		intent.putExtra("ENCODE_DATA", message);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) { // the person has no barcode
			// scanner
			return;
		}
		Toast.makeText(
				this,
				"Please open the following address on the target computer: "
						+ message, Toast.LENGTH_SHORT).show();
	}

}