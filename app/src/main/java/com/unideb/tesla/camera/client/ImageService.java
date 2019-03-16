package com.unideb.tesla.camera.client;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface ImageService {

    @Multipart
    @PUT("api/image")
    Call<ResponseBody> upload(
            @Part MultipartBody.Part file,
            @Part("timestamp") double timestamp,
            @Part("longitude") double longitude,
            @Part("latitude") double latitude,
            @Part("mac") String mac
    );

}
