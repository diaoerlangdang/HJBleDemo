package com.wise.wisekit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.wise.wisekit.fragment.BaseFragment;
import com.wise.wisekit.widget.TabBarButton;

public abstract class BaseTabActivity extends FragmentActivity {

    protected BaseFragment[] mFragments = new BaseFragment[0];
    protected TabBarButton[] mTabButtons = new TabBarButton[0];

    private int currentIndex = -1;

    //获取页面布局id
    protected abstract int getPageLayoutId();

    //子类实现
    protected abstract  void initTabBar();

    //子类实现
    protected abstract void initFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(getPageLayoutId());

        initTabBar();
        initFragment();
        setFragmentIndicator(0);

        initView();
    }

    //子类可重载初始化
    protected void initView() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentIndex >=0 && currentIndex < mFragments.length) {
            mFragments[currentIndex].willShowFragment();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentIndex >=0 && currentIndex < mFragments.length) {
            mFragments[currentIndex].willHideFragment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mFragments[currentIndex].onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }



    public void setFragmentIndicator(int which) {

        if (which >= mFragments.length || which < 0 || which >= mTabButtons.length || which == currentIndex) {
            return;
        }

        if (currentIndex >= 0) {
            mFragments[currentIndex].willHideFragment();
        }

        mFragments[which].willShowFragment();

        FragmentTransaction fragmentTransaction =  getSupportFragmentManager().beginTransaction();

        for (int i=0; i<mFragments.length; i++) {

            if (i == which) {
                fragmentTransaction.show(mFragments[i]);
                mTabButtons[i].setSelectedButton(true);
            }
            else {
                fragmentTransaction.hide(mFragments[i]);
                mTabButtons[i].setSelectedButton(false);
            }
        }
        fragmentTransaction.commit();

//        getSupportFragmentManager().beginTransaction().hide(mFragments[0]).hide(mFragments[1]).hide(mFragments[2]).show(mFragments[which]).commit();

        currentIndex = which;
    }

    /**双击事件*/
    public void chatDoubleListener() {

    }
}
