package com.example.milinguis;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText et;
    private InputStreamReader isr;
    private ObjectInputStream ois;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        et = (EditText) findViewById(R.id.editText);
        requestPermissionsNeeded();
    }

    private void requestPermissionsNeeded(){
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "Necesito permisos para descargar los archivos en su celular.", Toast.LENGTH_SHORT).show();

            requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }

        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "Necesito permisos para leer los archivos en su celular.", Toast.LENGTH_SHORT).show();

            requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, 2);
        }
    }

    public void connectToSocket(View v){
        String ipv4 = et.getText().toString();

        ClientRxThread clientRxThread = new ClientRxThread( ipv4, 1234);
        clientRxThread.start();
    }

    private class ClientRxThread extends Thread {
        String dstAddress;
        int dstPort;

        ClientRxThread(String address, int port) {
            dstAddress = address;
            dstPort = port;
        }

        private void downloadFiles() throws IOException{
            File file;
            List<Song> songLists;
            FileOutputStream fos = null;

            try {
                songLists = (List<Song>) ois.readObject();

                for(Song song : songLists){
                    file = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                            song.getName());
                    fos = new FileOutputStream(file);
                    fos.write(song.getBytesFile());
                }

            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if(fos!=null){
                    fos.close();
                }
            }

            // socket.close();

            MainActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "Descarga completa",
                            Toast.LENGTH_LONG).show();
                }});
        }

        @Override
        public void run() {
            Socket socket = null;
            String currentSongName = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                ois = new ObjectInputStream(socket.getInputStream());

                downloadFiles();

            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "No se pudo conectar con esa IP, intente nuevamente.";
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            } finally {
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        }
    }
}