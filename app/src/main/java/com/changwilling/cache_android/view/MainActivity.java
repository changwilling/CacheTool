package com.changwilling.cache_android.view;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.changwilling.cache_android.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        Button bt_cach_expire= (Button) findViewById(R.id.bt_cache_expire);
        Button bt_cache_json= (Button) findViewById(R.id.bt_cache_json);
        Button bt_cach_pic= (Button) findViewById(R.id.bt_cach_pic);
        bt_cach_expire.setOnClickListener(this);
        bt_cache_json.setOnClickListener(this);
        bt_cach_pic.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_cache_expire://有效期管理
                Intent intent=new Intent(MainActivity.this,ExpireManageActivity.class);
                startActivity(intent);
                break;
            case R.id.bt_cache_json://json数据缓存

                break;
            case R.id.bt_cach_pic://图片数据缓存

                break;
        }
    }
}
