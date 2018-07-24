package me.blankboy.androidtcpclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Login extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ((TextView) findViewById(R.id.title)).setText("Login to " + Variables.PrimaryServer.UniqueIdentity());
    }

    void onLoginClick(View view) {
        TextView usernameBox = findViewById(R.id.usernameBox);
        TextView passwordBox = findViewById(R.id.passwordBox);

        String username = usernameBox.getText().toString();
        String password = passwordBox.getText().toString();

        if (username == "") usernameBox.setError("This field is required!");
        if (username.contains(":")) usernameBox.setError("Username cannot contain ':'");
        if (password == "") passwordBox.setError("This field is required!");
        if (password.contains(":")) passwordBox.setError("Password cannot contain ':'");
        if (usernameBox.getError() != null || passwordBox.getError() != null) return;

        Variables.PrimaryServer.Login(username, password, true);

        finish();
    }
}
