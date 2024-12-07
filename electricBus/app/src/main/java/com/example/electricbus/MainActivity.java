package com.example.electricbus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.example.electricbus.databinding.ActivityMainBinding;
import com.example.electricbus.models.Bus;
import com.example.electricbus.models.Constants;
import com.example.electricbus.models.Station;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiActivity;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
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

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,GoogleMap.OnPolylineClickListener {

    boolean isPermissionGranted = false;
    ActivityMainBinding binding;


    GoogleMap mGoogleMap;

    private FirebaseFirestore database;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private Location lastLocation;
    private int GPS_REQUEST_CODE = 9001;
    String KEY_ID;
    private FusedLocationProviderClient mLocalClient;
    EditText editText;
    int passengerCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkMyPermission();
        KEY_ID = getIntent().getStringExtra(Constants.KEY_BUS_ID);
        Toast.makeText(this,KEY_ID,Toast.LENGTH_SHORT).show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.status.setKeyListener(null);

        String[] statusList =  {"forward","reverse","stop"};
        ArrayAdapter<String> statusAdap = new ArrayAdapter<>(getApplicationContext(),R.layout.drop_down_item,statusList);
        binding.status.setAdapter(statusAdap);


      /*  database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .document(KEY_ID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.status.setText(documentSnapshot.getString(Constants.status));
                        binding.passanger.setText(documentSnapshot.getString(Constants.passenger));
                    }
                });

       */
      //  binding.status.setText("stop");
        binding.passanger.setText("0");





        passengerCount = 0;
        binding.upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passengerCount = Integer.parseInt(binding.passanger.getText().toString());
                passengerCount+=1;
                binding.passanger.setText(""+passengerCount);
            }
        });

        binding.downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passengerCount = Integer.parseInt(binding.passanger.getText().toString());
                passengerCount-=1;
                binding.passanger.setText(""+passengerCount);
            }
        });





        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!binding.status.getText().toString().isEmpty()){

                    database = FirebaseFirestore.getInstance();
                    DocumentReference documentReference =
                            database.collection(Constants.KEY_COLLECTION_USER).document(KEY_ID);
                    documentReference.update(
                            Constants.passenger,binding.passanger.getText().toString(),
                            Constants.status,binding.status.getText().toString()
                    ).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        });



        initMap();

        mLocalClient = LocationServices.getFusedLocationProviderClient(this);

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
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        initStationData();
        //getCurLoc();








        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                Geocoder geocoder = new Geocoder(MainActivity.this);
                try {
                    Toast.makeText(MainActivity.this, "click", Toast.LENGTH_SHORT).show();

                    ArrayList<Address>  arrAdr = (ArrayList<Address>) geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);


                    database = FirebaseFirestore.getInstance();
                    DocumentReference documentReference =
                            database.collection(Constants.KEY_COLLECTION_USER).document(KEY_ID);
                    documentReference.update(
                            Constants.addressLine,arrAdr.get(0).getAddressLine(0),
                            Constants.latitude,arrAdr.get(0).getLatitude(),
                            Constants.longitude,arrAdr.get(0).getLongitude(),
                            Constants.adminArea,arrAdr.get(0).getAdminArea(),
                            Constants.featureName,arrAdr.get(0).getFeatureName(),
                            Constants.locality,arrAdr.get(0).getLocality(),
                            Constants.date,new Date()

                    ).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "failure", Toast.LENGTH_SHORT).show();
                        }
                    });

                   /* FirebaseFirestore database = FirebaseFirestore.getInstance();
                    HashMap<String, Object> busHashMap = new HashMap<>();
                    busHashMap.put(Constants.busId,"Bus04");
                    busHashMap.put(Constants.password,"1234");
                    busHashMap.put(Constants.addressLine,arrAdr.get(0).getAddressLine(0));
                    busHashMap.put(Constants.latitude,arrAdr.get(0).getLatitude());
                    busHashMap.put(Constants.longitude,arrAdr.get(0).getLongitude());
                    busHashMap.put(Constants.adminArea,arrAdr.get(0).getAdminArea());
                    busHashMap.put(Constants.featureName,arrAdr.get(0).getFeatureName());
                    busHashMap.put(Constants.locality,arrAdr.get(0).getLocality());
                    busHashMap.put(Constants.date,new Date());

                    database.collection(Constants.KEY_COLLECTION_USER)
                            .add(busHashMap)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(MainActivity.this, "update", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(exception -> {
                                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                            });


                    Log.d("Addr", arrAdr.get(0).getAddressLine(0));
                     */

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });







    }

    private void getCurLoc() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    private void checkMyPermission() {


        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
                Toast.makeText(MainActivity.this, "permission Granted", Toast.LENGTH_SHORT).show();

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
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        client.connect();
    }



/*
    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;
        if(currentLocationMarker != null){
            currentLocationMarker.remove();
        }
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

 */


    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        /*
        Toast.makeText(MainActivity.this, "click", Toast.LENGTH_SHORT).show();
        int midPoint = (int)Math.floor(polyline.getPoints().size()/2);
        for(int a =0;)




        LatLng latLng = polyline.getPoints().get(0);
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {


            ArrayList<Address>  arrAdr = (ArrayList<Address>) geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);

            database = FirebaseFirestore.getInstance();
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USER).document(KEY_ID);
            documentReference.update(
                    Constants.addressLine,arrAdr.get(0).getAddressLine(0),
                    Constants.latitude,arrAdr.get(0).getLatitude(),
                    Constants.longitude,arrAdr.get(0).getLongitude(),
                    Constants.adminArea,arrAdr.get(0).getAdminArea(),
                    Constants.featureName,arrAdr.get(0).getFeatureName(),
                    Constants.locality,arrAdr.get(0).getLocality(),
                    Constants.date,new Date()

            ).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "failure", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d("Addr", arrAdr.get(0).getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

    }

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
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }
/*
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
                    Station station = new Station();
                    station.name = documentChange.getDocument().getString(Constants.featureName);
                    station.latitude = la;
                    station.longitude = lo;
                    station.stationNo = Integer.parseInt(Objects.requireNonNull(documentChange.getDocument().getString("Station")));

                    MarkerOptions markerOptions = new MarkerOptions().position(busLoc).title(documentChange.getDocument().getString(Constants.featureName));
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    station.marker = mGoogleMap.addMarker(markerOptions);


                    stationList.add(station);

                      CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(busLoc, 18);
                     mGoogleMap.moveCamera(cameraUpdate);
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busLoc, 16f));
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    String id = documentChange.getDocument().getString(Constants.busId);

                }else if(documentChange.getType()== DocumentChange.Type.MODIFIED){

                    String id = documentChange.getDocument().getString(Constants.busId);

                }else if(documentChange.getType() == DocumentChange.Type.REMOVED){

                    String id = documentChange.getDocument().getString(Constants.busId);

                }
            }
        }
    };

 */


}
