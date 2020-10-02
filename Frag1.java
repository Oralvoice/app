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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Frag1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //タイトルの画像のImageView
        ImageView title_imageView = view.findViewById(R.id.title_imageView);
        title_imageView.setImageBitmap(resizeImage(0.3f, R.drawable.title));

        //個人練習モード、単語選択のActivityに移動するButton
        Button button1 = view.findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    //単語選択のActivityを始める
                    Intent intent = new Intent(getActivity(), Activity1.class);
                    startActivity(intent);
                } else {
                    showToast("ログインしてください");
                }
            }
        });

        //オンライン対戦モード、マッチングのActivityに移動するButton
        Button button2 = view.findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    //マッチングのActivityを始める
                    Intent intent = new Intent(getActivity(), Activity2.class);
                    startActivity(intent);
                } else {
                    showToast("ログインしてください");
                }
            }
        });
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
                getActivity().getApplicationContext(),
                showString,
                Toast.LENGTH_LONG
        );
        toast.show();
    }
}
