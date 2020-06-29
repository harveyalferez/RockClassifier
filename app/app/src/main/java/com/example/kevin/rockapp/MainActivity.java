package com.example.kevin.rockapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    //for CameraIntent
    private final int PICK_IMAGE_REQUEST = 1;
    private final int TAKE_IMAGE_REQUEST = 2;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    //setUp for tensorflow
    //public static final int INPUT_SIZE = 224;
    //public static final int IMAGE_MEAN = 128;
    //public static final float IMAGE_STD = 128.0f;
    public static final int INPUT_SIZE = 100;
    public static final int IMAGE_MEAN = 50;
    public static final float IMAGE_STD = 50.0f;
    //public static final String INPUT_NAME = "Placeholder";
    public static final String INPUT_NAME = "conv2d_input";
    //public static final String OUTPUT_NAME = "final_result";
    public static final String OUTPUT_NAME = "dense_1/Softmax";
    private static final String MODEL_FILE = "file:///android_asset/output_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";

    public Classifier classifier;
    public Executor executor = Executors.newSingleThreadExecutor();

    private TextView TV;
    private TextView TV2;
    private ImageView iV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TV = findViewById(R.id.textView);
        TV2 = findViewById(R.id.textView2);
        iV = findViewById(R.id.imageView);
        Button photoButton = findViewById(R.id.selectPhoto);

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_PERMISSION_CODE);
                } else {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        initTensorFlowAndLoadModel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new
                        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }

        }
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    public List<Classifier.Recognition> analyze(Bitmap bitmap)
    {
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        return results;
    }

    public void selectPhoto(View v) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST);
    }

    public static Bitmap rotate(Bitmap bitmap, InputStream inputStream) {
        int degrees = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                default:
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    //set Picture in the ImageView (iV)
    public void setPicture(Bitmap bp)
    {
        /* Modify ImageView size
        iV.requestLayout();
        iV.getLayoutParams().width = selectedImage.getWidth();
        iV.getLayoutParams().height = selectedImage.getHeight();

        Orientation
         bp = rotate(bp, inputStream);
        */
        Bitmap scaledBp =  Bitmap.createScaledBitmap(bp, iV.getWidth(), iV.getHeight(), false);
        iV.setImageBitmap(scaledBp);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (reqCode) {
                case PICK_IMAGE_REQUEST:
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        List<Classifier.Recognition> results = analyze(selectedImage);
                        TV.setText(results.get(0).toString());
                        setPicture(selectedImage);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                case CAMERA_REQUEST:
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    List<Classifier.Recognition> results = analyze(photo);
                    String clase = results.get(0).toString().substring(results.get(0).toString().indexOf(" ") + 1, results.get(0).toString().indexOf("(") - 1);
                    String accuracy = results.get(0).toString().substring(results.get(0).toString().indexOf("(") + 1, results.get(0).toString().indexOf(")"));
                    TV.setText("Class: "+clase);
                    TV2.setText("Accuracy: "+accuracy);
                    iV.setImageBitmap(photo);
                    //setPicture(photo);
                    break;

            }
        }
    }

}
