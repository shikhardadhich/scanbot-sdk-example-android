package io.scanbot.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import io.scanbot.sdk.camera.CameraOpenCallback;
import io.scanbot.sdk.camera.ScanbotCameraView;
import io.scanbot.sdk.core.payformscanner.model.PayFormRecognitionResult;
import io.scanbot.sdk.ScanbotSDK;


import io.scanbot.sdk.core.payformscanner.DetectionResult;
import io.scanbot.sdk.payformscanner.PayFormScannerFrameHandler;

import io.scanbot.sdk.payformscanner.PayFormScanner;

import io.scanbot.sdk.util.log.Logger;
import io.scanbot.sdk.util.log.LoggerProvider;

import org.jetbrains.annotations.NotNull;

import io.scanbot.sdk.SdkLicenseError;
import io.scanbot.sdk.camera.FrameHandlerResult;


public class PayformScannerActivity extends AppCompatActivity {

    private final Logger logger = LoggerProvider.getLogger();

    private ScanbotCameraView cameraView;
    private TextView resultView;

    boolean flashEnabled = false;

    public static Intent newIntent(Context context) {
        return new Intent(context, PayformScannerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payform_scanner);

        getSupportActionBar().hide();

        cameraView = (ScanbotCameraView) findViewById(R.id.camera);
        cameraView.lockToLandscape();

        cameraView.setCameraOpenCallback(new CameraOpenCallback() {
            @Override
            public void onCameraOpened() {
                cameraView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraView.useFlash(flashEnabled);
                        cameraView.continuousFocus();
                    }
                }, 700);
            }
        });

        resultView = (TextView) findViewById(R.id.result);

        ScanbotSDK scanbotSDK = new ScanbotSDK(this);
        final PayFormScanner payFormScanner = scanbotSDK.payFormScanner();
        PayFormScannerFrameHandler payFormScannerFrameHandler = PayFormScannerFrameHandler.attach(cameraView, payFormScanner);

        payFormScannerFrameHandler.addResultHandler(new PayFormScannerFrameHandler.ResultHandler() {
            @Override
            public boolean handle(@NotNull FrameHandlerResult<? extends DetectionResult, ? extends SdkLicenseError> frameHandlerResult) {
                if (frameHandlerResult instanceof FrameHandlerResult.Success) {
                    DetectionResult detectionResult = ((FrameHandlerResult.Success<DetectionResult>) frameHandlerResult).getValue();
                    if (detectionResult != null && detectionResult.form.isValid()) {
                        long a = System.currentTimeMillis();

                        try {
                            final PayFormRecognitionResult result = payFormScanner.recognizeForm(detectionResult.lastFrame, detectionResult.frameWidth, detectionResult.frameHeight, 0);
                            startActivity(PayformResultActivity.newIntent(PayformScannerActivity.this, result.payformFields));
                        } finally {
                            long b = System.currentTimeMillis();
                            logger.d("PayFormScanner", "Total scanning (sec): " + (b - a) / 1000f);
                        }
                    }
                }
                return false;
            }
        });

        findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                flashEnabled = !flashEnabled;
                cameraView.useFlash(flashEnabled);
            }
        });

        Toast.makeText(
                this,
                scanbotSDK.isLicenseActive()
                        ? "License is active"
                        : "License is expired",
                Toast.LENGTH_LONG
        ).show();
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
}
