package com.example.oralvoice;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Frag4 extends Fragment {

    //FirebaseUIで使う値
    private static final int RC_SIGN_IN = 123;
    private static final int RESULT_OK = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag4, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //CloudFirestoreのインスタンス、ユーザごとのコレクションのパス
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));

        //アバターのImageView
        ImageView avatar_imageView = view.findViewById(R.id.avatar_imageView);
        setAvatarImg(user_docRef, avatar_imageView, 0.2f);

        //ユーザ名のTextView
        TextView textView1 = view.findViewById(R.id.textView1);
        textView1.setText(getUserData(1));

        //ユーザIDのTextView
        TextView textView2 = view.findViewById(R.id.textView2);
        textView2.setText(getUserData(2));

        //メールアドレスのTextView
        TextView textView3 = view.findViewById(R.id.textView3);
        textView3.setText(getUserData(0));

        //利用開始日のTextView
        TextView textView4 = view.findViewById(R.id.textView4);
        setFirestoreData(user_docRef, "date", textView4, "");

        //レベルのTextView
        TextView textView5 = view.findViewById(R.id.textView5);
        setFirestoreData(user_docRef, "level", textView5, "");

        //アカウントを切り替えるButton
        Button button1 = view.findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                startFirebaseUI();
            }
        });

        //アカウントを削除するButton
        Button button2 = view.findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });
    }

    @Override
    public void onStart() {
        //このFragmentが画面にでるときにはアバターとレベルを最新のものに更新する
        super.onStart();
        Fragment currentFragment = this;
        View view = currentFragment.getView();
        //Firebaseの処理
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        //アバター
        ImageView avatar_imageView = view.findViewById(R.id.avatar_imageView);
        setAvatarImg(user_docRef, avatar_imageView, 0.2f);
        //レベル
        TextView textView5 = view.findViewById(R.id.textView5);
        setFirestoreData(user_docRef, "level", textView5, "");
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                //成功
                //showToast("ログイン成功!");

                //新しいアカウントなら初期データを追加
                checkNewUser();

                refleshCurrentFragment();
            } else {
                //失敗
                //showToast("ログイン失敗!");

                refleshCurrentFragment();
            }
        }
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

    //新しいアカウントか確認し、新しいアカウントなら初期データをセットする関数
    public void checkNewUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(getUserData(2))
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
                                //showToast("exists!");
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

    //cloud firestoreに新しいアカウント用の初期データを追加する関数
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
                        showToast("初期データセット完了!");
                        refleshCurrentFragment();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //失敗
                        showToast("書き込み失敗");
                    }
                });
    }

    //Calendarクラスのインスタンスから日付の文字列を返す(yyyy/MM/dd)
    public String calendarToString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE);
    }

    //アカウントおよびそのデータを削除する関数
    public void deleteAccount() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userID = getUserData(2);
        if (currentUser == null) {
            showToast("ログインしてください!");
        } else {
            currentUser.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                showToast("アカウントを削除しました");
                                //startFirebaseUI();
                                deleteData(userID);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showToast("アカウントの削除失敗, ログインしなおして再度試してください");
                            mAuth.signOut();
                            startFirebaseUI();
                        }
                    });
        }
    }

    //アカウントのセーブデータを削除する関数
    public void deleteData(String userID) {
        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_no_error);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userID)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showToast("セーブデータを削除しました");
                        startFirebaseUI();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("データの削除に失敗、再度試してください");
                    }
                });

        Map<String,Object> updates = new HashMap<>();
        updates.put(userID, FieldValue.delete());

        for (String str: word) {
            db.collection("ranking").document(str)
                    .update(updates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //showToast("ランキングからデータを削除しました");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showToast("ランキングからのデータの削除に失敗、再度試してください");
                        }
                    });
        }
    }

    //レベルに応じてアバターの画像をbitmap形式でセットする関数
    public void setAvatarImg (DocumentReference docRef, final ImageView imageView, final float ratio) {
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                //アバターの画像のID
                int[] avatar = {R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3, R.drawable.avatar4, R.drawable.avatar5};
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Object data = document.get("level");
                        if (data != null) {
                            String str = data.toString();
                            int level = Integer.parseInt(str);
                            if (level == 1) {
                                imageView.setImageBitmap(resizeImage(ratio, avatar[0]));
                            } else if (level < 5) {
                                imageView.setImageBitmap(resizeImage(ratio, avatar[1]));
                            } else if (level < 9) {
                                imageView.setImageBitmap(resizeImage(ratio, avatar[2]));
                            } else if (level < 15) {
                                imageView.setImageBitmap(resizeImage(ratio, avatar[3]));
                            } else {
                                imageView.setImageBitmap(resizeImage(ratio, avatar[4]));
                            }
                        }
                    }
                }
            }
        });
    }

    //これを実行するフラグメントをリフレッシュする関数
    public void refleshCurrentFragment() {
        FragmentManager fm = getFragmentManager();
        if (fm != null) {
            Fragment currentFragment = this;
            FragmentTransaction ft = fm.beginTransaction();
            ft.detach(currentFragment);
            ft.attach(currentFragment);
            ft.commit();
        } else {
            showToast("fm == null");
        }
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

    //Toastで指定の文字列を表示する関数
    public void showToast(String showString) {
        Toast toast = Toast.makeText (
                getActivity(),
                showString,
                Toast.LENGTH_LONG
        );
        toast.show();
    }
}

