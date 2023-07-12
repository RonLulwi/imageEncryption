package com.ronlu.imageencryption;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
public abstract class Steganography {
    private static final int BITS_PER_BYTE = 8;
    public static Bitmap hideText(Bitmap originalImage, String text) {
        // Convert the text to binary representation
        String msg = text + ",";
        StringBuilder binaryText = new StringBuilder();
        for (char c : msg.toCharArray()) {
            String charBinary = Integer.toBinaryString(c);
            binaryText.append(String.format("%8s", charBinary).replace(' ', '0'));
        }

        // Make sure the image has enough capacity to store the text
        int imageCapacity = originalImage.getWidth() * originalImage.getHeight();
        int requiredCapacity = binaryText.length();
        if (requiredCapacity > imageCapacity) {
            throw new IllegalArgumentException("Text is too long to be hidden in the image.");
        }

        // Create a copy of the original image
        Bitmap modifiedImage = originalImage.copy(originalImage.getConfig(), true);

        // Set the LSB of each pixel's color channel to the corresponding bit of the text
        int textIndex = 0;
        for (int y = 0; y < modifiedImage.getHeight(); y++) {
            for (int x = 0; x < modifiedImage.getWidth(); x++) {
                int pixel = modifiedImage.getPixel(x, y);

                int alpha = Color.alpha(pixel);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Modify the LSB of each color channel
                if (textIndex < requiredCapacity) {
                    red = modifyLSB(red, binaryText.charAt(textIndex++));
                }
                if (textIndex < requiredCapacity) {
                    green = modifyLSB(green, binaryText.charAt(textIndex++));
                }
                if (textIndex < requiredCapacity) {
                    blue = modifyLSB(blue, binaryText.charAt(textIndex++));
                }

                int modifiedPixel = Color.argb(alpha, red, green, blue);
                modifiedImage.setPixel(x, y, modifiedPixel);
            }
        }
        return modifiedImage;
    }
    public static String extractText(Bitmap modifiedImage) {
        StringBuilder binaryText = new StringBuilder();

        for (int y = 0; y < modifiedImage.getHeight(); y++) {
            for (int x = 0; x < modifiedImage.getWidth(); x++) {
                int pixel = modifiedImage.getPixel(x, y);

                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Extract the LSB of each color channel
                binaryText.append(extractLSB(red));
                binaryText.append(extractLSB(green));
                binaryText.append(extractLSB(blue));
            }
        }

        StringBuilder extractedText = new StringBuilder();

        // Convert binary representation back to text
        for (int i = 0; i < binaryText.length(); i += BITS_PER_BYTE) {
            String charBinary = binaryText.substring(i, i + BITS_PER_BYTE);
            int extractedCharValue = Integer.parseInt(charBinary, 2);
            char extractedChar = (char) extractedCharValue;

            // Check for the end of the hidden message
            if (extractedChar == '\0') {
                break;
            }

            extractedText.append(extractedChar);
        }
        int i = extractedText.indexOf(",", 0);
        return extractedText.substring(0, i);

    }
    private static int modifyLSB(int channel, char bit) {
        if (bit == '0') {
            // Clear the LSB
            return channel & 0xFE;
        } else {
            // Set the LSB
            return channel | 0x01;
        }
    }
    private static int extractLSB(int channel) {
        return channel & 0x01;
    }
    public static void decodeIMG(Context context, Uri Img, ImageView imageView) {
        ImageDecoder.Source source = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            source = ImageDecoder.createSource(context.getContentResolver(),Img);
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,100, stream);
                imageView.setImageURI(Img);
                Toast.makeText(context, "Image Selected", Toast.LENGTH_SHORT).show();
            }catch (IOException e){
                Toast.makeText(context, "Something Went Wrong, Please Try Again.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

}