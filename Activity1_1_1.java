//点数を表示する結果画面
package com.example.oralvoice;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.photo.Photo;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class Activity1_1_1 extends AppCompatActivity {

    //MediaRecorderのための変数宣言
    private static String audio_fileName = null;
    private MediaRecorder recorder = null;

    //選択された単語の番号を他のactivityに移すためのkey
    public static final String WORD_NUMBER = "WORD_NUMBER";

    //選択された単語の番号
    int word_number;

    //口の動きの点数
    double mouth_score;

    TextView score_textView;

    //レベルのTextView
    TextView level_textView;

    //次のレベルまでのポイントのTextView
    TextView need_point_textView;

    //レベルアップしたときに表示される「レベルアップ!」のTextView
    TextView level_up_textView;

    //アバターが成長したときに表示される「アバターが成長しました」のTextView
    TextView avatar_grew_textView;

    //点数が自己ベストだったときに表示される「自己ベスト!」のTextView
    TextView personal_best_textView;

    //順位が上位10に入ったときに表示される「トップ10!」のTextView
    TextView top10_textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //CameraActivityからデータを受け取る
        Intent receiveDataIntent = getIntent();
        word_number = receiveDataIntent.getIntExtra(CameraActivity.WORD_NUMBER, 0);

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_with_error);

        //CloudFirestoreのインスタンス、各コレクションのパス
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));
        final DocumentReference ranking_docRef = db.collection("ranking").document(word[word_number]);

        setContentView(R.layout.activity1_1_1);

        setTitle("結果 (" + word[word_number] + ")");

        // Toolbarの設定
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //アドバイスのTextView
        TextView advice_textView = findViewById(R.id.advice_textView);

        //口の動きの点数を計算、アドバイスのTextViewに計算する関数ないからテキストをセット
        mouth_score = mouth_evaluation(word[word_number].toLowerCase(), advice_textView);

        //点数のTextView (音声の点数を計算した後その関数内で点数をセットする)
        score_textView = findViewById(R.id.score_textView);

        //音声の点数を計算
        download_fire_storage(word[word_number]);

        //更新後のレベルのTextView
        level_textView = findViewById(R.id.level_textView);

        //獲得レベルポイントのTextView
        TextView got_point_textView = findViewById(R.id.got_point_textView);
        got_point_textView.setText(String.valueOf(Math.round(mouth_score / 10)));

        //次のレベルまでのポイントのTextView
        need_point_textView = findViewById(R.id.need_point_textView);

        //レベルアップしたときに表示される「レベルアップ!」のTextView
        level_up_textView = findViewById(R.id.level_up_textView);

        //アバターが成長したときに表示される「アバターが成長しました」のTextView
        avatar_grew_textView = findViewById(R.id.avatar_grew_textView);

        //順位のTextView
        TextView rank_textView = findViewById(R.id.rank_textView);

        //点数が自己ベストだったときに表示される「自己ベスト!」のTextView
        personal_best_textView = findViewById(R.id.personal_best_textView);

        //順位が上位10に入ったときに表示される「トップ10!」のTextView
        top10_textView = findViewById(R.id.top10_textView);

        //各種ユーザデータを更新
        updateUserData(user_docRef, word[word_number], (int)Math.round(mouth_score));
        updateLevel(user_docRef, (int)Math.round(mouth_score));
        updateRanking(ranking_docRef, rank_textView, (int)Math.round(mouth_score));

        //カメラのActivityに移動するButton
        Button once_again_button = findViewById(R.id.once_again_button);
        once_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //カメラのActivityを始める
                Intent intent = new Intent(getApplication(), CameraActivity.class);
                intent.putExtra(WORD_NUMBER, word_number);
                startActivity(intent);
                finish();
            }
        });

        //お手本動画をダウンロードしPopupWindowで表示するButton
        Button teacher_video_button = findViewById(R.id.teacher_video_button);
        teacher_video_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (word_number == 0 || word_number == 2 || (word_number >= 11 && word_number <= 16)) {
                    showToast("お手本動画をダウンロード中");
                    showVideoPopup(v, word[word_number].toLowerCase());
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
                Intent intent = new Intent(getApplication(), Activity1_1.class);
                intent.putExtra(WORD_NUMBER, word_number);
                startActivity(intent);
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

    //指定のドキュメントリファレンスに指定のMapを書き込む関数
    public void updateDataToFirestore (DocumentReference docRef, Map<String, Object> data) {
        docRef.update(data)
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

    //ユーザのセーブデータを更新する関数
    public void updateUserData(final DocumentReference docRef, final String word, final int score) {
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();

                            Map<String, Object> newData = new HashMap<>();

                            if (!document.exists()) {
                                showToast("データベースエラー");
                            } else {
                                int plays = Integer.parseInt(String.valueOf(document.get("plays")));
                                int plays_day_0 = Integer.parseInt(String.valueOf(document.get("plays_day_0")));
                                int plays_month_0 = Integer.parseInt(String.valueOf(document.get("plays_month_0")));
                                int plays_year_0 = Integer.parseInt(String.valueOf(document.get("plays_year_0")));
                                int average = Integer.parseInt(String.valueOf(document.get("average")));
                                int average_day_0 = Integer.parseInt(String.valueOf(document.get("average_day_0")));
                                int average_month_0 = Integer.parseInt(String.valueOf(document.get("average_month_0")));
                                int average_year_0 = Integer.parseInt(String.valueOf(document.get("average_year_0")));
                                int word_plays = Integer.parseInt(String.valueOf(document.get(word + "_plays")));
                                int word_plays_day_0 = Integer.parseInt(String.valueOf(document.get(word + "_plays_day_0")));
                                int word_plays_month_0 = Integer.parseInt(String.valueOf(document.get(word + "_plays_month_0")));
                                int word_plays_year_0 = Integer.parseInt(String.valueOf(document.get(word + "_plays_year_0")));
                                int word_avg = Integer.parseInt(String.valueOf(document.get(word + "_AVG")));
                                int word_max = Integer.parseInt(String.valueOf(document.get(word + "_MAX")));
                                int today_word_average = Integer.parseInt(String.valueOf(document.get("today_" + word + "_AVG")));

                                //わかりやすさのため一度変数に代入する。
                                int new_plays = plays + 1;
                                int new_plays_day_0 = plays_day_0 + 1;
                                int new_plays_month_0 = plays_month_0 + 1;
                                int new_plays_year_0 = plays_year_0 + 1;
                                int new_average = (plays * average + score) / new_plays;
                                int new_average_day_0 = (plays_day_0 * average_day_0 + score) / new_plays_day_0;
                                int new_average_month_0 = (plays_month_0 * average_month_0 + score) / new_plays_month_0;
                                int new_average_year_0 = (plays_year_0 * average_year_0 + score) / new_plays_year_0;
                                int new_word_plays = word_plays + 1;
                                int new_word_plays_day_0 = word_plays_day_0 + 1;
                                int new_word_plays_month_0 = word_plays_month_0 + 1;
                                int new_word_plays_year_0 = word_plays_year_0 + 1;
                                int new_word_avg = (word_plays * word_avg + score) / new_word_plays;
                                if (score > word_max) {
                                    int new_word_max = score;
                                    newData.put(word + "_MAX", new_word_max);
                                    personal_best_textView.setVisibility(View.VISIBLE);
                                }
                                int new_today_word_average = (word_plays_day_0 * today_word_average + score) / new_word_plays_day_0;

                                newData.put("plays", new_plays);
                                newData.put("plays_day_0", new_plays_day_0);
                                newData.put("plays_month_0", new_plays_month_0);
                                newData.put("plays_year_0", new_plays_year_0);
                                newData.put("average", new_average);
                                newData.put("average_day_0", new_average_day_0);
                                newData.put("average_month_0", new_average_month_0);
                                newData.put("average_year_0", new_average_year_0);
                                newData.put(word + "_plays", new_word_plays);
                                newData.put(word + "_plays_day_0", new_word_plays_day_0);
                                newData.put(word + "_plays_month_0", new_word_plays_month_0);
                                newData.put(word + "_plays_year_0", new_word_plays_year_0);
                                newData.put( word + "_AVG", new_word_avg);
                                newData.put("today_" + word + "_AVG", new_today_word_average);

                                updateDataToFirestore(docRef, newData);
                            }
                        } else {
                            showToast("エラー");
                        }
                    }
                });
    }

    //レベルを更新する関数
    public void updateLevel(final DocumentReference docRef, int score) {
        //レベル計算用の値を取得、処理する
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                boolean levelUp = false;
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        int level = Integer.parseInt(String.valueOf(document.get("level")));
                        int level_point = Integer.parseInt(String.valueOf(document.get("point")));
                        int next_point = Integer.parseInt(String.valueOf(document.get("next_point")));

                        //レベルを計算
                        level_point += (int)Math.round(score / 10.0);
                        if (level_point >= next_point) {
                            level_point -= next_point;
                            level += 1;
                            next_point += 10;
                            levelUp = true;
                        }

                        level_textView.setText(String.valueOf(level));
                        level_textView.setTextSize(30);
                        need_point_textView.setText(String.valueOf(next_point - level_point));

                        if (levelUp) {
                            level_up_textView.setVisibility(View.VISIBLE);
                            if (level == 2 || level == 5 || level == 9 || level == 15) {
                                avatar_grew_textView.setVisibility(View.VISIBLE);
                            }
                        }

                        //レベル関係の値を更新
                        setDataToFirestore(docRef, "level", level);
                        setDataToFirestore(docRef, "point", level_point);
                        setDataToFirestore(docRef, "next_point", next_point);

                    } else {
                        showToast("データベースのエラー");
                    }
                } else {
                    showToast("データベースへのアクセスに失敗しました");
                }
            }
        });
    }

    //自己ベストの点数または初めてならランキングに追加する関数 (点数のランクをTextViewにセットする処理も含む)
    public void updateRanking(final DocumentReference docRef, final TextView rank_text_view, final int score) {
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                        final String UserID = getUserData(1);

                        Map<String, Object> rankingData;

                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                rankingData = (document.getData());
                                Object data = document.get(UserID);

                                //dataがnullならランキング初参加
                                if (data == null) {
                                    setDataToFirestore(docRef, UserID, score);
                                } else if (Integer.parseInt(String.valueOf(data)) < score) {
                                    setDataToFirestore(docRef, UserID, score);
                                }

                                //取得したランキングを降順にソート
                                List<Map.Entry<String, Object>> list_entries = new ArrayList<>(rankingData.entrySet());
                                Collections.sort(list_entries, (obj1, obj2) -> Integer.valueOf(obj2.getValue().toString()).compareTo(Integer.valueOf(obj1.getValue().toString())));
                                int count = 1;
                                for(Map.Entry<String, Object> entry : list_entries) {
                                    if (Integer.parseInt(String.valueOf(entry.getValue())) <= score) {
                                        rank_text_view.setText(String.valueOf(count));
                                        rank_text_view.setTextSize(30);

                                        if (count <= 10) {
                                            top10_textView.setVisibility(View.VISIBLE);
                                        }
                                        break;
                                    } else {
                                        count += 1;
                                    }
                                }
                            }
                        }
                    }
                });
    }

    //レベルアップ時のPopupWindowを表示する関数
    void showVideoPopup(View v, String word) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.popup1, null);

        PopupWindow pw = new PopupWindow(this);
        pw.setContentView(layout);

        pw.setFocusable(true);
        pw.setAnimationStyle(R.style.Animation1);
        pw.setBackgroundDrawable(null);

        VideoView teacher_videoView = layout.findViewById(R.id.teacher_videoView);
        storageRef.child("teacher_video/" + word + ".mp4")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        teacher_videoView.setVideoURI(uri);
                        teacher_videoView.seekTo(0);

                        //親Viewを指定し、PopupWindowを表示
                        pw.showAtLocation(v, Gravity.CENTER, 1, 1);
                    }
                });

        //再生するButton
        final boolean[] pressed = {false};
        ImageButton start_pause_button = layout.findViewById(R.id.start_pause_button);
        start_pause_button.setImageResource(R.drawable.startvideo2);
        start_pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pressed[0]) {
                    teacher_videoView.start();
                    pressed[0] = true;
                    start_pause_button.setImageResource(R.drawable.pausevideo);
                } else {
                    teacher_videoView.pause();
                    pressed[0] = false;
                    start_pause_button.setImageResource(R.drawable.startvideo2);
                }

            }
        });

        //リスタートするButton
        ImageButton restart_button = layout.findViewById(R.id.restart_button);
        restart_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                teacher_videoView.seekTo(0);
                teacher_videoView.start();
                pressed[0] = true;
                start_pause_button.setImageResource(R.drawable.pausevideo);
            }
        });

        teacher_videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                teacher_videoView.seekTo(0);
                pressed[0] = false;
                start_pause_button.setImageResource(R.drawable.startvideo2);
            }
        });

        //消すButton
        ImageButton dismiss_button = layout.findViewById(R.id.dismiss_button);
        dismiss_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do Something

                //Close Window
                pw.dismiss();
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

