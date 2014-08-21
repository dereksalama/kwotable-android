package com.dereksalama.kwotabl;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.identitytoolkit.client.GitkitClient;
import com.google.identitytoolkit.model.Account;
import com.google.identitytoolkit.model.IdToken;


public class Login extends FragmentActivity implements View.OnClickListener {

    private GitkitClient client;
    private static final String TAG =  "Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = GitkitClient.newBuilder(this, new GitkitClient.SignInCallbacks() {
            @Override
            public void onSignIn(IdToken idToken, Account account) {
                Log.d(TAG, "sign in complete");
                openQuoteActivity();
            }

            @Override
            public void onSignInFailed() {
                Log.d(TAG, "sign in failed");
                Toast.makeText(Login.this, "Sign in failed", Toast.LENGTH_LONG).show();
            }
        }).build();

        IdToken idToken = client.getSavedIdToken();
        Account account = client.getSavedAccount();
        if (idToken != null && account != null) {
            Log.d(TAG, "already signed in");
            openQuoteActivity();
        } else {
            showSignInPage();
        }
    }

    private void openQuoteActivity() {
        Intent i = new Intent(this, QuoteBook.class);
        startActivity(i);
    }

    private void showSignInPage() {
        setContentView(R.layout.login);
        Button button = (Button) findViewById(R.id.sign_in_button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            Log.d(TAG, "sign in click");
            client.startSignIn();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (!client.handleActivityResult(requestCode, resultCode, intent)) {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!client.handleIntent(intent)) {
            super.onNewIntent(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
