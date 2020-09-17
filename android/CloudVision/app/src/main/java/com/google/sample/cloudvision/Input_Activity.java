package com.google.sample.cloudvision;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Input_Activity extends AppCompatActivity {
    public Intent b_intent, n_intent;
    public Button back, next;
    public EditText empty_bs_et, meal_bs_et, wegiht_et;
    public static int empty_bs, meal_bs, weight = 0;
    public static int standard_weight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_activity);

        empty_bs_et = findViewById(R.id.editText1);
        meal_bs_et = findViewById(R.id.editText2);
        wegiht_et = findViewById(R.id.editText3);

        back = findViewById(R.id.back);
        next = findViewById(R.id.next);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                b_intent  = new Intent(Input_Activity.this, First_Activity.class);
                startActivity(b_intent);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                n_intent = new Intent(Input_Activity.this, MainActivity.class);
                startActivity(n_intent);
            }
        });


    }
}