/************************************ここから口の動きの点数計算*/
    //口の動きの点数を計算する関数
    public double mouth_evaluation(String userWord, TextView text_advice){
        // 撮影したフレームをすべて取得する
        int frameNum = -1;
        ArrayList<Mat> frames = new ArrayList<>();
        while(true){
            frameNum++;
            String frameFilename = String.valueOf(frameNum) + ".jpg";
            final String framePath = getFilesDir().getPath() + File.separator + frameFilename;
            File frameFile = new File(framePath);
            if (!frameFile.exists()) {
                if (frameNum == 0){
                    showToast("動画を撮影してください");
                    return -1;
                }
                break;
            }
            Mat mat = Imgcodecs.imread(framePath);
            frames.add(mat);
            frameFile.delete();
        }
        // ユーザが発音した単語の分類器の数　と　全分類器を取得する
        int wordCascadeNum = -1;
        ArrayList<File> wordCascadeFiles = new ArrayList<>();
        while(true){
            wordCascadeNum++;
            String wordCascadeFilename = String.valueOf(wordCascadeNum) + ".xml";
            final String wordCascadePath = getFilesDir().getPath() + File.separator + wordCascadeFilename;
            File wordCascadeFile = new File(wordCascadePath);
            wordCascadeFiles.add(wordCascadeFile);
            if (!wordCascadeFile.exists()) {
                //showToast(faceCascadePath + " not found");
                try (InputStream inputStream = getAssets().open("cascades/" + userWord + "/" + wordCascadeFilename);
                     FileOutputStream fileOutputStream = new FileOutputStream(wordCascadeFile, false)) {
                    byte[] buffer = new byte[2048];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        // ユーザの動画のフレーム数と分類器数の差
        int diffFramesAndCascade = frameNum - wordCascadeNum;
        if (diffFramesAndCascade < 0){
            showToast("動画のフレーム数が足りません");
            return -1;
        }
        // 何枚ごとにフレームを捨てるか（何枚ごとに評価に使用するフレームとするか）
        int everyThrowAway;
        if (diffFramesAndCascade != 0){
            everyThrowAway = frameNum / diffFramesAndCascade;
        } else{
            everyThrowAway = frameNum + 1;
        }
        // 何枚捨てたか記憶する
        int throwAwayNum = 0;
        // framesの中から口元評価に使用するフレームを選択し、選択したフレームに映る口の形を評価する
        int score[] = new int[wordCascadeNum];
        for (int i = 0; i < frameNum; i++){
            if ((throwAwayNum != diffFramesAndCascade) && (((i+1) % everyThrowAway == 0))){
                throwAwayNum++;
            } else {
                Mat face = cutFaceFromImage(frames.get(i));
                Mat underFace = new Mat(face, new Rect(face.cols()/6, face.rows()/2, face.cols()*4/6, face.rows()/2));
                CascadeClassifier cascade = new CascadeClassifier(wordCascadeFiles.get(i - throwAwayNum).getAbsolutePath());
                score[i - throwAwayNum] = getMouthShapeScore(underFace, cascade);
                wordCascadeFiles.get(i - throwAwayNum).delete();
            }
        }
        /*showToast(String.valueOf(wordCascadeNum));
        showToast("FrameNum" + frameNum);
        showToast("ThrowAwayNum" + throwAwayNum);*/
        double ans = 0.0;
        int minScore = 100;
        int minIndex = 0;
        for (int i = 0; i < wordCascadeNum; i++){
            ans += score[i];
            if (minScore > score[i]){
                minScore = score[i];
                minIndex = i;
            }
        }
        int wordLen = userWord.length(); // 7
        int temp = wordCascadeNum/wordLen;  // 2
        int minWordNum = minIndex / temp;
        if (userWord.equals("english")) {
            userWord = "English";
        }
        String c = userWord.substring(minWordNum, minWordNum+1);
        if (userWord.equals("English")) {
            if (c.equals("s") || c.equals("h")) {
                c = "sh";
            }
        } else if (userWord.equals("think")) {
            if (c.equals("t") || c.equals("h")) {
                c = "th";
            }
        }
        if(ans/wordCascadeNum < 60) {
            text_advice.setText(userWord + "の " + c + " の発音がおかしいよ");
        } else if (ans/wordCascadeNum < 90) {
            text_advice.setText(userWord + "の " + c + "の発音を気を付けると高得点が狙えるよ");
        } else {
            text_advice.setText("完璧！！");
        }

        if (ans/wordCascadeNum + 30 > 100) {
            return 100;
        }
        return ans / wordCascadeNum + 20;
    }

    // 画像から顔の
    public Mat cutFaceFromImage(Mat img){
        // 顔検出ファイルの名前
        final String faceCascadeFilename = "haarcascade_frontalface_default.xml";
        // 顔検出ファイルのパス
        final String faceCascadePath = getFilesDir().getPath() + File.separator + faceCascadeFilename;
        File faceCascadeFile = new File(faceCascadePath);
        // ファイルが存在しないならassetsフォルダからコピーしてくる
        if (!faceCascadeFile.exists()) {
            //showToast(faceCascadePath + " not found");
            try (InputStream inputStream = getAssets().open("cascades/" + faceCascadeFilename);
                 FileOutputStream fileOutputStream = new FileOutputStream(faceCascadeFile, false)) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
                showToast("show cutFaceFromImage");
                return img;
            }
        }

        CascadeClassifier faceCascade = new CascadeClassifier(faceCascadeFile.getAbsolutePath());
        MatOfRect faceRects = new MatOfRect();
        Size minSize = new Size(img.width() / 7,img.height() / 7);
        // 顔が検出されるまで続ける
        for(int i = 5; i > 2; i-=2){
            faceCascade.detectMultiScale(img, faceRects,1.11, i,0, minSize);
            if (faceRects.toArray().length != 0){
                break;
            }
        }
        // 顔が検出されたら切り取る
        if(faceRects.toArray().length > 0){
            int centerImg[] = {img.width()/2, img.height()/2};
            Rect roi = new Rect(0, 0, 0, 0);
            for(Rect rect : faceRects.toArray()){
                // 検出されたなかで画像の中心に近いものを顔と認識する
                if (Math.sqrt(Math.pow(centerImg[0] - (roi.x + roi.width / 2), 2) + Math.pow(centerImg[1] - (roi.y + roi.height / 2), 2)) > Math.sqrt(Math.pow(centerImg[0] - (rect.x + rect.width / 2), 2) + Math.pow(centerImg[1] - (rect.y + rect.height / 2), 2))){
                    roi = rect;
                }
            }
            // 顔を切り取る
            img = new Mat(img, roi);
        }
        // 顔検出ファイルを削除
        faceCascadeFile.delete();
        // 顔が検出されなかった場合、なにも切り取らずにimgが返される
        return img;
    }

    public int getMouthShapeScore(Mat mouth, CascadeClassifier cascade){

        MatOfRect mouthRects = new MatOfRect();
        double rate = 0.0;
        for(int i = 0; i<6; i++){
            cascade.detectMultiScale(mouth, mouthRects, 1.11, 1 + i);
            if(mouthRects.toArray().length > 0){
                rate += 1;
            }
        }

        return (int)(rate/6*100);
    }
/*****************************************ここまで口の動き*/

/*****************************************ここから音声の点数計算*/
    //storageから評価する画像ファイル(png)をダウンロードする関数
    private void download_fire_storage(String word) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference test_Ref = storageRef.child("test.png");
        String imgPath = getFilesDir().getPath()+"/test.png";
        File imgFile = new File(imgPath);

        test_Ref.getFile(imgFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
        voice_evaluation(imgPath, word);//分類器に画像ファイルを入力して点数を取得
    }

    //分類器(学習モデル)に評価する画像を入力して評価(点数)を取得する関数
    public void voice_evaluation(String Path, String word) {
        //ダウンロードする分類器を選択
        FirebaseCustomRemoteModel remoteModel =
                new FirebaseCustomRemoteModel.Builder(word).build();
        //Firebaseから分類器をダウンロード
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        // Download complete. Depending on your app, you could enable
                        // the ML feature, or switch from the local model to the remote
                        // model, etc.
                    }
                });

        //filesから評価する画像をbitmap形式で取得
        Bitmap InputImage = BitmapFactory.decodeFile(Path);;
        Bitmap bitmap = Bitmap.createScaledBitmap(InputImage, 256, 256, true);
        final ByteBuffer input = ByteBuffer.allocateDirect(256 * 256 * 3 * 4).order(ByteOrder.nativeOrder());
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int px = bitmap.getPixel(x, y);

                // Get channel values from the pixel value.
                int r = Color.red(px);
                int g = Color.green(px);
                int b = Color.blue(px);

                // Normalize channel values to [-1.0, 1.0]. This requirement depends
                // on the model. For example, some models might require values to be
                // normalized to the range [0.0, 1.0] instead.
                float rf = (r - 127) / 255.0f;
                float gf = (g - 127) / 255.0f;
                float bf = (b - 127) / 255.0f;

                input.putFloat(rf);
                input.putFloat(gf);
                input.putFloat(bf);
            }
        }
        //TensorFlow Liteインタープリターを初期化
        FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
                .addOnCompleteListener(new OnCompleteListener<File>() {
                    @Override
                    public void onComplete(@NonNull Task<File> task) {
                        File modelFile = task.getResult();
                        if (modelFile != null) {
                            Interpreter interpreter = new Interpreter(modelFile);
                            int bufferSize = 2 * java.lang.Float.SIZE / java.lang.Byte.SIZE;
                            ByteBuffer modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
                            interpreter.run(input, modelOutput);
                            modelOutput.rewind();
                            FloatBuffer probabilities = modelOutput.asFloatBuffer();
                            float probability = probabilities.get(0);//音声の点数

                            //平均を計算し、点数のTextViewにセット
                            int voice_score = (int)Math.round(probability);
                            int average_score = (int) Math.round((mouth_score + voice_score) / 2);
                            score_textView.setText(String.valueOf(average_score));
                        }
                    }
                });
    }
}
