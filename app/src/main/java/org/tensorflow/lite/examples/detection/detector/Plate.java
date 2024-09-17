package org.tensorflow.lite.examples.detection.detector;

public class Plate
{
    public int first;
    public int second;
    public int cityCode;
    public String characterEn;
    public String characterFa;


    public Plate(int first, int second, int cityCode, String characterEn)
    {
        this.first = first;
        this.second = second;
        this.cityCode = cityCode;
        this.characterEn = characterEn;
    }


    @Override
    public String toString() {
        if (characterEn == "X")
            return "";
        else
            return first+ characterEn + second + cityCode ;
    }
}
