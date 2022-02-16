package com.example.cameramin;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageProcessor {
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Bitmap removeMove(List<Bitmap> bmpArray) {
        try {
            int width = bmpArray.get(0).getWidth();
            int height = bmpArray.get(0).getHeight();
            Bitmap resultBitmap = bmpArray.get(0).copy(Bitmap.Config.ARGB_8888, true);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    List<Integer> pixelArray = new ArrayList<>();

                    int finalI = i;
                    int finalJ = j;
                    bmpArray.forEach((bmp) -> {
                        pixelArray.add(bmp.getPixel(finalI, finalJ));
                    });

                    List<Integer> sortedPixelArray = sortFromLowesToHighest(pixelArray);

                    int newPixelValue = Color.argb(255, sortedPixelArray.get(sortedPixelArray.size() / 2), sortedPixelArray.get(sortedPixelArray.size() / 2), sortedPixelArray.get(sortedPixelArray.size() / 2));
                    resultBitmap.setPixel(i, j, sortedPixelArray.get(sortedPixelArray.size() / 2));
                }
            }

            return resultBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<List<Byte>> getPixelArrays(List<Bitmap> bmpArray) {
        int width = bmpArray.get(0).getWidth();
        int height = bmpArray.get(0).getHeight();

        List<List<Byte>> pixelArrays = new ArrayList<>();
        for (int i = 0; i < (width * height); i++) {
            pixelArrays.add(new ArrayList<Byte>());
        }

        for (int i = 0; i < bmpArray.size(); i++) {
            Bitmap bmp = bmpArray.get(i);
            List<Byte> pixelValues = getPixelsValuesFromBitmap(bmp);

            for (int j = 0; j < pixelValues.size(); j++) {
                pixelArrays.get(j).add(pixelValues.get(j));
            }
        }

        return pixelArrays;
    }

    private List<Byte> sortFromLowestToHighest(List<Byte> arr) {
        Collections.sort(arr, new Comparator<Byte>()
        {
            @Override
            public int compare(Byte x, Byte y)
            {
                return x - y;
            }
        });

        return arr;
    }

    private List<Integer> sortFromLowesToHighest(List<Integer> arr) {
        Collections.sort(arr, new Comparator<Integer>()
        {
            @Override
            public int compare(Integer x, Integer y)
            {
                return x - y;
            }
        });

        return arr;
    }

    private List<Byte> getPixelsValuesFromBitmap(@NonNull Bitmap bitmap) {
        List<Byte> argbValues = new ArrayList<>();
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                byte pixel = (byte) bitmap.getPixel(i, j);
                argbValues.add(pixel);
            }
        }

        return argbValues;
    }
}
