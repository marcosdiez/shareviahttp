package guru.stefma.shareviahttp.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import guru.stefma.shareviahttp.MyHttpServer;
import guru.stefma.shareviahttp.R;
import guru.stefma.shareviahttp.Util;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_CODE = 1024;

    private TextView link_msg;
    private View bttnRate;
    private View stopServer;
    private View share;
    private View changeIp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupViews();
        createViewClickListener();
        setupPickItemView();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    private void setupViews() {
        link_msg = (TextView) findViewById(R.id.link_msg);
        bttnRate = findViewById(R.id.button_rate);
        stopServer = findViewById(R.id.stop_server);
        share = findViewById(R.id.button_share_url);
        changeIp = findViewById(R.id.change_ip);
    }

    private void createViewClickListener() {
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
                Toast.makeText(thisActivity, R.string.now_sharing_anymore,
                        Toast.LENGTH_SHORT).show();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, preferedServerUrl);
                startActivity(Intent.createChooser(i, MainActivity.this.getString(R.string.share_url)));
            }
        });

        changeIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createChangeIpDialog();
            }
        });
    }

    private void setupPickItemView() {
        findViewById(R.id.pick_items).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isKitKatOrHigher = getResources().getBoolean(R.bool.isKitKatOrHigher);
                if (isKitKatOrHigher) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQUEST_CODE);
                }
            }
        });
    }

    void createChangeIpDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.change_ip);
        b.setSingleChoiceItems(listOfServerUris, 0,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        preferedServerUrl = listOfServerUris[whichButton]
                                .toString();
                        saveServerUrlToClipboard();
                        setLinkMessageToView(link_msg);
                        dialog.dismiss();
                    }
                });
        b.create().show();
    }

    private void setViewsVisible() {
        findViewById(R.id.link_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.navigation_layout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<Uri> uriList = getFileUris(data);
            initHttpServer(uriList);
            saveServerUrlToClipboard();
            setLinkMessageToView(link_msg);
            setViewsVisible();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private ArrayList<Uri> getFileUris(Intent data) {
        ArrayList<Uri> theUris = new ArrayList<Uri>();
        Uri dataUri = data.getData();
        if (dataUri != null) {
            theUris.add(dataUri);
        } else {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); ++i) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                theUris.add(uri);
            }
        }
        return theUris;
    }
}
