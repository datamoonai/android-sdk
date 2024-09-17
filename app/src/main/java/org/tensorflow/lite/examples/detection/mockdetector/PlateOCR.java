package org.tensorflow.lite.examples.detection.mockdetector;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.examples.detection.detector.Classifier;
import org.tensorflow.lite.examples.detection.detector.Plate;
import org.tensorflow.lite.examples.detection.detector.PlateLetter;


import java.io.IOException;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PlateOCR {
    Comparator<Classifier.Recognition> comparator = new Comparator<Classifier.Recognition>() {
        @Override
        public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
            return Float.compare(o1.getLocation().left, o2.getLocation().left);
        }
    };

    Classifier.Recognition lastPlate = null;
    Long lastPlateChange = null;
    Random random = new Random();
    Integer lastId = 1;
    public PlateOCR(AssetManager assetManager) throws IOException
    {

    }
    Classifier.Recognition getLastPlate()
    {
        if (lastPlate == null)
            return null;

        Classifier.Recognition result = new Classifier.Recognition(lastPlate.getId(), lastPlate.getTitle(), lastPlate.getConfidence(), lastPlate.getLocation());
        result.setPlate(lastPlate.getPlate());

        return result;
    }

    Classifier.Recognition getRandomPlate()
    {
        if (lastPlateChange == null)
        {
            lastPlateChange = new Date().getTime() +2000;
        }

        if(new Date().getTime() < lastPlateChange)
            return getLastPlate();

        if (lastPlate == null)
        {
            int top = 400+random.nextInt(800);
            int left =400+random.nextInt(800);
            lastId += 1;
            lastPlate = new Classifier.Recognition(lastId.toString(),"plate", Math.min(0.5f + random.nextFloat(), 1f) , new RectF(left,top,left + 400,top +100));
            lastPlate.setPlate(new Plate(11 + random.nextInt(88), 111+random.nextInt(887), 11+random.nextInt(88), PlateLetter.getPlateLetterArray()[random.nextInt(27)].getEnglish()));
            //lastPlate.setPlate(new Plate(11, 111, 11, "alef"));
        }
        else
        {
            lastPlate = null;
        }

        lastPlateChange = new Date().getTime() + 1 + random.nextInt(2000);

        return getLastPlate();

    }
    public List<Classifier.Recognition> getPlates(Bitmap sourceBitmap, boolean singleFrame)
    {
        List<Classifier.Recognition> finalResults = new LinkedList<>();

        if(singleFrame)
        {
            int top = 400+random.nextInt(400);
            int left =400+random.nextInt(400);
            Classifier.Recognition result = new Classifier.Recognition(lastId.toString(),"plate", Math.min(0.5f + random.nextFloat(), 1f) , new RectF(left,top,left + 400,top +100));
            result.setPlate(new Plate(11 + random.nextInt(88), 111+random.nextInt(887), 11+random.nextInt(88), PlateLetter.getPlateLetterArray()[random.nextInt(27)].getEnglish()));
            finalResults.add(result);
        }
        else
        {
            Classifier.Recognition result = getRandomPlate();
            if(result != null)
                finalResults.add(result);
        }


        return finalResults;
    }


    public void setUseNNAPI(final boolean isChecked) {

    }


    public void setNumThreads(final int numThreads) {

    }

}
