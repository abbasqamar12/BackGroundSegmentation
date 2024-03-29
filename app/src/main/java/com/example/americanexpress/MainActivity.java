package com.example.americanexpress;

import static android.os.Environment.getExternalStoragePublicDirectory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.print.PrintHelper;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.AREventListener;
import ai.deepar.ar.CameraResolutionPreset;
import ai.deepar.ar.DeepAR;
import ai.deepar.ar.DeepARImageFormat;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AREventListener {

    // Default camera lens value, change to CameraSelector.LENS_FACING_BACK to initialize with back camera
    // Default camera lens value, change to CameraSelector.LENS_FACING_FRONT to initialize with front camera
    private final int defaultLensFacing = CameraSelector.LENS_FACING_BACK;
    private ARSurfaceProvider surfaceProvider = null;
    private int lensFacing = defaultLensFacing;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ByteBuffer[] buffers;
    private int currentBuffer = 0;
    private boolean buffersInitialized = false;
    private static final int NUMBER_OF_BUFFERS = 2;
    private static final boolean useExternalCameraTexture = false;

    private DeepAR deepAR;

    private int currentMask = 0;
    private int currentEffect = 0;
    private int currentFilter = 0;
    private int selectedCityFilter = 0;

    private int screenOrientation;

    ArrayList<String> effects;

    private boolean recording = false;
    private boolean currentSwitchRecording = false;

    private int width = 0;
    private int height = 0;

    private File videoFileName;

    private Uri imageToPrint;
    public static String sendImageToPrint = "IMAGE_TO_PRINT";
    ImageView imageAmericanPrideLogo, imageCityLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageAmericanPrideLogo = findViewById(R.id.imgLogoAP);
        imageCityLogo = findViewById(R.id.imgLogoCity);
        Intent intent = getIntent();
        selectedCityFilter = intent.getIntExtra(LocationSelectorActivity.selectedCity, 0);
        selectedCityFilter = 1;
        changeLogo(selectedCityFilter);

    }

    private void changeLogo(int selectedCityFilter) {
        switch (selectedCityFilter) {
            case 0: {
                imageAmericanPrideLogo.setImageDrawable(getDrawable(R.drawable.logo_light));
                imageCityLogo.setImageDrawable(getDrawable(R.drawable.las_vegas_light));
            }
            break;
            case 1: {
                imageAmericanPrideLogo.setImageDrawable(getDrawable(R.drawable.logo_dark));
                imageCityLogo.setImageDrawable(getDrawable(R.drawable.las_vegas_dark));
            }
            break;
            case 2: {
                imageAmericanPrideLogo.setImageDrawable(getDrawable(R.drawable.logo_light));
                imageCityLogo.setImageDrawable(getDrawable(R.drawable.new_york_light));
            }
            break;
            case 3: {
                imageAmericanPrideLogo.setImageDrawable(getDrawable(R.drawable.logo_dark));
                imageCityLogo.setImageDrawable(getDrawable(R.drawable.new_york_dark));
            }
            break;
            default: {
                //imageAmericanPrideLogo.setImageDrawable(getDrawable(R.drawable.logo_light));
                // imageCityLogo.setImageDrawable(getDrawable(R.drawable.las_vegas_light));
            }
        }
    }

    @Override
    protected void onStart() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO},
                    1);
        } else {
            // Permission has already been granted
            initialize();
        }
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return; // no permission
                }
            }
            initialize();
        }
    }

    private void initialize() {
        initializeDeepAR();
        initializeFilters();
        initialiseViews();
    }

    private void initializeFilters() {
        effects = new ArrayList<>();
        effects.add("las_vegas_new.deepar");
        effects.add("las_vegas_2.deepar");
        effects.add("new_york_1.deepar");
        effects.add("new_york_2.deepar");

        effects.add("galaxy_background.deepar");
        effects.add("flower_face.deepar");
        effects.add("Fire_Effect.deepar");

    }

    private void initialiseViews() {
        ImageButton previousMask = findViewById(R.id.previousMask);
        ImageButton nextMask = findViewById(R.id.nextMask);
        TextView printImageButton = findViewById(R.id.printButton);

        SurfaceView arView = findViewById(R.id.surface);

        arView.getHolder().addCallback(this);

        // Surface might already be initialized, so we force the call to onSurfaceChanged
        arView.setVisibility(View.GONE);
        arView.setVisibility(View.VISIBLE);

        final ImageButton screenshotBtn = findViewById(R.id.recordButton);
        screenshotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deepAR.takeScreenshot();
            }
        });

        ImageButton switchCamera = findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
                //unbind immediately to avoid mirrored frame.
                ProcessCameraProvider cameraProvider = null;
                try {
                    cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setupCamera();
            }
        });

        ImageButton openActivity = findViewById(R.id.openActivity);
        openActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, BasicActivity.class);
                MainActivity.this.startActivity(myIntent);
            }


        });

        printImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printImage(imageToPrint);
            }
        });


        final TextView screenShotModeButton = findViewById(R.id.screenshotModeButton);
        final TextView recordModeBtn = findViewById(R.id.recordModeButton);

        recordModeBtn.getBackground().setAlpha(0x00);
        screenShotModeButton.getBackground().setAlpha(0xA0);

        screenShotModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSwitchRecording) {
                    if (recording) {
                        Toast.makeText(getApplicationContext(), "Cannot switch to screenshots while recording!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    recordModeBtn.getBackground().setAlpha(0x00);
                    screenShotModeButton.getBackground().setAlpha(0xA0);
                    screenshotBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deepAR.takeScreenshot();
                        }
                    });

                    currentSwitchRecording = !currentSwitchRecording;
                }
            }
        });


        recordModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!currentSwitchRecording) {

                    recordModeBtn.getBackground().setAlpha(0xA0);
                    screenShotModeButton.getBackground().setAlpha(0x00);
                    screenshotBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (recording) {
                                deepAR.stopVideoRecording();
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri contentUri = Uri.fromFile(videoFileName);
                                mediaScanIntent.setData(contentUri);
                                sendBroadcast(mediaScanIntent);
                                Toast.makeText(getApplicationContext(), "Recording " + videoFileName.getName() + " saved.", Toast.LENGTH_LONG).show();
                            } else {
                                videoFileName = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "video_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp4");
                                deepAR.startVideoRecording(videoFileName.toString(), width / 2, height / 2);
                                Toast.makeText(getApplicationContext(), "Recording started.", Toast.LENGTH_SHORT).show();
                            }
                            recording = !recording;
                        }
                    });

                    currentSwitchRecording = !currentSwitchRecording;
                }
            }
        });

        previousMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoPrevious();
            }
        });

        nextMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoNext();
            }
        });

    }

    /*
            get interface orientation from
            https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164
         */
    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    private void initializeDeepAR() {
        deepAR = new DeepAR(this);
        deepAR.setLicenseKey(Helper.DEEP_AR_LICENSE_KEY);
        deepAR.initialize(this, this);
        setupCamera();
    }

    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        CameraResolutionPreset cameraResolutionPreset = CameraResolutionPreset.P640x480;
        int width;
        int height;
        int orientation = getScreenOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraResolutionPreset.getWidth();
            height = cameraResolutionPreset.getHeight();
        } else {
            width = cameraResolutionPreset.getHeight();
            height = cameraResolutionPreset.getWidth();
        }

        Size cameraResolution = new Size(width, height);
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        if (useExternalCameraTexture) {
            Preview preview = new Preview.Builder()
                    .setTargetResolution(cameraResolution)
                    .build();

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);
            if (surfaceProvider == null) {
                surfaceProvider = new ARSurfaceProvider(this, deepAR);
            }
            preview.setSurfaceProvider(surfaceProvider);
            surfaceProvider.setMirror(lensFacing == CameraSelector.LENS_FACING_FRONT);
        } else {
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(cameraResolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer);
            buffersInitialized = false;
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);
        }
    }

    private void initializeBuffers(int size) {
        this.buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            this.buffers[i] = ByteBuffer.allocateDirect(size);
            this.buffers[i].order(ByteOrder.nativeOrder());
            this.buffers[i].position(0);
        }
    }

    private ImageAnalysis.Analyzer imageAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            if (!buffersInitialized) {
                buffersInitialized = true;
                initializeBuffers(ySize + uSize + vSize);
            }

            byte[] byteData = new byte[ySize + uSize + vSize];

            int width = image.getWidth();
            int yStride = image.getPlanes()[0].getRowStride();
            int uStride = image.getPlanes()[1].getRowStride();
            int vStride = image.getPlanes()[2].getRowStride();
            int outputOffset = 0;
            if (width == yStride) {
                yBuffer.get(byteData, outputOffset, ySize);
                outputOffset += ySize;
            } else {
                for (int inputOffset = 0; inputOffset < ySize; inputOffset += yStride) {
                    yBuffer.position(inputOffset);
                    yBuffer.get(byteData, outputOffset, Math.min(yBuffer.remaining(), width));
                    outputOffset += width;
                }
            }
            //U and V are swapped
            if (width == vStride) {
                vBuffer.get(byteData, outputOffset, vSize);
                outputOffset += vSize;
            } else {
                for (int inputOffset = 0; inputOffset < vSize; inputOffset += vStride) {
                    vBuffer.position(inputOffset);
                    vBuffer.get(byteData, outputOffset, Math.min(vBuffer.remaining(), width));
                    outputOffset += width;
                }
            }
            if (width == uStride) {
                uBuffer.get(byteData, outputOffset, uSize);
                outputOffset += uSize;
            } else {
                for (int inputOffset = 0; inputOffset < uSize; inputOffset += uStride) {
                    uBuffer.position(inputOffset);
                    uBuffer.get(byteData, outputOffset, Math.min(uBuffer.remaining(), width));
                    outputOffset += width;
                }
            }

            buffers[currentBuffer].put(byteData);
            buffers[currentBuffer].position(0);
            if (deepAR != null) {
                deepAR.receiveFrame(buffers[currentBuffer],
                        image.getWidth(), image.getHeight(),
                        image.getImageInfo().getRotationDegrees(),
                        lensFacing == CameraSelector.LENS_FACING_FRONT,
                        DeepARImageFormat.YUV_420_888,
                        image.getPlanes()[1].getPixelStride()
                );
            }
            currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS;
            image.close();
        }
    };


    private String getFilterPath(String filterName) {
        if (filterName.equals("none")) {
            return null;
        }
        return "file:///android_asset/" + filterName;
    }

    private void gotoNext() {
        currentEffect = (currentEffect + 1) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    private void gotoPrevious() {
        currentEffect = (currentEffect - 1 + effects.size()) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    @Override
    protected void onStop() {
        recording = false;
        currentSwitchRecording = false;
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (surfaceProvider != null) {
            surfaceProvider.stop();
            surfaceProvider = null;
        }
        deepAR.release();
        deepAR = null;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surfaceProvider != null) {
            surfaceProvider.stop();
        }
        if (deepAR == null) {
            return;
        }
        deepAR.setAREventListener(null);
        deepAR.release();
        deepAR = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If we are using on screen rendering we have to set surface view where DeepAR will render
        deepAR.setRenderSurface(holder.getSurface(), width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }

    @Override
    public void screenshotTaken(Bitmap bitmap) {
        CharSequence now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date());
        try {
            File imageFile = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "image_" + now + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            imageFile.setReadable(true, false);

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            // Uri photoURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", createImageFile());
            // imageToPrint = contentUri;
            goToPrintActivity(contentUri);
            // sendMail(contentUri, new String[]{"qamar.abbas@vmstechs.com"});
            Toast.makeText(MainActivity.this, "Screenshot " + imageFile.getName() + " saved.", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private void goToPrintActivity(Uri contentUri) {
        Intent intent = new Intent(MainActivity.this, PrintActivity.class);
        intent.putExtra(MainActivity.sendImageToPrint, contentUri.toString());
        startActivity(intent);

    }

    @Override
    public void videoRecordingStarted() {

    }

    @Override
    public void videoRecordingFinished() {

    }

    @Override
    public void videoRecordingFailed() {

    }

    @Override
    public void videoRecordingPrepared() {

    }

    @Override
    public void shutdownFinished() {

    }

    @Override
    public void initialized() {
        // Restore effect state after deepar release
        // deepAR.switchEffect("effect",  (effects.get(selectedCityFilter)));
        currentEffect = selectedCityFilter;
        currentEffect = (currentEffect + effects.size()) % effects.size();
        deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
    }

    @Override
    public void faceVisibilityChanged(boolean b) {

    }

    @Override
    public void imageVisibilityChanged(String s, boolean b) {

    }

    @Override
    public void frameAvailable(Image image) {

    }

    @Override
    public void error(ARErrorType arErrorType, String s) {

    }


    @Override
    public void effectSwitched(String s) {

    }


    public void sendMail(Uri fileUri, String[] userEmail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        //emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"email@example.com"});
        emailIntent.putExtra(Intent.EXTRA_EMAIL, userEmail);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "subject here");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "body text");
       /* File root = Environment.getExternalStorageDirectory();
        String pathToMyAttachedFile = "temp/attachement.xml";
        File file = new File(root, pathToMyAttachedFile);
        if (!file.exists() || !file.canRead()) {
            return;
        }
        Uri uri = Uri.fromFile(file);*/
        Log.d("IMAGE", "Imae Path : " + fileUri);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
    }

    public void printImage(Uri fileToPrint) {
        PrintHelper bitmapPrintHelper = new PrintHelper(MainActivity.this);
        bitmapPrintHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        Bitmap bitmapToPrint = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
        // bitmapPrintHelper.printBitmap("Testing Print", bitmapToPrint);
        try {
            bitmapPrintHelper.printBitmap("Testing Print", fileToPrint);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}