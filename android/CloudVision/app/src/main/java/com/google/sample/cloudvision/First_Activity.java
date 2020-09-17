package com.google.sample.cloudvision;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class First_Activity extends AppCompatActivity {
    public Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_activity);

        start = findViewById(R.id.button);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(First_Activity.this, Input_Activity.class);
                startActivity(intent);
            }
        });
    }
}