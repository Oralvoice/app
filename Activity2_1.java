package com.example.oralvoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

import java.util.HashMap;
import java.util.Map;

public class Activity2_1 extends AppCompatActivity {

    //Activity遷移時に使うkey
    public static final String ROOM_ID = "ROOM_ID";
    public static final String OWN_NUM = "OWN_NUM";
    public static final String ROUND = "ROUND";
    public static final String WORD_NUM1 = "WORD_NUM1";
    public static final String WORD_NUM2 = "WORD_NUM2";
    public static final String WORD_NUM3 = "WORD_NUM3";

    //部屋名 (CloudFirestoreのDocument名)
    String roomId;

    //プレイヤー1か2か (部屋を作った人が1、既存の部屋に入った人が2)
    int own_num;

    //現在のラウンド
    int round;

    //点数
    int score;

    //3ラウンド分の単語の識別番号
    int word_num1;
    int word_num2;
    int word_num3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity2_1);

        //Activity2またはOnlineBattle_CameraActivityから必要なデータを受け取る
        Intent intent = getIntent();
        round = intent.getIntExtra(OnlineBattle_CameraActivity.ROUND, 0);
        if (round == 0) {
            roomId = intent.getStringExtra(Activity2.ROOM_ID);
            own_num = intent.getIntExtra(Activity2.OWN_NUM, 0);
            word_num1 = intent.getIntExtra(Activity2.WORD_NUM1, 0);
            word_num2 = intent.getIntExtra(Activity2.WORD_NUM2, 0);
            word_num3 = intent.getIntExtra(Activity2.WORD_NUM3, 0);
        } else {
            roomId = intent.getStringExtra(OnlineBattle_CameraActivity.ROOM_ID);
            own_num = intent.getIntExtra(OnlineBattle_CameraActivity.OWN_NUM, 0);
            word_num1 = intent.getIntExtra(OnlineBattle_CameraActivity.WORD_NUM1, 0);
            word_num2 = intent.getIntExtra(OnlineBattle_CameraActivity.WORD_NUM2, 0);
            word_num3 = intent.getIntExtra(OnlineBattle_CameraActivity.WORD_NUM3, 0);
            score = intent.getIntExtra(OnlineBattle_CameraActivity.SCORE, 0);
        }

        setTitle("オンライン対戦");

        // Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //CloudFirestoreのインスタンス、DocumentReferenceに対戦部屋のID(Document名)を使う
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("room").document(roomId);

        //自分の名前のTextView
        TextView user_name_textView = findViewById(R.id.user_name_textView);
        user_name_textView.setText(getUserData(1));

        //相手の名前のTextView
        TextView enemy_name_textView = findViewById(R.id.enemy_name_textView);
        if (own_num == 1) {
            setFirestoreData(docRef, "user2", enemy_name_textView, "");
        } else {
            setFirestoreData(docRef, "user1", enemy_name_textView, "");
        }

        //3ラウンド分の単語のTextView
        TextView word_textView1 = findViewById(R.id.word_textView1);
        TextView word_textView2 = findViewById(R.id.word_textView2);
        TextView word_textView3 = findViewById(R.id.word_textView3);
        if (round != 0) {
            final String[] word = getResources().getStringArray(R.array.words_with_error);

            word_textView1.setText(word[word_num1]);
            word_textView2.setText(word[word_num2]);
            word_textView3.setText(word[word_num3]);
        }

        if(round == 0) {

            //実装されている単語を入れる配列
            final String[] word = getResources().getStringArray(R.array.words_with_error);

            word_textView1.setText(word[word_num1]);
            word_textView2.setText(word[word_num2]);
            word_textView3.setText(word[word_num3]);

            Button start_round_button = findViewById(R.id.start_round_button);
            String s = "ラウンド" + (round + 1) + "の発音を開始";
            start_round_button.setText(s);
            start_round_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent2 = new Intent(getApplication(), OnlineBattle_CameraActivity.class);
                    intent2.putExtra(ROOM_ID, roomId);
                    intent2.putExtra(OWN_NUM, own_num);
                    intent2.putExtra(ROUND, round);
                    intent2.putExtra(WORD_NUM1, word_num1);
                    intent2.putExtra(WORD_NUM2, word_num2);
                    intent2.putExtra(WORD_NUM3, word_num3);
                    startActivity(intent2);
                    finish();
                }
            });
        } else {
            //Firestoreの部屋名のドキュメント内に1回目の点数を書き込む関数を実行
            if (round == 1) {   //ラウンド1(1回目の発音終了後)ならば
                if (own_num == 1) {  //プレイヤー1なら
                    judge(docRef, "fscore1", "fscore2", score);
                }
                if (own_num == 2) {  //プレイヤー2なら
                    judge(docRef, "fscore2", "fscore1", score);
                }
            }
            if (round == 2) {
                if (own_num == 1) {  //プレイヤー1なら
                    judge(docRef, "sscore1", "sscore2", score);
                }
                if (own_num == 2) {  //プレイヤー2なら
                    judge(docRef, "sscore2", "sscore1", score);
                }
            }
            if (round == 3) {
                if (own_num == 1) {  //プレイヤー1なら
                    judge(docRef, "tscore1", "tscore2", score);
                }
                if (own_num == 2) {  //プレイヤー2なら
                    judge(docRef, "tscore2", "tscore1", score);
                }
            }

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
                                    if (round >= 1) {
                                        TextView win_or_lose_textView1 = findViewById(R.id.win_or_lose_textView1);
                                        int fscore1 = Integer.parseInt(String.valueOf(document.get("fscore1")));    //プレイヤー1の1回目の点数を取得
                                        int fscore2 = Integer.parseInt(String.valueOf(document.get("fscore2")));    //プレイヤー2の1回目の点数を取得
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

                                        if (round >= 2) {
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

                                            if (round == 3) {
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
                                }
                            }
                        }
                    });

            if (round == 1 || round == 2) {
                Button start_round_button = findViewById(R.id.start_round_button);
                String s = "ラウンド" + (round + 1) + "の発音を開始";
                start_round_button.setText(s);
                start_round_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent2 = new Intent(getApplication(), OnlineBattle_CameraActivity.class);

                        intent2.putExtra(ROOM_ID, roomId);
                        intent2.putExtra(OWN_NUM, own_num);
                        intent2.putExtra(ROUND, round);
                        intent2.putExtra(WORD_NUM1, word_num1);
                        intent2.putExtra(WORD_NUM2, word_num2);
                        intent2.putExtra(WORD_NUM3, word_num3);
                        startActivity(intent2);
                        finish();
                    }
                });
            }
            if (round == 3) {                   //ラウンド3(3回目の発音が終わった後)なので対戦結果を表示するActivityに遷移
                Intent intent1 = new Intent(getApplication(), Activity2_1_1.class);

                intent1.putExtra(ROOM_ID, roomId);
                intent1.putExtra(OWN_NUM, own_num);
                intent1.putExtra(WORD_NUM1, word_num1);
                intent1.putExtra(WORD_NUM2, word_num2);
                intent1.putExtra(WORD_NUM3, word_num3);
                startActivity(intent1);
                finish();
            }
        }
    }
    //点数をCloudFirestoreに保存し、相手プレイヤーの点数がCloudFirestoreに保存されるまでループを行う関数
    public void judge(final DocumentReference docRef, final String myname, final String yourname, final int score){
        showToast("judge!");
        Map<String, Object> user = new HashMap<>();
        user.put(myname, score);
        docRef.update(user)
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

        //score2に値が入るまで無限ループ
        final boolean[] getdata = {false};
        for (int i = 0; i < 200 ; i++) {
            docRef.get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if(!getdata[0])
                                    if (document.exists()) {
                                        String score = String.valueOf(document.get(yourname));
                                        if (!score.equals("0")) {
                                            getdata[0] = true;
                                        }
                                    }
                            }
                        }
                    });
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
