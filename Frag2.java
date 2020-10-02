package com.example.oralvoice;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Frag2 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //CloudFirestoreのインスタンス、ユーザごとのコレクションのパス
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference user_docRef = db.collection("users").document(getUserData(2));

        //ユーザの日付に関連するデータを更新、単語ごとのデータに欠損があれば追加
        checkUserWordData();
        updateUserDateData();

        //今日の練習回数のテキスト
        TextView today_plays_textView = view.findViewById(R.id.today_plays_textView);
        setFirestoreData(user_docRef, "plays_day_0", today_plays_textView, "", "回");

        //練習回数のTextView
        TextView plays_textView = view.findViewById(R.id.plays_textView);

        //選択している期間のTextView
        TextView time_range_textView = view.findViewById(R.id.time_range_textView);

        //平均点のTextView
        TextView average_textView = view.findViewById(R.id.average_textView);

        //今日の練習回数の円グラフ
        PieChart pieChart = view.findViewById(R.id.pieChart);
        setUpPieChart(pieChart);

        //今日の平均点のレーダーチャート
        RadarChart radarChart = view.findViewById(R.id.radarChart);
        setUpRadarChart(radarChart);

        //練習回数の棒グラフ
        BarChart barChart = view.findViewById(R.id.barChart);

        //平均点の折れ線グラフ
        LineChart lineChart = view.findViewById(R.id.lineChart);

        //今月今年通算のグラフのSpinner
        String spinner_choice[] = {"30日間", "1年間", "通算"};
        Spinner spinner = view.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner1, spinner_choice);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                barChart.clear();
                lineChart.clear();
                Spinner spinner = (Spinner)adapterView;
                int num = spinner.getSelectedItemPosition();
                switch (num) {
                    case 0:
                        setFirestoreData(user_docRef, "plays_month_0", plays_textView, "(計", "回)");
                        time_range_textView.setText("日");
                        setFirestoreData(user_docRef, "average_month_0", average_textView, "(この期間の平均 : ", "点)");
                        setUpBarChart(barChart, 0);
                        setUpLineChart(lineChart, 0);
                        break;
                    case 1:
                        setFirestoreData(user_docRef, "plays_year_0", plays_textView, "(計", "回)");
                        time_range_textView.setText("月");
                        setFirestoreData(user_docRef, "average_year_0", average_textView, "(この期間の平均 : ", "点)");
                        setUpBarChart(barChart, 1);
                        setUpLineChart(lineChart, 1);
                        break;
                    case 2:
                        setFirestoreData(user_docRef, "plays", plays_textView, "(計", "回)");
                        time_range_textView.setText("年");
                        setFirestoreData(user_docRef, "average", average_textView, "(この期間の平均 : ", "点)");
                        setUpBarChart(barChart, 2);
                        setUpLineChart(lineChart, 2);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    public void onStart() {
        //このFragmentが画面に出るときにはグラフやデータを更新する
        super.onStart();
        Fragment currentFragment = this;
        View view = currentFragment.getView();
        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        TextView today_plays_textView = view.findViewById(R.id.today_plays_textView);
        setFirestoreData(user_docRef, "plays_day_0", today_plays_textView, "", "回");
        TextView plays_textView = view.findViewById(R.id.plays_textView);
        setFirestoreData(user_docRef, "plays_month_0", plays_textView, "(計", "回)");
        TextView average_textView = view.findViewById(R.id.average_textView);
        setFirestoreData(user_docRef, "average_month_0", average_textView, "(この期間の平均 : ", "点)");
        PieChart pieChart = view.findViewById(R.id.pieChart);
        RadarChart radarChart = view.findViewById(R.id.radarChart);
        BarChart barChart = view.findViewById(R.id.barChart);
        LineChart lineChart = view.findViewById(R.id.lineChart);
        pieChart.clear();
        radarChart.clear();
        barChart.clear();
        lineChart.clear();
        setUpPieChart(pieChart);
        setUpRadarChart(radarChart);
        setUpBarChart(barChart, 0);
        setUpLineChart(lineChart, 0);
    }

    //今日の練習の円グラフをセットアップし表示する関数
    private void setUpPieChart(PieChart pieChart) {

        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setEntryLabelTextSize(9f);

        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {

                                //単語を入れる配列
                                final String[] word = getResources().getStringArray(R.array.words_no_error);

                                int plays_day_0 = Integer.parseInt(String.valueOf(document.get("plays_day_0")));

                                List<PieEntry> entries = new ArrayList<>();
                                if (plays_day_0 != 0) {
                                    for (String s : word) {
                                        int word_plays_day_0 = Integer.parseInt(String.valueOf(document.get(s + "_plays_day_0")));
                                        if (word_plays_day_0 != 0) {
                                            float per = ((float) word_plays_day_0 / (float) plays_day_0) * 100;
                                            entries.add(new PieEntry(per, s));
                                        }
                                    }
                                } else {
                                    entries.add(new PieEntry(100, "データなし"));
                                }

                                PieDataSet set = new PieDataSet(entries, "DataSet");
                                set.setColors(ColorTemplate.MATERIAL_COLORS);
                                set.setDrawValues(true);

                                PieData pieData = new PieData(set);
                                pieData.setValueFormatter(new PercentFormatter());
                                pieData.setValueTextSize(10f);
                                pieData.setValueTextColor(Color.WHITE);

                                pieChart.setData(pieData);
                                pieChart.invalidate();
                                pieChart.animate();
                            }
                        }
                    }

                });
    }

    //今日の練習のレーダーチャートをセットアップし表示する関数
    private void setUpRadarChart(RadarChart radarChart) {

        radarChart.getDescription().setEnabled(false);
        radarChart.getLegend().setEnabled(false);
        XAxis xAxis = radarChart.getXAxis();
        xAxis.setEnabled(true);
        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0);
        yAxis.setAxisMaximum(80);
        yAxis.setDrawLabels(true);
        yAxis.setTextSize(8f);
        yAxis.setLabelCount(5);

        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                //単語を入れる配列
                                final String[] word = getResources().getStringArray(R.array.words_no_error);
                                List<String> label = new ArrayList<>();

                                List<RadarEntry> entries = new ArrayList<>();
                                int plays_day_0 = Integer.parseInt(String.valueOf(document.get("plays_day_0")));
                                if (plays_day_0 != 0) {
                                    for (String s : word) {
                                        int word_plays_day_0 = Integer.parseInt(String.valueOf(document.get(s + "_plays_day_0")));
                                        int today_word_AVG = Integer.parseInt(String.valueOf(document.get("today_" + s + "_AVG")));
                                        if (word_plays_day_0 != 0) {
                                            entries.add(new RadarEntry(today_word_AVG));
                                            label.add(s);
                                        }
                                    }
                                } else {
                                    entries.add(new RadarEntry(0));
                                    label.add("データなし");
                                }

                                xAxis.setValueFormatter(new IndexAxisValueFormatter(label));

                                RadarDataSet set = new RadarDataSet(entries, "DataSet");
                                set.setColor(Color.RED);
                                set.setLineWidth(1.5f);


                                RadarData radarData = new RadarData(set);
                                radarData.setValueTextSize(0f);
                                radarData.setValueTextColor(Color.RED);

                                radarChart.setData(radarData);
                                radarChart.invalidate();
                                radarChart.animate();
                            }
                        }
                    }
                });
    }

    //指定の期間の単語別練習回数の棒グラフをセットアップし表示する関数
    private void setUpBarChart(BarChart barChart, int num) {

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextSize(8f);
        xAxis.enableGridDashedLine(0f, 10f, 0f);
        YAxis l_yAxis = barChart.getAxisLeft();
        l_yAxis.setAxisMinimum(0);
        YAxis r_yAxis = barChart.getAxisRight();
        r_yAxis.setEnabled(false);

        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                float max = 0;

                                //単語を入れる配列
                                final String[] word = getResources().getStringArray(R.array.words_no_error);
                                List<BarEntry> entries = new ArrayList<>();
                                int count;
                                switch (num) {
                                    case 0:
                                        count = 0;
                                        for (String s: word) {
                                            int word_plays_30days = 0;
                                            for (int i = 0; i < 30; i++) {
                                                if (document.get(s + "_plays_day_" + i) != null) {
                                                    int word_plays_day_i = Integer.parseInt(String.valueOf(document.get(s + "_plays_day_" + i)));
                                                    word_plays_30days += word_plays_day_i;
                                                }
                                            }
                                            entries.add(new BarEntry(count, word_plays_30days));
                                            count += 1;
                                        }
                                        break;
                                    case 1:
                                        count = 0;
                                        for (String s: word) {
                                            int word_plays_12months = 0;
                                            for (int i = 0; i < 12; i++) {
                                                int word_plays_month_i = Integer.parseInt(String.valueOf(document.get(s + "_plays_month_" + i)));
                                                word_plays_12months += word_plays_month_i;
                                            }
                                            entries.add(new BarEntry(count, word_plays_12months));
                                            count += 1;
                                        }
                                        break;
                                    case 2:
                                        count = 0;
                                        for (String s: word) {
                                            int word_plays = Integer.parseInt(String.valueOf(document.get(s + "_plays")));
                                            entries.add(new BarEntry(count, word_plays));
                                            count += 1;
                                        }
                                        break;
                                }

                                for (BarEntry a: entries) {
                                    if (max < a.getY()) {
                                        max = a.getY();
                                    }
                                }
                                l_yAxis.setAxisMaximum((float)Math.round(max * 1.2f));
                                l_yAxis.setLabelCount(Math.round(max * 1.2f));
                                xAxis.setValueFormatter(new IndexAxisValueFormatter(word));
                                xAxis.setLabelCount(word.length);
                                xAxis.setLabelRotationAngle(-90);
                                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                                BarDataSet set = new BarDataSet(entries, "DataSet");
                                //set.setColors(Color.argb(255, 255, 180, 180));
                                set.setColors(Color.RED);
                                set.setDrawValues(false);

                                BarData barData = new BarData(set);
                                barData.setBarWidth(barData.getBarWidth() * 0.5f);

                                barChart.setData(barData);
                                barChart.invalidate();
                                barChart.animate();
                            }
                        }
                    }
                });
    }

    //指定の期間の平均点の推移の折れ線グラフをセットアップし表示する関数
    private void setUpLineChart(LineChart lineChart, int num) {

        lineChart.setDrawGridBackground(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.enableGridDashedLine(0, 10, 0);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(0);
        YAxis l_yAxis = lineChart.getAxisLeft();
        l_yAxis.setAxisMinimum(0);
        l_yAxis.setAxisMaximum(100);

        final DocumentReference user_docRef = FirebaseFirestore.getInstance().collection("users").document(getUserData(2));
        user_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                ArrayList<String> label = new ArrayList<>();
                                ArrayList<Entry> values = new ArrayList<>();
                                Calendar now = Calendar.getInstance();
                                switch(num) {
                                    case 0:
                                        xAxis.setAxisMaximum(30);
                                        xAxis.setLabelCount(6);
                                        int last_month = now.get(Calendar.MONTH);
                                        int[] last_month_days = {31, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30};
                                        if (now.get(Calendar.YEAR) % 4 == 0) {
                                            last_month_days[2] += 1;
                                        }
                                        int now_date = now.get(Calendar.DATE);
                                        int start_day = now_date - 29;
                                        if (start_day < 0) {
                                            start_day += last_month_days[last_month];
                                        }
                                        for (int i = 0; i < 30; i++) {
                                            if (document.get("average_day_" + (29 - i)) != null) {
                                                int average_day_i = Integer.parseInt(String.valueOf(document.get("average_day_" + (29 - i))));
                                                values.add(new Entry(i, average_day_i, null, null));
                                                if (start_day + i > last_month_days[last_month]) {
                                                    label.add(String.valueOf(start_day + i - last_month_days[last_month]));
                                                } else {
                                                    label.add(String.valueOf(start_day + i));
                                                }
                                            }
                                        }
                                        break;
                                    case 1:
                                        xAxis.setAxisMaximum(12);
                                        xAxis.setLabelCount(12);
                                        int start_month = now.get(Calendar.MONTH) + 1 - 11;
                                        if (start_month < 1) {
                                            start_month += 12;
                                        }
                                        for (int i = 0; i < 12; i++) {
                                            int average_month_i = Integer.parseInt(String.valueOf(document.get("average_month_" + (11 - i))));
                                            values.add(new Entry(i, average_month_i, null, null));
                                            if (start_month + i > 12) {
                                                label.add(String.valueOf((start_month + i - 12)));
                                            } else {
                                                label.add(String.valueOf(start_month + i));
                                            }
                                        }
                                        break;
                                    case 2:
                                        String date_str = String.valueOf(document.get("date"));
                                        Calendar date = Calendar.getInstance();
                                        date.setTime(stringToDate(date_str));
                                        int play_years = now.get(Calendar.YEAR) - date.get(Calendar.YEAR);
                                        int start_year = date.get(Calendar.YEAR);
                                        xAxis.setAxisMaximum(play_years + 1);
                                        if (play_years > 10) {
                                            xAxis.setLabelCount((play_years + 1) / 5);
                                        } else {
                                            xAxis.setLabelCount(play_years + 1);
                                        }
                                        for (int i = 0; i < play_years + 1; i ++) {
                                            int average_year_i = Integer.parseInt(String.valueOf(document.get("average_year_" + (play_years - i))));
                                            values.add(new Entry(i, average_year_i, null, null));
                                            label.add(String.valueOf(start_year + i));
                                        }
                                        break;
                                }

                                xAxis.setValueFormatter(new IndexAxisValueFormatter(label));

                                LineDataSet set;
                                if (lineChart.getData() != null && lineChart.getData().getDataSetCount() > 0) {
                                    set = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
                                    set.setValues(values);
                                    lineChart.getData().notifyDataChanged();
                                    lineChart.notifyDataSetChanged();
                                } else {
                                    // create a dataset and give it a type
                                    set = new LineDataSet(values, "DataSet");

                                    set.setDrawIcons(false);
                                    set.setColor(Color.RED);
                                    set.setCircleColor(Color.BLACK);
                                    set.setLineWidth(1.5f);
                                    //set.setCircleRadius(10f);
                                    set.setDrawCircles(false);
                                    set.setDrawCircleHole(false);
                                    set.setValueTextSize(0f);
                                    set.setDrawFilled(false);
                                    set.setFormLineWidth(1f);
                                    set.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
                                    set.setFormSize(5.f);

                                    set.setFillColor(Color.BLUE);

                                    ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
                                    dataSets.add(set); // add the datasets

                                    // create a data object with the datasets
                                    LineData lineData = new LineData(dataSets);

                                    // set data
                                    lineChart.setData(lineData);
                                    lineChart.invalidate();
                                    lineChart.animate();
                                }
                            }
                        }
                    }
                });
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
    public void setFirestoreData (DocumentReference docRef, final String field, final TextView text_view, final String text, final String unit) {
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
                String set = text + str[0] + unit;
                text_view.setText(set);
            }
        });
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
