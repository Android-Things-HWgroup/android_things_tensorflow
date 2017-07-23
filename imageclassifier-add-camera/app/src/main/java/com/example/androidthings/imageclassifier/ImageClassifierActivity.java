/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.ImageView;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;

public class ImageClassifierActivity extends Activity {
    private static final String TAG = "ImageClassifierActivity";

    private ButtonInputDriver mButtonDriver;
    private boolean mProcessing;
    private String[] labels;
    private TensorFlowInferenceInterface inferenceInterface;
    private CameraHandler mCameraHandler;
    private ImagePreprocessor mImagePreprocessor;

    private PeripheralManagerService service = new PeripheralManagerService();
    private Gpio mButtonGpio;

    private TextureView mSurfaceView;

    private void initClassifier() {
        this.inferenceInterface = new TensorFlowInferenceInterface(
                getAssets(), Helper.MODEL_FILE);
        this.labels = Helper.readLabels(this);
    }

    private void destroyClassifier() {
        inferenceInterface.close();
    }

    private void doRecognize(Bitmap image) {
        float[] pixels = Helper.getPixels(image);

        // Feed the pixels of the image into the TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back into an array of confidence per category
        float[] outputs = new float[Helper.NUM_CLASSES];
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);

        // Get the results with the highest confidence and map them to their labels
        onPhotoRecognitionReady(Helper.getBestResults(outputs, labels));
    }

    private void initCamera(Surface su) throws CameraAccessException {
        mImagePreprocessor = new ImagePreprocessor();

        mCameraHandler = CameraHandler.getInstance();
        Handler threadLooper = new Handler(getMainLooper());
        mCameraHandler.initializeCamera(this, threadLooper,
                new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Bitmap bitmap = mImagePreprocessor.preprocessImage(imageReader.acquireNextImage());
                onPhotoReady(bitmap);
            }
        });
        mCameraHandler.setSurface(su);
    }

    private void closeCamera() {
        mCameraHandler.shutDown();
    }

    private void loadPhoto() {
        mCameraHandler.takePicture();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_main);

        mSurfaceView = (TextureView) findViewById(R.id.camera);
        Surface surfaceView = new Surface(mSurfaceView.getSurfaceTexture());

        try {
            initCamera(surfaceView);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        initClassifier();
        initButton();

        Log.d(TAG, "READY");
    }

    private void initButton() {
        try {
            String pin = Build.DEVICE.equals("rpi3") ? "BCM26" : "GPIO_37";

            mButtonGpio = service.openGpio(pin);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Value is true when the pin is LOW
            mButtonGpio.setActiveType(Gpio.ACTIVE_LOW);
            // Register the event callback.
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                private long t;
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    try {
                        if(gpio.getValue()){
                            t = System.currentTimeMillis();
                        }else{
                            long a = System.currentTimeMillis() - t;
                            if(a < 1500){
                                Log.d(TAG, "Running photo recognition");
                                mProcessing = true;
                                loadPhoto();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            Log.w(TAG, "Cannot find button. Ignoring push button. Use a keyboard instead.", e);
        }
    }

    private Bitmap getStaticBitmap() {
        Log.d(TAG, "Using sample photo in res/drawable/sampledog_224x224.png");
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.sampledog_224x224);
    }

    private void onPhotoReady(Bitmap bitmap) {
//        mImageViwe.setImageBitmap(bitmap);
        doRecognize(bitmap);
    }

    private void onPhotoRecognitionReady(String[] results) {
        Log.d(TAG, "RESULTS: " + Helper.formatResults(results));
        mProcessing = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonGpio != null) {
                mButtonGpio.unregisterGpioCallback(new GpioCallback() {});
                try {
                    mButtonGpio.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing GPIO", e);
                }
            }
        } catch (Throwable t) {
            // close quietly
        }
    }
}
