package com.example.milinguis;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText et;
    private InputStreamReader inputStreamReader;
    private DataInputStream dataInputStream;
    private TextView pbLabelUp;
    private ProgressBar progressBar;
    private TextView pbLabelDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        et = (EditText) findViewById(R.id.editText);
        pbLabelUp = (TextView) findViewById(R.id.progressBarLabelUp);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        pbLabelDown = (TextView) findViewById(R.id.progressBarLabelDown);

        requestPermissionsNeeded();
    }

    private void requestPermissionsNeeded(){
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(MainActivity.this, "Necesito permisos para descargar los archivos en su celular.", Toast.LENGTH_SHORT).show();

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if(checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.INTERNET))
                Toast.makeText(MainActivity.this, "Necesito permisos para descargar los archivos en su celular.", Toast.LENGTH_SHORT).show();

            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
        }
    }

    public void connectToSocket(View v){
        String ipv4 = et.getText().toString();
        ClientRxThread clientRxThread = new ClientRxThread( ipv4, 1234);
        clientRxThread.start();
    }

    private void myLog(final String msg){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbLabelDown.setText(msg);
            }
        });
    }

    private void setCurrentProgress(final int progress){
        MainActivity.this.runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                progressBar.setProgress(progress, true);
                pbLabelUp.setText("Recibiendo " + progress + " %");
            }
        });
    }

    private class ClientRxThread extends Thread {
        String dstAddress;
        int dstPort;

        ClientRxThread(String address, int port) {
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            String currentSongName = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                int songQuantity = dataInputStream.readInt();

                myLog("Se descargarán " + songQuantity + " canciones.");

                for(int i=0; i<songQuantity; i++)
                    downloadFile();

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                    Toast.makeText(MainActivity.this,
                            "Descarga completa",
                            Toast.LENGTH_LONG).show();
                    }});

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

        private void downloadFile() throws IOException {
            String name = dataInputStream.readUTF();
            long length = dataInputStream.readLong();

            myLog(name);
            Log.i("length", ""+length);

            final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            File file = new File( path, name );
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] bytes = new byte[ (int) length ];
            int progress = 0;

            for(int i=0; i<length; i++) {
                bytes[i] = dataInputStream.readByte();
                if( i % 1000 == 0 )
                    setCurrentProgress( (int) (i*100/length));
            }

            setCurrentProgress( 100 );
            fileOutputStream.write(bytes);
            fileOutputStream.close();
            myLog("Descarga terminada exitosamente.");
        }
    }
}