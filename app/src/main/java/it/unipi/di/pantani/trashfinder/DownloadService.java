package it.unipi.di.pantani.trashfinder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class DownloadService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        //Start download process
        downloadFile();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void downloadFile() {
        //Logic to download the file.
    }
}