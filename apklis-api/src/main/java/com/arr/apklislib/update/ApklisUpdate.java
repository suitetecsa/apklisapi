package com.arr.apklislib.update;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.arr.apklislib.update.callback.UpdateCallback;
import com.arr.apklislib.update.model.LastRelease;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApklisUpdate {
    private static final String TAG = "ApklisUpdate";
    private final APKLisService service;

    ApklisUpdate(APKLisService service) {
        this.service = service;
    }

    public void checkLastUpdate(String package_name, final UpdateCallback callback) {
        service.getPackageResponse(package_name)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getCount() == 0) {
                        callback.onError(new ApklisException("No existen apps publicadas con este nombre de paquete en apklis"));
                    } else {
                        callback.onLastUpdate(response.getResults().get(0).getLastRelease());
                    }
                }, throwable -> {
                    callback.onError((Exception) throwable);
                }).dispose();
    }

    public void hasAppUpdate(@NonNull Context context, UpdateCallback callback) {
        service.getPackageResponse(context.getPackageName())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getCount() == 0) {
                        callback.onError(new ApklisException("No existen apps publicadas con este nombre de paquete en apklis"));
                    } else {
                        LastRelease lastRelease = response.getResults().get(0).getLastRelease();
                        if (getCurrentVersionCode(context) < lastRelease.getVersionCode()) {
                            callback.onLastUpdate(lastRelease);
                        }
                    }
                }, throwable -> {
                    callback.onError((Exception) throwable);
                }).dispose();
    }

    private int getCurrentVersionCode(@NonNull Context context) {
        // Obtener el versionCode actual de la aplicación
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getCurrentVersionCode: " + e.getMessage(), e);
            ;
        }
        return 0;
    }

    public static class Builder {
        private APKLisService service;

        public Builder setService(APKLisService service) {
            this.service = service;
            return this;
        }

        public ApklisUpdate build() {
            if (service == null) {
                String API_URL = "https://api.apklis.cu/v2";
                service = new Retrofit.Builder()
                        .baseUrl(API_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build()
                        .create(APKLisService.class);
            }
            return new ApklisUpdate(service);
        }
    }
}
