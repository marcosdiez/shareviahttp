package com.MarcosDiez.shareviahttp.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.MarcosDiez.shareviahttp.BuildConfig;
import com.MarcosDiez.shareviahttp.R;
import com.MarcosDiez.shareviahttp.UriInterpretation;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_CODE = 1024;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupTextViews();
        setupNavigationViews();
        createViewClickListener();
        setupPickItemView();
        // debugSendFileActivity();
    }

    private void debugSendFileActivity() {
        if (!BuildConfig.BUILD_TYPE.equals("release")) {    // this should not happen
            String path = "/mnt/sdcard/m.txt";

            Intent intent = new Intent(this, SendFileActivity.class);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra(Intent.EXTRA_TEXT, path);
            // intent.setType("inode/directory");

            startActivity(intent);
        }
    }

    private void setupPickItemView() {
        findViewById(R.id.pick_items).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

    private void setViewsVisible() {
        findViewById(R.id.link_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.navigation_layout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<UriInterpretation> uriList = getFileUris(data);
            populateUriPath(uriList);
            initHttpServer(uriList);
            saveServerUrlToClipboard();
            setLinkMessageToView();
            setViewsVisible();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Read values from the "savedInstanceState"-object and put them in your textview
        super.onRestoreInstanceState(savedInstanceState);
        uriPath.setText(savedInstanceState.getCharSequence("uriPath"));
        link_msg.setText(savedInstanceState.getCharSequence("link_msg"));

        if (!"".equals(savedInstanceState.getCharSequence("uriPath"))) {
            setViewsVisible();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("link_msg", link_msg.getText());
        outState.putCharSequence("uriPath", uriPath.getText());
        // Save the values you need from your textview into "outState"-object
        super.onSaveInstanceState(outState);
    }


    private ArrayList<UriInterpretation> getFileUris(Intent data) {
        ArrayList<UriInterpretation> theUris = new ArrayList<UriInterpretation>();
        Uri dataUri = data.getData();
        if (dataUri != null) {
            theUris.add(new UriInterpretation(dataUri, this.getContentResolver()));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                getFileUrisFromClipboard(data, theUris);
            }
        }
        return theUris;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getFileUrisFromClipboard(Intent data, ArrayList<UriInterpretation> theUris) {
        ClipData clipData = data.getClipData();
        for (int i = 0; i < clipData.getItemCount(); ++i) {
            ClipData.Item item = clipData.getItemAt(i);
            Uri uri = item.getUri();
            theUris.add(new UriInterpretation(uri, this.getContentResolver()));
        }
    }
}
