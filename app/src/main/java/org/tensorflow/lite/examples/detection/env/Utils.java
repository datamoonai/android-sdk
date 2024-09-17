package org.tensorflow.lite.examples.detection.env;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import org.tensorflow.lite.examples.detection.detector.Classifier;
import org.tensorflow.lite.examples.detection.detector.PlateLetter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;


public class Utils {

    /**
     * Memory-map the model file in Assets.
     */
    public static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static void softmax(final float[] vals) {
        float max = Float.NEGATIVE_INFINITY;
        for (final float val : vals) {
            max = Math.max(max, val);
        }
        float sum = 0.0f;
        for (int i = 0; i < vals.length; ++i) {
            vals[i] = (float) Math.exp(vals[i] - max);
            sum += vals[i];
        }
        for (int i = 0; i < vals.length; ++i) {
            vals[i] = vals[i] / sum;
        }
    }

    public static float expit(final float x) {
        return (float) (1. / (1. + Math.exp(-x)));
    }

//    public static Bitmap scale(Context context, String filePath) {
//        AssetManager assetManager = context.getAssets();
//
//        InputStream istr;
//        Bitmap bitmap = null;
//        try {
//            istr = assetManager.open(filePath);
//            bitmap = BitmapFactory.decodeStream(istr);
//            bitmap = Bitmap.createScaledBitmap(bitmap, com.example.dmalpr.MainActivity.TF_OD_API_INPUT_SIZE, com.example.dmalpr.MainActivity.TF_OD_API_INPUT_SIZE, false);
//        } catch (IOException e) {
//            // handle exception
//            Log.e("getBitmapFromAsset", "getBitmapFromAsset: " + e.getMessage());
//        }
//
//        return bitmap;
//    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
//            return bitmap.copy(Bitmap.Config.ARGB_8888,true);
        } catch (IOException e) {
            // handle exception
            Log.e("getBitmapFromAsset", "getBitmapFromAsset: " + e.getMessage());
        }

        return bitmap;
    }

    /**
     * Returns a transformation matrix from one reference frame into another.
     * Handles cropping (if maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth Width of source frame.
     * @param srcHeight Height of source frame.
     * @param dstWidth Width of destination frame.
     * @param dstHeight Height of destination frame.
     * @param applyRotation Amount of rotation to apply from one frame to another.
     *  Must be a multiple of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     * cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    public static Bitmap processBitmap(Bitmap source, int size){

        int image_height = source.getHeight();
        int image_width = source.getWidth();

        Bitmap croppedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Matrix frameToCropTransformations = getTransformationMatrix(image_width,image_height,size,size,0,false);
        Matrix cropToFrameTransformations = new Matrix();
        frameToCropTransformations.invert(cropToFrameTransformations);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(source, frameToCropTransformations, null);

        return croppedBitmap;
    }

    public static Bitmap processBitmapWithBlackMargins(Bitmap source, int size) {
        int imageHeight = source.getHeight();
        int imageWidth = source.getWidth();

        float aspectRatio = (float) imageWidth / imageHeight;
        int targetWidth, targetHeight;

        if (imageWidth >= imageHeight) {
            targetWidth = size;
            targetHeight = Math.round(size / aspectRatio);
        } else {
            targetHeight = size;
            targetWidth = Math.round(size * aspectRatio);
        }

        Bitmap resizedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resizedBitmap);
        canvas.drawColor(Color.BLACK);

        int left = (size - targetWidth) / 2;
        int top = (size - targetHeight) / 2;

        RectF targetRect = new RectF(left, top, left + targetWidth, top + targetHeight);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0, 0, imageWidth, imageHeight), targetRect, Matrix.ScaleToFit.FILL);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, matrix, paint);

        return resizedBitmap;
    }

    public static void writeToFile(String data, Context context) {
        try {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "myFile.txt";

            File file = new File(baseDir + File.separator + fileName);

            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write(data.getBytes());
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static Bitmap cropBitmapByRectF(Bitmap originalBitmap, RectF rectF) {
        // Calculate the integer values of the RectF
        int left = (int) rectF.left;
        int top = (int) rectF.top;
        int right = (int) rectF.right;
        int bottom = (int) rectF.bottom;

        // Create a cropped bitmap using the RectF coordinates
        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);

        return croppedBitmap;
    }

    public static Bitmap cropBitmapByRectFWithMargin(Bitmap originalBitmap, RectF rectF, float marginPercentage) {
        // Calculate the integer values of the RectF
        int left = (int) rectF.left;
        int top = (int) rectF.top;
        int right = (int) rectF.right;
        int bottom = (int) rectF.bottom;

        // Calculate margins based on the percentage of the width and height
        int widthMargin = (int) ((right - left) * marginPercentage);
        int heightMargin = (int) ((bottom - top) * marginPercentage);

        // Apply margins to the cropping dimensions
        left = Math.max(0, left - widthMargin);
        top = Math.max(0, top - heightMargin);
        right = Math.min(originalBitmap.getWidth(), right + widthMargin);
        bottom = Math.min(originalBitmap.getHeight(), bottom + heightMargin);

        // Create a cropped bitmap using the adjusted coordinates
        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);

        return croppedBitmap;
    }

    public static Bitmap drawRectanglesOnBitmap(Bitmap bitmap, List<Classifier.Recognition> rectangles) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); // Create a mutable copy of the bitmap
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED); // Set the color for drawing rectangles
        paint.setStyle(Paint.Style.STROKE); // Set the style to stroke
        paint.setStrokeWidth(5); // Set the stroke width for better visibility

        for (Classifier.Recognition rect : rectangles) {
            canvas.drawRect(rect.getLocation(), paint); // Draw each RectF on the canvas
        }

        return mutableBitmap;
    }

    public static int calculateDistance(String str1, String str2) {
        return calculateDistanceRecursive(str1, str1.length(), str2, str2.length());
    }

    private static int calculateDistanceRecursive(String str1, int len1, String str2, int len2) {
        // Base cases: if either string is empty,
        // return the length of the other string
        if (len1 == 0) {
            return len2;
        }
        if (len2 == 0) {
            return len1;
        }

        // If the last characters of the strings are equal,
        // No operation is required
        if (str1.charAt(len1 - 1) == str2.charAt(len2 - 1)) {
            return calculateDistanceRecursive(str1, len1 - 1, str2, len2 - 1);
        }

        // Calculate cost of three possible operations
        // Insertion, Deletion, and Substitution
        int insertionCost = calculateDistanceRecursive(str1, len1, str2, len2 - 1);
        int deletionCost = calculateDistanceRecursive(str1, len1 - 1, str2, len2);
        int substitutionCost = calculateDistanceRecursive(str1, len1 - 1, str2, len2 - 1);

        // Return minimum of the three
        // costs plus 1 (for the operation)
        return 1 + Math.min(Math.min(insertionCost, deletionCost), substitutionCost);
    }

    public static int distance(String plate1, String plate2) {
        int count = 0;

        if (!plate1.substring(2, plate1.length() - 5).equals(plate2.substring(2, plate2.length() - 5))) {
            count = 1;
        }
        count += calculateDistance(plate1.substring(0, 2), plate2.substring(0, 2)) +
                calculateDistance(plate1.substring(plate1.length() - 5), plate2.substring(plate2.length() - 5));

        return count;
    }

    public static String getPersianPlate(String latinPlate)
    {
        if (latinPlate.isEmpty())
            return "";
        String character = latinPlate.substring(2, latinPlate.length() - 5);

        return latinPlate.substring(latinPlate.length() - 5) + PlateLetter.convertPersianToEnglish(character) + latinPlate.substring(0, 2);
    }

    public  static RectF getRectangle(int width, int height, float percentage)
    {
        return new RectF((int)(width * percentage),
                (int)(height * percentage),
                (int)(width - width *percentage),
                (int)(height - height * percentage));
    }

}


