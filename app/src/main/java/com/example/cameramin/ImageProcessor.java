package com.example.cameramin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageProcessor {
    public Bitmap removeMove(List<Bitmap> bmpArray) {
        try {
            List<List<Byte>> pixelArrays = getPixelArrays(bmpArray);
            Bitmap resultBitmap = bmpArray.get(0).copy(Bitmap.Config.ARGB_8888, true);

            for (int i = 0; i < resultBitmap.getHeight(); i++) {
                for (int j = 0; j < resultBitmap.getWidth(); j++) {
                    List<Byte> sortedPixelValues = sortFromLowestToHighest(pixelArrays.get(i));
                    resultBitmap.setPixel(j, i, sortedPixelValues.get(sortedPixelValues.size() / 2));
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
