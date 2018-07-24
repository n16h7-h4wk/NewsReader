package com.developer.ankit.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> contents = new ArrayList<>();
    ArrayAdapter mArrayAdapter ;
    SQLiteDatabase articleDB ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView mListView = (ListView)findViewById(R.id.list_view);
        mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        mListView.setAdapter(mArrayAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ContentActivity.class);
                intent.putExtra("content",contents.get(position));
                startActivity(intent);
            }
        });
        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");
        setListView();
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);
        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");
        if(c.moveToFirst()){
            titles.clear();
            contents.clear();
            do {
                titles.add(c.getString(titleIndex));
                contents.add(c.getString(contentIndex));
            }while(c.moveToNext());

            mArrayAdapter.notifyDataSetChanged();

        }
    }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url ;
            HttpURLConnection connection = null;
            try {
                url = new URL(strings[0]);
                connection = (HttpURLConnection)url.openConnection();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data!=-1){
                    result+=(char)data ;
                    data = reader.read();
                }
                Log.i("URL",result) ;
                JSONArray array = new JSONArray(result);
                int numberOfItems = 20 ;
                if(numberOfItems<20)
                    numberOfItems = array.length();

                articleDB.execSQL("DELETE FROM articles");

                for(int i=0;i<numberOfItems;i++){
                    String articleId = array.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    connection = (HttpURLConnection)url.openConnection();
                    in = connection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleInfo = "";
                    while(data!=-1){
                        articleInfo +=(char)data ;
                        data = reader.read();
                    }
                    JSONObject object = new JSONObject(articleInfo);
                    if(!object.isNull("title")&& !object.isNull("url")) {
                        String articleTitle = object.getString("title");
                        String articleUrl = object.getString("url");
                        url = new URL(articleUrl);
                        connection = (HttpURLConnection)url.openConnection();
                        in = connection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();
                        String articleContent = "";
                        while(data!=-1){
                            articleContent +=(char)data ;
                            data = reader.read();
                        }
                        String sql = "INSERT INTO articles (articleId , title, content) VALUES(? , ? , ?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);
                        statement.execute();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            setListView();
        }
    }
}
