package com.github.kilnn.refreshloadlayout.sample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.github.kilnn.refreshloadlayout.RefreshLoadLayout;


public class MainActivity extends AppCompatActivity {

    private RefreshLoadLayout mRefreshLoadLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefreshLoadLayout = (RefreshLoadLayout) findViewById(R.id.refresh_load_layout);
        ProgressRefreshView refreshView = new ProgressRefreshView(this);
        mRefreshLoadLayout.setRefreshView(refreshView);

        mRefreshLoadLayout.setRefreshTriggerDistance(100);
        mRefreshLoadLayout.setDragLimitDistance(800);
        mRefreshLoadLayout.setRefreshSlingshotDistance(200);

        mRefreshLoadLayout.setOnRefreshListener(new RefreshLoadLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLoadLayout.setRefreshing(false);
                    }
                }, 5000);

            }
        });
    }

}
