package com.project.sih.ambulancebookingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SetUpProfileActivity extends AppCompatActivity {

    TextView nameText;
    RadioGroup radioGroup;
    Button continueButton;
    String category = "user", name;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_profile);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.FILE_NAME_KEY), MODE_PRIVATE);

        nameText = findViewById(R.id.nameText);
        radioGroup = findViewById(R.id.radioGroup);
        continueButton = findViewById(R.id.continueButton);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if(checkedId == R.id.userRadioButton)
                    category = "user";
                else
                    category = "driver";
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name = nameText.getText().toString();
                if(name.length() < 3)
                    Toast.makeText(SetUpProfileActivity.this, "Name must be atleast 3 characters long", Toast.LENGTH_LONG)
                            .show();
                else {
                    // local data storage
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getResources().getString(R.string.NAME_KEY), name);
                    editor.putString(getResources().getString(R.string.CATEGORY_KEY), category);
                    editor.commit();

                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                    DatabaseReference databaseReference = firebaseDatabase.getReference();
                    String userId = sharedPreferences.getString(getResources().getString(R.string.USER_ID_KEY), "");
                    databaseReference.child("Users").child(userId).child("Name").setValue(name);
                    databaseReference.child("Users").child(userId).child("category").setValue(category);

                    Intent intent;

                    if(category.equals("user"))
                        intent = new Intent(SetUpProfileActivity.this, UserHome.class);
                    else
                        intent = new Intent(SetUpProfileActivity.this, SetUpProfileActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}
