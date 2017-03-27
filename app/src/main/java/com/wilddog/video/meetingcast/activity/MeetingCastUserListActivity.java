package com.wilddog.video.meetingcast.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import com.wilddog.video.meetingcast.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MeetingCastUserListActivity extends AppCompatActivity {

    @BindView(R.id.lv_user_list)
    ListView lvUsers;


    private List<String> userList = new ArrayList<>();

    private String participantId;
    private MyAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        ButterKnife.bind(this);
        userList.addAll(getIntent().getStringArrayListExtra("list"));
        adapter = new MyAdapter(userList, this);
        lvUsers.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }

        return super.onKeyDown(keyCode, event);

    }

    class MyAdapter extends BaseAdapter {
        private List<String> mList = new ArrayList<>();
        private LayoutInflater mInflater;
        @BindView(R.id.btn_item_invite)
        Button invite;
        @BindView(R.id.tv_item_participent)
        TextView id;

        MyAdapter(List<String> userList, Context context) {
            mList = userList;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {


            view = mInflater.inflate(R.layout.layout_invite_participent_list, null);
            ButterKnife.bind(this, view);
            id.setText(mList.get(i));
            invite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    participantId = mList.get(i);
                    Intent intent = new Intent();
                    intent.putExtra("participant", participantId);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
            return view;
        }
    }
}
