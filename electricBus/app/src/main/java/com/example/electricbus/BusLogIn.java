package com.example.electricbus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.electricbus.databinding.ActivityBusLogInBinding;
import com.example.electricbus.models.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class BusLogIn extends AppCompatActivity {
    private ActivityBusLogInBinding binding;
    String idOfBus,pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backButton.setOnClickListener(v-> onBackPressed());
        String[] busIdList = {"Bus01","Bus02","Bus03","Bus04"};

        ArrayAdapter<String> adapterBusId = new ArrayAdapter<>(getApplicationContext(),R.layout.drop_down_item,busIdList);
        binding.busID.setAdapter(adapterBusId);
        binding.busID.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                  idOfBus = binding.busID.getText().toString();
            }
        });

        binding.continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isValidDetail()){
                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    database.collection(Constants.KEY_COLLECTION_USER)
                            .whereEqualTo(Constants.busId,binding.busID.getText().toString())
                            .whereEqualTo(Constants.password,binding.password.getText().toString())
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if(task.isSuccessful() && task.getResult() != null
                                            && task.getResult().getDocumentChanges().size()>0){
                                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                        intent.putExtra(Constants.KEY_BUS_ID,documentSnapshot.getId());
                                        startActivity(intent);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });
                }
                else{
                    Toast.makeText(getApplicationContext(), "fill valid detail", Toast.LENGTH_SHORT).show();
                }

            }
        });




    }
    public boolean isValidDetail(){
        if(binding.busID.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"enter busId",Toast.LENGTH_SHORT).show();
            return false;
        }else if(Objects.requireNonNull(binding.password.getText()).toString().trim().isEmpty()){
            Toast.makeText(this,"enter password",Toast.LENGTH_SHORT).show();
            return false;
        }else{
            return true;
        }
    }
}