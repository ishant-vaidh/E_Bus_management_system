package com.example.electricbus;

import static com.example.electricbus.userMainActivity.busesList;
import static com.example.electricbus.userMainActivity.polylines;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.electricbus.DataParser;
import com.example.electricbus.DownloadUrl;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.HashMap;

public class GetBusDirectionData extends AsyncTask<Object,String,String> {


    GoogleMap mMap;
    String url;
    String googleDirectionsData;
    public static String durationk,distancek;
    int i=-1;


    LatLng latLng;



    @Override
    protected String doInBackground(Object... objects) {
        mMap = (GoogleMap)objects[0];
        url = (String)objects[1];
        latLng = (LatLng) objects[2];



        DownloadUrl downloadUrl = new DownloadUrl();
        try{
            googleDirectionsData = downloadUrl.readUrl(url);
            Log.d("deep",googleDirectionsData.toString());

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
       // polyLineList = parser.parsePolyLine(s);
       // displayDirection(polyLineList);


        durationk = directionList.get("duration");
        distancek = directionList.get("distance");
        Log.d("ramji",durationk+" "+distancek);



         /*


        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("duration ="+duration);
        markerOptions.snippet("Distance ="+distance);


            busesList.get(i).marker = mMap.addMarker(markerOptions);

          */


    }
    public void displayDirection(String[] polyLineLise){
        int count = polyLineLise.length;


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
