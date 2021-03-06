package com.banshouweng.bswBase.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.banshouweng.bswBase.base.activity.BaseActivity;
import com.banshouweng.bswBase.utils.NetUtil;

/**
 * 《一个Android工程的从零开始》
 *
 * @author 半寿翁
 * @博客：
 * @CSDN http://blog.csdn.net/u010513377/article/details/74455960
 * @简书 http://www.jianshu.com/p/1410051701fe
 */
public class NetBroadcastReceiver extends BroadcastReceiver {

    public NetEvent event = BaseActivity.event;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        // 如果相等的话就说明网络状态发生了变化
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            int mobileNetState = NetUtil.getNetWorkState(context);
            // 接口回调传过去状态的类型
            event.onNetChanged(mobileNetState);
        }
    }

    // 自定义接口
    public interface NetEvent {
        void onNetChanged(int mobileNetState);
    }
}
