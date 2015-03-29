package guru.stefma.shareviahttp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import guru.stefma.shareviahttp.R;

public class DebugActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        botao();
        final Button button = (Button) findViewById(R.id.button_rate);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                botao();
            }
        });
    }

    void botao() {
        String path = "/mnt/sdcard/.mixzing/bbb.txt";

        Intent intent = new Intent(this, SendFileActivity.class);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.putExtra(Intent.EXTRA_TEXT, path);
        intent.setType("inode/directory");

        startActivity(intent);
    }
}
