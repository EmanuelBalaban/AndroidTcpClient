package me.blankboy.androidtcpclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.net.*;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //LaunchActivity(Banner.class);

        if (isServerLoggedIn){
            try {
                InetSocketAddress address = (InetSocketAddress) MainActivity.socket.getRemoteSocketAddress();
                Log("Connected to " + address.getHostString() + ":" + address.getPort() + " from last instance!");
            } catch (Exception ignored){

            }
        }
    }

    @Override
    protected  void onPause(){
        super.onPause();
        Intent resultIntent = new Intent();
        //resultIntent.putExtra(...);  // put data that you want returned to activity A
        setResult(Activity.RESULT_OK, resultIntent);
    }

    public static Socket socket;
    public static boolean isServerLoggedIn = false;

    public  void onClick(View view){
        int id = view.getId();
        if (id == R.id.sendButton){
            if (!isServerLoggedIn){
                return;
            }
            final EditText messField = findViewById(R.id.messageEditText);
            final String message = messField.getText().toString();
            if (socket == null || socket.isClosed() || socket.isOutputShutdown()){
                Log("\nCannot send message if not connected to server!");
                return;
            }
            if (message.equals("")) {
                Log("\nPlease enter a message!");
                return;
            }
            try {
                //socket.getOutputStream().write(message.getBytes());
                SendMessage(socket, message);
                messField.setText("");
                hideKeyboardFrom(this, messField);
                Log("\nYou: " + message);
            } catch (Exception ex) {
                Log("\nConnection with server might be down!");
            }
        }
        if (id == R.id.connectButton){
            Disconnect();
            final String hostname = ((EditText) findViewById(R.id.ipEditText)).getText().toString();
            final Integer port = Integer.valueOf(((EditText) findViewById(R.id.portEditText)).getText().toString());
            ClearConsole();
            Log("Connecting to " + hostname + ":" + port + "...");
            new Thread(new Runnable() {
                public void run(){
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(hostname, port), 1000);
                        Log(" Connected!");
                    } catch (SocketTimeoutException ex){
                        Log("\nUnable to connect! (SocketTimeoutException)");
                    } catch (UnknownHostException ex){
                        Log("\nUnable to connect! (UnknownHostException)");
                    } catch (IOException ex) {
                        Log("\nUnable to connect! (IOException)");
                    } catch (Exception ignored){

                    } finally {
                        if (socket.isConnected()) {
                            new Thread(new Runnable() {
                                public void run() {
                                    while (socket.isConnected()) {
                                        String message;
                                        try {
                                            if (socket.getInputStream().available() > 0) {
                                                message = ReadMessage(socket);
                                                if (!isServerLoggedIn) {
                                                    if (message.equalsIgnoreCase("[LOGIN_OK]")) {
                                                        isServerLoggedIn = true;
                                                        Log("\nLogged in successfully!");
                                                    } else if (message.startsWith("[LOGIN_REJECT]")) {
                                                        ActivityTimeout = true;
                                                        isServerLoggedIn = false;
                                                        Log("\nLogin request was rejected with status '" + message.substring("[LOGIN_REJECT]".length()) + "'");
                                                        Log("\nDisconnected from server.");
                                                        try {
                                                            socket.close();
                                                        }
                                                        catch (Exception ignored){

                                                        }
                                                    }
                                                }
                                                else Log("\nServer: " + message);
                                            }
                                        } catch (Exception ignored) {

                                        }
                                    }
                                }
                            }).start();
                            LaunchActivity(Login.class);
                        }
                    }
                }
            }).start();
        }
        if (id == R.id.connectQRButton){
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan qr code displayed on server page to connect!");
            //integrator.setBeepEnabled(true);
            //integrator.setOrientationLocked(true);
            integrator.initiateScan();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            vibrate(250);
            String result = scanResult.getContents();
            Log("QRCode: " + result);
            if (result.contains(":")){
                ((TextView)findViewById(R.id.ipEditText)).setText(result.split(":")[0]);
                ((TextView)findViewById(R.id.portEditText)).setText(result.split(":")[1]);
                onClick(findViewById(R.id.connectButton));
            }
        }
    }

    public static void Disconnect(){
        isServerLoggedIn = false;
        try {
            if (socket != null && socket.isConnected()) {
                SendMessage(socket, "[GOODBYE]");
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
        } catch (Exception ignored){

        }
    }
    public void vibrate(int milliseconds) {
        ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(milliseconds);
    }
    @SuppressLint("SetTextI18n")
    public void Log(String text){
        final TextView console = findViewById(R.id.debugText);
        console.setText(console.getText() + text);
    }
    public  void ClearConsole(){
        final TextView console = findViewById(R.id.debugText);
        console.setText("");
    }
    public static boolean ActivityTimeout = false;
    void LaunchActivity(Class activity){
        ActivityTimeout = false;
        startActivity(new Intent(this, activity));
    }
    public static void hideKeyboardFrom(Context context, View view) {
        ((InputMethodManager) Objects.requireNonNull(context.getSystemService(Activity.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public static void SendMessage(Socket socket, String msg)
    {
        try {
            while (socket.getInputStream().available() > 0) {
            }

            byte[] data = msg.getBytes();
            byte[] sizeinfo = new byte[4];

            sizeinfo[0] = (byte)data.length;
            sizeinfo[1] = (byte)(data.length >> 8);
            sizeinfo[2] = (byte)(data.length >> 16);
            sizeinfo[3] = (byte)(data.length >> 24);


            socket.getOutputStream().write(sizeinfo);
            socket.getOutputStream().write(data);
        } catch (Exception ignored){

        }
    }
    public static String ReadMessage(Socket socket)
    {
        byte[] sizeinfo = new byte[4];

        int totalread, currentread;

        try {
            currentread = totalread = socket.getInputStream().read(sizeinfo);

            while (totalread < sizeinfo.length && currentread > 0) {
                currentread = socket.getInputStream().read(sizeinfo, totalread, sizeinfo.length - totalread);


                totalread += currentread;
            }

            int messagesize = 0;

            messagesize |= sizeinfo[0];
            messagesize |= (((int) sizeinfo[1]) << 8);
            messagesize |= (((int) sizeinfo[2]) << 16);
            messagesize |= (((int) sizeinfo[3]) << 24);

            byte[] data = new byte[messagesize];

            totalread = 0;
            currentread = totalread = socket.getInputStream().read(data, totalread, data.length - totalread);

            while (totalread < messagesize && currentread > 0) {
                currentread = socket.getInputStream().read(data, totalread, data.length - totalread);
                totalread += currentread;
            }

            return new String(data, 0, totalread);
        } catch (Exception ex){
            return  "";
        }
    }
}
