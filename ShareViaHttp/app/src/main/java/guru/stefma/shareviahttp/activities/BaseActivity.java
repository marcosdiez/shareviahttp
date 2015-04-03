package guru.stefma.shareviahttp.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import java.util.ArrayList;

import guru.stefma.shareviahttp.MyHttpServer;
import guru.stefma.shareviahttp.Util;

public class BaseActivity extends ActionBarActivity {

    protected static MyHttpServer httpServer = null;
    protected String preferedServerUrl;
    protected CharSequence[] listOfServerUris;
    protected final Activity thisActivity = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        Toast.makeText(this, "URL has been copied to the clipboard.",
                Toast.LENGTH_SHORT).show();
    }
}
