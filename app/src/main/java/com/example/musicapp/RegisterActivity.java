package com.example.musicapp;

import android.content.Intent;
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
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText edtEmail, edtPassword , edtPasswordAgain;
    private Button btnRegister;

    boolean isNewUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);



        mAuth = FirebaseAuth.getInstance();
        edtEmail = findViewById(R.id.id_editTextEmail);
        edtPassword = findViewById(R.id.id_editTextPassword);
        edtPasswordAgain = findViewById(R.id.id_editTextPasswordAgain);
        btnRegister = findViewById(R.id.id_btnRegister);



        btnRegister.setOnClickListener(v -> registerUser());


    }
    private void registerUser(){
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String passwordAgain = edtPasswordAgain.getText().toString();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!password.equals(passwordAgain)){
            Toast.makeText(this,"Nhập lại mật khẩu không đúng" , Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Log.d("Register", "CreateUserWithEmail: successful");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveUserDataToFirestore(user, email, password);
                }
            } else {
                Log.e("Register", "Lỗi đăng ký: ", task.getException());
                Toast.makeText(RegisterActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserDataToFirestore(FirebaseUser user, String email, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("password", password);
        userData.put("isNewUser", true);

        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Register", "Dữ liệu lưu Firestore thành công!");
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Register", "Lỗi lưu dữ liệu Firestore", e);
                    Toast.makeText(RegisterActivity.this, "Không thể lưu dữ liệu vào Firestore!", Toast.LENGTH_SHORT).show();
                });
    }
}
