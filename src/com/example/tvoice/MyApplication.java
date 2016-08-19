package com.example.tvoice;

import com.baidu.apistore.sdk.ApiStoreSDK;

import android.app.Application;

public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        ApiStoreSDK.init(this, "92e10d91b8c56e3698f2b2efb6f2a4c0");
    }
}
