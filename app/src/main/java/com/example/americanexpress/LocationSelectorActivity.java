package com.example.americanexpress;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LocationSelectorActivity extends AppCompatActivity implements View.OnClickListener {

    TextView txtHeading;
    ImageView imgLasVegas, imgNewYork, imgTexas, imgLosAngeles;

    public static String selectedCity = "CITY_INDEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selector);
        txtHeading = findViewById(R.id.txtSelectLocation);
        imgLasVegas = findViewById(R.id.imgLasVegas);
        imgNewYork = findViewById(R.id.imgNewYork);
        imgTexas = findViewById(R.id.imgTexas);
        imgLosAngeles = findViewById(R.id.imgLasAngeles);
        setOnClickListener();
        String text = "Select your favourite location<br>and be the part of <font color=\"#850F0F\">the american pride</font>";
        txtHeading.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);


    }

    private void setOnClickListener() {
        imgLasVegas.setOnClickListener(this);
        imgNewYork.setOnClickListener(this);
        imgTexas.setOnClickListener(this);
        imgLosAngeles.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgLasVegas) {
            moveToCameraScreen(0);
        } else if (id == R.id.imgNewYork) {
            moveToCameraScreen(1);
        } else if (id == R.id.imgTexas) {
            moveToCameraScreen(2);
        } else if (id == R.id.imgLasAngeles) {
            moveToCameraScreen(3);
        } else {
            Toast.makeText(this, "Clicked Wrong Button", Toast.LENGTH_LONG).show();
        }
    }

    private void moveToCameraScreen(int cityIndex) {
        Intent locationActivity = new Intent(this, MainActivity.class);
        locationActivity.putExtra(selectedCity,cityIndex);
        startActivity(locationActivity);
    }
}