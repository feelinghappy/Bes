package com.iir_eq;

import android.app.Application;
import android.content.Context;

public class BesApplication extends Application{

	private  Context applicationContext;
	CrashExceptionHandler crashExceptionHandler ;
	@Override
	public void onCreate() {
		super.onCreate();
		applicationContext = getApplicationContext();
		crashExceptionHandler = CrashExceptionHandler.getInstance();
		crashExceptionHandler.init(applicationContext);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	
}
