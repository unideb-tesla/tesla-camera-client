package com.unideb.tesla.camera.client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DeviceService {

    @PUT("api/device")
    Call<DeviceRequest> put(@Body DeviceRequest deviceRequest);

    @POST("api/device")
    Call<ResponseBody> post(@Body DeviceRequest deviceRequest);

    @GET("api/device/{mac}")
    Call<DeviceRequest> get(@Path("mac") String mac);

}
