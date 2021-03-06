package com.xukui.cameraocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class IdcardOcrFragment extends Fragment {

    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;

    private ConstraintLayout mCameraContainer;
    private PreviewView mCameraPreview;
    private ImageButton mCameraSwitchButton;
    private ImageButton mCameraCaptureButton;
    private ImageButton mCameraPhotoButton;
    private ImageView mPhotoImageView;

    private ExecutorService mCameraExecutor;
    private DisplayManager mDisplayManager;
    private Preview mPreview;
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Camera mCamera;
    private ProcessCameraProvider mCameraProvider;

    private int mDisplayId = -1;
    private int mLensFacing = CameraSelector.LENS_FACING_BACK;
    private File mOutputDirectory;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOutputDirectory = Mapp.getInstance().getExternalFilesDir("Camera");
        if (mOutputDirectory == null) {
            mOutputDirectory = new File(Mapp.getInstance().getFilesDir(), "Camera");
        }
        if (mOutputDirectory != null && !mOutputDirectory.exists()) {
            mOutputDirectory.mkdirs();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_idcard_ocr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        setView();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateCameraUi();
        updateCameraSwitchButton();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
        }
        if (mDisplayManager != null) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
    }

    private void initView(View view) {
        mCameraContainer = view.findViewById(R.id.camera_container);
        mCameraPreview = view.findViewById(R.id.camera_preview);
        mCameraSwitchButton = view.findViewById(R.id.camera_switch_button);
        mCameraCaptureButton = view.findViewById(R.id.camera_capture_button);
        mCameraPhotoButton = view.findViewById(R.id.camera_photo_button);
        mPhotoImageView = view.findViewById(R.id.photo_imageView);

        mCameraExecutor = Executors.newSingleThreadExecutor();

        mDisplayManager = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
    }

    private void setView() {
        mCameraPreview.post(() -> {
            mDisplayId = mCameraPreview.getDisplay().getDisplayId();

            updateCameraUi();
            setUpCamera();
        });
    }

    private void updateCameraUi() {
        mCameraSwitchButton.setEnabled(false);
        mCameraSwitchButton.setOnClickListener(v -> {
            if (mLensFacing == CameraSelector.LENS_FACING_FRONT) {
                mLensFacing = CameraSelector.LENS_FACING_BACK;

            } else {
                mLensFacing = CameraSelector.LENS_FACING_FRONT;
            }

            bindCameraUseCases();
        });

        mCameraCaptureButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mImageCapture != null && mCameraExecutor != null) {
//                    存文件拍摄
//                    ImageCapture.Metadata metadata = new ImageCapture.Metadata();
//                    metadata.setReversedHorizontal(mLensFacing == CameraSelector.LENS_FACING_FRONT);
//
//                    File outputFile = new File(mOutputDirectory, "CameraX_" + System.currentTimeMillis() + ".jpg");
//
//                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(outputFile)
//                            .setMetadata(metadata)
//                            .build();
//
//                    mImageCapture.takePicture(outputFileOptions, mCameraExecutor, new ImageCapture.OnImageSavedCallback() {
//
//                        @Override
//                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(".jpg");
//                            MediaScannerConnection.scanFile(getContext(), new String[]{outputFile.getAbsolutePath()}, new String[]{mimeType}, null);
//
//                            Log.e("dddddd", "拍照成功:" + outputFile.getAbsolutePath());
//                        }
//
//                        @Override
//                        public void onError(@NonNull ImageCaptureException exception) {
//                            Log.e("dddddd", "拍照失败", exception);
//                        }
//
//                    });

//                    不存文件拍摄
                    mImageCapture.takePicture(mCameraExecutor, new ImageCapture.OnImageCapturedCallback() {

                        private byte[] getJpegBytes(ImageProxy.PlaneProxy planeProxy) {
                            ByteBuffer byteBuffer = planeProxy.getBuffer();
                            byte[] bytes = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bytes);
                            return bytes;
                        }

                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            ImageProxy.PlaneProxy[] planeProxies = image.getPlanes();
                            ImageProxy.PlaneProxy planeProxy = planeProxies[0];
                            byte[] bytes = getJpegBytes(planeProxy);
                            super.onCaptureSuccess(image);
                            Log.e("dddddd", "拍照成功, 大小:" + (bytes.length / 1024f) + "kb");
                            Log.e("dddddd", "宽高角度:" + image.getWidth() + ", " + image.getHeight() + ", " + image.getImageInfo().getRotationDegrees());

                            mHandler.post(() -> {
                                if (mPhotoImageView != null) {
                                    Glide.with(requireContext())
                                            .load(bytes)
                                            .into(mPhotoImageView);
                                }
                            });
                        }

//                        private YuvToRgbConverter converter;
//                        private Bitmap outputBitmap;
//
//                        @SuppressLint("UnsafeExperimentalUsageError")
//                        @Override
//                        public void onCaptureSuccess2(@NonNull ImageProxy image) {
//                            if (converter == null) {
//                                converter = new YuvToRgbConverter(requireContext());
//                            }
//
//                            if (outputBitmap == null) {
//                                outputBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.RGB_565);
//                            }
//
//                            converter.yuvToRgb(image.getImage(), outputBitmap);
//                            super.onCaptureSuccess(image);
//
//                            mHandler.post(new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    if (mPhotoImageView != null) {
//                                        Glide.with(requireContext())
//                                                .load(outputBitmap)
//                                                .into(mPhotoImageView);
//                                    }
//                                }
//
//                            });
//                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            //连续拍摄期间切换镜头, 可能会报异常, 不过不会崩溃, 因为已经拦截并且是正常现象, 所以不需要提示
                        }

                    });
                }
            }

        });

