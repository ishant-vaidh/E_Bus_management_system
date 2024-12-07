package com.example.electricbus;

import static com.example.electricbus.userMainActivity.busesList;

import static com.example.electricbus.userMainActivity.polylines;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;


import com.google.android.gms.maps.GoogleMap;
//import com.google.maps.android;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Object;
import java.util.List;

public class GetDirectionData extends AsyncTask<Object,String,String> {

    GoogleMap mMap;
    String url;
    String googleDirectionsData;
    String duration,distance;
    int i=-1;


    LatLng latLng;



    @Override
    protected String doInBackground(Object... objects) {
        mMap = (GoogleMap)objects[0];
        url = (String)objects[1];
        latLng = (LatLng) objects[2];

       if(!objects[3].toString().equals("station")) {
           i = Integer.parseInt((String) objects[3]);
       }

        DownloadUrl downloadUrl = new DownloadUrl();
        try{
            googleDirectionsData = downloadUrl.readUrl(url);
            Log.d("deepesh",googleDirectionsData.toString());

        }catch (IOException e){
            e.printStackTrace();
        }

        return googleDirectionsData;
    }

    @Override
    protected void onPostExecute(String s){

        HashMap<String, String> directionList = null;
        String[] polyLineList;
        DataParser parser = new DataParser();
        directionList = parser.parseDirections(s);
        polyLineList = parser.parsePolyLine(s);
        displayDirection(polyLineList);


        duration = directionList.get("duration");
        distance = directionList.get("distance");


        if(i!=-1) {
            busesList.get(i).marker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("duration ="+duration);
        markerOptions.snippet("Distance ="+distance);

        if(i!=-1) {
            busesList.get(i).marker = mMap.addMarker(markerOptions);

        }
    }
    public void displayDirection(String[] polyLineLise){
        int count = polyLineLise.length;

        Log.d("devesh", Arrays.toString(polyLineLise));


        for(int i=0;i<count;i++){
            PolylineOptions options = new PolylineOptions();
            options.color(Color.RED);
            options.width(10);
            options.addAll(PolyUtil.decode(polyLineLise[i]));
            polylines.add(mMap.addPolyline(options));
          // polyline =  mMap.addPolyline(options);
        }



    }
}
