package com.example.oralvoice;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.photo.Photo;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class OnlineBattle_CameraActivity extends AppCompatActivity {

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
    final OnlineBattle_CameraActivity.CountDown countDown = new OnlineBattle_CameraActivity.CountDown(contNumber, interval);

    //Activity遷移時に使うkey
    public static final String ROOM_ID = "ROOM_ID";
    public static final String OWN_NUM = "OWN_NUM";
    public static final String ROUND = "ROUND";
    public static final String WORD_NUM1 = "WORD_NUM1";
    public static final String WORD_NUM2 = "WORD_NUM2";
    public static final String WORD_NUM3 = "WORD_NUM3";
    public static final String SCORE = "SCORE";

    //発音する単語の識別番号 (わかりやすいようにこれを使う)
    int word_number;

    //部屋名 (CloudFirestoreのDocument名)
    String roomId;

    //プレイヤー1か2か (部屋を作った人が1、既存の部屋に入った人が2)
    int own_num;

    //現在のラウンド
    int round;

    //3ラウンド分の単語の識別番号
    int word_num1;
    int word_num2;
    int word_num3;

    //口の動きの点数

    //口の動きと音声の平均点
    int score;

    //撮影を停止するButton
    Button stop_button;

    //カウントダウンのTextView
    TextView timerText;
    int mouth_score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_with_error);

        //発音記号の画像IDを入れる配列
        final int[] phonetic_symbol_id = getResourceIds(getResources(), R.array.phonetic_ids_with_error);

        Intent intent = getIntent();
        roomId = intent.getStringExtra(Activity2_1.ROOM_ID);
        own_num = intent.getIntExtra(Activity2_1.OWN_NUM, 0);
        round = intent.getIntExtra(Activity2_1.ROUND, 0);
        word_num1 = intent.getIntExtra(Activity2_1.WORD_NUM1, 0);
        word_num2 = intent.getIntExtra(Activity2_1.WORD_NUM2, 0);
        word_num3 = intent.getIntExtra(Activity2_1.WORD_NUM3, 0);

        switch (round) {
            case 0:
                word_number = word_num1;
                break;
            case 1:
                word_number = word_num2;
                break;
            case 2:
                word_number = word_num3;
                break;
        }

        //マイクの使用リクエスト
        ActivityCompat.requestPermissions(this, audio_permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //録音するファイルのパス
        audio_fileName = getExternalCacheDir().getAbsolutePath();
        audio_fileName += "/test.wav";

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
                //点数計算、Activity遷移はstopCamera内に記述
                stopRecording();
                stopCamera();
                uploadAudio();
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
        camera = null;
        preview = null;
        countDown.cancel();
        showToast("処理中");

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_with_error);

        //口の動きの点数を計算
        mouth_score = (int)Math.round(mouth_evaluation(word[word_number]));

        //音声の点数を計算
        download_fire_storage(word[word_number]);

        //音声の点数を計算する際に非同期処理が発生するので対戦画面に戻る処理はその非同期処理が終わった時に行う (voice_evaluation関数内で行う)
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
                    imageAnalysis.setAnalyzer(cameraExecutor, new OnlineBattle_CameraActivity.MyImageAnalyzer());
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

/************************************ここから口の動きの点数計算*/
    //口の動きの点数を計算する関数
    public double mouth_evaluation(String userWord){
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

        if (ans/wordCascadeNum + 30 > 100) {
            return 100;
        }
        return ans / wordCascadeNum + 20;
    }

    public Mat cutFaceFromImage(Mat img){
        final String faceCascadeFilename = "haarcascade_frontalface_default.xml";
        final String faceCascadePath = getFilesDir().getPath() + File.separator + faceCascadeFilename;
        File faceCascadeFile = new File(faceCascadePath);
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
        for(int i = 5; i > 2; i-=2){
            faceCascade.detectMultiScale(img, faceRects,1.11, i,0, minSize);
            if (faceRects.toArray().length != 0){
                break;
            }
        }
        if(faceRects.toArray().length > 0){
            int centerImg[] = {img.width()/2, img.height()/2};
            Rect roi = new Rect(0, 0, 0, 0);
            for(Rect rect : faceRects.toArray()){
                if (Math.sqrt(Math.pow(centerImg[0] - (roi.x + roi.width / 2), 2) + Math.pow(centerImg[1] - (roi.y + roi.height / 2), 2)) > Math.sqrt(Math.pow(centerImg[0] - (rect.x + rect.width / 2), 2) + Math.pow(centerImg[1] - (rect.y + rect.height / 2), 2))){
                    roi = rect;
                }
            }
            img = new Mat(img, roi);
        }
        faceCascadeFile.delete();
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
/*****************************************ここまで口の動きの点数*/

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
                            score = (int) Math.round((mouth_score + voice_score) / 2);

                            Intent intent = new Intent(getApplication(), Activity2_1.class);
                            intent.putExtra(ROOM_ID, roomId);
                            intent.putExtra(ROUND, round + 1);
                            intent.putExtra(WORD_NUM1, word_num1);
                            intent.putExtra(WORD_NUM2, word_num2);
                            intent.putExtra(WORD_NUM2, word_num2);
                            intent.putExtra(WORD_NUM3, word_num3);
                            intent.putExtra(OWN_NUM, own_num);
                            intent.putExtra(SCORE, score);

                            finish();
                            startActivity(intent);
                        }
                    }
                });
    }
}
