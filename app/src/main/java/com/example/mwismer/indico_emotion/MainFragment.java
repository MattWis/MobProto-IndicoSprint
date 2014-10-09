package com.example.mwismer.indico_emotion;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by alexadkins on 10/6/14.
 */

public class MainFragment extends Fragment {

    public MainFragment() {
    }

    View rootView;
    Context context;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        setupViews();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        context = activity;
        super.onAttach(activity);
    }


    public void postJson(String inputStr) {
        final String URL = "http://api.indico.io/political";
//        final String URL = "http://api.indico.io/fer";
//      Post params to be sent to the server
        HashMap<String, String> params = new HashMap<String, String>();

        params.put("text", inputStr);

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

    private void setupViews() {
        Button sendButton = (Button) rootView.findViewById(R.id.main_input_button);
        Log.i(MainActivity.class.getSimpleName(), "Made it here");
        sendButton.setOnClickListener(sendButtonListener());
        Log.i(MainActivity.class.getSimpleName(), "Made it there");
    }

    public View.OnClickListener sendButtonListener(){
        Log.i(MainActivity.class.getSimpleName(), "In sendButtonListener");
        // stuff to do when button is clicked
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = ((EditText) rootView.findViewById(R.id.main_input_entry));
                if (input.getText().toString().equals("")){
                    Toast.makeText(context, "You didn't type anything in!", Toast.LENGTH_SHORT).show();
                    return;
                }
                postJson(input.getText().toString());
                input.setText("");
            }
        };
    }
}
