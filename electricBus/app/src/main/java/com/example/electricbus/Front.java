package com.example.electricbus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.electricbus.databinding.ActivityFrontBinding;
import com.example.electricbus.databinding.ActivityUserMainBinding;

public class Front extends AppCompatActivity {
    private ActivityFrontBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFrontBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.adminButton.setOnClickListener(new View.OnClickListener() {
          @Override
        public void onClick(View v) {
          startActivity(new Intent(getApplicationContext(),BusLogIn.class));
       }
      });


        binding.buttonSignIn.setOnClickListener(v->{
            if(isValidSignInDetails()){
                signIn();
           }
        });
    }

    private void signIn(){
        Intent intent = new Intent(getApplicationContext(),front1.class);
       // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private Boolean isValidSignInDetails(){
        if(binding.inputMobile.getText().toString().trim().isEmpty()){
            showToast("enter mobile no");
            return false;
        }else if(binding.inputMobile.getText().toString().length()>10){
            showToast("enter valid mobile no");
            return false;
        }else if(binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        }else{
            return true;
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
}