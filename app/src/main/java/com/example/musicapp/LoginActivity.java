package com.example.musicapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText edtEmail, edtPassword;
    private Button btnLogin ;
    private TextView txtRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d("DEBUG", "LoginActivity onCreate chạy");


        mAuth = FirebaseAuth.getInstance();
        edtEmail = findViewById(R.id.id_editTextEmail);
        edtPassword = findViewById(R.id.id_editTextPassword);
        btnLogin = findViewById(R.id.id_btnLogin);
        txtRegister = findViewById(R.id.id_txtRegister);



        Intent intent = getIntent();
        String email = intent.getStringExtra("email");
        String password = intent.getStringExtra("password");

        if(email != null && password != null){
            edtEmail.setText(email);
            edtPassword.setText(password);
        } else {
            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {
                Log.d("DEBUG", "Tự động điền email và password: " + user.getEmail());
                edtEmail.setText(user.getEmail()); // Điền email

                // Lấy mật khẩu từ SharedPreferences (do Firebase không cung cấp)
                SharedPreferences sharedPref = getSharedPreferences("UserData", MODE_PRIVATE);
                String savedPassword = sharedPref.getString("password", "");
                edtPassword.setText(savedPassword); // Điền password nếu có lưu trước đó
            }
        }



        btnLogin.setOnClickListener(v -> loginUser());
        txtRegister.setOnClickListener(v -> {
            Intent intent1 = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent1);
        });
    }

    private void loginUser(){
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    Toast.makeText(getApplicationContext(), "Đăng nhập thành công! Chào : " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhạp thất bại! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
