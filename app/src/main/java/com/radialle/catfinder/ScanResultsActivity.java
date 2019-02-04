package com.radialle.catfinder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ScanResultsActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_results);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        final String[] matchPaths = bundle.getStringArray("matchPaths");
        String[] matchNames = bundle.getStringArray("matchNames");
        String[] matchDescs = bundle.getStringArray("matchDescs");

        HashMap<String,String> hashMap;
        ArrayList<HashMap<String,String>> data = new ArrayList<>();
        for (int i = 0; i < matchPaths.length; i++) {
            hashMap = new HashMap<>();
            hashMap.put("listViewItemImage", matchPaths[i]);
            hashMap.put("listViewItemTitle", matchNames[i]);
            hashMap.put("listViewItemDesc", matchDescs[i]);
            data.add(hashMap);
        }

        String[] from = { "listViewItemImage", "listViewItemTitle", "listViewItemDesc" };
        int[] to = { R.id.listViewItemImage, R.id.listViewItemTitle, R.id.listViewItemDesc };

        this.listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                File file = new File(matchPaths[pos]);
                Uri uri = FileProvider.getUriForFile(
                        getParent(),
                        getApplicationContext().getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                ClipData clip = ClipData.newPlainText("", matchPaths[pos]);
                ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                manager.setPrimaryClip(clip);

                Toast.makeText(
                        getApplicationContext(),
                        R.string.file_path_clipboard,
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }
        });
        listView.setAdapter(new SimpleAdapter(
                getBaseContext(), data, R.layout.scan_result_item, from, to
        ));
    }

    @Override
    public boolean onSupportNavigateUp() {
        this.onBackPressed();
        return true;
    }

}
