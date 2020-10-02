package com.example.oralvoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Activity2_1_1 extends AppCompatActivity {

    //部屋名 (CloudFirestoreのDocument名)
    String roomId;

    //プレイヤー1か2か (部屋を作った人が1、既存の部屋に入った人が2)
    int own_num;

    //プレイヤーごとの勝利数
    int point1;
    int point2;

    //3ラウンド分の単語の識別番号
    int word_num1;
    int word_num2;
    int word_num3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity2_1_1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Activity2_1から部屋名、プレイヤー番号、word_numを受け取る
        Intent intent = getIntent();
        roomId = intent.getStringExtra(Activity2_1.ROOM_ID);
        own_num = intent.getIntExtra(Activity2_1.OWN_NUM, 0);
        word_num1 = intent.getIntExtra(Activity2_1.WORD_NUM1, 0);
        word_num2 = intent.getIntExtra(Activity2_1.WORD_NUM2, 0);
        word_num3 = intent.getIntExtra(Activity2_1.WORD_NUM3, 0);

        //CloudFirestoreのインスタンス、DocumentReferenceに対戦部屋のID(Document名)を使う
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("room").document(roomId);

        //各ラウンドごとの勝敗を判定し、勝利数を算出する
        judgeDataToFirestore(docRef);

        setTitle("オンライン対戦 (結果)");

        // Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //3ラウンド分の単語のTextView
        TextView word_textView1 = findViewById(R.id.word_textView1);
        TextView word_textView2 = findViewById(R.id.word_textView2);
        TextView word_textView3 = findViewById(R.id.word_textView3);

        TextView enemy_name_textView = findViewById(R.id.enemy_name_textView);
        if (own_num == 1) {
            setFirestoreData(docRef, "user2", enemy_name_textView, "vs.");
        }

        //3ラウンド分の単語のTextView
        final String[] word = getResources().getStringArray(R.array.words_with_error);
        word_textView1.setText(word[word_num1]);
        word_textView2.setText(word[word_num2]);
        word_textView3.setText(word[word_num3]);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //3ラウンド分、2人分の点数を表示する
        TextView myfscore_textView = findViewById(R.id.myfscore_textView);
        TextView enemyfscore_textView = findViewById(R.id.enemyfscore_textView);

        TextView mysscore_textView = findViewById(R.id.mysscore_textView);
        TextView enemysscore_textView = findViewById(R.id.enemysscore_textView);

        TextView mytscore_textView = findViewById(R.id.mytscore_textView);
        TextView enemytscore_textView = findViewById(R.id.enemytscore_textView);

        if (own_num == 1) {
            setFirestoreData(docRef, "fscore1", myfscore_textView, "");
            setFirestoreData(docRef, "fscore2", enemyfscore_textView, "");

            setFirestoreData(docRef, "sscore1", mysscore_textView, "");
            setFirestoreData(docRef, "sscore2", enemysscore_textView, "");

            setFirestoreData(docRef, "tscore1", mytscore_textView, "");
            setFirestoreData(docRef, "tscore2", enemytscore_textView, "");
        } else {
            setFirestoreData(docRef, "fscore2", myfscore_textView, "");
            setFirestoreData(docRef, "fscore1", enemyfscore_textView, "");

            setFirestoreData(docRef, "sscore2", mysscore_textView, "");
            setFirestoreData(docRef, "sscore1", enemysscore_textView, "");

            setFirestoreData(docRef, "tscore2", mytscore_textView, "");
            setFirestoreData(docRef, "tscore1", enemytscore_textView, "");
        }

        //ラウンド、勝敗に応じてラウンドごとの勝敗を表示する
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                TextView win_or_lose_textView1 = findViewById(R.id.win_or_lose_textView1);
                                int fscore1 = Integer.parseInt(String.valueOf(document.get("fscore1")));
                                int fscore2 = Integer.parseInt(String.valueOf(document.get("fscore2")));
                                if (fscore1 > fscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView1.setText("勝利!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView1.setText("敗北!");
                                    }
                                } else if (fscore1 < fscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView1.setText("敗北!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView1.setText("勝利!");
                                    }
                                } else {
                                    win_or_lose_textView1.setText("引き分け!");
                                }


                                TextView win_or_lose_textView2 = findViewById(R.id.win_or_lose_textView2);
                                int sscore1 = Integer.parseInt(String.valueOf(document.get("sscore1")));
                                int sscore2 = Integer.parseInt(String.valueOf(document.get("sscore2")));
                                if (sscore1 > sscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView2.setText("勝利!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView2.setText("敗北!");
                                    }
                                } else if (sscore1 < sscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView2.setText("敗北!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView2.setText("勝利!");
                                    }
                                } else {
                                    win_or_lose_textView2.setText("引き分け!");
                                }

                                TextView win_or_lose_textView3 = findViewById(R.id.win_or_lose_textView3);
                                int tscore1 = Integer.parseInt(String.valueOf(document.get("tscore1")));
                                int tscore2 = Integer.parseInt(String.valueOf(document.get("tscore2")));
                                if (tscore1 > tscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView3.setText("勝利!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView3.setText("敗北!");
                                    }
                                } else if (tscore1 < tscore2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView3.setText("敗北!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView3.setText("勝利!");
                                    }
                                } else {
                                    win_or_lose_textView3.setText("引き分け!");
                                }
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
                deleteRoom();
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //相手の状態に応じて部屋を削除する関数
    public void deleteRoom() {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("room").document(roomId);

        setDataToFirestore(docRef, "accessuser" + own_num, "2");
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String accessuser = "2";
                                if (own_num == 1) {
                                    accessuser = String.valueOf(document.get("accessuser2"));
                                } else if (own_num == 2){
                                    accessuser = String.valueOf(document.get("accessuser1"));
                                }

                                if (accessuser.equals("2")) { //相手も終了していたら
                                    docRef.delete() //部屋を削除
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                }
                                            });
                                }
                            }

                        }
                    }
                });

    }

    public void judgeDataToFirestore(DocumentReference docRef) {
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                int fscore1 = Integer.parseInt(String.valueOf(document.get("fscore1")));
                                int fscore2 = Integer.parseInt(String.valueOf(document.get("fscore2")));
                                int sscore1 = Integer.parseInt(String.valueOf(document.get("sscore1")));
                                int sscore2 = Integer.parseInt(String.valueOf(document.get("sscore2")));
                                int tscore1 = Integer.parseInt(String.valueOf(document.get("tscore1")));
                                int tscore2 = Integer.parseInt(String.valueOf(document.get("tscore2")));
                                if (fscore1 < fscore2) {
                                    point2 += 1;
                                } else if (fscore1 > fscore2) {
                                    point1 += 1;
                                }
                                if (sscore1 < sscore2) {
                                    point2 += 1;
                                } else if (sscore1 > sscore2) {
                                    point1 += 1;
                                }
                                if (tscore1 < tscore2) {
                                    point2 += 1;
                                } else if (tscore1 > tscore2) {
                                    point1 += 1;
                                }

                                //勝ちか負けか引き分けかのTextView
                                TextView win_or_lose_textView = findViewById(R.id.win_or_lose_textView);
                                if (point1 > point2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView.setText("勝利!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView.setText("敗北!");
                                    }
                                } else if (point1 < point2) {
                                    if (own_num == 1) {
                                        win_or_lose_textView.setText("敗北!");
                                    } else if (own_num == 2) {
                                        win_or_lose_textView.setText("勝利!");
                                    }
                                } else {
                                    win_or_lose_textView.setText("引き分け!");
                                }

                                //自分の勝ったラウンド数のTextView
                                TextView user_point_textView = findViewById(R.id.user_point_textView);
                                if (own_num == 1) {
                                    user_point_textView.setText(String.valueOf(point1));
                                } else if (own_num == 2) {
                                    user_point_textView.setText(String.valueOf(point2));
                                }

                                //敵の勝ったラウンド数のTextView
                                TextView enemy_point_textView = findViewById(R.id.enemy_point_textView);
                                if (own_num == 1) {
                                    enemy_point_textView.setText(String.valueOf(point2));
                                } else if (own_num == 2) {
                                    enemy_point_textView.setText(String.valueOf(point1));
                                }
                            }
                        }
                    }
                });
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

    //指定のドキュメントリファレンスの指定のフィールドに指定のデータを書き込む関数
    public void setDataToFirestore (DocumentReference docRef, String field, Object data) {
        Map<String, Object> field_data = new HashMap<>();
        field_data.put(field, data);
        docRef.update(field_data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //showToast("成功");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("データベースへの書き込みに失敗");
                    }
                });
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
