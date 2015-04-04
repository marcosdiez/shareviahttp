package guru.stefma.shareviahttp.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import com.github.mrengineer13.snackbar.SnackBar;

import java.util.ArrayList;

import guru.stefma.shareviahttp.MyHttpServer;
import guru.stefma.shareviahttp.R;
import guru.stefma.shareviahttp.Util;

public class BaseActivity extends ActionBarActivity {

    protected static MyHttpServer httpServer = null;
    protected String preferedServerUrl;
    protected CharSequence[] listOfServerUris;
    protected final Activity thisActivity = this;

    // LinkMessageView
    private TextView link_msg;

    // NavigationViews
    protected View bttnRate;
    protected View stopServer;
    protected View share;
    protected View changeIp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupLinkMsgView() {
        link_msg = (TextView) findViewById(R.id.link_msg);
    }

    protected void setupNavigationViews() {
        bttnRate = findViewById(R.id.button_rate);
        stopServer = findViewById(R.id.stop_server);
        share = findViewById(R.id.button_share_url);
        changeIp = findViewById(R.id.change_ip);
    }

    protected void createViewClickListener() {
        bttnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String theUrl = "https://market.android.com/details?id="
                        + Util.getPackageName();
                Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(theUrl));
                startActivity(browse);
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

    protected void initHttpServer(ArrayList<Uri> myUris) {
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
}
