//単語、発音記号、単語の意味の画面
package com.example.oralvoice;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Activity1_1 extends AppCompatActivity {

    //選択された単語の番号を他のactivityに移すためのkey
    public static final String WORD_NUMBER = "WORD_NUMBER";

    //選択された単語の番号
    int word_number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_with_error);

        //意味を入れる配列
        final String[] meaning = getResources().getStringArray(R.array.meanings_with_error);

        //発音記号の画像IDを入れる配列
        final int[] phonetic_symbol_id = getResourceIds(getResources(), R.array.phonetic_ids_with_error);

        //Activity1またはActivity1_1_1からデータを受け取る
        Intent receiveDataIntent = getIntent();
        int act1_word_number = receiveDataIntent.getIntExtra(Activity1.WORD_NUMBER, 0);
        int act1_1_1_word_number = receiveDataIntent.getIntExtra(Activity1_1_1.WORD_NUMBER, 0);

        //Activity1かActivity1_1_1のうち、受け取ったほうのデータを使う
        if (act1_word_number != 0) {
            word_number = act1_word_number;
        } else if (act1_1_1_word_number != 0) {
            word_number = act1_1_1_word_number;
        } else {
            word_number = 0;
        }

        //CloudFirestoreのインスタンス、各コレクションのパス
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));
        final DocumentReference ranking_docRef = db.collection("ranking").document(word[word_number]);

        setContentView(R.layout.activity1_1);

        setTitle(word[word_number]);

        // Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //単語のTextView
        TextView word_textView = findViewById(R.id.word_textView);
        word_textView.setText(word[word_number]);

        //発音記号の画像のImageView
        ImageView phonetic_symbol_imageView = findViewById(R.id.phonetic_symbol_imageView);
        phonetic_symbol_imageView.setImageBitmap(resizeImage(0.15f, phonetic_symbol_id[word_number]));

        //単語の意味のTextView
        TextView meaning_textView = findViewById(R.id.meaning_textView);
        meaning_textView.setText(meaning[word_number]);

        //発音開始のButton
        Button start_button = findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 以前に撮った画像がもし残っていたら全て削除
                int i = -1;
                while(true){
                    i++;
                    String frameFilename = i + ".jpg";
                    final String framePath = getFilesDir().getPath() + File.separator + frameFilename;
                    File frameFile = new File(framePath);
                    if (!frameFile.exists()) {
                        break;
                    }
                    frameFile.delete();
                }
                //カメラのActivityを始める
                Intent intent = new Intent(getApplication(), CameraActivity.class);
                intent.putExtra(WORD_NUMBER, word_number);
                startActivity(intent);
                finish();
            }
        });

        //練習回数のTextView
        TextView times_textView = findViewById(R.id.times_textView);
        setFirestoreData(user_docRef, word[word_number] + "_plays", times_textView, "");

        //平均点のTextView
        TextView average_textView = findViewById(R.id.average_textView);
        setFirestoreData(user_docRef, word[word_number] + "_AVG", average_textView, "");

        //最高点のTextView
        TextView max_textView = findViewById(R.id.max_textView);
        setFirestoreData(user_docRef, word[word_number] + "_MAX", max_textView, "");

        //順位のTextView
        ranking_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        TextView rank_textView = findViewById(R.id.rank_textView);

                        Map<String, Object> rankingData;

                        if(task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                rankingData = (document.getData());

                                //取得したランキングを降順にソート
                                List<Map.Entry<String, Object>> list_entries = new ArrayList<>(rankingData.entrySet());
                                Collections.sort(list_entries, new Comparator<Map.Entry<String, Object>>() {
                                    public int compare(Map.Entry<String, Object> obj1, Map.Entry<String, Object> obj2) {
                                        return Integer.valueOf(obj2.getValue().toString()).compareTo(Integer.valueOf(obj1.getValue().toString()));
                                    }
                                });

                                //ランキングから自分の名前を探す
                                String userId = getUserData(1);
                                int count = 1;
                                boolean found = false;
                                for (Map.Entry<String, Object> entry : list_entries) {
                                    if (entry.getKey().equals(userId)) {
                                        rank_textView.setText(String.valueOf(count));
                                        found = true;
                                        break;
                                    }
                                    count += 1;
                                }
                                if (!found) {
                                    rank_textView.setText("データなし");
                                }
                            } else {
                                rank_textView.setText("エラー");
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        TextView rank_textView = findViewById(R.id.rank_textView);
                        rank_textView.setText("取得失敗");
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

    //指定のドキュメントリファレンスの指定のフィールドのデータを指定のTextViewにセットする関数
    public void setFirestoreData (DocumentReference docRef, final String field, final TextView text_view, final String text) {
        final String[] str = new String[1];
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Object data = document.get(field);
                        if (data != null) {
                            str[0] = data.toString();
                        } else {
                            str[0] = "ERROR";
                        }
                    } else {
                        str[0] = "no_data";
                    }
                } else {
                    str[0] = "failure";
                }
                String set = text + str[0];
                text_view.setText(set);
            }
        });
    }

    //ユーザ情報を取得し、リクエストされたデータを返す関数
    public String getUserData (int requestNum) {
        //0:メールアドレス, 1:ユーザ名, 2:ユーザID
        String[] userData = new String[3];

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userData[0] = currentUser.getEmail();
            userData[1] = currentUser.getDisplayName();
            userData[2] = currentUser.getUid();
        } else {
            userData[0] = "ログインエラー";
            userData[1] = "ログインエラー";
            userData[2] = "ログインエラー";
        }
        return userData[requestNum];
    }

    //画像を指定の割合に縮小しBitmapで返す関数
    public Bitmap resizeImage(float ratio, int id) {
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), id);
        int imageWidth1 = bitmap1.getWidth();
        int imageHeight1 = bitmap1.getHeight();
        Matrix matrix1 = new Matrix();
        matrix1.preScale(ratio, ratio);
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap1, 0, 0, imageWidth1, imageHeight1, matrix1, true);

        return bitmap2;
    }

    //リソースファイルにあるリソースIDの配列を返す関数
    public static int[] getResourceIds(Resources resources, int arrayId) {
        final TypedArray array = resources.obtainTypedArray(arrayId);
        try {
            final int[] resourceIds = new int[array.length()];
            for (int i = 0; i < resourceIds.length; ++i) {
                resourceIds[i] = array.getResourceId(i, 0);
            }
            return resourceIds;
        } finally {
            array.recycle();
        }
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
