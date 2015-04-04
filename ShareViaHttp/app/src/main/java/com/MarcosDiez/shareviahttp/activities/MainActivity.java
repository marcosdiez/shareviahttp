package com.MarcosDiez.shareviahttp.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

import com.MarcosDiez.shareviahttp.R;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_CODE = 1024;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupLinkMsgView();
        setupNavigationViews();
        createViewClickListener();
        setupPickItemView();
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
            setLinkMessageToView();
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
