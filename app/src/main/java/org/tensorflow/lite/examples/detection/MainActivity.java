package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.tensorflow.lite.examples.detection.detector.Constants;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.env.Utils;

import org.tensorflow.lite.examples.detection.detector.Classifier;
import org.tensorflow.lite.examples.detection.mockdetector.PlateOCR;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;



import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int DETECTOR_REQUEST_CODE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImage = findViewById(R.id.btnSelectImage);
        cameraButton = findViewById(R.id.cameraButton);
        detectButton = findViewById(R.id.detectButton);
        imageView = findViewById(R.id.imageView);
        txtPlate = findViewById(R.id.txtPlate);

        // این دکمه برای دریافت تصویر از دوربین و یافتن پلاک در تصویر دوربین است
        cameraButton.setOnClickListener(v ->
                {
                    Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
                    startActivityForResult(intent, DETECTOR_REQUEST_CODE);
                })
                ;


        // این دکمه برای انتخاب یک عکس از گالری است
        selectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);

        });

        // این دکمه برای شناسایی پلاک در عکس انتخاب شده است
        detectButton.setOnClickListener(v -> {
            Handler handler = new Handler();

            new Thread(() -> {
                // کلاس اصلی خوانش پلاک یک عکس را به عنوان ورودی دریافت می کند و لیست پلاک های خوانده شده را برمیگرداند
                final List<Classifier.Recognition> results = detector.getPlates(cropBitmap, true);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // در این قسمت ما نتایج را بر روی عکس نمایش میدهیم
                        // شما میتوانید نتایج را برای بررسی های بعدی استفاده کنید
                        handleResult(cropBitmap, results);
                    }
                });
            }).start();

        });

        this.sourceBitmap = Utils.getBitmapFromAsset(MainActivity.this, "testein.jpg");
        this.cropBitmap = this.sourceBitmap.copy(Bitmap.Config.ARGB_8888,true);
        this.imageView.setImageBitmap(this.sourceBitmap);

        initBox();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                sourceBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                this.cropBitmap = this.sourceBitmap.copy(Bitmap.Config.ARGB_8888,true);
                this.imageView.setImageBitmap(this.sourceBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            // نتیجه ای که ذخیره کرده ایم اینجا در دسترس است
            if (requestCode == DETECTOR_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String detectedObject = data.getStringExtra("plate"); // Retrieve the data
                // Do something with the detected object (e.g., display it)
                Toast.makeText(this, "پلاک تشخیص داده شده: " + detectedObject, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static final Logger LOGGER = new Logger();

    public static final int TF_OD_API_INPUT_SIZE = 416 * 4;

    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "pf_416.tflite"; //"yolov4-416-fp32.tflite"

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/char.txt"; // coco.txt

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = true;
    private Integer sensorOrientation = 90;

    // این کلاس اصلی برای خوانش پلاک است
    public PlateOCR detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private OverlayView trackingOverlay;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;

    private Button cameraButton, detectButton, selectImage;
    private ImageView imageView;

    public TextView txtPlate;
    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker = new MultiBoxTracker(this);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));

        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);

        try {
            detector = new PlateOCR(getAssets());
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    // این تابع جهت نمایش نتایج روی تصویر استفاده می شود
    private void handleResult(Bitmap bitmap, List<Classifier.Recognition> results) {
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            // هر پلاک یک موقیعت یا باندینگ باکس دارد
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= Constants.MINIMUM_CONFIDENCE_TF_OD_API) {
                canvas.drawRect(location, paint);
                // متن پلاک خوانده شده را در یک تکست باکس میریزیم
                txtPlate.setText(Utils.getPersianPlate(result.getPlate().toString()));
//                cropToFrameTransform.mapRect(location);
//
//                result.setLocation(location);
//                mappedRecognitions.add(result);
            }
        }
//        tracker.trackResults(mappedRecognitions, new Random().nextInt());
//        trackingOverlay.postInvalidate();
        imageView.setImageBitmap(bitmap);
    }
}
