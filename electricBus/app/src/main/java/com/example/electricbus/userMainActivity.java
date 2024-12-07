package com.example.electricbus;

import static com.example.electricbus.Front2.selectedStation;
import static com.example.electricbus.GetBusDirectionData.distancek;
import static com.example.electricbus.GetBusDirectionData.durationk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.electricbus.databinding.ActivityUserMainBinding;
import com.example.electricbus.models.Bus;
import com.example.electricbus.models.Constants;
import com.example.electricbus.GetBusDirectionData;
import com.example.electricbus.models.Station;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.http.Url;

public class userMainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener

{
    private ActivityUserMainBinding binding;
    boolean isPermissionGranted = false;
    GoogleMap mGoogleMap;


    public static List<Polyline> polylines= new ArrayList<>();

    private FirebaseFirestore database;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private ArrayList<Marker> markers;
    private Location lastLocation;
    private int GPS_REQUEST_CODE = 9001;

    String KEY_ID;
    private  double startLatitude,startLongitude,endLatitude,endLongitude;

    public static ArrayList<Bus> busesList;


    private FusedLocationProviderClient mLocalClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        markers = new ArrayList<>();
        busesList = new ArrayList<>();
        assert actionBar != null;
        actionBar.setTitle("Nearby Buses");
        actionBar.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.background_title));

       checkMyPermission();

       binding.click.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               binding.distance.setText(distancek);
               binding.time.setText(durationk);
               binding.available.setText(getIntent().getStringExtra("avail"));
           }
       });



        KEY_ID = getIntent().getStringExtra(Constants.KEY_BUS_ID);
        mLocalClient = LocationServices.getFusedLocationProviderClient(this);
        initMap();


    }



    private void initMap() {
        if (isPermissionGranted) {
            if (isGPSEnable()) {
                SupportMapFragment supportMapFragment = new SupportMapFragment();
                supportMapFragment.getMapAsync(this);
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(binding.container.getId(), supportMapFragment);
                supportMapFragment.getMapAsync(this);
                ft.commit();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        buildGoogleApiClient();
        mGoogleMap.setMyLocationEnabled(true);

        init();
     //   getCurLoc();
        initStationData();
        timeRemain();




        //getCurrentLocation




    }

    private void init() {


        busesList = new ArrayList<>();
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error!=null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType()==DocumentChange.Type.ADDED){
                    double la = (double) Objects.requireNonNull(documentChange.getDocument().get(Constants.latitude));
                    double lo = (double) Objects.requireNonNull(documentChange.getDocument().get(Constants.longitude));
                    LatLng busLoc = new LatLng(la,lo);
                    Bus bus = new Bus();
                    bus.Id = documentChange.getDocument().getString(Constants.busId);
                    bus.latitude = la;
                    bus.longitude = lo;

                    MarkerOptions markerOptions = new MarkerOptions().position(busLoc).title(documentChange.getDocument().getString(Constants.busId));
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    bus.marker = mGoogleMap.addMarker(markerOptions);

                    assert bus.marker != null;
                    bus.marker.setTag(busesList.size());
                    busesList.add(bus);

                  //  CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(busLoc, 18);
                   // mGoogleMap.moveCamera(cameraUpdate);
                   //mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busLoc, 16f));
                    //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    // String id = documentChange.getDocument().getString(Constants.busId);

                }else if(documentChange.getType()== DocumentChange.Type.MODIFIED){
                    for(int i=0;i<busesList.size();i++){
                        if(busesList.get(i).Id.equals(documentChange.getDocument().getString(Constants.busId))){
                            if(busesList.get(i).marker != null){
                                busesList.get(i).marker.remove();
                            }
                            double la = (double) Objects.requireNonNull(documentChange.getDocument().get(Constants.latitude));
                            double lo = (double) Objects.requireNonNull(documentChange.getDocument().get(Constants.longitude));
                            LatLng busLoc = new LatLng(la,lo);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(busLoc);
                            markerOptions.title(documentChange.getDocument().getString(Constants.busId));
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                            busesList.get(i).marker = mGoogleMap.addMarker(markerOptions);

                        }
                    }

                    String id = documentChange.getDocument().getString(Constants.busId);

                }else if(documentChange.getType() == DocumentChange.Type.REMOVED){

                    String id = documentChange.getDocument().getString(Constants.busId);

                }
            }
        }
    };

    private void addBusLocation(String id){


    }
    private  void updateBusLocation(String id){

    }


    private void getCurLoc() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocalClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful()&&task.getResult()!=null) {
                Location location = task.getResult();
                gotoLocation(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void gotoLocation(double latitude, double longitude) {
        endLatitude = latitude;
        endLongitude = longitude;

        com.google.android.gms.maps.model.LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("your are here");
        mGoogleMap.addMarker(markerOptions);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
        mGoogleMap.moveCamera(cameraUpdate);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

    }



    private boolean isGPSEnable() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean provideEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (provideEnable) {
            return true;
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("GPS Permission")
                    .setMessage("GPS is required for this app to work, Please enable GPS")
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    }))
                    .setCancelable(false)
                    .show();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (providerEnable) {
                Toast.makeText(this, "GPS is enable", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "GPS is disable", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private boolean checkMyPermission() {

        Dexter.withContext(this).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
                Toast.makeText(userMainActivity.this, "permission Granted", Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                isPermissionGranted = false;
                startActivity(intent);

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();

            }
        }).check();

        if (isPermissionGranted) {
            if (client == null) {
                buildGoogleApiClient();
            }
        }

        return isPermissionGranted;
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        client.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this,"hi",Toast.LENGTH_SHORT).show();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
         locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        Toast.makeText(this,"hi2",Toast.LENGTH_SHORT).show();
        lastLocation = location;
        if(currentLocationMarker != null){
            currentLocationMarker.remove();
        }
        endLongitude = location.getLongitude();
        endLongitude = location.getLatitude();
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("You are here");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        currentLocationMarker = mGoogleMap.addMarker(markerOptions);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomBy(10));
        if(client!=null){
            LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        }

    }





    public void timeRemain(){
        //selectedBusId = getIntent().getStringExtra(Constants.KEY_BUS_ID);

        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .document(KEY_ID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        startLatitude = (double) documentSnapshot.get(Constants.latitude);
                        startLongitude = (double) documentSnapshot.get(Constants.longitude);
                        endLatitude = busesList.get(selectedStation).latitude;
                        endLongitude = busesList.get(selectedStation).longitude;
                        Object [] dataTransfer = new Object[4];

                        String url = getDirectionUrl();

                        GetBusDirectionData getBusDirectionData = new GetBusDirectionData();

                        dataTransfer[0] = mGoogleMap;
                        dataTransfer[1] = url;
                        dataTransfer[2] = new LatLng(startLatitude,startLongitude);
                        getBusDirectionData.execute(dataTransfer);


                    }
                });

    }

    /*
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
       startLatitude = marker.getPosition().latitude;
       startLongitude = marker.getPosition().longitude;

       if(marker.getTag()!=null){
           Object [] dataTransfer = new Object[4];

           String url = getDirectionUrl();

           GetBusDirectionData getBusDirectionData = new GetBusDirectionData();

           dataTransfer[0] = mGoogleMap;
           dataTransfer[1] = url;
           dataTransfer[2] = new LatLng(startLatitude,startLongitude);
           dataTransfer[3] = marker.getTag().toString();
           getBusDirectionData.execute(dataTransfer);

       }
        return false;
    }

     */


    public ArrayList<Station> stationList;
    int limit=0;
    private void initStationData(){
        if(limit==0){
            limit+=1;
        }else{
            return;
        }
        stationList = new ArrayList<>();
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_STATION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null
                                && task.getResult().getDocumentChanges().size()>0){
                            for(int i=0;i<task.getResult().getDocumentChanges().size();i++){
                                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);
                                double la = (double) Objects.requireNonNull(documentSnapshot.get(Constants.latitude));
                                double lo = (double) Objects.requireNonNull(documentSnapshot.get(Constants.longitude));
                                LatLng busLoc = new LatLng(la,lo);
                                Station station = new Station();
                                station.name = documentSnapshot.getString(Constants.featureName);
                                station.latitude = la;
                                station.longitude = lo;
                                station.stationNoString = documentSnapshot.getString("Station");
                                station.stationNo = Integer.parseInt(Objects.requireNonNull(documentSnapshot.getString("Station")));

                                MarkerOptions markerOptions = new MarkerOptions().position(busLoc).title(documentSnapshot.getString(Constants.featureName));
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                station.marker = mGoogleMap.addMarker(markerOptions);
                                station.marker.setTag("station");

                                stationList.add(station);

                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(busLoc, 18);
                                mGoogleMap.moveCamera(cameraUpdate);
                                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busLoc, 16f));
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                            }
                            Collections.sort(stationList,(obj1, obj2) -> obj1.stationNoString.compareTo(obj2.stationNoString));


                            for(int mm=0;mm<stationList.size()-1;mm++){
                                Log.d("stationList",stationList.get(mm).stationNoString);
                                Object [] dataTransfer = new Object[4];
                                String url = getPathUrl(stationList.get(mm).latitude,stationList.get(mm).longitude,stationList.get(mm+1).latitude,stationList.get(mm+1).longitude);
                                GetDirectionData getDirectionData = new GetDirectionData();
                                dataTransfer[0] = mGoogleMap;
                                dataTransfer[1] = url;
                                dataTransfer[2] = new LatLng(stationList.get(mm).latitude,stationList.get(mm).longitude);
                                dataTransfer[3] = stationList.get(mm).marker.getTag().toString();
                                getDirectionData.execute(dataTransfer);

                            }



                        }
                    }
                });
        //  .addSnapshotListener(eventListener);

    }

    private String getPathUrl(double sla,double slo,double ela,double elo){
        StringBuilder googleDirectionUrl = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        googleDirectionUrl.append("origin="+sla+","+slo);
        googleDirectionUrl.append("&destination="+ela+","+elo);
        googleDirectionUrl.append("&key="+"AIzaSyAf1hm8aXRGBs1NStze--fAWa1_2KDDeWs");
        return googleDirectionUrl.toString();


    }

    private String getDirectionUrl(){
        StringBuilder googleDirectionUrl = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        googleDirectionUrl.append("origin="+startLatitude+","+startLongitude);
        googleDirectionUrl.append("&destination="+endLatitude+","+endLongitude);
        googleDirectionUrl.append("&key="+"AIzaSyAf1hm8aXRGBs1NStze--fAWa1_2KDDeWs");
        return googleDirectionUrl.toString();


    }



}