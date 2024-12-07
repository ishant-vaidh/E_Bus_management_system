package com.example.electricbus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.electricbus.models.Constants;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class setStation extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    boolean isPermissionGranted = false;
    List<Address> addressList= new ArrayList<>();
    EditText editText;



    GoogleMap mGoogleMap;

    private FirebaseFirestore database;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private Location lastLocation;
    private int GPS_REQUEST_CODE = 9001;
    String KEY_ID;
    private FusedLocationProviderClient mLocalClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_station);
        checkMyPermission();
        KEY_ID = getIntent().getStringExtra(Constants.KEY_BUS_ID);
        Toast.makeText(this,KEY_ID,Toast.LENGTH_SHORT).show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        editText = findViewById(R.id.count);
        editText.setText("1");

        Button button = findViewById(R.id.send);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData();
            }
        });




        initMap();

        mLocalClient = LocationServices.getFusedLocationProviderClient(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type here to search");

        MarkerOptions mo = new MarkerOptions();


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!query.equals("")){
                    Geocoder geocoder = new Geocoder(setStation.this);
                    try {
                        addressList = geocoder.getFromLocationName(query,1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    for(int i=0;i<addressList.size();i++){
                        Address myAddress = addressList.get(i);
                        LatLng latLng = new LatLng(myAddress.getLatitude(),myAddress.getLongitude());
                        mo.position(latLng);
                        mo.title("Your search result");
                        mGoogleMap.addMarker(mo);
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                    }

                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }


    private void sendData(){

        int lll = Integer.parseInt(editText.getText().toString());

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> busHashMap = new HashMap<>();


        ;
        busHashMap.put("Station",editText.getText().toString());
        busHashMap.put(Constants.addressLine,addressList.get(0).getAddressLine(0));
        busHashMap.put(Constants.latitude,addressList.get(0).getLatitude());
        busHashMap.put(Constants.longitude,addressList.get(0).getLongitude());
        busHashMap.put(Constants.adminArea,addressList.get(0).getAdminArea());
        busHashMap.put(Constants.featureName,addressList.get(0).getFeatureName());
        busHashMap.put(Constants.locality,addressList.get(0).getLocality());


        database.collection("BusStation")
                .add(busHashMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(setStation.this, "update", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(setStation.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
        lll+=1;
        editText.setText(""+lll);

    }

    private void initMap() {
        if (isPermissionGranted) {
            if (isGPSEnable()) {
                SupportMapFragment supportMapFragment = new SupportMapFragment();
                supportMapFragment.getMapAsync(this);
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(R.id.container, supportMapFragment);
                supportMapFragment.getMapAsync(this);
                ft.commit();
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
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
        getCurLoc();





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


        Dexter.withContext(this).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
                Toast.makeText(setStation.this, "permission Granted", Toast.LENGTH_SHORT).show();

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


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        // locationRequest.setPriority(LocationRequest.PARCELABLE_WRITE_RETURN_VALUE);
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
}