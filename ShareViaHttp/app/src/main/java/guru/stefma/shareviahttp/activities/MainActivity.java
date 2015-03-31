package guru.stefma.shareviahttp.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import guru.stefma.shareviahttp.MyHttpServer;
import guru.stefma.shareviahttp.R;
import guru.stefma.shareviahttp.Util;

public class MainActivity extends ActionBarActivity {

    public static final int REQUEST_CODE = 1024;

    static MyHttpServer httpServer = null;
    String preferedServerUri;
    CharSequence[] listOfServerUris;
    final Activity thisActivity = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupShareButton();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setTitleTextColor(getResources().getColor(R.color.light_blue));
        setSupportActionBar(toolbar);
    }

    private void setupShareButton() {
        findViewById(R.id.pick_item).setOnClickListener(new View.OnClickListener() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<Uri> uriList = getFileUris(data);
            init(uriList);
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

    void init(ArrayList<Uri> uris) {
        Util.context = this.getApplicationContext();
        ArrayList<Uri> myUris = uris;
        if (myUris == null || myUris.size() == 0) {
            finish();
            return;
        }

        httpServer = new MyHttpServer(9999);
        listOfServerUris = httpServer.ListOfIpAddresses();
        preferedServerUri = listOfServerUris[0].toString();

        loadUrisToServer(myUris);
    }

    void loadUrisToServer(ArrayList<Uri> myUris) {
        MyHttpServer.SetFiles(myUris);
        serverUriChanged();
    }

    void serverUriChanged() {
        sendLinkToClipBoard(preferedServerUri);
    }

    private void sendLinkToClipBoard(String url) {
        ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(url, url));

        Toast.makeText(this, "URL has been copied to the clipboard.",
                Toast.LENGTH_SHORT).show();
    }
}
