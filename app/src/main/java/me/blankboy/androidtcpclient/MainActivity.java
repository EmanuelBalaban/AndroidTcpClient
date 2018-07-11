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

import java.net.*;
import java.util.Date;
import java.util.Objects;

import me.blankboy.tcpclient.*;

public abstract class MainActivity extends AppCompatActivity implements ConnectionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Variables.IsServerConnected())
            Log("Connected to saved server " + Variables.PrimaryServer.UniqueIdentity());
    }

    @Override
    protected  void onPause(){
        super.onPause();
        Intent resultIntent = new Intent();
        //resultIntent.putExtra(...);  // put data that you want returned to activity A
        setResult(Activity.RESULT_OK, resultIntent);
    }

    @Override
    public void onMessageReceived(Message message, Connection sender){
        if (sender.equals(Variables.PrimaryServer)){
            Log("\n" + message.Text);
        }
    }
    @Override
    public void onDataReceived(byte[] data, Date time, Connection sender){

    }
    @Override
    public void onException(Exception ex, Connection sender){

    }
    @Override
    public void onLog(Log log, Connection sender){
        if (sender.equals(Variables.PrimaryServer)){
            Log("\n[" + String.valueOf(log.Type) + "] " + log.Text);
        }
    }
    @Override
    public void onLoginResponse(boolean IsLoggedIn, Connection sender){
        if (IsLoggedIn && sender.equals(Variables.PrimaryServer)){
            Variables.InitializeSecondaryServer(sender.Hostname, sender.Port, sender.Username, sender.Password);
        }
    }

    public  void onClick(View view){
        int id = view.getId();
        if (id == R.id.sendButton) {
            if (Variables.IsServerConnected()) {
                Log("\nCannot send message if not connected to server!");
                return;
            }

            final EditText messField = findViewById(R.id.messageEditText);
            final String message = messField.getText().toString();

            if (message.equals("")) {
                Log("\nPlease enter a message!");
                return;
            }

            Variables.PrimaryServer.SendMessage(message);
            messField.setText("");
            hideKeyboardFrom(this, messField);
            Log("\nYou: " + message);
        }
        if (id == R.id.connectButton){
            final String hostname = ((EditText) findViewById(R.id.ipEditText)).getText().toString();
            final Integer port = Integer.valueOf(((EditText) findViewById(R.id.portEditText)).getText().toString());

            ClearConsole();

            Log("Connecting to " + hostname + ":" + port + "...");

            Variables.PrimaryServer = new Connection(hostname, port);
            Variables.PrimaryServer.addListener(this);

            if (Variables.PrimaryServer.IsConnected()){
                Log(" Connected!");
            } else{

            }

            /*
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
            */
        }
        if (id == R.id.connectQRButton){
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan qr code displayed on server page to connect!");
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
    public void vibrate(int milliseconds) {
        ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(milliseconds);
    }
    @SuppressLint("SetTextI18n")
    public void Log(String text){
        final TextView console = findViewById(R.id.debugText);
        console.setText(console.getText() + text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                console.invalidate();
            }
        });
    }
    public  void ClearConsole(){
        final TextView console = findViewById(R.id.debugText);
        console.setText("");
    }
    void LaunchActivity(Class activity){
        startActivity(new Intent(this, activity));
    }
    public static void hideKeyboardFrom(Context context, View view) {
        ((InputMethodManager) Objects.requireNonNull(context.getSystemService(Activity.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
