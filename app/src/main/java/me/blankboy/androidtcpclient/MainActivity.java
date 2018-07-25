package me.blankboy.androidtcpclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Objects;

import me.blankboy.extensions.Channel;
import me.blankboy.extensions.ChannelListener;
import me.blankboy.extensions.Message;
import me.blankboy.tcpclientv2.*;

public class MainActivity extends AppCompatActivity implements ChannelListener, LogReceiver, ConnectionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // Methods
    public  void onClick(View view){
        int id = view.getId();
        if (id == R.id.sendFile){
            if (Variables.Server == null || Variables.Server.Secondary == null || !Variables.Server.Secondary.IsConnected()) {
                Log("\nCannot send file if not connected to server!");
                return;
            }

            final EditText messField = findViewById(R.id.messageEditText);
            hideKeyboardFrom(this, messField);

            selectFile();
        }
        if (id == R.id.sendButton) {
            if (Variables.Server == null || Variables.Server.Primary == null || !Variables.Server.Primary.IsConnected()) {
                Log("\nCannot send message if not connected to server!");
                return;
            }

            final EditText messField = findViewById(R.id.messageEditText);
            final String message = messField.getText().toString();
            messField.setText("");
            hideKeyboardFrom(this, messField);

            if (message.equals("")) {
                Log("\nPlease enter a message!");
                return;
            }

            Variables.Server.SendMessage(message);
        }
        if (id == R.id.connectButton){
            final String hostname = ((EditText) findViewById(R.id.ipEditText)).getText().toString();
            final Integer port = Integer.valueOf(((EditText) findViewById(R.id.portEditText)).getText().toString());

            ClearConsole();

            Log("Connecting to " + hostname + ":" + port + "...");

            try {
                Variables.Server = new Channel(hostname, port, getContentResolver());
                Variables.Server.addListener(this);
                Variables.Server.Primary.addListener(this);
                Variables.Server.Console.addListener(this);
                Variables.Server.Primary.Console.addListener(this);
            } catch (Exception ex) {
                Log(" Unable to connect!");
            }
        }
        if (id == R.id.connectQRButton){
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan qr code displayed on server page to connect!");
            integrator.initiateScan();
        }
    }

    void selectFile(){
        Intent n = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        n.addCategory(Intent.CATEGORY_OPENABLE);
        n.setType("*/*");
        startActivityForResult(n, READ_REQUEST_CODE);
    }

    private static final int READ_REQUEST_CODE = 42;

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == READ_REQUEST_CODE) {
                if (intent != null) {
                    Uri result = intent.getData();
                    try {
                        Variables.Server.SendFile(result, this);
                    } catch (Exception e) {
                        Log("\n" + e.toString());
                    }
                    System.gc();
                }
            } else {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (scanResult != null) {
                    vibrate(250);
                    String result = scanResult.getContents();
                    Log("QRCode: " + result);
                    if (result.contains(":")) {
                        ((TextView) findViewById(R.id.ipEditText)).setText(result.split(":")[0]);
                        ((TextView) findViewById(R.id.portEditText)).setText(result.split(":")[1]);
                        onClick(findViewById(R.id.connectButton));
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void Log(String text){
        final String text2 = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView console = findViewById(R.id.debugText);
                console.setText(console.getText() + text2);
                console.invalidate();
            }
        });
    }
    public  void ClearConsole(){
        final TextView console = findViewById(R.id.debugText);
        console.setText("");
    }

    public void vibrate(int milliseconds) {
        ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(milliseconds);
    }

    public static void hideKeyboardFrom(Context context, View view) {
        ((InputMethodManager) Objects.requireNonNull(context.getSystemService(Activity.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // Channel stuff
    @Override
    public void onMessageReceived(Message message, Channel sender) {
        Log("\nServer: " + message.Text);
    }
    @Override
    public void onException(Exception ex, Channel sender) {
        Log("\n" + ex.toString());
    }

    @Override
    public void onLogReceived(Log log) {
        Log("\n" + log.toString("[{type}] {text}", Log.DateFormat));
    }

    // Connection Stuff
    @Override
    public void onDataReceived(DataPackage dataPackage, Connection sender) {

    }
    @Override
    public void onException(Exception ex, Connection sender) {

    }
    @Override
    public void onStatusChanged(StatusType newStatus, Connection sender) {

    }
}
/*
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import me.blankboy.tcpclient.*;

import static me.blankboy.androidtcpclient.Variables.*;

public class MainActivity extends AppCompatActivity implements ConnectionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //if (IsServerConnected()) Log("Connected to saved server " + PrimaryServer.UniqueIdentity());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onMessageReceived(Message message, Connection sender) {
        if (message.Text.startsWith("[QUERY_RESULT]")) {
            if (sender.LastQUERY != null && sender.LastQUERY.equalsIgnoreCase("[query]data_server")) {
                String queryResult = message.Text.substring("[QUERY_RESULT]".length());
                if (queryResult.contains(":")) {
                    String[] ar = queryResult.split(":");
                    if (ar.length >= 2) {
                        Log("\nReceived data server connection info!");
                        SecondaryServer = new Connection(ar[0], Integer.valueOf(ar[1]));
                        SecondaryServer.IsDataConnection = true;
                        SecondaryServer.Connect();
                        SecondaryServer.Login(FileUtils.getIPAddress(true), String.valueOf(sender.Socket.getLocalPort()), false);

                        //SecondaryServer.IsLoggedIn = true;
                        while (!SecondaryServer.IsLoggedIn) ;
                        Log("\nAuthenticated to data channel!");
                        SecondaryServer.addListener(this);
                    }
                }
            }
        } else if (message.Text.startsWith("[RESULT]")) {
            if (message.Text.equalsIgnoreCase("[RESULT]OK")) {
                if (lastCommand.startsWith("[COMMAND]RECEIVE:")) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                    if (sendSelectedFile) {
                        Log("\nSending file.");
                        sendSelectedFile = false;
                        byte[] data = readAllBytes(result);
                        SecondaryServer.Send(data);
                    }
                }
            }
        } else if (sender.equals(PrimaryServer)) Log("\n" + message.Text);
    }

    HashMap<Connection, String> Files = new HashMap<>();

    @Override
    public void onDataReceived(byte[] data, Date time, Connection sender) {
        if (sender.equals(SecondaryServer)) {
            try {
                boolean urgent = false;
                if (data.length <= SecondaryServer.MaximumUMSize) {
                    String UrgentMessage = new String(data);
                    if (UrgentMessage.startsWith("[COMMAND]RECEIVE:")) {
                        urgent = true;
                        String file = UrgentMessage.substring("[COMMAND]RECEIVE:".length());
                        if (file != "") {
                            createFolder(AppMainDirectory);
                            Files.put(sender, file);
                            Log("\nIncoming File: " + file);
                        }
                    }
                    if (UrgentMessage.equalsIgnoreCase("[RESULT]OK")) {
                        if (lastCommand.startsWith("[COMMAND]RECEIVE:")) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                //e.printStackTrace();
                            }
                            if (sendSelectedFile) {
                                Log("\nSending file.");
                                sendSelectedFile = false;
                                byte[] fileData = readAllBytes(result);
                                SecondaryServer.Send(fileData);
                                System.gc();
                            }
                        }
                    }
                }
                if (!urgent) {
                    if (Files.containsKey(sender)){
                        createFolder(AppMainDirectory);
                        String file = Files.get(sender);
                        FileOutputStream outputStream;
                        try {
                            File newFile = new File(new File(getFolderPath(AppMainDirectory)), file);
                            newFile.createNewFile();
                            outputStream = new FileOutputStream(newFile);
                            outputStream.write(data);
                            outputStream.close();

                            Log("\nFile '" + file + "' was successfully downloaded!");

                            Files.remove(sender);
                        } catch (Exception e) {
                            Log("\n" + e.toString());
                        }
                    }
                }
            }
            catch (Exception ex){
                Log("\n" + ex.toString());
            }
        }
    }

    @Override
    public void onException(Exception ex, Connection sender) {

    }

    @Override
    public void onLog(Log log, Connection sender) {
        if (sender.equals(PrimaryServer)) {
            Log("\n[" + String.valueOf(log.Type) + "] " + log.Text);
        }
    }

    @Override
    public void onLoginResponse(boolean IsLoggedIn, Connection sender) {
        if (IsLoggedIn && sender.equals(PrimaryServer)) {
            PrimaryServer.SendMessage("[QUERY]DATA_SERVER");
            // InitializeSecondaryServer(sender.Hostname, sender.Port, sender.Username, sender.Password);
        }
    }

    private static final int READ_REQUEST_CODE = 42;
    boolean sendSelectedFile = false;
    public  void onClick(View view){
        int id = view.getId();
        if (id == R.id.sendFile){
            if (PrimaryServer == null || !PrimaryServer.IsConnected()) {
                Log("\nCannot send file if not connected to server!");
                return;
            }

            final EditText messField = findViewById(R.id.messageEditText);
            hideKeyboardFrom(this, messField);

            sendSelectedFile = true;
            selectFile();
        }
        if (id == R.id.sendButton) {
            if (PrimaryServer == null || !PrimaryServer.IsConnected()) {
                Log("\nCannot send message if not connected to server!");
                return;
            }

            final EditText messField = findViewById(R.id.messageEditText);
            final String message = messField.getText().toString();
            messField.setText("");
            hideKeyboardFrom(this, messField);

            if (message.equals("")) {
                Log("\nPlease enter a message!");
                return;
            }

            boolean pool = false;
            if (message.equalsIgnoreCase("[send]")) {
                pool = true;
                if (result == null){
                    Log("\nFirst select a file to send!");
                    return;
                }
                byte[] data = readAllBytes(result);
                SecondaryServer.Send(data);
            }
            if (message.equalsIgnoreCase("[select]")){
                pool = true;
                selectFile();
            }

            if (!pool){
                PrimaryServer.SendMessage(message);
                Log("\nYou: " + message);
            }
        }
        if (id == R.id.connectButton){
            final String hostname = ((EditText) findViewById(R.id.ipEditText)).getText().toString();
            final Integer port = Integer.valueOf(((EditText) findViewById(R.id.portEditText)).getText().toString());

            ClearConsole();

            //Log("Connecting to " + hostname + ":" + port + "...");

            PrimaryServer = new Connection(hostname, port);
            PrimaryServer.addListener(this);
            PrimaryServer.Connect(hostname, port, 1000);

            if (PrimaryServer.IsConnected()){
                //Log(" Connected!");
                while (!PrimaryServer.IsWaitingForData) ;
                PrimaryServer.IsLoggedIn = true;
                PrimaryServer.SendMessage("[QUERY]DATA_SERVER");
                //LaunchActivity(Login.class);
            } else{
                Log("\nUnable to connect!");
            }
        }
        if (id == R.id.connectQRButton){
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan qr code displayed on server page to connect!");
            integrator.initiateScan();
        }
    }
    void selectFile(){
        Intent n = new Intent(Intent.ACTION_GET_CONTENT); //Intent.ACTION_OPEN_DOCUMENT
        n.addCategory(Intent.CATEGORY_OPENABLE);
        n.setType("* /*");
        startActivityForResult(n, READ_REQUEST_CODE);
    }


    Uri result = null;

    String lastCommand;
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == READ_REQUEST_CODE) {
                if (intent != null) {
                    result = intent.getData();
                    String filename = getFileName(this, result);
                    byte[] data = readAllBytes(result);
                    Log("\nSelected: " + filename);
                    lastCommand = "[COMMAND]RECEIVE:" + filename + ":" + getMD5Hash(data) + ":" + data.length;
                    SecondaryServer.SendMessage(lastCommand);
                    System.gc();
                }
            } else {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (scanResult != null) {
                    vibrate(250);
                    String result = scanResult.getContents();
                    Log("QRCode: " + result);
                    if (result.contains(":")) {
                        ((TextView) findViewById(R.id.ipEditText)).setText(result.split(":")[0]);
                        ((TextView) findViewById(R.id.portEditText)).setText(result.split(":")[1]);
                        onClick(findViewById(R.id.connectButton));
                    }
                }
            }
        }
    }

    public void vibrate(int milliseconds) {
        ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(milliseconds);
    }
    @SuppressLint("SetTextI18n")
    public void Log(String text){
        final String text2 = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView console = findViewById(R.id.debugText);
                console.setText(console.getText() + text2);
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
}*/
