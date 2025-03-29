package com.example.musicapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;
import android.database.Cursor;
import android.provider.MediaStore;
import android.net.Uri;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InformationActivity extends AppCompatActivity {

    private Firebase mAuth;
    private String imgPath;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    Uri fileUri;
    private EditText edtFullName, edtPhoneNumber;
    private Button btnSave , btnTakePhoto, btnSelectImage;
    private ImageView imgAvatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        imgAvatar = findViewById(R.id.imgAvatar);

        btnTakePhoto.setOnClickListener(v -> requestCameraPermission());
        btnSelectImage.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveInformation());
    }

    private void saveInformation(){
        String fullName = edtFullName.getText().toString().trim();
        String phoneNumber = edtPhoneNumber.getText().toString().trim();

        if (fullName.isEmpty()) {
            edtFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (phoneNumber.isEmpty()) {
            edtPhoneNumber.setError("Vui lòng nhập số điện thoại");
            return;
        }

        if (fileUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("user_images/" + user.getUid() + ".jpg");
        Log.d("FirebaseUpload", "imgPath: " + imgPath);
        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString(); // Lấy URL tải ảnh

                    Map<String, Object> additionalInfo = new HashMap<>();
                    additionalInfo.put("fullName", fullName);
                    additionalInfo.put("phoneNumber", phoneNumber);
                    additionalInfo.put("imgUrl", imageUrl); // Lưu URL ảnh thay vì đường dẫn

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("additionalInfo", additionalInfo);

                    db.collection("users").document(user.getUid())
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e("Firestore", "Lỗi khi lưu", e));

                })).addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải ảnh lên Firebase!", Toast.LENGTH_SHORT).show();
                    Log.e("FirebaseStorage", "Lỗi upload ảnh", e);
                });
    }

    // lưu đươgnf dẫn tuyệt
    private String getRealPathFromURI(Context context, Uri uri) {
        if (uri == null) return null;

        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return null;

            FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            File file = new File(context.getCacheDir(), "temp_image.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            pfd.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }




    // kiêmr tra và yêu cầu quyên ftruy cập camera trước khi chụp ảnh
    private void requestCameraPermission(){
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);

        }else {
            capturePicture();
        }
    }

    // sử lý khi người dùng cấp quyền camera
    public void onRequestCameraPermission(int requestCode , @Nonnull String[] permission, @Nonnull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                capturePicture();
            } else {
                Toast.makeText(this, "Không có quyền truy cập camera", Toast.LENGTH_SHORT).show();
                Log.e("CameraPermission","Không có quyền truy cập camera");
            }
        }
    }

    public void capturePicture(){
        if(isCameraSupport()){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            fileUri = getOutPutMediaFileUri();

            if(fileUri != null){
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "Không thể tạo file ảnh", Toast.LENGTH_SHORT).show();
                Log.e("CameraPermission","Không thể tạo file ảnh");
            }
        }
    }

    private Uri getOutPutMediaFileUri(){
        File file = getOutPutMediaFile();
        if(file == null){
            return null;
        }
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
    }

    private File getOutPutMediaFile(){
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CameraDemo");
        if(!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.e("CameraPermission","Không thể tạo thư mục lưu ảnh" + mediaStorageDir.getAbsolutePath());
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile = new File(mediaStorageDir, "IMG_" + timeStamp + ".jpg");

        try {
            if (mediaFile.createNewFile()) {
                Log.d("CameraDemo", "File ảnh đã được tạo: " + mediaFile.getAbsolutePath());
            } else {
                Log.e("CameraDemo", "Tạo file ảnh thất bại!");
            }
        } catch (Exception e) {
            Log.e("CameraDemo", "Lỗi khi tạo file ảnh: " + e.getMessage());
        }

        return mediaFile;
    }

    private boolean isCameraSupport(){
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }


    private void openGallery(){
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode == RESULT_OK ){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                if (fileUri != null) {
                    imgAvatar.setImageURI(null); // Để tránh lỗi cache ảnh
                    imgAvatar.setImageURI(fileUri);

                    // Cập nhật MediaStore để ảnh xuất hiện trong Gallery
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(fileUri);
                    sendBroadcast(mediaScanIntent);
                } else {
                    Log.e("CameraDemo", "fileUri null, không thể hiển thị ảnh!");
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                imgAvatar.setImageURI(data.getData());
            } else {
                Log.e("CameraDemo", "Chụp ảnh thất bại hoặc bị hủy!");
            }
        }

    }


}

