package com.radialle.catfinder;

import com.google.firebase.ml.vision.label.FirebaseVisionLabel;

import java.io.File;
import java.util.List;

public class ScannerMatch {

    private File file;
    private String entityId;
    private float confidence;
    private List<FirebaseVisionLabel> labels;

    public File getFile() {
        return this.file;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public List<FirebaseVisionLabel> getLabels() {
        return this.labels;
    }

    public ScannerMatch(
            File file, String entityId, float confidence, List<FirebaseVisionLabel> labels
    ) {
        this.file = file;
        this.entityId = entityId;
        this.confidence = confidence;
        this.labels = labels;
    }

}
