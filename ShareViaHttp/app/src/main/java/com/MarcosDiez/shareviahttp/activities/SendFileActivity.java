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

package com.MarcosDiez.shareviahttp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;

import com.MarcosDiez.shareviahttp.UriInterpretation;
import com.github.mrengineer13.snackbar.SnackBar;

import java.util.ArrayList;

import com.MarcosDiez.shareviahttp.R;

public class SendFileActivity extends BaseActivity {

    private TextView uriPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);

        setupToolbar();
        setupLinkMsgView();
        setupNavigationViews();
        createViewClickListener();
        setupOwnViews();

        ArrayList<UriInterpretation> uriList = getFileUris();
        populateUriPath(uriList);
        initHttpServer(uriList);
        saveServerUrlToClipboard();
        setLinkMessageToView();
    }

    private void populateUriPath(ArrayList<UriInterpretation> uriList) {
        StringBuilder output = new StringBuilder();
        String sep = "\n";
        output.append(uriList.size() > 1 ? "Files:" : "File:");
        output.append(sep);
        for( UriInterpretation thisUriInterpretation : uriList){
            output.append(thisUriInterpretation.getPath() + sep);
        }
        uriPath.setText(output.toString());
    }

    private void setupOwnViews() {
        uriPath = (TextView) findViewById(R.id.uriPath);
    }

    private ArrayList<UriInterpretation> getFileUris() {
        Intent dataIntent = getIntent();
        ArrayList<UriInterpretation> theUris = new ArrayList<UriInterpretation>();

        if (Intent.ACTION_SEND_MULTIPLE.equals(dataIntent.getAction())) {
            ArrayList<Parcelable> list = dataIntent
                    .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list != null) {
                for (Parcelable parcelable : list) {
                    Uri stream = (Uri) parcelable;
                    if (stream != null) {
                        theUris.add(new UriInterpretation(stream));
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
                new SnackBar.Builder(thisActivity)
                        .withMessage("Error obtaining the file path")
                        .withDuration(SnackBar.LONG_SNACK)
                        .show();
                return null;
            }

            myUri = Uri.parse(tempString);

            if (myUri == null) {
                new SnackBar.Builder(thisActivity)
                        .withMessage("Error obtaining the file path")
                        .withDuration(SnackBar.LONG_SNACK)
                        .show();
                return null;
            }
        }

        theUris.add(new UriInterpretation(myUri));
        return theUris;
    }
}