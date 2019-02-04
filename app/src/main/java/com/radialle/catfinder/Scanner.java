package com.radialle.catfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Scanner {

    private Context context;
    private boolean isRunning;

    private static String entityId = "/m/01yrx";

    private List<ScannerMatch> matches;

    private OnScannerStatusUpdateListener statusUpdateListener;
    private OnScannerFinishedListener scannerFinishedListener;
    private OnScannerCanceledListener scannerCanceledListener;

    private int bitmapArrayPtr;
    private Bitmap[] bitmapArray;
    private File[] bitmapFileArray;

    private File[] getFileArray(File[] list) {
        List<File> retList = new ArrayList<>();
        for (File file: list) {
            if (file.isDirectory()) {
                File[] rec = getFileArray(file.listFiles());
                for (File item: rec) { retList.add(item); }
                continue;
            }
            retList.add(file);
        }
        return retList.toArray(new File[retList.size()]);
    }

    private void scanCallback(
            File file, boolean hasEntity, float confidence, List<FirebaseVisionLabel> labels
    ) {

        if (!this.isRunning()) {
            this.scannerCanceledListener.onScannerCanceled();
            return;
        }

        this.bitmapArrayPtr++;

        if (hasEntity) {
            matches.add(new ScannerMatch(file, this.entityId, confidence, labels));
        }

        if (bitmapArrayPtr >= this.bitmapArray.length) {
            this.scannerFinishedListener.onScannerFinished(
                    this.matches.toArray(new ScannerMatch[this.matches.size()])
            );
            this.isRunning = false;
            return;
        }

        scanForEntity();
    }

    private void scanForEntity() {

        this.statusUpdateListener.onScannerStatusUpdate(
                context.getResources().getString(
                        R.string.scanner_status_progress,
                        (this.bitmapArrayPtr + 1), this.bitmapArray.length
                )
        );

        final File file = this.bitmapFileArray[this.bitmapArrayPtr];
        Bitmap bitmap = this.bitmapArray[this.bitmapArrayPtr];

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                .getVisionLabelDetector();

        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionLabel> labels) {
                        for (FirebaseVisionLabel label: labels) {
                            if (label.getEntityId().equals(entityId)) {
                                scanCallback(
                                        file, true, label.getConfidence(), labels
                                );
                                return;
                            }
                        }
                        scanCallback(file, false, 0f, labels);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("CatFinder/Scanner", e.getMessage());
                        scanCallback(file, false, 0f, null);
                    }
                });

    }

    private void createBitmapArray() {
        statusUpdateListener.onScannerStatusUpdate(
                context.getResources().getString(R.string.scanner_status_creating_image_list)
        );
        File[] fileArray = getFileArray(Environment.getExternalStorageDirectory().listFiles());

        Bitmap bitmap;
        ArrayList<Bitmap> bitmapList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();

        for (File file: fileArray) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null) { continue; }
            bitmapList.add(bitmap);
            fileList.add(file);
        }

        this.bitmapArray = bitmapList.toArray(new Bitmap[bitmapList.size()]);
        this.bitmapFileArray = fileList.toArray(new File[fileList.size()]);
    }

    public void stop() {
        if (!this.isRunning()) { return; }
        this.isRunning = false;
    }

    public void run() {
        if (this.isRunning()) { return; }
        this.isRunning = true;

        // Reset counters
        this.bitmapArrayPtr = 0;

        // Reset results
        this.matches = new ArrayList<>();

        this.createBitmapArray();
        this.scanForEntity();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public Scanner(
            Context context,
            OnScannerStatusUpdateListener statusUpdateListener,
            OnScannerFinishedListener scannerFinishedListener,
            OnScannerCanceledListener scannerCanceledListener) {
        this.context = context;
        this.statusUpdateListener = statusUpdateListener;
        this.scannerFinishedListener = scannerFinishedListener;
        this.scannerCanceledListener = scannerCanceledListener;
        this.isRunning = false;
    }

}
