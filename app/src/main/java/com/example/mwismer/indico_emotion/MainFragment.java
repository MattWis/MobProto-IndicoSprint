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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private ImageView imageView;
    Camera mCamera;

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

        mCamera = getCameraInstance(1);
        Camera.Parameters Params = mCamera.getParameters();
//        CameraPreview prev = new CameraPreview(getActivity(), mCamera);
        SurfaceView layout = (SurfaceView) rootView.findViewById(R.id.picBox);
        try {
            mCamera.setPreviewDisplay(layout.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        String vidAddress = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
        Uri vidUri = Uri.parse(vidAddress);
        vidView.setVideoURI(vidUri);
        rootView.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vidView.start();
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
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
                                    takephoto();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.execute();

                    }
                };
                timer.schedule(task, 0, 5000);

            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        context = activity;
        super.onAttach(activity);
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
            postJson(toGrayscale(bitmap));
        }
    }

    public double[][] toGrayscale(Bitmap img) {
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

        return oddGrayscale;
    }

    public class CameraPreview extends SurfaceView implements
            SurfaceHolder.Callback {
        private static final String TAG = "Camera Preview";
        private SurfaceHolder mHolder;
        public Camera mCamera;

        @SuppressWarnings("deprecation")
        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mCamera.setDisplayOrientation(90);

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the
            // preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();

            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private void takephoto() throws IOException {
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                Log.i("DebugDebug", "I took a picture");
                mCamera.release();
            }
        });

//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
//            startActivityForResult(intent, REQUEST_CODE);
//        } else {
//            Log.d(MainActivity.class.getSimpleName(), "Nulls in the places");
//        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                c = Camera.open(cameraId);
            } else {
                c = Camera.open();
            }
        } catch (Exception e) {
            c = null;
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }
}


