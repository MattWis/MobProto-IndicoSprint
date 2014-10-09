package com.example.mwismer.indico_emotion;

import com.google.gson.Gson;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.ImageView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    private final static String DEBUG_TAG = "PlaceholderFragment";
    private static final int REQUEST_CODE = 1;
    private Bitmap bitmap;
    private ImageView imageView;

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.result);

        final Button cam = (Button) rootView.findViewById(R.id.camera);
        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("HI", "Activity :" + imageView);
        InputStream stream = null;
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                // recycle unused bitmaps
                if (bitmap != null) {
                    bitmap.recycle();
                }
                stream = getActivity().getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);

                // Making it square squashes the dimensions oddly. Source of error?
                bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, false);

                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d("SUP", "why oh why");

            int[] pixels = new int[(48 * 48)];
            bitmap.getPixels(pixels, 0, 48, 0, 0, 48, 48);

            Log.d("SUP", toGrayscaleJSON(bitmap));
        }
    }

    public String toGrayscaleJSON(Bitmap img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        double[][] oddGrayscale = new double[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = pixels[i * w + j];
                double gray = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel);
                oddGrayscale[i][j] = 1 / gray;
            }
        }

        return (new Gson()).toJson(oddGrayscale);
    }
}
