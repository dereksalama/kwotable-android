package com.dereksalama.kwotabl;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.identitytoolkit.client.GitkitClient;
import com.google.identitytoolkit.model.Account;
import com.google.identitytoolkit.model.IdToken;
import com.google.identitytoolkit.util.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QuoteBook extends FragmentActivity {

    public static final String BASE_URL = "https://localhost:8888";
    private static final String DOWNLOAD_URL = BASE_URL + "/download?";


    private GitkitClient client;
    private static final String TAG = "QuoteBook";

    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_book);

        client = GitkitClient.newBuilder(this, new GitkitClient.SignInCallbacks() {
            @Override
            public void onSignIn(IdToken idToken, Account account) {}

            @Override
            public void onSignInFailed() {}
        }).build();

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.quote_list);
        listView.setAdapter(listAdapter);

    }

    @Override
    public void onResume() {
        super.onResume();
        IdToken idToken = client.getSavedIdToken();
        Account account = client.getSavedAccount();
        if (idToken != null && account != null) {
            Log.d(TAG, "already signed in");
        } else {
            Log.d(TAG, "not signed in");

            //send back to login page
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        showAccount(account);
        loadQuotes();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.quote_book, menu);
        return true;
    }

    private void loadQuotes() {

        final StringBuilder reqUrl = new StringBuilder();
        reqUrl.append(DOWNLOAD_URL);
        reqUrl.append("id_token=");
        reqUrl.append(client.getSavedIdToken());

        AsyncTask<Void, Void, Integer> uploadTask = new AsyncTask<Void, Void, Integer>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(QuoteBook.this);
                dialog.show();
            }

            @Override
            protected Integer doInBackground(Void... param) {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(reqUrl.toString()).openConnection();
                    conn.setDoInput(true);

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                        return conn.getResponseCode();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));

                    String responseStr = reader.readLine();
                    if (responseStr != null) {
                        JSONArray jArray = new JSONArray(responseStr);
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject jObj = jArray.getJSONObject(i);

                            String quote = jObj.getString("quote") + " - " +
                                    jObj.getString("author");

                            listAdapter.add(quote);
                        }
                    }
                    return conn.getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
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
                    listAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplicationContext(), "Error loading quotes :(",
                            Toast.LENGTH_SHORT).show();
                }
            }

        };

        try {
            uploadTask.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void showAccount(Account account) {
        ((TextView) findViewById(R.id.account_email)).setText(account.getEmail());

        if (account.getDisplayName() != null) {
            ((TextView) findViewById(R.id.account_name)).setText(account.getDisplayName());
        }

        if (account.getPhotoUrl() != null) {
            final ImageView pictureView = (ImageView) findViewById(R.id.account_picture);
            new AsyncTask<String, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(String... arg) {
                    try {
                        byte[] result = HttpUtils.get(arg[0]);
                        return BitmapFactory.decodeByteArray(result, 0, result.length);
                    } catch (IOException e) {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if (bitmap != null) {
                        pictureView.setImageBitmap(bitmap);
                    }
                }
            }.execute(account.getPhotoUrl());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add) {
            Intent intent = new Intent(this, NewQuote.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

}
