package android.kepoo.com.remoteprint_app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    Button btnConnect,btnDisconnect,btnPrint;
    EditText etTextBox;
    TextView lblPrinterName;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte[] readBufferProcess;
    int readBufferPosition;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnPrint = findViewById(R.id.btn_print);

        etTextBox = findViewById(R.id.input_txt);

        lblPrinterName = findViewById(R.id.tv_printer_name);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TraceBluetoothDevice();
                    accessBluetoothPrinter();

                }catch (Exception io){
                    io.printStackTrace();
                }
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    disconnectBlueTooth();
                }catch (Exception io){
                    io.printStackTrace();
                }
            }
        });

        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    printData();
                    Log.e("tag","wkwkwkwkwkwk");
                }catch (Exception io){
                    io.printStackTrace();
                }
            }
        });
    }



    void TraceBluetoothDevice(){
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null){
                lblPrinterName.setText("No Bluetooth Adapter Found");
            }
            if (bluetoothAdapter.isEnabled()){
                Intent enableSign = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableSign,0);
            }

            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();

            if (pairedDevice.size()>0){
                for (BluetoothDevice pairedDev: pairedDevice){

                    if (pairedDev.getName().equals("BlueTooth Printer")){
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer Attached: "+pairedDev.getName());
                        break;
                    }
                }
            }

            lblPrinterName.setText("Bluetooth printer Attached");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    void accessBluetoothPrinter()throws IOException{
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            beginListenToData();
        }catch (Exception ex){

        }
    }

   void beginListenToData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBufferProcess = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while(!Thread.currentThread().isInterrupted() && !stopWorker){
                        try {
                             int byteAvailable = inputStream.available();
                             if (byteAvailable > 0){
                                 byte[] packetByte = new byte[byteAvailable];
                                 inputStream.read(packetByte);

                                 for (int i=0; i < byteAvailable; i++){
                                     byte b = packetByte[i];
                                     if (b == delimiter){
                                         byte[] encodedByte = new byte[readBufferPosition];
                                         System.arraycopy(
                                                 readBufferProcess,0,
                                                 encodedByte,0,
                                                 encodedByte.length
                                         );
                                         final String data = new String(encodedByte,"US-ASCII");
                                         readBufferPosition = 0;
                                         handler.post(new Runnable() {
                                             @Override
                                             public void run() {
                                                 lblPrinterName.setText(data);
                                             }
                                         });
                                     }else{
                                         readBufferProcess[readBufferPosition++]=b;
                                     }
                                 }
                             }
                        }catch (Exception e){
                            stopWorker = true;
                        }
                    }
                }
            });
            thread.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void printData()throws IOException{
        try {
            Log.e("tag","1");
            String msg = etTextBox.getText().toString();
            msg+="\n";
            Log.e("tag","2");
            //Log.e("tag",msg.getBytes().toString());
            outputStream.write(msg.getBytes());
            Log.e("tag","3");
            lblPrinterName.setText("Printing Text...");
            Log.e("tag","4");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void disconnectBlueTooth ()throws IOException{
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer Disconnected.");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
