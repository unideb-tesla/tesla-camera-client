package com.unideb.tesla.camera.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientOnImageAvailableListener implements ImageReader.OnImageAvailableListener {

    public static final String MEDIA_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final float ROTATION_90 = 90.0f;

    private ImageService imageService;
    private String mac;

    public ClientOnImageAvailableListener(ImageService imageService, String mac) {
        this.imageService = imageService;
        this.mac = mac;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {

        // acquire image
        Image image = reader.acquireLatestImage();

        // rotate image
        byte[] rotatedImage = rotateImage(image, ROTATION_90);

        // save temporary image
        File temporaryImage;
        try {
            temporaryImage = saveTemporaryImage(rotatedImage);
        } catch (IOException e) {
            e.printStackTrace();
            image.close();
            return;
        }

        // close image
        image.close();

        // POST image to the endpoint then delete
        putImage(temporaryImage);

    }

    private byte[] rotateImage(Image image, float rotation){

        // get image bytes
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // decode bytes to bitmap format
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // create rotation matrix
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);

        // rotate bitmap
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // get result as byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] result = byteArrayOutputStream.toByteArray();

        return result;

    }

    private File saveTemporaryImage(byte[] imageBytes) throws IOException {

        // generate random name for image
        String temporaryImageName = "tesla_" + UUID.randomUUID().toString() + ".jpg";

        // create file reference
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), temporaryImageName);

        // save image
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(imageBytes);

        // close streams
        fileOutputStream.close();

        return file;

    }

    private void putImage(final File image){

        // multipart data from image
        RequestBody requestFile = RequestBody.create(MediaType.parse(MEDIA_TYPE_IMAGE_JPEG), image);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", image.getName(), requestFile);

        // collect extra data
        double timestamp = (double) System.currentTimeMillis();
        double longitude = 999.999;
        double latitude = 999.999;

        if(MainActivity.CLIENT_LOCATION_LISTENER.getLocation() != null){
            longitude = MainActivity.CLIENT_LOCATION_LISTENER.getLocation().getLongitude();
            latitude = MainActivity.CLIENT_LOCATION_LISTENER.getLocation().getLatitude();
        }

        Call<ResponseBody> call = imageService.upload(body, timestamp, longitude, latitude, mac);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("POST", "SUCCESS!!!");
                image.delete();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("POST", "FAILED!!!");
                t.printStackTrace();
                image.delete();
            }
        });

    }

}
