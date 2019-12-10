package io.scanbot.example;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.doo.snap.camera.CameraOpenCallback;
import net.doo.snap.camera.ScanbotCameraView;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import org.jetbrains.annotations.NotNull;

import io.scanbot.sdk.ScanbotSDK;
import io.scanbot.sdk.SdkLicenseError;
import io.scanbot.sdk.barcode.BarcodeDetectorFrameHandler;
import io.scanbot.sdk.barcode.entity.BarcodeItem;
import io.scanbot.sdk.barcode.entity.BarcodeScanningResult;
import io.scanbot.sdk.camera.FrameHandlerResult;

public class BarcodeScannerWithFinderActivity extends AppCompatActivity implements BarcodeDetectorFrameHandler.ResultHandler, AdapterView.OnItemSelectedListener {

    private ScanbotCameraView cameraView;

    private boolean flashEnabled = false;

    private String encoding = null;


    public static Intent newIntent(final Context context) {
        return new Intent(context, BarcodeScannerWithFinderActivity.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_barcode_scanner_with_finder);

        getSupportActionBar().hide();

        cameraView = (ScanbotCameraView) findViewById(R.id.camera);
        cameraView.setCameraOpenCallback(new CameraOpenCallback() {
            @Override
            public void onCameraOpened() {
                cameraView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraView.continuousFocus();
                        cameraView.useFlash(flashEnabled);
                    }
                }, 700);
            }
        });

        BarcodeDetectorFrameHandler barcodeDetectorFrameHandler = BarcodeDetectorFrameHandler.attach(cameraView, new ScanbotSDK(this));

        // Default detection interval is 10000 ms
        barcodeDetectorFrameHandler.setDetectionInterval(2000);

        barcodeDetectorFrameHandler.addResultHandler(this);

        findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashEnabled = !flashEnabled;
                cameraView.useFlash(flashEnabled);
            }
        });


        final Spinner encodingSpinner = findViewById(R.id.encoding_spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.encoding_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encodingSpinner.setAdapter(adapter);
        encodingSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }

    public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
        encoding = (pos == 0 ? null : parent.getItemAtPosition(pos).toString());
    }

    public void onNothingSelected(final AdapterView<?> parent) {
        //
    }

    private void showBarcodeResult(final BarcodeScanningResult result) {
        if (result.getBarcodeItems() == null || result.getBarcodeItems().size() == 0) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraView.stopPreview();

                // The current Barcode Detector of this app supports only one barcode item as result!
                // For multiple barcode results see the beta-barcode-scanner example project.
                final BarcodeItem barcodeItem = result.getBarcodeItems().get(0);

                final String value;
                if (encoding != null) {
                    value = Charset.forName(encoding).decode(ByteBuffer.wrap(barcodeItem.getText().getBytes())).toString();
                } else {
                    value = barcodeItem.getText();
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeScannerWithFinderActivity.this);

                builder.setTitle("Result")
                        .setMessage(
                                "FORMAT: " + barcodeItem.getBarcodeFormat().toString() +
                                        "\n\nVALUE: " + value
                        );

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        cameraView.continuousFocus();
                        cameraView.startPreview();
                    }
                });

                final AlertDialog dialog = builder.create();
                dialog.show();


                /* You can implement a suitable result handler, like create a contact, open URL, etc.
                final ParsedResult parsedResult = ResultParser.parseResult(rawResult);
                switch (parsedResult.getType()) {
                    case ADDRESSBOOK:
                        // ...
                        break;
                    case URI:
                        // ...
                        break;
                    // ...
                }
                */
            }
        });
    }


    @Override
    public boolean handleResult(@NotNull FrameHandlerResult<BarcodeScanningResult, SdkLicenseError> result) {
        if (result instanceof FrameHandlerResult.Success) {
            BarcodeScanningResult recognitionResult = (BarcodeScanningResult) ((FrameHandlerResult.Success) result).getValue();
            if (recognitionResult != null) {
                showBarcodeResult(recognitionResult);
            }
        }
        return false;
    }
}

