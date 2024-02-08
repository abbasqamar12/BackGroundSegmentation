package com.example.americanexpress;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.americanexpress.databinding.ActivityTandCactivityBinding;

public class TandCActivity extends AppCompatActivity implements View.OnClickListener {

    TextView txtSubmit;
    CheckBox checkBoxTnC, checkBoxAgeRestriction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tand_cactivity);
        txtSubmit = findViewById(R.id.txtSubmit);
        checkBoxTnC = findViewById(R.id.chkBxTermAndConditions);
        checkBoxAgeRestriction = findViewById(R.id.chkBxLegalAge);
        setOnClickListener();
    }

    private void setOnClickListener() {
        txtSubmit.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.txtSubmit) {
            if (!checkBoxTnC.isChecked()) {
                Toast.makeText(this, getString(R.string.tAndC_not_checked), Toast.LENGTH_LONG).show();
            } else if (!checkBoxAgeRestriction.isChecked()) {
                Toast.makeText(this, getString(R.string.legal_not_checked), Toast.LENGTH_LONG).show();
            } else {
                Intent locationActivity = new Intent(this, LocationSelectorActivity.class);
                startActivity(locationActivity);
            }
        } else {
            Toast.makeText(this, "Clicked Wrong Button", Toast.LENGTH_LONG).show();
        }

    }

}