package com.example.electricbus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.electricbus.databinding.ActivityFront1Binding;

public class front1 extends AppCompatActivity {
    ActivityFront1Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFront1Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.findbusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(front1.this,Front2.class));
            }
        });
    }
}