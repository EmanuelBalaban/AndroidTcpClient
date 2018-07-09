package me.blankboy.androidtcpclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Login extends AppCompatActivity {

    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.socket = MainActivity.socket;

        InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        ((TextView) findViewById(R.id.title)).setText("Login to " + socketAddress.getHostString() + ":" + socketAddress.getPort());

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!MainActivity.ActivityTimeout) {
                    try {
                        Thread.sleep(10);
                    }
                    catch (Exception ex){

                    }
                }
                finish();
            }
        }).start();
    }

    void onLoginClick(View view) throws NoSuchAlgorithmException {
        TextView title = (TextView) findViewById(R.id.title);
        TextView usernameBox = (TextView) findViewById(R.id.usernameBox);
        TextView passwordBox = (TextView) findViewById(R.id.passwordBox);

        String username = usernameBox.getText().toString();
        String password = passwordBox.getText().toString();

        if (username == "") usernameBox.setError("This field is required!");
        if (username.contains(":")) usernameBox.setError("Username cannot contain ':'");
        if (password == "") passwordBox.setError("This field is required!");
        if (password.contains(":")) passwordBox.setError("Password cannot contain ':'");
        if (usernameBox.getError() != null || passwordBox.getError() != null) return;

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] digest = md.digest();
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }


        String message = "[LOGIN_REQUEST]" + username + ":" + sb.toString();

        MainActivity.SendMessage(socket, message);

        finish();
    }
}
