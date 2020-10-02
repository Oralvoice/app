package com.example.oralvoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity2 extends AppCompatActivity {

    //自分のユーザ名
    String userName = getUserData(1);

    //CloudFirestoreのインスタンス
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Activity遷移時に使うkey
    public static final String ROOM_ID = "ROOM_ID";
    public static final String OWN_NUM = "OWN_NUM";
    public static final String WORD_NUM1 = "WORD_NUM1";
    public static final String WORD_NUM2 = "WORD_NUM2";
    public static final String WORD_NUM3 = "WORD_NUM3";

    //マッチングしていないとfalse、マッチングするとtrueに代わる
    public boolean matched = false;

    //部屋名 (CloudFirestoreのDocument名)
    public String roomId;

    //プレイヤー1か2か (部屋を作った人が1、既存の部屋に入った人が2)
    public int own_num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity2);

        setTitle("オンライン対戦");

        // Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //以下マッチングの処理
        db.collection("room")
                .whereEqualTo("accessuser2", "0")   //取得するデータをプレイヤー2が入っていないデータに限定する
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult().size() == 0) { //空き部屋がない場合
                            //実装されているすべての単語を入れる配列
                            final String[] word = getResources().getStringArray(R.array.words_no_error);

                            ArrayList<String> word10 = new ArrayList<>(Arrays.asList(word));    //上の配列をリストに保存する
                            List<String> shuffled = new ArrayList<>(word10); // この後ランダムに並べ替えられる、list のコピー
                            Collections.shuffle(shuffled); // shuffled の要素をランダムに並び替える。
                            List<String> word5 = shuffled.subList(0, 3); // shuffled の前から2つの要素を持つ新しいリスト
                            String[] array = word5.toArray(new String[word5.size()]);

                            int word_num1 = getWordNum(array[0]);
                            int word_num2 = getWordNum(array[1]);
                            int word_num3 = getWordNum(array[2]);

                            //部屋を作った時の初期値やプレイヤー1のデータを格納するマップ
                            Map<String, Object> room = new HashMap<>();

                            //accessuser1,2はプレイヤーがいたら1、いなかったら0、すでにマッチを終了したら2になる値
                            room.put("accessuser1", "1");   //部屋を作った時自分がプレイヤー1なので"1"
                            room.put("accessuser2", "0");   //プレイヤー2はまだいないので"0"

                            //user1,2はプレイヤー1,2の名前を入れる。
                            room.put("user1", userName);

                            //fscore1,2、sscore1,2、tscore1,2はラウンド1,2,3のプレイヤーごとの点数
                            room.put("fscore1", 0);
                            room.put("sscore1", 0);
                            room.put("tscore1", 0);

                            //対戦に使う3ラウンド分の単語
                            room.put("word1", array[0]);
                            room.put("word2", array[1]);
                            room.put("word3", array[2]);

                            //CloudFirestoreに自動生成IDのDocument(部屋)を追加する
                            db.collection("room")  //空き部屋を作る
                                    .add(room)  //部屋のデータを追加
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            //accessuser2が1になるまで無限ループ
                                            for (int i = 0; i < 100; i++) {
                                                documentReference.get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    DocumentSnapshot document = task.getResult();
                                                                    if (document.exists()) {
                                                                        String accessuser2 = String.valueOf(document.get("accessuser2"));   //プレイヤー2の状態を取得
                                                                        if (accessuser2.equals("1")) {  //プレイヤー2がアクセス状態になれば
                                                                            //マッチングしたと判定
                                                                            if (!matched) {
                                                                                roomId = document.getReference().getId();   //部屋名をroomIDに格納
                                                                                own_num = 1;    //部屋を作ったので自分はプレイヤー1

                                                                                //対戦のActivityへと遷移、対戦に必要なデータを渡す
                                                                                Intent intent = new Intent(getApplication(), Activity2_1.class);
                                                                                intent.putExtra(ROOM_ID, roomId);
                                                                                intent.putExtra(OWN_NUM, own_num);
                                                                                intent.putExtra(WORD_NUM1, word_num1);
                                                                                intent.putExtra(WORD_NUM2, word_num2);
                                                                                intent.putExtra(WORD_NUM3, word_num3);
                                                                                startActivity(intent);
                                                                                finish();
                                                                            }
                                                                            matched = true;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        });
                                                try {
                                                    Thread.sleep(50);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (matched) {
                                                if (own_num != 0) {

                                                } else {
                                                    showToast("データベースエラー");
                                                }
                                            } else {
                                                //showToast("マッチング失敗");
                                                finish();
                                            }

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                        }
                                    });
                        } else {   //空き部屋がある場合
                            DocumentReference docRef = null;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                docRef = document.getReference();
                                Map<String, Object> player2 = new HashMap<>();
                                player2.put("accessuser2", "1");    //player2にプレイヤー2をアクセス状態("1")で追加
                                player2.put("user2", userName);        //player2に"user2",プレイヤー2の名前を追加
                                player2.put("fscore2", 0);          //player2に"fscore2",(初期値)0を追加
                                player2.put("sscore2", 0);          //player2に"sscore2",(初期値)0を追加
                                player2.put("tscore2", 0);          //player2に"tscore2",(初期値)0を追加

                                    docRef.update(player2)      //player2に入っているデータをFirestoreの自分の入っている部屋内に追加、更新
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void avoid) {
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                            }
                                        });

                                docRef.get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        String word1 = String.valueOf(document.get("word1"));       //ラウンド1で発音する単語を取得
                                                        String word2 = String.valueOf(document.get("word2"));       //ラウンド2発音する単語を取得
                                                        String word3 = String.valueOf(document.get("word3"));       //ラウンド3発音する単語を取得

                                                        int word_num1 = getWordNum(word1);
                                                        int word_num2 = getWordNum(word2);
                                                        int word_num3 = getWordNum(word3);

                                                        own_num = 2;        //部屋に入ったのでプレイヤー2
                                                        roomId = document.getReference().getId();       //部屋名をroomIDに格納
                                                        //showToast("マッチングしました");

                                                        //対戦のActivityへと遷移、対戦に必要なデータを渡す
                                                        Intent intent = new Intent(getApplication(), Activity2_1.class);
                                                        intent.putExtra(ROOM_ID, roomId);
                                                        intent.putExtra(OWN_NUM, own_num);
                                                        intent.putExtra(WORD_NUM1, word_num1);
                                                        intent.putExtra(WORD_NUM2, word_num2);
                                                        intent.putExtra(WORD_NUM3, word_num3);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                }
                                            }
                                        });
                                break;
                            }
                        }
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

    //指定の単語の識別番号を返す関数
    public int getWordNum(String word) {
        int word_num = 0;
        final String[] array = getResources().getStringArray(R.array.words_with_error);
        int count = 0;
        for (String s: array) {
            if (word.equals(s)) {
                word_num = count;
                break;
            }
            count += 1;
        }
        return word_num;
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
