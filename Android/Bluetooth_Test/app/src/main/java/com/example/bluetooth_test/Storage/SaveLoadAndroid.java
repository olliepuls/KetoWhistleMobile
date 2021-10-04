package com.example.bluetooth_test.Storage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class SaveLoadAndroid {
    public static int REQUEST_CODE = 3476;

    public static boolean save(Context context, String filename, Serializable data) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
        } catch (Exception e) {
            Log.e("SaveLoadAndroid", "save: ", e);
            return false;
        }
        return true;
    }

    public static Object load(Context context, String filename) {
        try {
            FileInputStream fileInputStream = context.openFileInput(filename);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            return objectInputStream.readObject();
        }catch (Exception e){
            Log.e("SaveLoadAndroid", "load: ", e);
        }
        return null;
    }


    public static boolean saveText(Context context, String filename, String data, boolean overwrite) {
        try{
            File file = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS
                    ), filename);
            file.setReadable(true, false);

            if(!file.exists()) file.createNewFile();

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), !overwrite);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(data);
            bw.close();
            fw.close();
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);

        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean checkStoragePermissions(Activity context){
        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            context.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
            return false;
        }
        return true;
    }

    public static boolean checkRequestPermissionsResult(int requestCode, int[] grantResults) {
                return (requestCode == REQUEST_CODE && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }
}
