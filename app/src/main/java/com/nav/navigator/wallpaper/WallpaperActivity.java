package com.nav.navigator.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;

public class WallpaperActivity extends AppCompatActivity {
    private ImageView imageView;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);

        imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(imageView);

        Button pickButton = new Button(this);
        pickButton.setText("Choose Photo / GIF from Phone");
        pickButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        layout.addView(pickButton);

        setContentView(layout);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                                Toast.makeText(this, "Wallpaper updated!", Toast.LENGTH_SHORT).show();
                            }
                            if (inputStream != null) inputStream.close();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}
