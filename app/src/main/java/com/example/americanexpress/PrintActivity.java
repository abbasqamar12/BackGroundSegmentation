package com.example.americanexpress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class PrintActivity extends AppCompatActivity {


    ImageView imageToPrint;
    TextView txtRetake, txtPrint;
    private Uri imageUri;
    Bitmap imageBitmap,finalImageToPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        imageToPrint = findViewById(R.id.imgToPrint);
        txtPrint = findViewById(R.id.txtPrint);
        txtRetake = findViewById(R.id.txtRetake);

        Intent intent = getIntent();
        String image_path = intent.getStringExtra(MainActivity.sendImageToPrint);
        imageUri = Uri.parse(image_path);
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_watermark);
        try {
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap croppedBitmap = Bitmap.createBitmap(imageBitmap, 0, (int) (imageBitmap.getHeight() * 0.15), imageBitmap.getWidth(), (int) (imageBitmap.getHeight() * 0.75    ));
        //Bitmap croppedBitmap = Bitmap.createBitmap(imageBitmap, 0, (int) (imageBitmap.getHeight() * 0.2), imageBitmap.getWidth(), imageBitmap.getHeight());
        finalImageToPrint =  addWatermark(croppedBitmap, logoBitmap, 0.10f);
        // imageToPrint.setImageBitmap(finalImageToPrint);

        txtRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        txtPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printImage(finalImageToPrint);
            }
        });

    }


    public void printImage(Bitmap fileToPrint) {
        PrintHelper bitmapPrintHelper = new PrintHelper(PrintActivity.this);
        bitmapPrintHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        bitmapPrintHelper.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        bitmapPrintHelper.printBitmap("Testing Print", fileToPrint, new PrintHelper.OnPrintFinishCallback() {
            @Override
            public void onFinish() {
                resetJourney();
            }
        });
    }

    private void resetJourney() {
        Intent intent = new Intent(PrintActivity.this, TandCActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * Embeds an image watermark over a source image to produce
     * a watermarked one.
     *
     * @param source    The source image where watermark should be placed
     * @param watermark Watermark image to place
     * @param ratio     A float value < 1 to give the ratio of watermark's height to image's height,
     *                  try changing this from 0.20 to 0.60 to obtain right results
     */
    public Bitmap addWatermark(Bitmap source, Bitmap watermark, float ratio) {
        Canvas canvas;
        Paint paint;
        Bitmap bmp;
        Matrix matrix;
        RectF r;

        int width, height;
        float scale;

        Bitmap croppedBitmap = Bitmap.createBitmap(source, 0, (int) (source.getHeight() * 0.1), source.getWidth(), (int) (source.getHeight() * 0.9));

        imageToPrint.setImageBitmap(croppedBitmap);

        width = source.getWidth();
        height = source.getHeight();

        // Create the new bitmap
        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

        // Copy the original bitmap into the new one
        canvas = new Canvas(bmp);
        canvas.drawBitmap(source, 0, 0, paint);

        // Scale the watermark to be approximately to the ratio given of the source image height
        scale = (float) (((float) height * ratio) / (float) watermark.getHeight());
        Log.d("BITMAP","Height: "+height+" Ratio: "+ratio+" Watermark height: "+watermark.getHeight());
        Log.d("BITMAP","Scale size: "+scale);

        // Create the matrix
        matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Determine the post-scaled size of the watermark
        r = new RectF(0, 0, watermark.getWidth(), watermark.getHeight());
        matrix.mapRect(r);

        // Move the watermark to the bottom right corner
        //matrix.postTranslate(width - r.width(), height - r.height());
        matrix.postTranslate(width - r.width(), 0);

        // Draw the watermark
        canvas.drawBitmap(watermark, matrix, paint);

        return bmp;
    }
}