//
//        // In the background, load latest photo taken (if any) for gallery thumbnail
//        lifecycleScope.launch(Dispatchers.IO) {
//            outputDirectory.listFiles {
//                file ->
//                        EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
//            }?.max() ?.let {
//                setGalleryThumbnail(Uri.fromFile(it))
//            }
//        }
//
//        // Listener for button used to capture photo
//        controls.findViewById<ImageButton> (R.id.camera_capture_button).setOnClickListener {
//
//            // Get a stable reference of the modifiable image capture use case
//            imageCapture ?.let {
//                imageCapture ->
//
//                        // Create output file to hold the image
//                        val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
//
//                // Setup image capture metadata
//                val metadata = Metadata().apply {
//
//                    // Mirror image when using the front camera
//                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
//                }
//
//                // Create output options object which contains file + metadata
//                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
//                        .setMetadata(metadata)
//                        .build()
//
//                // Setup image capture listener which is triggered after photo has been taken
//                imageCapture.takePicture(
//                        outputOptions, cameraExecutor, object :ImageCapture.OnImageSavedCallback {
//                    override fun onError(exc:ImageCaptureException){
//                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                    }
//
//                    override fun onImageSaved(output:ImageCapture.OutputFileResults){
//                        val savedUri = output.savedUri ?:Uri.fromFile(photoFile)
//                        Log.d(TAG, "Photo capture succeeded: $savedUri")
//
//                        // We can only change the foreground Drawable using API level 23+ API
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            // Update the gallery thumbnail with latest picture taken
//                            setGalleryThumbnail(savedUri)
//                        }
//
//                        // Implicit broadcasts will be ignored for devices running API level >= 24
//                        // so if you only target API level 24+ you can remove this statement
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                            requireActivity().sendBroadcast(
//                                    Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
//                            )
//                        }
//
//                        // If the folder selected is an external media directory, this is
//                        // unnecessary but otherwise other apps will not be able to access our
//                        // images unless we scan them using [MediaScannerConnection]
//                        val mimeType = MimeTypeMap.getSingleton()
//                                .getMimeTypeFromExtension(savedUri.toFile().extension)
//                        MediaScannerConnection.scanFile(
//                                context,
//                                arrayOf(savedUri.toFile().absolutePath),
//                                arrayOf(mimeType)
//                        ) {
//                            _, uri ->
//                                    Log.d(TAG, "Image capture scanned into media store: $uri")
//                        }
//                    }
//                })
//
//                // We can only change the foreground Drawable using API level 23+ API
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//                    // Display flash animation to indicate that photo was captured
//                    container.postDelayed({
//                            container.foreground = ColorDrawable(Color.WHITE)
//                            container.postDelayed(
//                                    {container.foreground = null}, ANIMATION_FAST_MILLIS)
//                    }, ANIMATION_SLOW_MILLIS)
//                }
//            }
//        }
//

//
//        // Listener for button used to view the most recent photo
//        controls.findViewById<ImageButton> (R.id.photo_view_button).setOnClickListener {
//            // Only navigate when the gallery has photos
//            if (true == outputDirectory.listFiles() ?.isNotEmpty()){
//                Navigation.findNavController(
//                        requireActivity(), R.id.fragment_container
//                ).navigate(CameraFragmentDirections
//                        .actionCameraToGallery(outputDirectory.absolutePath))
//            }
//        }
    }

    private void setUpCamera() throws IllegalStateException {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException("操作相机失败");
            } catch (InterruptedException e) {
                throw new IllegalStateException("操作相机失败");
            }

            if (hasBackCamera()) {
                mLensFacing = CameraSelector.LENS_FACING_BACK;

            } else if (hasFrontCamera()) {
                mLensFacing = CameraSelector.LENS_FACING_FRONT;

            } else {
                throw new IllegalStateException("前后摄像头都无法使用");
            }

            updateCameraSwitchButton();
            bindCameraUseCases();
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void updateCameraSwitchButton() {
        mCameraSwitchButton.setEnabled(hasBackCamera() && hasFrontCamera());
    }

    private boolean hasBackCamera() {
        try {
            return mCameraProvider == null ? false : mCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);

        } catch (CameraInfoUnavailableException e) {
            return false;
        }
    }

    private boolean hasFrontCamera() {
        try {
            return mCameraProvider == null ? false : mCameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);

        } catch (CameraInfoUnavailableException e) {
            return false;
        }
    }

    @SuppressLint("RestrictedApi")
    private void bindCameraUseCases() throws IllegalStateException {
        DisplayMetrics metrics = new DisplayMetrics();
        mCameraPreview.getDisplay().getRealMetrics(metrics);
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        int rotation = mCameraPreview.getDisplay().getRotation();

        if (mCameraProvider == null) {
            throw new IllegalStateException("相机初始化失败");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(mLensFacing).build();

        mPreview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();

        mImageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setTargetRotation(rotation)
                .setTargetResolution(new Size(720, 1280))
                .build();

        mImageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        mImageAnalysis.setAnalyzer(mCameraExecutor, new LuminosityAnalyzer(new AnalysisCallBack() {

            @Override
            public void onAnalysis(int luma, double framesPerSecond, long timestamp) {
//                Log.e("ddddd", "------相机分析------");
//                Log.e("ddddd", "luma: " + luma);
//                Log.e("ddddd", "framesPerSecond: " + framesPerSecond);
//                Log.e("ddddd", "timestamp: " + timestamp);
            }

        }));

        mCameraProvider.unbindAll();

        try {
            mCamera = mCameraProvider.bindToLifecycle(this, cameraSelector, mPreview, mImageCapture, mImageAnalysis);
            mPreview.setSurfaceProvider(mCameraPreview.getSurfaceProvider());

        } catch (Exception e) {
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = 1d * Math.max(width, height) / Math.min(width, height);

        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;

        } else {
            return AspectRatio.RATIO_16_9;
        }
    }

    private class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

        private int frameRateWindow = 8;
        private ArrayDeque<Long> frameTimestamps = new ArrayDeque<>(5);
        private ArrayList<AnalysisCallBack> listeners = new ArrayList<>();
        private long lastAnalyzedTimestamp = 0L;
        private double framesPerSecond = -1.0D;

        public LuminosityAnalyzer(@NonNull AnalysisCallBack listener) {
            listeners.add(listener);
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (listeners.isEmpty()) {
                image.close();
                return;
            }

            long currentTime = System.currentTimeMillis();
            frameTimestamps.push(currentTime);

            while (frameTimestamps.size() >= frameRateWindow) {
                frameTimestamps.removeLast();
            }

            Long timestampFirst = frameTimestamps.peekFirst();
            if (timestampFirst == null) {
                timestampFirst = currentTime;
            }

            Long timestampLast = frameTimestamps.peekLast();
            if (timestampLast == null) {
                timestampLast = currentTime;
            }

            framesPerSecond = 1.0D / ((timestampFirst - timestampLast) / (1.0D * Math.max(frameTimestamps.size(), 1))) * 1000.0D;
            lastAnalyzedTimestamp = frameTimestamps.getFirst();

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

            byte[] data = toByteArray(buffer);
            int pixels = 0;
            for (byte datum : data) {
                pixels += datum & 0xFF;
            }
            int luma = pixels / data.length;

            for (AnalysisCallBack callBack : listeners) {
                callBack.onAnalysis(luma, framesPerSecond, lastAnalyzedTimestamp);
            }

            image.close();
        }

        private byte[] toByteArray(ByteBuffer buffer) {
            buffer.rewind();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            return data;
        }
    }

    public interface AnalysisCallBack {
        void onAnalysis(int luma, double framesPerSecond, long timestamp);
    }

    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {

        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (getView() == null) {
                return;
            }

            if (mDisplayId != displayId) {
                return;
            }

            int rotation = getView().getDisplay().getRotation();

            if (mImageCapture != null) {
                mImageCapture.setTargetRotation(rotation);
            }

            if (mImageAnalysis != null) {
                mImageAnalysis.setTargetRotation(rotation);
            }
        }

    };

}