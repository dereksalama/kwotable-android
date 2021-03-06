package com.dereksalama.kwotabl;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.identitytoolkit.client.GitkitClient;
import com.google.identitytoolkit.model.Account;
import com.google.identitytoolkit.model.IdToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NewQuote extends FragmentActivity {

    private static final String UPLOAD_URL = QuoteBook.BASE_URL + "/upload";

    private static final String TAG = "NewQuote";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_quote);

        Button saveButton = (Button) findViewById(R.id.button_save_quote);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText quoteText = (EditText) findViewById(R.id.quote_input);
                EditText quoteAuthor = (EditText) findViewById(R.id.author_input);

                if (quoteText.getText().length() == 0) {
                    Toast.makeText(NewQuote.this, "No quote entered!", Toast.LENGTH_SHORT).show();
                } else if (quoteAuthor.getText().length() == 0) {
                    Toast.makeText(NewQuote.this, "No author entered!", Toast.LENGTH_SHORT).show();
                } else {
                    saveQuote(quoteText.getText().toString(), quoteAuthor.getText().toString());
                    finish();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        IdToken idToken = GitkitClient.getSavedIdToken(this);
        Account account = GitkitClient.getSavedAccount(this);
        if (idToken != null && account != null) {
            Log.d(TAG, "already signed in");
        } else {
            Log.d(TAG, "not signed in");

            //send back to login page
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void saveQuote(final String quoteText, final String quoteAuthor) {

        AsyncTask<Void, Void, Integer> uploadTask = new AsyncTask<Void, Void, Integer>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(NewQuote.this);
                dialog.show();
            }

            @Override
            protected Integer doInBackground(Void... param) {
                Map<String, Object> params = new HashMap<String, Object>();
                HttpURLConnection conn = null;
                try {
                    //params.put("local_id", GitkitClient.getSavedIdToken(NewQuote.this).getLocalId());
                    params.put("quote", quoteText);
                    params.put("author", quoteAuthor);
                    params.put("id_token", GitkitClient.getSavedIdToken(NewQuote.this).getTokenString());
                    byte[] body = new JSONObject(params).toString().getBytes();

                    /*
                    StringBuilder reqUrl = new StringBuilder();
                    reqUrl.append(UPLOAD_URL)
                            .append("?timestamp=")
                            .append(System.currentTimeMillis());

                    String checksum;
                    try {
                        checksum = ChecksumUtil.makeCheck(reqUrl.toString().getBytes());
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        return null;
                    }
                    */

                    conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                    //conn.setRequestProperty("X-CHECKSUM", checksum);
                    conn.setDoInput(true);

                    conn.setDoOutput(true);
                    conn.setFixedLengthStreamingMode(body.length);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestMethod("POST");
                    OutputStream os = conn.getOutputStream();
                    os.write(body);
                    os.close();

                    return conn.getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                return HttpURLConnection.HTTP_BAD_REQUEST;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (result == HttpURLConnection.HTTP_ACCEPTED) {
                    Toast.makeText(getApplicationContext(), "Quote Saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Error saving quote :(", Toast.LENGTH_SHORT).show();
                }
            }

        };

        try {
            uploadTask.execute(null,null,null);
            uploadTask.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
