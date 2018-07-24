package me.blankboy.androidtcpclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
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

        if (IsServerConnected())
            Log("Connected to saved server " + PrimaryServer.UniqueIdentity());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent resultIntent = new Intent();
        //resultIntent.putExtra(...);  // put data that you want returned to activity A
        setResult(Activity.RESULT_OK, resultIntent);
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
                        //SecondaryServer.Login(sender.Username, sender.Password);
                        SecondaryServer.IsLoggedIn = true;
                        while (!SecondaryServer.IsLoggedIn) ;
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

    String getFolderPath(String fname){
        return Environment.getExternalStorageDirectory() + File.separator + fname;
    }
    public void createFolder(String fname) {
        String myfolder = getFolderPath(fname);
        File f = new File(myfolder);
        if (!f.exists())
            f.mkdir();
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
        n.setType("*/*");
        startActivityForResult(n, READ_REQUEST_CODE);
    }
    private byte[] readAllBytes(Uri uri) {
        byte[] result = null;
        try {
            InputStream iStream =   getContentResolver().openInputStream(uri);
            result = getBytes(iStream);
        }
        catch (Exception ex){

        }
        finally {
            return result;
        }
    }
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
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
    String getFileName(Context context, Uri fileUri){
        String filename = FileUtils.getRealPathFromUri(this, result);
        if (filename.contains("/"))
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        return  filename;
    }
    String getMD5Hash(byte[] value){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(value);
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }
        catch (Exception ex){
            return null;
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
}
