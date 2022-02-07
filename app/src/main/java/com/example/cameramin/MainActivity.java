package com.example.cameramin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler UIHandler = new Handler();

    private static final int PERMISSION_REQUEST_CAMERA = 34634;

    private static final int FRAME_DELAY = 3000;
    private static final int FRAME_NUMBER = 3;

    private ImageView imageView;
    private Button startBtn;

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    YUVtoRGB translator = new YUVtoRGB();
    ImageProcessor imageProcessor = new ImageProcessor();


    private ImageCapture imageCapture;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CAMERA);

        }

        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "Delmove");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (!success) {
            // Do something on success
            Toast.makeText(MainActivity.this, "Failed to create dir for saving!", Toast.LENGTH_SHORT).show();
        }

        imageView = findViewById(R.id.imageView);
        startBtn = findViewById(R.id.recordBtn);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Runnable cameraProviderFutureListener = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                            initCamera(cameraProvider);
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                cameraProviderFuture.addListener(cameraProviderFutureListener, ContextCompat.getMainExecutor(MainActivity.this));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission successfully granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        File appDir = new File(Environment.getExternalStorageDirectory() + "/Delmove");
        File sessionDir = new File(appDir, Calendar.getInstance().getTime().toString().replaceAll(":", "."));
        sessionDir.mkdir();

        final int[] frameCounter = {FRAME_NUMBER};

        List<Bitmap> bitmaps = new ArrayList<>();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

//        imageCapture = new ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .build();

        ImageAnalysis imageSaving = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1024, 768))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageSaving.setAnalyzer(executorService, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
                Bitmap bitmap = translator.translateYUV(img, MainActivity.this);
                bitmaps.add(bitmap);
                saveToAppFolder(bitmap, sessionDir);

                frameCounter[0]--;
                if (frameCounter[0] <= 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageSaving.clearAnalyzer();
                            cameraProvider.unbind(imageSaving);
                            Toast.makeText(MainActivity.this, "Successfully caprured!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Bitmap result = imageProcessor.removeMove(bitmaps);
                    saveToAppFolder(result, sessionDir);
                }

                try {
                    Thread.sleep(FRAME_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                image.close();
            }
        });

        ImageAnalysis imageDisplaying = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1024, 768))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageDisplaying.setAnalyzer(ContextCompat.getMainExecutor(MainActivity.this),
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
                        Bitmap bitmap = translator.translateYUV(img, MainActivity.this);

                        imageView.setRotation(image.getImageInfo().getRotationDegrees());
                        imageView.setImageBitmap(bitmap);

                        image.close();
                    }
                });

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageSaving, imageDisplaying);
//        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture);
    }

    private void capturePhoto(Executor executor) {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Photo has been saved successfully.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );
    }

    private void saveToAppFolder(Bitmap bmp, File sessionFolder) {
        File appDir = new File(Environment.getExternalStorageDirectory() + "/Delmove");

        String filename = Calendar.getInstance().getTime().toString();
        filename = filename.replaceAll(":", ".") + ".png";

        File file = new File(sessionFolder, filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
