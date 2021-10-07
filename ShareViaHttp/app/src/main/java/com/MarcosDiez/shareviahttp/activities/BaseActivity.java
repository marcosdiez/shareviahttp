package com.MarcosDiez.shareviahttp.activities;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.MarcosDiez.shareviahttp.BuildConfig;
import com.MarcosDiez.shareviahttp.DisplayRawFileFragment;
import com.MarcosDiez.shareviahttp.MyHttpServer;
import com.MarcosDiez.shareviahttp.R;
import com.MarcosDiez.shareviahttp.UriInterpretation;

import net.glxn.qrgen.android.QRCode;

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
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
    }

    protected void setupTextViews() {
        link_msg = (TextView) findViewById(R.id.link_msg);
        uriPath = (TextView) findViewById(R.id.uriPath);
    }

    protected void setupNavigationViews() {
        stopServer = findViewById(R.id.stop_server);
        share = findViewById(R.id.button_share_url);
        changeIp = findViewById(R.id.change_ip);
    }

    protected void createViewClickListener() {
        stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyHttpServer p = httpServer;
                httpServer = null;
                if (p != null) {
                    p.stopServer();
                }
                cancelNotification();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        Bitmap myBitmap = QRCode.from(link_msg.getText().toString()).bitmap();
        ImageView qrImage= (ImageView) findViewById(R.id.QRcode);
        if (qrImage!=null)  qrImage.setImageBitmap(myBitmap);
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

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pIntent = PendingIntent.getActivity(this,0, notificationIntent, 0);
        showNotification(pIntent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        generateBarCodeIfPossible();
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

    @Override
    public void onBackPressed(){
        MyHttpServer p = httpServer;
        httpServer = null;
        if (p != null) {
            p.stopServer();
        }
        cancelNotification();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onBackPressed();
    }

    public void showNotification(PendingIntent pIntent){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.getString(R.string.server_running)).setContentIntent(pIntent);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1,builder.build());
    }

    public void cancelNotification(){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }
}
