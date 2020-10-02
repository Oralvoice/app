package com.example.oralvoice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;

import com.example.oralvoice.ui.main.SectionsPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //画面下のタブにセットするアイコンのID
    private int[] tab_icons = {R.drawable.ic_tab_home, R.drawable.ic_tab_record, R.drawable.ic_tab_ranking, R.drawable.ic_tab_user};

    //FirebaseUIで使う値
    private static final int RC_SIGN_IN = 123;

    //openCVのライブラリをロード
    static {
        System.loadLibrary("opencv_java4");
    }

    //CloudFirestoreのインスタンス
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ここで1秒間スリープし、スプラッシュを表示させたままにする。
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        // スプラッシュthemeを通常themeに変更する
        setTheme(R.style.AppTheme);

        if (db == null) {
            showToast("db_null");
        }

        //FirebaseAuthのインスタンス、使っているユーザ
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        //未ログインならログイン画面(FirebaseUI)に移動
        if (currentUser == null) {
            startFirebaseUI();
        } else {
            //ログイン状態ならユーザの日付に関連するデータを更新、単語ごとのデータに欠損があれば追加
            checkUserWordData();
            updateUserDateData();
        }

        setContentView(R.layout.activity_main);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        setUpTabIcons(tabs);

    }

    //タブにアイコンをセットする関数
    private void setUpTabIcons(TabLayout tabLayout) {
        for (int i = 0; i < tab_icons.length; i++) {
            tabLayout.getTabAt(i).setIcon(tab_icons[i]);
        }
    }

    //firebaseUIに移る関数
    public void startFirebaseUI () {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setTheme(R.style.AppTheme)
                        .build(),
                RC_SIGN_IN);
    }

    //firebaseUIのonActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                //成功
                //showToast("ログイン成功!");

                //新しいアカウントなら初期データを追加
                checkNewUser();
            } else {
                //失敗
                //showToast("ログイン失敗!");
            }
        }
    }

    //新しいアカウントか確認し、新しいアカウントなら初期データをセットする関数
    public void checkNewUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = getUserData(2);
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();

                    //document.exists()がfalseなら新しいアカウント
                    if(!document.exists()) {
                        //初期データをセット
                        setDefaultData();
                    } else {
                        showToast("exists!");
                    }
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("チェック失敗");
                    }
                });
    }

    //cloudFirestoreに新しいアカウント用の初期データを追加する関数
    public void setDefaultData() {
        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_no_error);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userName = getUserData(1);
        String userId = getUserData(2);
        Calendar now = Calendar.getInstance();
        String now_string = calendarToString(now);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", userName);
        userData.put("level", 1);
        userData.put("point", 0);
        userData.put("next_point", 10);
        userData.put("date", now_string);
        userData.put("recent_date_str", now_string);
        userData.put("plays", 0);
        userData.put("average", 0);
        for (int i = 0; i < 30; i++) {
            userData.put("average_day_" + i, 0);
            userData.put("plays_day_" + i, 0);
        }
        for (int i = 0; i < 12; i++) {
            userData.put("average_month_" + i, 0);
            userData.put("plays_month_" + i, 0);
        }
        userData.put("average_year_0", 0);
        userData.put("plays_year_0", 0);
        for (String str : word) {
            userData.put(str + "_plays", 0);
            userData.put(str + "_MAX", 0);
            userData.put(str + "_AVG", 0);
            userData.put("today_" + str + "_AVG", 0);
            for (int i = 0; i < 30; i++) {
                userData.put(str + "_plays_day_" + i, 0);
            }
            for (int i = 0; i < 12; i++) {
                userData.put(str + "_plays_month_" + i, 0);
            }
            userData.put(str + "_plays_year_0", 0);
        }

        showToast("初期データセット実行!");
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //成功
                        //showToast("初期データセット完了!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //失敗
                        //showToast("書き込み失敗");
                    }
                });
    }

    //Calendarクラスのインスタンスから日付の文字列を返す(yyyy/MM/dd)
    public String calendarToString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE);
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

    //データベースのユーザのデータの日付関連のデータを更新する関数
    public void updateUserDateData() {
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Calendar now = Calendar.getInstance();
                                String recent_date_str = String.valueOf(document.get("recent_date_str"));
                                Calendar recent_date = Calendar.getInstance();
                                recent_date.setTime(stringToDate(recent_date_str));

                                int year_difference = now.get(Calendar.YEAR) - recent_date.get(Calendar.YEAR);
                                if (year_difference >= 2) {
                                    //空白の年ありで年を越したとき
                                    //越した年数に応じてyear_iを更新、12か月以上間が空いているのでmonth_iとday_iをすべて0

                                    updateYear_i(year_difference);
                                    updateMonth_i(0);
                                    updateDay_i(0);

                                } else if (year_difference  == 1){
                                    //year_iを1年分更新

                                    updateYear_i(1);

                                    int month_difference = now.get(Calendar.MONTH) - now.get(Calendar.MONTH) + 12;
                                    if (month_difference >= 2) {
                                        //空白の年無しで年を越し、空白の月ありで月を越したとき
                                        //越した月数に応じてmonth_iを更新、30日以上間が空いているのでday_iをすべて0

                                        updateMonth_i(month_difference);
                                        updateDay_i(0);

                                    } else if (month_difference == 1) {
                                        //空白の年無しで年を越し、空白の月無しで月を越したとき(12月から1月への年越し)
                                        //month_iを1か月分更新、経過日数に応じてday_iを更新、経過日数は月の日数に応じて計算(12/31から1/31だったらday_iはすべて0)

                                        updateMonth_i(1);

                                        int day_difference = now.get(Calendar.DATE) - recent_date.get(Calendar.DATE) + 31;
                                        if (day_difference >= 30) {
                                            day_difference = 0;
                                        }
                                        updateDay_i(day_difference);

                                    } else {
                                        //1年後の同じ月のとき
                                        //month_iとday_iをすべて0

                                        updateMonth_i(0);
                                        updateDay_i(0);

                                    }
                                } else {
                                    //年を越していないとき
                                    //year_iはそのまま

                                    int month_difference = now.get(Calendar.MONTH) - now.get(Calendar.MONTH);
                                    if (month_difference >= 2) {
                                        //年を越さず、空白の月ありで月を越したとき
                                        //越した月数に応じてmonth_iを更新、30日以上間が空いているのでday_iはすべて0

                                        updateMonth_i(month_difference);
                                        updateDay_i(0);

                                    } else if (month_difference == 1) {
                                        //年を越さず、空白の月無しで月を越したとき
                                        //month_iを1か月分更新、経過日数に応じてdat_iを更新、経過日数は月の日数に応じて計算

                                        updateMonth_i(1);
                                        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
                                        int day_difference = now.get(Calendar.DATE) - recent_date.get(Calendar.DATE) + days[recent_date.get(Calendar.MONTH)];
                                        if (recent_date.get(Calendar.YEAR) % 4 == 0 && recent_date.get(Calendar.MONTH) == Calendar.FEBRUARY) {  //閏年の時は2月が+
                                            day_difference += 1;
                                        }
                                        if (day_difference >= 30) {
                                            day_difference = 0;
                                        }
                                        updateDay_i(day_difference);

                                    } else {
                                        //年も月も越していないとき
                                        //経過日数に応じてday_iを更新、ほかはそのまま

                                        int day_difference = now.get(Calendar.DATE) - recent_date.get(Calendar.DATE);
                                        if (day_difference != 0) {
                                            if (day_difference >= 30) {
                                                day_difference = 0;
                                            }
                                            updateDay_i(day_difference);
                                        }
                                    }
                                }

                                setDataToFirestore(user_docRef, "recent_date_str", calendarToString(now));
                            }
                        }
                    }
                });
    }

    //(yyyy/MM/dd)の文字列からDate型の値を返す関数
    public Date stringToDate(String str) {
        String format = "yyyy/MM/dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setLenient(false);

        try {
            return dateFormat.parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

    //year_iのデータを指定の年数分ずらす関数
    public void updateYear_i(int year_difference) {
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Map<String, Object> allData = new HashMap<>();
                                allData = document.getData();
                                Map<String, Object> year_i = new HashMap<>();

                                Date date_date = document.getDate("date_date");
                                Calendar date_cal = Calendar.getInstance();
                                date_cal.setTime(date_date);
                                Calendar now = Calendar.getInstance();
                                int play_years = now.get(Calendar.YEAR) - date_cal.get(Calendar.YEAR);
                                for (int i = 0; i < year_difference; i++) {
                                    year_i.put("average_year_" + i, 0);
                                    year_i.put("plays_year_" + i, 0);
                                }
                                for (int i = play_years - year_difference; i >= 0; i--) {
                                    year_i.put("average_year_" + (i + year_difference), allData.get("average_year_" + i));
                                    year_i.put("plays_year_" + (i + year_difference), allData.get("plays_year_" + i));
                                }
                                updateDataToFirestore(user_docRef, year_i);
                            }
                        }
                    }
                });
    }

    //month_iのデータを指定の月数分ずらす関数、引数が0のときはmonth_iをすべて0
    public void updateMonth_i(int month_difference) {
        final String[] word = getResources().getStringArray(R.array.words_no_error);
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));

        Map<String, Object> month_i = new HashMap<>();
        if (month_difference != 0) {
            user_docRef.get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Map<String, Object> allData = new HashMap<>();
                                    allData = document.getData();

                                    for (int i = 0; i < month_difference; i++) {
                                        month_i.put("average_month_" + i, 0);
                                        month_i.put("plays_month_" + i, 0);
                                        for (String s: word) {
                                            month_i.put(s + "_plays_month_" + i, 0);
                                        }
                                    }
                                    for (int i = 11 - month_difference; i >= 0; i--) {
                                        month_i.put("average_month_" + (i + month_difference), allData.get("average_month_" + i));
                                        month_i.put("plays_month_" + (i + month_difference), allData.get("plays_month_" + i));
                                        for (String s: word) {
                                            month_i.put(s + "_plays_month_" + (i + month_difference), allData.get(s + "_plays_month_" + i));
                                        }
                                    }
                                    updateDataToFirestore(user_docRef, month_i);
                                }
                            }
                        }
                    });
        } else {
            for (int i = 0; i < 12; i++) {
                month_i.put("average_month_" + i, 0);
                month_i.put("plays_month_" + i, 0);
                for (String s : word) {
                    month_i.put(s + "_plays_month_" + i, 0);
                }
            }
            updateDataToFirestore(user_docRef, month_i);
        }
    }

    //day_iのデータを指定の日数分ずらす関数、引数が0のときはday_iをすべて0
    public void updateDay_i(int day_difference) {
        final String[] word = getResources().getStringArray(R.array.words_no_error);
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));

        Map<String, Object> day_i = new HashMap<>();
        for (String s: word) {
            day_i.put("today_" + s + "_AVG", 0);
        }
        if (day_difference != 0) {
            user_docRef.get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Map<String, Object> allData = new HashMap<>();
                                    allData = document.getData();

                                    for (int i = 0; i < day_difference; i++) {
                                        day_i.put("average_day_" + i, 0);
                                        day_i.put("plays_day_" + i, 0);
                                        for (String s: word) {
                                            day_i.put(s + "_plays_day_" + i, 0);
                                        }
                                    }
                                    for (int i = 29 - day_difference; i >= 0; i--) {
                                        day_i.put("average_day_" + (i + day_difference), allData.get("average_day_" + i));
                                        day_i.put("plays_day_" + (i + day_difference), allData.get("plays_day_" + i));
                                        for (String s: word) {
                                            day_i.put(s + "_plays_day_" + (i + day_difference), allData.get(s + "_plays_day_" + i));
                                        }
                                    }
                                    updateDataToFirestore(user_docRef, day_i);
                                }
                            }
                        }
                    });
        } else {
            for (int i = 0; i < 30; i++) {
                day_i.put("average_day_" + i, 0);
                day_i.put("plays_day_" + i, 0);
                for (String s : word) {
                    day_i.put(s + "_plays_day_" + i, 0);
                }
            }
            updateDataToFirestore(user_docRef, day_i);
        }
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

    //ユーザの単語ごとのデータを確認する関数
    public void checkUserWordData() {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));

        final String[] word = getResources().getStringArray(R.array.words_no_error);
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                for (String s: word) {
                                    Object data = document.get(s + "_plays");
                                    if (data == null) {
                                        setWordDefaultData(s);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    //単語を追加実装した際など、CloudFirestoreに単語の初期データをセットする関数
    public void setWordDefaultData(String word) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));

        Map<String, Object> word_default_data = new HashMap<>();

        word_default_data.put(word + "_plays", 0);
        word_default_data.put(word + "_MAX", 0);
        word_default_data.put(word + "_AVG", 0);
        word_default_data.put("today_" + word + "_AVG", 0);

        for (int i = 0; i < 30; i++) {
            word_default_data.put(word + "_plays_day_" + i, 0);
        }
        for (int i = 0; i < 12; i++) {
            word_default_data.put(word + "_plays_month_" + i, 0);
        }

        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Calendar now = Calendar.getInstance();
                                String start_date_str = String.valueOf(document.get("date"));
                                Calendar start_date = Calendar.getInstance();
                                start_date.setTime(stringToDate(start_date_str));

                                int play_years = now.get(Calendar.YEAR) - start_date.get(Calendar.YEAR);
                                for (int i = 0; i <= play_years; i++) {
                                    word_default_data.put(word + "_plays_year_" + i, 0);
                                }
                            }
                            updateDataToFirestore(user_docRef, word_default_data);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        updateDataToFirestore(user_docRef, word_default_data);
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

    //ファイル書き込み関数、テスト用
    public void saveTextData(String file_name, String data) {
        try(FileOutputStream fileOutputStream = openFileOutput(file_name, Context.MODE_APPEND)) {
            fileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}