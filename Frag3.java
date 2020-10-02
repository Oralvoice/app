package com.example.oralvoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Frag3 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //単語を入れる配列
        final String[] word = getResources().getStringArray(R.array.words_no_error);

        //ランキングのListView
        ListView ranking_listView = view.findViewById(R.id.ranking_listView);

        //単語選択のSpinner
        Spinner word_spinner = view.findViewById(R.id.word_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), R.layout.spinner1, word);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        word_spinner.setAdapter(adapter);
        word_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Spinner spinner = (Spinner)adapterView;

                String word = spinner.getSelectedItem().toString();
                displayRanking(word, ranking_listView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onStart() {
        //このFragmentが画面に出るときにはListViewに最新のランキングをセットする
        super.onStart();
        Fragment currentFragment = this;
        View view = currentFragment.getView();
        ListView listView = view.findViewById(R.id.ranking_listView);
        displayRanking("ask", listView);
    }

    //指定のListViewに指定の単語のランキングをセットする関数
    public void displayRanking(String word, ListView listView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ranking_docRef = db.collection("ranking").document(word);

        ranking_docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        Map<String, Object> rankingData;

                        if(task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                rankingData = (document.getData());

                                //取得したランキングを降順にソート
                                List<Map.Entry<String, Object>> list_entries = new ArrayList<>(rankingData.entrySet());
                                Collections.sort(list_entries, new Comparator<Map.Entry<String, Object>>() {
                                    public int compare(Map.Entry<String, Object> obj1, Map.Entry<String, Object> obj2) {
                                        return Integer.valueOf(obj2.getValue().toString()).compareTo(Integer.valueOf(obj1.getValue().toString()));
                                    }
                                });

                                if (list_entries.size() == 1) {
                                    TextView no_data_textView = getView().findViewById(R.id.no_data_textView);
                                    no_data_textView.setVisibility(View.VISIBLE);
                                } else {
                                    TextView no_data_textView = getView().findViewById(R.id.no_data_textView);
                                    no_data_textView.setVisibility(View.INVISIBLE);
                                }

                                //ListViewにセット
                                ArrayList<RankingData> ranking_list = new ArrayList<>();
                                int rank = 0;
                                int recent_point = 1000;
                                int player_count = 0;

                                String name = null;
                                String point = null;

                                for (Map.Entry<String, Object> entry : list_entries) {
                                    if (recent_point > Integer.parseInt(entry.getValue().toString())) {
                                        recent_point = Integer.parseInt(entry.getValue().toString());
                                        if (rank != 0) {
                                            RankingData data = new RankingData();
                                            data.setRank(String.valueOf(rank));
                                            data.setName(name);
                                            if (rank <= 3 && player_count != 0) {
                                                data.setOther_num("(他" + player_count + "名)");
                                            } else {
                                                data.setOther_num("");
                                            }
                                            data.setPoint(point);

                                            ranking_list.add(data);
                                        }

                                        name = entry.getKey();
                                        point = entry.getValue().toString();
                                        rank += 1;
                                        player_count = 0;

                                    } else {
                                        player_count += 1;
                                    }
                                }
                                MyAdapter rankingAdapter = new MyAdapter(getActivity().getApplicationContext());
                                rankingAdapter.setRankingList(ranking_list);
                                listView.setAdapter(rankingAdapter);
                            }
                        }
                    }
                });
    }
}

//ランキングに使う各種データのクラス
class RankingData {
    long id;
    String rank;
    String name;
    String other_num;
    String point;

    public long getId() {
        return id;
    }

    public void setId(long tmp) {
        this.id = tmp;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String tmp) {
        this.rank = tmp;
    }

    public String getName() {
        return name;
    }

    public void setName(String tmp) {
        this.name = tmp;
    }

    public String getOther_num() {
        return other_num;
    }

    public void setOther_num(String tmp) {
        this.other_num = tmp;
    }

    public String getPoint() {
        return point;
    }

    public void setPoint(String tmp) {
        this.point = tmp;
    }
}

//ListViewにランキングをセットするためのアダプター
class MyAdapter extends BaseAdapter {
    Context context;
    LayoutInflater layoutInflater = null;
    ArrayList<RankingData> rankingList;

    public MyAdapter(Context context) {
        this.context = context;
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setRankingList(ArrayList<RankingData> rankingList) {
        this.rankingList = rankingList;
    }

    @Override
    public int getCount() {
        return rankingList.size();
    }

    @Override
    public Object getItem(int position) {
        return rankingList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rankingList.get(position).getId();
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.ranking_row1, parent, false);

        ((TextView)convertView.findViewById(R.id.rank_textView)).setText(rankingList.get(position).getRank());
        ((TextView)convertView.findViewById(R.id.name_textView)).setText(rankingList.get(position).getName());
        ((TextView)convertView.findViewById(R.id.other_number_textView)).setText(rankingList.get(position).getOther_num());
        ((TextView)convertView.findViewById(R.id.point_textView)).setText(rankingList.get(position).getPoint());

        //上位3位は文字サイズを大きく、順位の数字の背景に画像をセットする、そのままだと見にくいので3位と4位の間は少し間をあける
        if (position < 3) {
            ((TextView)convertView.findViewById(R.id.rank_textView)).setTextSize(30.0f);
            ((TextView)convertView.findViewById(R.id.name_textView)).setTextSize(23.0f);
            ((TextView)convertView.findViewById(R.id.point_textView)).setTextSize(30.0f);

            convertView.findViewById(R.id.rank_textView).setPadding(0, 20, 0, 0);

            switch (position) {
                case 0:
                    convertView.findViewById(R.id.rank_textView).setBackgroundResource(R.drawable.s_crown1);
                    break;
                case 1:
                    convertView.findViewById(R.id.rank_textView).setBackgroundResource(R.drawable.s_crown2);
                    break;
                case 2:
                    convertView.findViewById(R.id.rank_textView).setBackgroundResource(R.drawable.s_crown3);
                    break;
            }
        } else if (position == 3) {
            convertView.findViewById(R.id.rank_textView).setPadding(0, 60, 0, 0);
        }

        return convertView;
    }
}