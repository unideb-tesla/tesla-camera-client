package com.unideb.tesla.camera.client;

import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ClientOnImageAvailableListener implements ImageReader.OnImageAvailableListener {

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

        // POST image to the endpoint
        postImage(temporaryImage);

        // TODO: delete temporary image
        // temporaryImage.delete();

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

    // TODO: rename to PUT
    private void postImage(File image){

        Uri fileUri = Uri.fromFile(image);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), image);

        MultipartBody.Part body = MultipartBody.Part.createFormData("image", image.getName(), requestFile);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl("http://192.168.0.109:8080/")
                .build();

        ImageService imageService = retrofit.create(ImageService.class);
        Call<ResponseBody> call = imageService.upload(body, 100, 50, 50, "kekmac");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("POST", "SUCCESS!!!");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("POST", "FAILED!!!");
                t.printStackTrace();
            }
        });

    }

}
