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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.identitytoolkit.client.GitkitClient;
import com.google.identitytoolkit.model.Account;
import com.google.identitytoolkit.model.IdToken;
import com.google.identitytoolkit.util.HttpUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QuoteBook extends FragmentActivity {

    public static final String BASE_URL = "http://10.0.2.2:8888";
    private static final String DOWNLOAD_URL = BASE_URL + "/download?";

    private static final String TAG = "QuoteBook";

    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quote_book);


        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.quote_list);
        listView.setAdapter(listAdapter);

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

        final String charset = "UTF-8";
        String query;
        try {
            query = String.format("id_token=%s", URLEncoder.encode(GitkitClient.getSavedIdToken(this).getLocalId(), charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error loading quotes :(",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        reqUrl.append(query);

        reqUrl.append("&timestamp=");
        reqUrl.append(System.currentTimeMillis());

        AsyncTask<Void, Void, List<QuoteResponseData>> uploadTask = new AsyncTask<Void, Void, List<QuoteResponseData>>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(QuoteBook.this);
                dialog.show();
            }

            @Override
            protected List<QuoteResponseData> doInBackground(Void... param) {
                HttpURLConnection conn = null;
                try {
                    String checksum;
                    try {
                        checksum = ChecksumUtil.makeCheck(reqUrl.toString().getBytes());
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        return null;
                    }

                    conn = (HttpURLConnection) new URL(reqUrl.toString()).openConnection();
                    conn.setRequestProperty("X-CHECKSUM", checksum);
                    conn.setDoInput(true);
                    conn.setRequestProperty("Accept-Charset", charset);



                    if (conn.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                        return null;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));

                    String responseStr = reader.readLine();
                    if (responseStr != null) {

                        Gson gson = new Gson();
                        Type collectionType = new TypeToken<List<QuoteResponseData>>(){}.getType();
                        return gson.fromJson(responseStr, collectionType);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<QuoteResponseData> quotes) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (quotes != null) {
                    listAdapter.clear();
                    for (QuoteResponseData quote : quotes) {
                        listAdapter.add(quote.toString());
                    }
                    listAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplicationContext(), "Error loading quotes :(",
                            Toast.LENGTH_SHORT).show();
                }

                if (listAdapter.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), "No quotes yet :(",
                            Toast.LENGTH_SHORT).show();
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
