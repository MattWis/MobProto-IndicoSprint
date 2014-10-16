package com.example.mwismer.indico_emotion;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by alexadkins on 10/6/14.
 */

public class MainFragment extends Fragment {
    private static final int REQUEST_CODE = 1;
    private Bitmap bitmap;
    private ImageView imageView;
//    private ImageView happyImage

    private HashMap<String, Integer> h = new HashMap<String, Integer>(){{
        put("Angry", R.drawable.ic_angry);
        put("Fear", R.drawable.ic_fear);
        put("Happy", R.drawable.ic_happy);
//        h.put("Launcher", R.drawable.ic_launcher);
        put("Neutral", R.drawable.ic_neutral);
        put("Sad", R.drawable.ic_sad);
        put("Surprise", R.drawable.ic_surprise);
    }};

    public MainFragment() {
    }

    View rootView;
    Context context;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        imageView = (ImageView) rootView.findViewById(R.id.result);
//        happyImage = (ImageView) rootView.findViewById(R.id.happy);
//        happyImage.setImageResource(R.drawable.ic_happy);


        final Button cam = (Button) rootView.findViewById(R.id.camera);
        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_CODE);
                } else {
                    Log.d(MainActivity.class.getSimpleName(), "Nulls in the places");
                }
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        context = activity;
        super.onAttach(activity);
    }

    public String getHighestEmotion(final JSONObject response) {
        double highestVal = 0;
        double currentVal = 0;
        String highest = "";
        for (String key : h.keySet()) {
            try {
                currentVal = response.getDouble(key);
                if (key.equals("Sad")) {
                    currentVal /= 10;
                }

                if (key.equals("Fear")) {
                    currentVal /= 5;
                }
                if (currentVal > highestVal) {
                    highestVal = currentVal;
                    highest = key;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return highest;
    }


    public void postJson(double[][] grayScale) {
        final String URL = "http://api.indico.io/fer";
        //Post params to be sent to the server
        HashMap<String, double[][]> params = new HashMap<String, double[][]>();

        params.put("face", grayScale);
        JsonObjectRequest req = new JsonObjectRequest(
                URL,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i(MainActivity.class.getSimpleName(), response.toString(4));
                            Log.i(MainActivity.class.getSimpleName(), response.getString("Happy"));
                            imageView.setImageResource(h.get(getHighestEmotion(response)));
//                            RelativeLayout.removeView());
//                            ((MainActivity) getActivity()).switchFragment(new ImageDisplay());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.e("Error: ", error.getMessage());
                    }
                }
        );

//      add the request object to the queue to be executed
        ApplicationController.getInstance().addToRequestQueue(req);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        InputStream stream = null;
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                // recycle unused bitmaps
                if (bitmap != null) {
                    bitmap.recycle();
                }
                stream = getActivity().getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                BitmapDrawable ob = new BitmapDrawable(bitmap);
//                imageView.setImageBitmap(bitmap);
                imageView.setRotation(90);
                imageView.setBackgroundDrawable(ob);
                imageView.setRotation(-90);

                // Making it square squashes the dimensions oddly. Source of error?
                bitmap = Bitmap.createScaledBitmap(bitmap, 48, 86, false);
                bitmap = Bitmap.createBitmap(bitmap, 0, 19, 48, 48, null, false);

//                imageView.setImageBitmap(bitmap);
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
            postJson(toGrayscale(bitmap));
        }
    }

    public double[][] toGrayscale(Bitmap img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[] pixels = new int[w * h];
        img.getPixels(pixels, 0, w, 0, 0, w, h);

        double[][] oddGrayscale = new double[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = pixels[i * w + j];
                double gray = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel);
                oddGrayscale[i][j] = 1 / gray;
            }
        }

        return oddGrayscale;
    }
}
