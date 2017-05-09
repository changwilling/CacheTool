package com.changwilling.cache_android.view;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.changwilling.cache_android.R;
import com.changwilling.cache_android.cache.IExpireCacheManager;
import com.changwilling.cache_android.cache.Impl.ExpireCacheManagerImpl;
import com.changwilling.cache_android.util.SpTools;

/**
 * 有效期管理的demo页面
 */
public class ExpireManageActivity extends AppCompatActivity {
    private TextView tv_for_data_expire;
    private TextView tv_data_content;
    private Button bt_get_data;
    private Button bt_refresh_data_expire;
    private SpTools spTools;
    private ExpireCacheManagerImpl expireCacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expire_manage);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        tv_for_data_expire= (TextView) findViewById(R.id.tv_for_data_expire);
        tv_data_content= (TextView) findViewById(R.id.tv_data_content);
        bt_get_data= (Button) findViewById(R.id.bt_get_data);
        bt_refresh_data_expire= (Button) findViewById(R.id.bt_refresh_data_expire);
    }

    private void initData() {
        //初始化需要的工具对象
        spTools=new SpTools(this,"expireData");
        expireCacheManager= ExpireCacheManagerImpl.getInstance();
        //1.获取当前缓存的数据，并且显示数据
        //1.1.从偏好设置中读取对应的数据，如果没有，则设置新的数据

        String lastData = spTools.getString("expireData", "");
        //2.查看当前的数据是否过期，并且显示是否过期
        if (TextUtils.isEmpty(lastData)){//说明第一次使用该功能，还没有数据
            setNewestData();
        }else {//有数据，则需要判断是否在有效期内
            //显示数据
            tv_data_content.setText(lastData);
            getExpireState();
        }
    }

    private void initListener() {
        bt_refresh_data_expire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//查看数据是否过期
                getExpireState();
            }
        });
        bt_get_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//获取最新的数据
                setNewestData();
            }
        });
    }

    /**
     * 设置最新的数据，并且设定有效期
     */
    private void setNewestData() {
        //1.设置数据
        spTools.putString("expireData",System.currentTimeMillis()+"");
        //2.设置有效期
        expireCacheManager.setExpire(IExpireCacheManager.KEY_FOR_EXPIRE_TEST,IExpireCacheManager.TIME_FOR_EXPIRE_TEST);
        //3.刷新显示，检查是否数据过期
        getExpireState();
    }

    /**
     * 查看是否过期的方法
     */
    private void getExpireState() {
        //显示是否有效
        boolean expire = expireCacheManager.isExpire(IExpireCacheManager.KEY_FOR_EXPIRE_TEST);
        if (expire){//表示已经过期
            tv_for_data_expire.setText("数据已经过期");
        }else {//表示数据还未过期
            tv_for_data_expire.setText("数据还未过期");
        }
    }
}
