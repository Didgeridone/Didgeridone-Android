package com.didgeridone.didgeridone_andriod;

import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONArray;







public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ArrayList<String> allTasks = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    HashMap<Integer, Object> mapper = new HashMap<Integer, Object>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        new DownloadTask().execute("https://didgeridone.herokuapp.com/task/56c3ad2db2273e8c7c9d3612");

        final ListView myList;
        myList = (ListView) findViewById(R.id.listView);

        myList.setClickable(true);
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(Integer.toString(position));
                String obj = mapper.get((position)).toString();

                try {
                    JSONObject jsonObj = new JSONObject(obj);
                  System.out.println(jsonObj);


                    // To call this activity do this...
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("Reminder_User_Id", "56c3ad2db2273e8c7c9d3612");
                    intent.putExtra("Reminder_Task_Id", jsonObj.get("task_id").toString());
                    intent.putExtra("Reminder_Name", jsonObj.get("name").toString());
                    intent.putExtra("Reminder_Latitude", Double.parseDouble(jsonObj.get("lat").toString()));
                    intent.putExtra("Reminder_Longitude", Double.parseDouble(jsonObj.get("long").toString()));
                    intent.putExtra("Reminder_Radius", Double.parseDouble(jsonObj.get("radius").toString()));
                    startActivity(intent);

                } catch (Exception e) {
                    Log.d("Didgeridoo","Exception",e);
                }

            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, allTasks);
        adapter.clear();
        adapter.notifyDataSetChanged();
        myList.setAdapter(adapter);
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {
                return downloadContent(params[0]);
            } catch (IOException e) {
                return "Unable to retrieve data. URL may be invalid.";
            }
        }

        @Override

        protected void onPostExecute(String result) {
            System.out.println("Onpostexecute: " + allTasks);
            adapter.notifyDataSetChanged();

        }
    }



    private String downloadContent(String myurl) throws IOException {
        InputStream is = null;
        int length = 50000;



        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //conn.setReadTimeout(10000);
            //conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();
            String contentAsString = convertInputStreamToString(is, length);



            try {
                // Parse the entire JSON string
                JSONObject root = new JSONObject(contentAsString);
                // get the array of tasks from JSON
                JSONObject user = root.getJSONObject("user");
//                System.out.println(user);
                JSONArray tasks = user.getJSONArray("tasks");
//                System.out.println(tasks);

                for(int i=0;i<tasks.length();i++) {
                    // parse the JSON object into fields and values
                    JSONObject jsonTasks = tasks.getJSONObject(i);
                    String name = jsonTasks.getString("name");
                    allTasks.add(name);
                    int position = allTasks.indexOf(name);
                    mapper.put(position, jsonTasks);
                }


            } catch (Exception e) {
                Log.d("Didgeridoo","Exception",e);
            }

            return contentAsString;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String convertInputStreamToString(InputStream stream, int length) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[length];
        reader.read(buffer);
        return new String(buffer);
    }

}
