package com.example.electricbus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.electricbus.databinding.ActivityFront2Binding;
import com.example.electricbus.models.Bus;
import com.example.electricbus.models.Constants;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Front2 extends AppCompatActivity {
    ActivityFront2Binding binding;
    private List<Bus> busList;
    private busAdapter busAdap;
    private FirebaseFirestore database;

    public static int selectedStation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFront2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        busList = new ArrayList<>();
        busAdap = new busAdapter(busList,Front2.this);
        binding.recyclerView.setAdapter(busAdap);


    }

    @Override
    protected void onStart() {
        super.onStart();
        getBuses();
        binding.refreshLayoutHome.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                busList.clear();
                getBuses();
                Log.d("ramji",busList.toString());
            }
        });
    }
    private void getBuses(){
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null
                                && task.getResult().getDocumentChanges().size()>0){
                            for(int i=0;i<task.getResult().getDocumentChanges().size();i++){
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);

                                Bus bus = new Bus();
                                bus.KeyId = documentSnapshot.getId();
                                bus.Id = documentSnapshot.getString(Constants.busId);
                                String passStr = documentSnapshot.getString("passenger");
                                int passInt = Integer.parseInt(passStr);
                                passInt = 50-passInt;
                                bus.available = String.valueOf(passInt);
                                String str = documentSnapshot.getString(Constants.status);
                                if(str.equals("reverse")){
                                    bus.fromTo = "Chirgaon To RailwayStation";
                                    binding.fromStation.setText("Chirgaon");
                                    binding.toStation.setText("Railway Station");
                                    bus.status = "available";
                                }else if(str.equals("forward")){
                                    bus.fromTo = "RailwayStation To Chirgaon";
                                    bus.status = "available";
                                    binding.fromStation.setText("Railway Staion");
                                    binding.toStation.setText("Chirgaon");
                                }else{
                                    bus.fromTo = "Bus is not available";
                                    bus.status = "unavailable";
                                }

                                busList.add(bus);
                                busAdap.notifyDataSetChanged();
                                binding.recyclerView.setVisibility(View.VISIBLE);


                            }
                        }
                    }
                });
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error!=null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType()==DocumentChange.Type.ADDED){

                    Bus bus = new Bus();
                    bus.KeyId = documentChange.getDocument().getId();
                    bus.Id = documentChange.getDocument().getString(Constants.busId);
                    bus.available = documentChange.getDocument().getString(Constants.available);
                    bus.status = documentChange.getDocument().getString(Constants.status);
                    bus.fromTo = documentChange.getDocument().getString(Constants.fromTo);
                    busList.add(bus);
                    busAdap.notifyDataSetChanged();
                    binding.recyclerView.setVisibility(View.VISIBLE);




                }else if(documentChange.getType()== DocumentChange.Type.MODIFIED){
                    for(int i=0;i<busList.size();i++){
                        if(busList.get(i).Id.equals(documentChange.getDocument().getString(Constants.busId))){

                        }
                    }

                   // String id = documentChange.getDocument().getString(Constants.busId);

                }else if(documentChange.getType() == DocumentChange.Type.REMOVED){

                   // String id = documentChange.getDocument().getString(Constants.busId);

                }
            }
        }
    };

    private void addBusLocation(String id){


    }
    private  void updateBusLocation(String id){

    }

}