package com.example.oralvoice;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class CameraActivity extends AppCompatActivity {

    //カメラの使用リクエスト
    private int REQUEST_CODE_FOR_CAMERA_PERMISSIONS = 1234;;
    private final String[] CAMERA_REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    //マイクの使用リクエスト
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] audio_permissions = {Manifest.permission.RECORD_AUDIO};

    //カメラからの入力の出力先View
    private PreviewView previewView;
    private ImageView imageView;

    //CameraXのための変数宣言
    private Camera camera = null;
    private Preview preview = null;
    private ImageAnalysis imageAnalysis = null;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    //MediaRecorderのための変数宣言
    private static String audio_fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    //撮影したフレーム数
    private int frameNum = 0;

    //撮影前のカウントダウンのための変数宣言
    private int nowCount = 3;
    long contNumber = nowCount * 1000;
    long interval = 10;
    final CountDown countDown = new CountDown(contNumber, interval);

    //選択された単語の番号を他のactivityに移すためのkey
    public static final String WORD_NUMBER = "WORD_NUMBER";

    //選択された単語の番号
    int word_number;

    //撮影を停止するButton
    Button stop_button;

    //カウントダウンのTextView
    TextView timerText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //マイクの使用リクエスト
        ActivityCompat.requestPermissions(this, audio_permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //録音するファイルのパス
        audio_fileName = getExternalCacheDir().getAbsolutePath();
        audio_fileName += "/test.wav";

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_with_error);

        //発音記号の画像IDを入れる配列
        final int[] phonetic_symbol_id = getResourceIds(getResources(), R.array.phonetic_ids_with_error);

        //Activity1_1またはActivity1_1_1からデータを受け取る
        Intent receiveDataIntent = getIntent();
        int act1_1_word_number = receiveDataIntent.getIntExtra(Activity1_1.WORD_NUMBER, 0);
        int act1_1_1_word_number = receiveDataIntent.getIntExtra(Activity1_1.WORD_NUMBER, 0);

        //Activity1_1かActivity1_1_1のうち、受け取ったほうのデータを使う
        if (act1_1_word_number != 0) {
            word_number = act1_1_word_number;
        } else if (act1_1_1_word_number != 0) {
            word_number = act1_1_1_word_number;
        } else {
            word_number = 0;
        }

        setContentView(R.layout.camera_activity);

        //カメラのPreviewView
        previewView = findViewById(R.id.previewView);

        //単語と発音記号のLinearLayout
        LinearLayout linearLayout = findViewById(R.id.linear_layout);
        linearLayout.bringToFront();

        //単語のTextView
        TextView word_textView = findViewById(R.id.word_textView);
        word_textView.setText(word[word_number]);
        word_textView.bringToFront();

        //発音記号の画像のImageView
        ImageView phonetic_symbol_imageView = findViewById(R.id.phonetic_symbol_imageView);
        phonetic_symbol_imageView.setImageBitmap(resizeImage(0.2f, phonetic_symbol_id[word_number]));
        phonetic_symbol_imageView.bringToFront();

        //カウントダウンのTextView
        timerText = findViewById(R.id.timer_textView);

        //注意書きのTextView
        TextView camera_attention_textView = findViewById(R.id.camera_attention_textView);

        //プライバシー保護のTextView
        TextView privacy_attention_textView = findViewById(R.id.privacy_attention_textView);

        //カウントダウンを開始するButton
        Button rec_button = findViewById(R.id.rec_button);
        rec_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testCamera(v);
                //録音もカウントダウン後に始めるのでtestCamera(v)内に記述
                rec_button.setVisibility(View.INVISIBLE);
                camera_attention_textView.setVisibility(View.INVISIBLE);
                privacy_attention_textView.setVisibility(View.INVISIBLE);
            }
        });

        //撮影を停止するButton
        stop_button = findViewById(R.id.stop_button);
        stop_button.setVisibility(View.INVISIBLE);
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCamera();
                stopRecording();
                uploadAudio();
                updateUserDateData();
            }
        });

    }

    //カメラやマイクのリクエストに対する処理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_FOR_CAMERA_PERMISSIONS){
            if(checkPermissions()){
                startCamera();
            } else{
                this.finish();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted ) finish();
    }

