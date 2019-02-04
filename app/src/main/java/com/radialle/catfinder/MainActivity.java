package com.radialle.catfinder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 110;
    private boolean SCAN_START_PENDING = false;

    private Scanner scanner;
    private TextView statusText;
    private ProgressBar progressBar;
    private Button scanButton;

    public void openApplicationSettings() {
        startActivity(
                new Intent().
                        setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).
                        setData(Uri.fromParts("package", getPackageName(), null))
        );
    }

    public void showRequestPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_rationale_title)
                .setMessage(
                        String.format(
                                getResources().getString(R.string.permission_rationale_message),
                                getResources().getString(R.string.app_name)
                                )
                )
                .setNegativeButton(R.string.permission_rationale_cancel, null)
                .setPositiveButton(R.string.permission_rationale_open_settings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openApplicationSettings();
                            }
                        })
                .setCancelable(true)
                .show();
    }

    public boolean hasRequiredPermissions() {
        return (ContextCompat.checkSelfPermission(
           this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED);
    }

    public void requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        )) { showRequestPermissionRationale(); }
        else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    this.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            );
        }
    }

    public void scanButtonOnClick(View v) {
        if (!this.scanner.isRunning()) {
            startScan();
            return;
        }
        stopScan();
    }

    public void resetActivityComponents() {
        this.progressBar.setVisibility(View.INVISIBLE);
        this.statusText.setText(R.string.status_stopped);
        this.scanButton.setText(R.string.button_scan_start);
        this.scanButton.setEnabled(true);
    }

    public void stopScan() {
        this.scanButton.setEnabled(false);
        this.statusText.setText(R.string.status_stopping);
        this.scanner.stop();
    }

    public void startScan() {
        if (!this.hasRequiredPermissions()) {
            this.SCAN_START_PENDING = true;
            requestPermissions();
            return;
        }
        this.SCAN_START_PENDING = false;
        this.progressBar.setVisibility(View.VISIBLE);
        this.scanButton.setText(R.string.button_scan_stop);

        this.scanner.run();
    }

    @Override
    public void onRequestPermissionsResult(int code, String permissions[], int[] results) {
        if (code == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE &&
                results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED &&
                this.SCAN_START_PENDING) {
            startScan();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.progressBar = findViewById(R.id.progressBar);
        this.statusText = findViewById(R.id.statusText);
        this.scanButton = findViewById(R.id.scanButton);

        this.scanner = new Scanner(
                this,
                new OnScannerStatusUpdateListener() {
                    @Override
                    public void onScannerStatusUpdate(String text) {
                        statusText.setText(text);
                    }
                },
                new OnScannerFinishedListener() {
                    @Override
                    public void onScannerFinished(ScannerMatch[] matches) {
                        Intent intent = new Intent(
                                MainActivity.this, ScanResultsActivity.class
                        );

                        Arrays.sort(matches, new Comparator<ScannerMatch>() {
                            @Override
                            public int compare(ScannerMatch a, ScannerMatch b) {
                                if (a.getConfidence() == b.getConfidence()) {
                                    return 0;
                                }
                                if (a.getConfidence() < b.getConfidence()) {
                                    return 1;
                                }
                                return -1;
                            }
                        });

                        File file;
                        List<String> matchPaths = new ArrayList<>();
                        List<String> matchNames = new ArrayList<>();
                        List<String> matchDescs = new ArrayList<>();
                        for (ScannerMatch match : matches) {
                            file = match.getFile();
                            matchPaths.add(file.getPath());
                            matchNames.add(file.getName());
                            matchDescs.add(
                                    getString(
                                            R.string.scan_results_confidence,
                                            match.getConfidence()
                                    )
                            );
                        }
                        intent.putExtra("matchPaths", matchPaths.toArray(
                                new String[matchPaths.size()]
                        ));
                        intent.putExtra("matchNames", matchNames.toArray(
                                new String[matchNames.size()]
                        ));
                        intent.putExtra("matchDescs", matchDescs.toArray(
                                new String[matchDescs.size()]
                        ));
                        startActivity(intent);

                        resetActivityComponents();
                    }
                },
                new OnScannerCanceledListener() {
                    @Override
                    public void onScannerCanceled() {
                        resetActivityComponents();
                    }
                }
        );

    }
}
