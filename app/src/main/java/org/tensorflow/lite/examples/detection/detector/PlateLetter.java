package org.tensorflow.lite.examples.detection.detector;

public class PlateLetter {
    private String persian;
    private String english;
    private int value;

    public static PlateLetter[] getPlateLetterArray() {
        PlateLetter[] plateLetters = {
                new PlateLetter("الف", "alef", 0),
                new PlateLetter("ب", "b", 1),
                new PlateLetter("ج", "j", 2),
                new PlateLetter("ل", "l", 3),
                new PlateLetter("م", "m", 4),
                new PlateLetter("ن", "n", 5),
                new PlateLetter("ق", "q", 6),
                new PlateLetter("و", "v", 7),
                new PlateLetter("ه", "h", 8),
                new PlateLetter("ی", "y", 9),
                new PlateLetter("د", "d", 10),
                new PlateLetter("س", "s", 11),
                new PlateLetter("ص", "sad", 12),
                new PlateLetter("معلول", "malol", 13),
                new PlateLetter("ت", "t", 14),
                new PlateLetter("ط", "ta", 15),
                new PlateLetter("ع", "ein", 16),
                new PlateLetter("D", "diplomat", 17),
                new PlateLetter("S", "siyasi", 18),
                new PlateLetter("پ", "p", 19),
                new PlateLetter("تشریفات", "tashrifat", 20),
                new PlateLetter("ث", "the", 21),
                new PlateLetter("ز", "ze", 22),
                new PlateLetter("ش", "she", 23),
                new PlateLetter("ف", "fe", 24),
                new PlateLetter("ک", "kaf", 25),
                new PlateLetter("گ", "gaf", 26),
                new PlateLetter("#", "#", 27)
        };

        return plateLetters;
    }

    public PlateLetter(String persian, String english, int value) {
        this.persian = persian;
        this.english = english;
        this.value = value;
    }

    public String getPersian() {
        return persian;
    }

    public String getEnglish() {
        return english;
    }

    public int getValue() {
        return value;
    }

    public static String convertPersianToEnglish(String input) {
        for (PlateLetter plateLetter : getPlateLetterArray()) {
            if (plateLetter.getEnglish().equals(input)) {
                return plateLetter.getPersian();
            }
        }

        return "";
    }

}
