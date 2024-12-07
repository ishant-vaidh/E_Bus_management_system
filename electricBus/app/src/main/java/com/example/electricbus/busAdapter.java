package com.example.electricbus;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.electricbus.databinding.ItemContainerBusDetailsBinding;
import com.example.electricbus.models.Bus;
import com.example.electricbus.models.Constants;

import java.util.List;

public class busAdapter extends RecyclerView.Adapter<busAdapter.ViewHolder> {
    private final List<Bus> buses;
    Context context;

    public busAdapter(List<Bus> buses,Context context) {
        this.buses = buses;
        this.context = context;
    }

    @NonNull
    @Override
    public busAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerBusDetailsBinding itemContainerBusDetailsBinding = ItemContainerBusDetailsBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ViewHolder(itemContainerBusDetailsBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull busAdapter.ViewHolder holder, int position) {
        holder.setBusesData(buses.get(position));
    }

    @Override
    public int getItemCount() {
        return buses.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        ItemContainerBusDetailsBinding binding;

        public ViewHolder(ItemContainerBusDetailsBinding itemContainerBusDetailsBinding) {
            super(itemContainerBusDetailsBinding.getRoot());
            binding = itemContainerBusDetailsBinding;
        }
        void setBusesData(Bus bus){
            binding.busID.setText(bus.Id);
            binding.status.setText(bus.status);
            binding.available.setText(bus.available);
            String abc = String.valueOf(bus.available);
            binding.fromTo.setText(bus.fromTo);
            if(!bus.status.equals("unavailable")) {
                binding.cardViewOFMatchFrag.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, userMainActivity.class);
                        intent.putExtra(Constants.KEY_BUS_ID, bus.KeyId);
                        intent.putExtra("avail", abc);
                        context.startActivity(intent);
                    }
                });
            }
        }
    }
}
