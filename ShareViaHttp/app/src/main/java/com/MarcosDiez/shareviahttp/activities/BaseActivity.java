package com.MarcosDiez.shareviahttp.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.MarcosDiez.shareviahttp.UriInterpretation;
import com.github.mrengineer13.snackbar.SnackBar;

import java.util.ArrayList;

import com.MarcosDiez.shareviahttp.MyHttpServer;
import com.MarcosDiez.shareviahttp.R;
import com.MarcosDiez.shareviahttp.Util;

public class BaseActivity extends ActionBarActivity {

    protected static MyHttpServer httpServer = null;
    protected String preferedServerUrl;
    protected CharSequence[] listOfServerUris;
    protected final Activity thisActivity = this;

    // LinkMessageView
    private TextView link_msg;

    // NavigationViews
    protected View bttnQrCode;
    protected View stopServer;
    protected View share;
    protected View changeIp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.context = this;
    }

    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    protected void setupLinkMsgView() {
        link_msg = (TextView) findViewById(R.id.link_msg);
    }

    protected void setupNavigationViews() {
        bttnQrCode = findViewById(R.id.button_qr_code);
        stopServer = findViewById(R.id.stop_server);
        share = findViewById(R.id.button_share_url);
        changeIp = findViewById(R.id.change_ip);
    }

    protected void createViewClickListener() {
        bttnQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateBarCodeIfPossible();
            }
        });

        stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyHttpServer p = httpServer;
                httpServer = null;
                if (p != null) {
                    p.stopServer();
                }
                new SnackBar.Builder(thisActivity)
                        .withMessage(getString(R.string.now_sharing_anymore))
                        .withDuration(SnackBar.SHORT_SNACK)
                        .show();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, preferedServerUrl);
                startActivity(Intent.createChooser(i, BaseActivity.this.getString(R.string.share_url)));
            }
        });

        changeIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createChangeIpDialog();
            }
        });
    }

    public void generateBarCodeIfPossible() {
        // TODO: Create a QR-Code online and download the image
        // TODO: after the image was downloaded show the QR-Code in own activity
        Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
        intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
        intent.putExtra("ENCODE_DATA", link_msg.getText().toString());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new SnackBar.Builder(thisActivity)
                    .withMessageId(R.string.qr_code_not_available)
                    .withDuration(SnackBar.MED_SNACK)
                    .show();
            return;
        }
    }

    private void createChangeIpDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.change_ip);
        b.setSingleChoiceItems(listOfServerUris, 0,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        preferedServerUrl = listOfServerUris[whichButton]
                                .toString();
                        saveServerUrlToClipboard();
                        setLinkMessageToView();
                        dialog.dismiss();
                    }
                });
        b.create().show();
    }

    protected void initHttpServer(ArrayList<UriInterpretation> myUris) {
        Util.context = this.getApplicationContext();
        if (myUris == null || myUris.size() == 0) {
            finish();
            return;
        }

        httpServer = new MyHttpServer(9999);
        listOfServerUris = httpServer.ListOfIpAddresses();
        preferedServerUrl = listOfServerUris[0].toString();

        MyHttpServer.SetFiles(myUris);
    }

    protected void saveServerUrlToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(preferedServerUrl, preferedServerUrl));

        new SnackBar.Builder(thisActivity)
                .withMessage("URL has been copied to the clipboard.")
                .withDuration(SnackBar.MED_SNACK)
                .show();
    }

    protected void setLinkMessageToView() {
        link_msg.setPaintFlags(link_msg.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link_msg.setText(preferedServerUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rate_app:
                String theUrl = "market://details?id="
                        + Util.getPackageName();
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(theUrl));
                startActivity(browse);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
