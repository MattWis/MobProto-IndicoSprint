package com.example.mwismer.indico_emotion;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by alexadkins on 10/6/14.
 */

public class MainFragment extends Fragment {
    private static final int REQUEST_CODE = 1;
    private Bitmap bitmap;
    static TextView emotion;
    Camera mCamera;
    Timer timer;

    public MainFragment() {
    }

    View rootView;
    VideoView vidView;
    Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        vidView = (VideoView) rootView.findViewById(R.id.myVideo);
        emotion = (TextView) rootView.findViewById(R.id.emotion);

        /** CAMERA CODE **/
        mCamera = Camera.open(1);
        SurfaceView layout = (SurfaceView) rootView.findViewById(R.id.picBox);
        final SurfaceHolder holder = layout.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        });

        timer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        try {
                            if (!holder.isCreating()) {
                                takephoto();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.execute();
            }
        };


        String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
        Uri vidUri = Uri.parse(vidAddress);
        vidView.setVideoURI(vidUri);
        final Button play = (Button) rootView.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                play.setVisibility(View.GONE);
                vidView.start();

                timer.schedule(task, 5000, 5000);

            }
        });
        return rootView;
    }

    private void takephoto() throws IOException {
        mCamera.takePicture(
            null, null, null,
            new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                mCamera.startPreview();
                Log.i("DebugDebug", "Preview");
                if (bytes != null) {
                    Log.i("DebugDebug", bytes.length + "");

                    postJsonWithBytes(bytes);
                    Log.i("DebugDebug", "Done posting");
                } else {
                     Log.i("DebugDebug", "Null bytes?");
                }
            }
                     });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }

        if (mCamera != null) {
            mCamera.release();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        context = activity;
        super.onAttach(activity);
    }

    public void postJsonWithBytes(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        bitmap = Bitmap.createScaledBitmap(bitmap, 48, 86, false);
        bitmap = Bitmap.createBitmap(bitmap, 0, 19, 48, 48, null, false);
        postJson(toGrayscale(bitmap));
    }

    public static void postJson(double[][] grayScale) {
        final String URL = "http://api.indico.io/fer";
        //Post params to be sent to the server
        HashMap<String, double[][]> params = new HashMap<String, double[][]>();

        params.put("face", grayScale);

        Log.i("DebugDebug", "Sending to indico");
        JsonObjectRequest req = new JsonObjectRequest(
                URL,
                new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i("DebugDebug", "Got return from indico");
                            emotion.setText(getHighestEmotion(response));

                            Log.i(MainActivity.class.getSimpleName(), response.toString(4));
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

    private static HashMap<String, String> h = new HashMap<String, String>(){{
        put("Angry", "Angry");
        put("Fear", "Fear");
        put("Happy", "Happy");
        put("Neutral", "Neutral");
        put("Sad", "Sad");
        put("Surprise", "Surprise");
    }};

    public static String getHighestEmotion(final JSONObject response) {
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


