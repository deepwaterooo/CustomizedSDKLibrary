package com.banshouweng.bswBase.base.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.banshouweng.bswBase.receiver.NetBroadcastReceiver;
import com.banshouweng.bswBase.ui.activity.MainActivity;
import com.banshouweng.bswBase.utils.CommonUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 《一个Android工程的从零开始》
 *
 * @author 半寿翁
 * @博客：
 * @CSDN http://blog.csdn.net/u010513377/article/details/74455960
 * @简书 http://www.jianshu.com/p/1410051701fe
 */
public abstract class BaseActivity extends AppCompatActivity // 这些是用来方便自己用的封装，项目中用不到的直接comment掉或删掉（前备份）
implements NetBroadcastReceiver.NetEvent {

    // 网络状态监听接受者
    public static NetBroadcastReceiver.NetEvent event;
    private int netMobile; // 网络类型: 我觉得是可以在最底层实现与判断的 https://blog.csdn.net/mxiaoyem/article/details/50708052
    
    // 用于传递的上下文信息
    public Context mContext;
    public Activity mActivity;

    // 虚拟按键控件
    private View decorView;

    // 当前打开Activity存储List: 这种方式与调配使用不同的launchMode有什么不同呢？调配launchMode会不会更高效一些？但这种方法封成library应该更方便管理
    private static List<Activity> activities = new ArrayList<>();
    // 调用backTo方法时，验证该Activity是否已经存在
    private static Map<Class<?>, Activity> activitiesMap = new HashMap<>();

    private Toast toast;
    private long clickTime = 0L;

    // 加载提示框
    private CustomProgressDialog customProgressDialog;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preventCreateTwice();

        // 禁止横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        decorView = getWindow().getDecorView();

        activities.add(this);
        activitiesMap.put(getClass(), this);
        mContext = getApplicationContext();
        mActivity = this;

        event = this;
        isNetworkAvailable();
        
        // 下面的代码可以写在BaseActivity里面
        highApiEffects();

        initSDK();
        getBundle(getIntent().getBundleExtra("bundle"));

        // 不传文本
        customProgressDialog = new CustomProgressDialog(activity, R.style.progress_dialog_loading);
        // 传递文本
        customProgressDialog = new CustomProgressDialog(activity, R.style.progress_dialog_loading, "玩命加载中。。。");
    }
    //  网络变化回调方法
    //  @param mobileNetState 当前的网络状态
    @Override
        public void onNetChanged(int mobileNetState) { // 这里不是可以在这里实现的吗？
        // https://blog.csdn.net/mxiaoyem/article/details/50708052
    }
    //  检测当的网络（Wlan、3G/2G）状态
    //  @return true 表示网络可用
    public boolean isNetworkAvailable() {
        // 这里调用系统services，并没有利用NetUtil的扩展，且没有分辨使用的是数据流量，还是wifi
        ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                //  当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    //  当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
    // 显示加载提示框
    public void showLoadDialog() {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    customProgressDialog.show();
                }
            });
    }
    // 隐藏加载提示框
    public void hideLoadDialog() {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (customProgressDialog != null && customProgressDialog.isShowing()) {
                        customProgressDialog.dismiss();
                    }
                }
            });
    }
    
    //  防止重复创建的问题，第一次安装完成启动，和home键退出点击launcher icon启动会重复
    private void preventCreateTwice() { // 这里应该是仅指launcher activity吗，还是指所有呢？
        if (!isTaskRoot() 
            && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) 
            && getIntent().getAction() != null
            && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
        }
    }

    @Override
        protected void onStart() {
        hideVirtualKey();
        super.onStart();
    }
    //  隐藏虚拟按键和状态栏
    //  https:// blog.csdn.net/smileiam/article/details/69055963  沉浸式键盘遮挡输入框解决方案
    private void hideVirtualKey() {
        int flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; //  hide
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flag = flag | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//                     | View.SYSTEM_UI_FLAG_FULLSCREEN;//  隐藏状态栏
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flag = flag | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        // 判断当前版本在4.0以上并且存在虚拟按键，否则不做操作
        if (Build.VERSION.SDK_INT < 19 || ! checkDeviceHasNavigationBar()) {
            // 一定要判断是否存在按键，否则在没有按键的手机调用会影响别的功能。如之前没有考虑到，导致图传全屏变成小屏显示。
//             return;
        } else {
            //  获取属性
            decorView.setSystemUiVisibility(flag);
        }
    }

    //  沉浸式
    //  https:// blog.csdn.net/smileiam/article/details/69055963  沉浸式键盘遮挡输入框解决方案
    @TargetApi(Build.VERSION_CODES.KITKAT)
        private void highApiEffects() {
        getWindow().getDecorView().setFitsSystemWindows(true);
        // 透明状态栏 @顶部
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // 透明导航栏 @底部 这一句不要加，目的是防止沉浸式状态栏和部分底部自带虚拟按键的手机（比如华为）发生冲突，注释掉就好了
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    //  判断是否存在虚拟按键
    //  @return 是否存在
    public boolean checkDeviceHasNavigationBar() {
        boolean hasNavigationBar = false;
        Resources rs = getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            @SuppressLint("PrivateApi") Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {
            CommonUtils.log().e(getName(), e);
        }
        return hasNavigationBar;
    }

    //  跳转页面
    //  @param targetActivity 所跳转的目的Activity类
    public void jumpTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivity(intent);
        } else {
            CommonUtils.log().e(getName(), "mActivity not found for " + targetActivity.getSimpleName());
        }
    }

    //  跳转页面
    //  @param targetActivity 所跳转的目的Activity类
    //  @param bundle         跳转所携带的信息
    public void jumpTo(Class<?> targetActivity, Bundle bundle) {
        Intent intent = new Intent(this, targetActivity);
        if (bundle != null) {
            intent.putExtra("bundle", bundle);
        }
        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivity(intent);
        } else {
            CommonUtils.log().e(getName(), "mActivity not found for " + targetActivity.getSimpleName());
        }
    }

    //  跳转页面
    //  @param targetActivity 所跳转的Activity类
    //  @param requestCode    请求码
    public void jumpTo(Class<?> targetActivity, int requestCode) {
        Intent intent = new Intent(this, targetActivity);
        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivityForResult(intent, requestCode);
        } else {
            CommonUtils.log().e(getName(), "mActivity not found for " + targetActivity.getSimpleName());
        }
    }

    //  跳转页面
    //  @param targetActivity 所跳转的Activity类
    //  @param bundle         跳转所携带的信息
    //  @param requestCode    请求码
    public void jumpTo(Class<?> targetActivity, int requestCode, Bundle bundle) {
        Intent intent = new Intent(this, targetActivity);
        if (bundle != null) {
            intent.putExtra("bundle", bundle);
        }
        if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivityForResult(intent, requestCode);
        } else {
            CommonUtils.log().e(getName(), "mActivity not found for " + targetActivity.getSimpleName());
        }
    }

    //  重启当前Activity
    protected void restartSelf() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    //  获取当前Activity类名
    //  @return 类名字符串
    protected String getName() {
        return getClass().getSimpleName();
    }
    //  获取类名
    //  @param targetClass 需要获取名称的类
    //  @return 类名字符串
    protected String getName(Class<?> targetClass) {
        return targetClass.getSimpleName();
    }

    //  消息提示框
    //  https:// www.jianshu.com/p/4551734b3c21
    //  @param message 提示消息文本
    @SuppressLint("ShowToast")
        public void toast(String message) {
        try {
            if (toast != null) {
                toast.setText(message);
            } else {
                toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {
            // 解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
            toast.show();
            Looper.loop();
        }
    }

    //  消息提示框
    //  https:// www.jianshu.com/p/4551734b3c21
    //  @param messageId 提示消息文本ID
    @SuppressLint("ShowToast")
        protected void toast(int messageId) {
        try {
            if (toast != null) {
                toast.setText(messageId);
            } else {
                toast = Toast.makeText(mContext, messageId, Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {
            // 解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            toast = Toast.makeText(mContext, messageId, Toast.LENGTH_SHORT);
            toast.show();
            Looper.loop();
        }
    }

    //  消息提示框
    //  @param messageId 提示消息文本ID
    @SuppressLint("ShowToast")
        protected void toast(int messageId, int time) {
        try {
            if (toast != null) {
                toast.setText(messageId);
            } else {
                toast = Toast.makeText(mContext, messageId, time);
            }
            toast.show();
        } catch (Exception e) {
            // 解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            Toast.makeText(mContext, messageId, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    //  关闭所有Activity（除MainActivity以外）
    public void finishActivity() {
        for (Activity activity : activities) {
            if (activity.getClass().equals(MainActivity.class))
                break;
            activity.finish();
        }
    }

    //  返回历史界面
    //  @param targetActivity 指定的Activity对应的class
    public static void backTo(Class<?> targetActivity) {
        int size = activities.size();
        if (activitiesMap.get(targetActivity) != null)
            for (int i = size - 1; i >= 0; i--) {
                if (targetActivity.equals(activities.get(i).getClass())) {
                    break;
                } else {
                    activities.get(i).finish();
                }
            }
        else
            CommonUtils.log().e(activities.get(size - 1).getClass().getSimpleName(), "mActivity not open for " + targetActivity.getSimpleName());
    }

    @Override
        protected void onDestroy() {
        super.onDestroy();
        //  移除当前的Activity
        activities.remove(this);
        activitiesMap.remove(getClass());
        //  清除网络状态接受者
        event = null;
    }

    //  简化获取View
    //  @param viewId View的ID
    //  @param <T>    将View转化为对应泛型，简化强转的步骤
    //  @return ID对应的View
    @SuppressWarnings("unchecked")
        public <T extends View> T getView(int viewId) {
        return (T) findViewById(viewId);
    }

    //  简化获取View
    //  @param view   父view
    //  @param viewId View的ID
    //  @param <T>    将View转化为对应泛型，简化强转的步骤
    //  @return ID对应的View
    @SuppressWarnings("unchecked")
        public <T extends View> T getView(View view, int viewId) {
        return (T) view.findViewById(viewId);
    }

    //  简化获取View
    //  @param layoutId 父布局Id
    //  @param viewId   View的ID
    //  @param <T>      将View转化为对应泛型，简化强转的步骤
    //  @return ID对应的View
    @SuppressWarnings("unchecked")
        public <T extends View> T getView(int layoutId, int viewId) {
        return (T) LayoutInflater.from(mContext).inflate(layoutId, null).findViewById(viewId);
    }

    //  设置点击事件
    //  @param layouts 点击控件Id
    protected void setOnClickListener(int... layouts) {
        for (int layout : layouts) {
            getView(layout).setOnClickListener(onClickListener);
        }
    }

    //  SDK初始化
    protected void initSDK() {
    }

    //  初始化Bundle
    protected abstract void getBundle(Bundle bundle);

    //  点击事件
    protected abstract void onViewClick(View v);

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            onViewClick(v);
        }
    };
}
