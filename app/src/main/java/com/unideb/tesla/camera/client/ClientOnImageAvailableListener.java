package com.unideb.tesla.camera.client;

import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;

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

        // save temporary image
        File temporaryImage;
        try {
            temporaryImage = saveTemporaryImage(image);
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

    private File saveTemporaryImage(Image image) throws IOException {

        // generate random name for image
        String temporaryImageName = "tesla_" + UUID.randomUUID().toString() + ".jpg";

        // create file reference
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), temporaryImageName);

        // get image bytes
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // save image
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(bytes);

        // close streams
        fileOutputStream.close();

        return file;

    }

    private void putImage(final File image){

        // Uri fileUri = Uri.fromFile(image);
        RequestBody requestFile = RequestBody.create(MediaType.parse(MEDIA_TYPE_IMAGE_JPEG), image);

        MultipartBody.Part body = MultipartBody.Part.createFormData("image", image.getName(), requestFile);

        // TODO: replace mock data with correct data
        Call<ResponseBody> call = imageService.upload(body, 100, 50, 50, mac);

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
