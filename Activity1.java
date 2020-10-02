//単語選択画面
package com.example.oralvoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity1 extends AppCompatActivity {

    //選択された単語の番号を他のactivityに移すためのkey
    public static final String WORD_NUMBER = "WORD_NUMBER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_no_error);

        //意味を入れる配列
        final String[] meaning = getResources().getStringArray(R.array.meanings_no_error);

        setContentView(R.layout.activity1);

        setTitle("単語選択");

        //Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //単語のリスト
        final List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < word.length; i++) {
            Map<String, String> map = new HashMap<>();
            map.put("main", word[i]);
            map.put("sub", meaning[i]);
            list.add(map);
        }

        //単語をListViewにセットするためのアダプター
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                list,
                R.layout.word_row1,
                new String[] {"main", "sub"},
                new int[] {R.id.text1, R.id.text2}
        );

        //単語の選択肢のListView
        ListView word_list_view = findViewById(R.id.word_list_view);
        word_list_view.setAdapter(adapter);
        word_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //単語、発音記号、単語の意味のActivityを始める
                Intent intent = new Intent(getApplication(), Activity1_1.class);
                intent.putExtra(WORD_NUMBER, position + 1);
                startActivity(intent);
            }
        });
    }

    //ToolBarの戻るボタンがタップされたときの処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            // Backボタンがタップされた場合
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //Toastで指定の文字列を表示する関数
    public void showToast(String showString) {
        Toast toast = Toast.makeText (
                getApplicationContext(),
                showString,
                Toast.LENGTH_LONG
        );
        toast.show();
    }
}
