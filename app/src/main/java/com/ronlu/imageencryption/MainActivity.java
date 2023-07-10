package com.ronlu.imageencryption;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ImageView main_IMG_image;
    private TextInputEditText main_EDT_message;
    private TextInputEditText main_LBL_decryptedMessage;
    private MaterialButton main_BTN_gallery;
    private MaterialButton main_BTN_save;
    private AppCompatButton main_BTN_encrypt;
    private AppCompatButton main_BTN_decrypt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }

    private void findViews() {
        main_IMG_image = findViewById(R.id.main_IMG_image);
        main_EDT_message = findViewById(R.id.main_EDT_message);
        main_LBL_decryptedMessage = findViewById(R.id.main_LBL_decryptedMessage);
        main_BTN_gallery = findViewById(R.id.main_BTN_gallery);
        main_BTN_save = findViewById(R.id.main_BTN_save);
        main_BTN_encrypt = findViewById(R.id.main_BTN_encrypt);
        main_BTN_decrypt = findViewById(R.id.main_BTN_decrypt);
    }

    private void initViews() {
        main_BTN_gallery.setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE));
        main_BTN_save.setOnClickListener(v -> saveToGallery());
        main_BTN_encrypt.setOnClickListener(v -> encrypt());
        main_BTN_decrypt.setOnClickListener(v -> decrypt());
    }

    private void saveToGallery() {
        Bitmap imageBitmap = getBitmapFromImage();
        if (imageBitmap == null) {
            Toast.makeText(this, "You must upload an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the system's current time to use it as the image's filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        // Define the directory and file name where the image will be saved
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/";
        String fileName = "IMG_" + timeStamp + ".png";

        File directoryPath = new File(directory);
        if (!directoryPath.exists()) {
            // Create the directory if it doesn't exist
            directoryPath.mkdirs();
        }

        File file = new File(directoryPath, fileName);
        OutputStream outStream;
        try {
            // Compress the bitmap into a PNG file format and save it to the specified location
            outStream = new FileOutputStream(file);
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            // Notify the MediaScanner about the new file so that it appears in the device's gallery
            MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/png"}, null);

            // Show a Toast message indicating that the image has been saved
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decrypt() {
        if(getBitmapFromImage() == null){
            Toast.makeText(this, "You need to choose an image first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String decryptedMessage = Steganography.extractText(getBitmapFromImage());
        if(!decryptedMessage.isEmpty()){
            main_LBL_decryptedMessage.setText(decryptedMessage);
            Toast.makeText(this, "Successes!", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this, "There Is No Hidden Message", Toast.LENGTH_SHORT).show();

    }
    private void encrypt() {
        if(getBitmapFromImage() != null && !Objects.requireNonNull(main_EDT_message.getText()).toString().isEmpty()){
            String message = main_EDT_message.getText().toString();
            main_IMG_image.setImageBitmap(Steganography.hideText(getBitmapFromImage(), message));
            Toast.makeText(this, "Successes!", Toast.LENGTH_SHORT).show();
        }else if(getBitmapFromImage() == null)
            Toast.makeText(this, "you must upload an image first!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "you must enter a message first!", Toast.LENGTH_SHORT).show();

    }
    private Bitmap getBitmapFromImage(){
        Drawable drawable = main_IMG_image.getDrawable();
        if(drawable==null)
            return null;
        // Convert the Drawable to a Bitmap
        Bitmap bitmap = null;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // If the Drawable is not a BitmapDrawable, create a new Bitmap and draw the Drawable onto it
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }
    private void permissionDenied() {
        Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
            showRationaleDialog();
        else
            showManuallyPermission();
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        openGalleryLauncher.launch(intent);
    }
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void showRationaleDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setTitle("Storage Permission required")
                .setMessage("To offer a comprehensive multimedia experience, we need permission to access and play videos from your device\\'s media library. Granting this permission will allow you to watch videos stored on your device within our app.")
                .setPositiveButton("Grant", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE))
                .setNegativeButton("Deny", (dialog, which) -> dialog.cancel());
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }
    private void showManuallyPermission() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setTitle("Storage Permission required")
                .setMessage("Don't ask again state. please grant permission manually")
                .setPositiveButton("Take Me There", (dialog, which) -> openAppSettings())
                .setNegativeButton("Deny", (dialog, which) -> dialog.cancel());
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }
    private void decodeIMG(Uri Img) {
        ImageDecoder.Source source = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            source = ImageDecoder.createSource(this.getContentResolver(),Img);
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,100, stream);
                main_IMG_image.setImageURI(Img);
                Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Toast.makeText(this, "Something Went Wrong, Please Try Again.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result)
                openGallery();
            else
                permissionDenied();
        }
    });
    private ActivityResultLauncher<Intent> openGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == RESULT_OK && result.getData().getData() != null ){
                Uri selectImage = result.getData().getData();
                decodeIMG(selectImage);
                main_IMG_image.setBackground(null);
            }
        }
    });
}