/*****************************************ここからカメラ関連の関数*/
    public void stopCamera(){
        Intent intent = new Intent(getApplication(), Activity1_1_1.class);
        intent.putExtra(WORD_NUMBER, word_number);

        camera = null;
        preview = null;
        countDown.cancel();
        showToast("処理中");
        //1秒間スリープ
        /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        finish();
        startActivity(intent);
    }

    public void testCamera(View v) {
        if (checkPermissions()) {
            frameNum = 0;
            //過去に撮影したフレームをすべて削除
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
            countDown.start();
            startCamera();

            //録音もカメラと同時に開始
            startRecording();
        } else {
            ActivityCompat.requestPermissions(this, CAMERA_REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_CAMERA_PERMISSIONS);
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context context = this;
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    preview = new Preview.Builder().build();
                    imageAnalysis = new ImageAnalysis.Builder().build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new CameraActivity.MyImageAnalyzer());
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageAnalysis);
                    preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
                } catch (Exception e) {

                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private Mat matPrevious = null;

        @Override
        public void analyze(@NonNull ImageProxy image) {
            /* Create cv::mat(RGB888) from image(NV21) */
            Mat matOrg = getMatFromImage(image);
            /* Fix image rotation (it looks image in PreviewView is automatically fixed by CameraX???) */
            Mat mat = fixMatRotation(matOrg);
            Mat matOutput = mat;

            if (nowCount == 0) {
                Imgcodecs.imwrite(getFilesDir().getPath() + File.separator + String.valueOf(frameNum) + ".jpg", matOutput);
                frameNum++;
            }

            /* Close the image otherwise, this function is not called next time */
            image.close();

            if (frameNum > 1000) {
                camera = null;
                preview = null;
                countDown.cancel();

                finish();
            }
        }

        private Mat getMatFromImage(ImageProxy image) {
            /* https://stackoverflow.com/questions/30510928/convert-android-camera2-api-yuv-420-888-to-rgb */
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            uBuffer.get(nv21, ySize, uSize);
            vBuffer.get(nv21, ySize + uSize, vSize);
            Mat yuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
            yuv.put(0, 0, nv21);
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV420sp2RGB,3);
            return mat;
        }

        private Mat fixMatRotation(Mat matOrg) {
            Mat mat;
            switch (previewView.getDisplay().getRotation()){
                default:
                case Surface.ROTATION_0:
                    mat = new Mat(matOrg.cols(), matOrg.rows(), matOrg.type());
                    Core.transpose(matOrg, mat);
                    Core.flip(mat, mat, 0);
                    break;
                case Surface.ROTATION_90:
                    mat = matOrg;
                    break;
                case Surface.ROTATION_270:
                    mat = matOrg;
                    Core.flip(mat, mat, -1);
                    break;
            }
            return mat;
        }
    }

    private boolean checkPermissions(){
        for(String permission : CAMERA_REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    class CountDown extends CountDownTimer {

        CountDown(long millisInFuture, long countDownInterval){
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            nowCount = 0;
            stop_button.setVisibility(View.VISIBLE);
            timerText.setVisibility(View.INVISIBLE);

        }

        // インターバルで呼ばれる
        public void onTick(long millisUntilFinished){
            long mm = millisUntilFinished / 1000 / 60;
            long ss = millisUntilFinished / 1000 % 60;
            long ms = millisUntilFinished - ss * 1000 - mm *1000*60;
            nowCount = (int) ms;
            timerText.setText(String.valueOf(ss + 1));
            timerText.bringToFront();
        }
    }
/**********************************ここまでカメラ関連*/

/**********************************ここから録音関連の関数*/
    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setOutputFile(audio_fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        try {
            recorder.prepare();
        } catch (IOException e) {
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    //3gpからwaveにする関数
    private String ffmpeg(String input){
        String output = getFilesDir().getPath()+"/test.wav";
        String cmd = String.format("-y -i %s -ac 1 -ar 48000 %s",input,output);
        int rc = FFmpeg.execute(cmd);

        if (rc == RETURN_CODE_SUCCESS) {

        } else if (rc == RETURN_CODE_CANCEL) {

        } else {

        }
        return output;
    }

    //音声ファイル(3gp)をwaveにしてstorageに送る関数
    public void uploadAudio() {
        String wave_name = ffmpeg(audio_fileName);
        Uri file = Uri.fromFile(new File(wave_name));
        FirebaseStorage storage = FirebaseStorage.getInstance();
        //StorageReference storageRef = storage.getReference().child(getUserData(2) + "/" + file.getLastPathSegment());
        StorageReference storageRef = storage.getReference().child("/" + file.getLastPathSegment());
        UploadTask uploadTask = storageRef.putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("storage失敗");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                showToast("storage成功");
                //API();
            }
        });
    }

    //FlaskをWebAPIとして使用し，AndroidとFlask間でHTTP通信してwebサーバ(AWS EC2)にあるapp.pyを実行する関数
    public void API() {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            //アクセスするwebサーバ（WEBサイト）のURL
            String urlGetText = "http://52.194.230.28:5000";
            URL url = new URL(urlGetText);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.connect();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200){
                inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/*********************************************************ここまで録音関連*/
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

    //Calendarクラスのインスタンスから日付の文字列を返す(yyyy/MM/dd)
    public String calendarToString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE);
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
