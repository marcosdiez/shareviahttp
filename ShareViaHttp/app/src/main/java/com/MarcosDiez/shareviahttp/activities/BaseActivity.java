package com.MarcosDiez.shareviahttp.activities;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.MarcosDiez.shareviahttp.BuildConfig;
import com.MarcosDiez.shareviahttp.DisplayRawFileFragment;
import com.MarcosDiez.shareviahttp.MyHttpServer;
import com.MarcosDiez.shareviahttp.R;
import com.MarcosDiez.shareviahttp.UriInterpretation;

import java.util.ArrayList;

public class BaseActivity extends AppCompatActivity {

    public static final int HANDLER_CONNECTION_START = 42;
    public static final int HANDLER_CONNECTION_END = 4242;
    protected static MyHttpServer httpServer = null;
    protected String preferredServerUrl;
    protected CharSequence[] listOfServerUris;
    // LinkMessageView
    protected TextView link_msg;
    protected TextView uriPath;
    // NavigationViews
    protected View bttnQrCode;
    protected View stopServer;
    protected View share;
    protected View changeIp;

    private Handler mHandler;

    public void sendConnectionStartMessage(String ipAddress) {
        Log.d("mm", "begin: " + ipAddress  + " " + this);
        mHandler.handleMessage(mHandler.obtainMessage(BaseActivity.HANDLER_CONNECTION_START, ipAddress));
    }

    public void sendConnectionEndMessage(String ipAddress) {
        Log.d("mm", "end: " + ipAddress + " " + this);
        mHandler.handleMessage(mHandler.obtainMessage(BaseActivity.HANDLER_CONNECTION_END, ipAddress));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case HANDLER_CONNECTION_START:
                        String msg = String.format(getString(R.string.connected_ip), (String) inputMessage.obj);
                        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
                        break;
                    case HANDLER_CONNECTION_END:
                        String msg2 = String.format(getString(R.string.disconnected_ip), (String) inputMessage.obj);
                        Snackbar.make(findViewById(android.R.id.content), msg2, Snackbar.LENGTH_LONG).show();
                        break;
                    default:
                        super.handleMessage(inputMessage);
                }
            }
        };
    }

    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    protected void setupTextViews() {
        link_msg = (TextView) findViewById(R.id.link_msg);
        uriPath = (TextView) findViewById(R.id.uriPath);
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
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.now_sharing_anymore), Snackbar.LENGTH_SHORT).show();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, preferredServerUrl);
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
        Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
        intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
        intent.putExtra("ENCODE_DATA", link_msg.getText().toString());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "You need to download the Barcode Scanner to generate QR Codes", Toast.LENGTH_LONG).show();
            openInPlayStore("com.google.zxing.client.android");
        }
    }

    private void createChangeIpDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.change_ip);
        b.setSingleChoiceItems(listOfServerUris, 0,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        preferredServerUrl = listOfServerUris[whichButton]
                                .toString();
                        saveServerUrlToClipboard();
                        setLinkMessageToView();
                        dialog.dismiss();
                    }
                });
        b.create().show();
    }

    protected void populateUriPath(ArrayList<UriInterpretation> uriList) {
        StringBuilder output = new StringBuilder();
        String sep = "\n";
        boolean first = true;
        for (UriInterpretation thisUriInterpretation : uriList) {
            if (first) {
                first = false;
            } else {
                output.append(sep);
            }
            output.append(thisUriInterpretation.getPath());
        }
        uriPath.setText(output.toString());
    }

    protected void initHttpServer(ArrayList<UriInterpretation> myUris) {
        if (myUris == null || myUris.size() == 0) {
            finish();
            return;
        }

        httpServer = new MyHttpServer(9999);
        listOfServerUris = httpServer.listOfIpAddresses();
        preferredServerUrl = listOfServerUris[0].toString();

        httpServer.setBaseActivity(this);
        httpServer.setFiles(myUris);

    }

    protected void saveServerUrlToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(preferredServerUrl, preferredServerUrl));

        Snackbar.make(findViewById(android.R.id.content), getString(R.string.url_clipboard), Snackbar.LENGTH_LONG).show();
    }

    protected void setLinkMessageToView() {
        link_msg.setPaintFlags(link_msg.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link_msg.setText(preferredServerUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showPrivacyPolicy() {
        DialogFragment newFragment = DisplayRawFileFragment.newInstance(getString(R.string.privacy_policy), R.raw.privacy_policy);
        newFragment.show(getFragmentManager(), "dialog");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rate_app:
                rate_this_app();
                return super.onOptionsItemSelected(item);
            case R.id.action_privacy_policy:
                showPrivacyPolicy();
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void rate_this_app() {
        String appName = BuildConfig.APPLICATION_ID;
        openInPlayStore(appName);
    }

    private void openInPlayStore(String appName) {
        String theUrl = "market://details?id=" + appName;
        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(theUrl));
        startActivity(browse);
    }
}
