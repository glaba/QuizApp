package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FinalScore extends FragmentActivity {
    ViewPager pager;
    Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_score);

        exitButton = (Button)findViewById(R.id.exitButton);
        pager = (ViewPager)findViewById(R.id.pager);

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FinalScore.this.setResult(GameLobby.GAME_OVER, null);
                finish();
            }
        });

        ScorePagerAdapter adapter = new ScorePagerAdapter(getSupportFragmentManager(), getIntent().getStringExtra("information"));
        pager.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        FinalScore.this.setResult(GameLobby.GAME_OVER, null);
        finish();
        super.onBackPressed();
    }

    public class Answer implements Serializable {
        String response, user;
        Boolean correct;
        int score;

        public String getUser() {
            return user;
        }

        public String getResponse() {
            return response;
        }

        public Boolean getCorrect() {
            return correct;
        }

        public int getScore() {
            return score;
        }

        public Answer(String response, String user, Boolean correct, int score) {
            this.user = user;
            this.response = response;
            this.correct = correct;
            this.score = score;
        }
    }

    public class ScorePagerAdapter extends FragmentStatePagerAdapter {
        ArrayList<Score> scoreList = new ArrayList<>();
        ArrayList<String> questions =  new ArrayList<>();
        ArrayList<ArrayList<Answer>> answers = new ArrayList<>();

        public ScorePagerAdapter(FragmentManager fm, String information) {
            super(fm);
            try {
                JSONObject object = new JSONObject(information);
                JSONArray scores = object.getJSONArray("scores");
                for (int i = 0; i < scores.length(); i++) {
                    scoreList.add(new Score(scores.getJSONObject(i).getString("name"), scores.getJSONObject(i).getInt("score")));
                }

                for (int i = 0; i < object.getJSONArray("responses").getJSONObject(0).getJSONArray("answers").length(); i++) {
                    questions.add(object.getJSONArray("questions").getJSONObject(i).getString("question"));
                    answers.add(new ArrayList<Answer>());
                    for (int j = 0; j < object.getJSONArray("responses").length(); j++) {
                        String user = object.getJSONArray("responses").getJSONObject(j).getString("name");
                        String answer = object.getJSONArray("responses").getJSONObject(j).getJSONArray("answers").getJSONObject(i).getString("response");
                        Boolean correct = object.getJSONArray("responses").getJSONObject(j).getJSONArray("answers").getJSONObject(i).getBoolean("correct");
                        int marginalScore = object.getJSONArray("responses").getJSONObject(j).getJSONArray("answers").getJSONObject(i).getInt("marginalScore");
                        Answer curAnswer = new Answer(answer, user, correct, marginalScore);
                        answers.get(i).add(curAnswer);
                    }
                }
            } catch (Exception e) {

            }
        }

        @Override
        public Fragment getItem(int i) {
            Bundle arguments = new Bundle();
            Fragment fragment;
            if (i % (answers.size() + 1) == 0) {
                fragment = new OverviewFragment();
                arguments.putSerializable("scores", scoreList);
            } else if (i % (answers.size() + 1) <= answers.size()){
                fragment = new QuestionResponsesFragment();
                arguments.putSerializable("questionResponses", answers.get(i % (answers.size() + 1) - 1));
                arguments.putString("question", questions.get(i % (answers.size() + 1) - 1));
            } else {
                return null;
            }
            fragment.setArguments(arguments);
            return fragment;
        }

        @Override
        public int getCount() {
            return answers.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "Overview";
            } else {
                return "Question #" + position;
            }
        }
    }

    public static class OverviewFragment extends Fragment {
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.overview_fragment, container, false);

            Bundle args = getArguments();
            ArrayList<Score> scoreList = (ArrayList<Score>)args.getSerializable("scores");

            ListView overviewList = (ListView)view.findViewById(R.id.overviewListView);

            FinalScoreItemAdapter adapter = new FinalScoreItemAdapter(view.getContext(), R.layout.live_update_list_item, scoreList);
            overviewList.setAdapter(adapter);

            return view;
        }
    }

    public static class QuestionResponsesFragment extends Fragment {
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.question_responses_fragment, container, false);

            Bundle args = getArguments();
            try {
                ArrayList<Answer> answers = (ArrayList<Answer>)args.getSerializable("questionResponses");
                String question = args.getString("question");

                TextView questionView = (TextView)view.findViewById(R.id.questionTextFrag);
                ListView answerList = (ListView)view.findViewById(R.id.questionListFrag);
                Log.wtf("Question", question);

                questionView.setText(question);

                FinalQuestionAdapter adapter = new FinalQuestionAdapter(view.getContext(), R.layout.live_update_list_item, answers);
                answerList.setAdapter(adapter);
            } catch (Exception e) {
                Log.wtf("Exception", e.getMessage());
            }

            return view;
        }
    }

    private static class FinalQuestionAdapter extends ArrayAdapter<Answer> {
        List<Answer> objects;

        public FinalQuestionAdapter(Context context, int resource, List<Answer> objects) {
            super(context, resource, objects);
            this.objects = objects;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.live_update_list_item, null);
            }

            TextView user = (TextView)v.findViewById(R.id.userTextView);
            TextView time = (TextView)v.findViewById(R.id.timeTextView);

            user.setText(objects.get(position).getUser() + ": " + objects.get(position).getResponse());
            time.setText(objects.get(position).getScore() + "");

            if (objects.get(position).getCorrect()) {
                v.setBackgroundColor(Color.rgb(0, 200, 0));
            } else {
                v.setBackgroundColor(Color.rgb(200, 0, 0));
            }

            return v;
        }
    }
}
