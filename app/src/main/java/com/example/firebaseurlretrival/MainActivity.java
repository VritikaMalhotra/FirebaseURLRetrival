package com.example.firebaseurlretrival;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    Button btnCaptureImage;
    static final int CAMERA_REQUEST_CODE = 1;
    private StorageReference storageReference;
    String currentPhotoPath;
    TextView textView;
    StorageReference image1;
    ProgressDialog progressDialog;
    EditText editText;
    ImageView imageView1;
    Uri uri1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCaptureImage = (Button)findViewById(R.id.btn_captureImage);
        storageReference = FirebaseStorage.getInstance().getReference();
        textView = (TextView)findViewById(R.id.textView);
        progressDialog = new ProgressDialog(this);
        editText = (EditText)findViewById(R.id.editText);
        imageView1 = (ImageView)findViewById(R.id.imageView);
    }

    public void UploadImage1(View view) {

        dispatchTakePictureIntent();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE)
        {
            /*Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            imageView1.setImageBitmap(bitmap);*/
            this.imageView1.setImageURI(uri1);

            if(resultCode == Activity.RESULT_OK)
            {

                File f = new File(currentPhotoPath);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                uploadImageToFirebase(f.getName(),contentUri);
            }
        }
    }

    private void uploadImageToFirebase(String name, Uri contentUri) {
        progressDialog.setMessage("Uploading Image...");
        progressDialog.show();
        image1 = storageReference.child("images/" + name);
        textView.setText(contentUri.toString());
        image1.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                progressDialog.dismiss();
                image1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        progressDialog.setMessage("URL uploading in Realtime Database...");
                        progressDialog.show();

                        textView.setText("URL : "+uri.toString());
                        DatabaseReference imagestore = FirebaseDatabase.getInstance().getReference().child("Images");
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put(editText.getText().toString(),String.valueOf(uri));

                        imagestore.updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "URL uploaded in Database", Toast.LENGTH_LONG).show();

                            }
                        });

                    }
                });



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(MainActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
            }
        });

    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                uri1 = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
                takePictureIntent.setType("image/*");
            }
        }
    }

}