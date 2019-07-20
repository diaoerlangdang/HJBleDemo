package com.wise.wisekit.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wise.wisekit.R;
import com.wise.wisekit.activity.BaseTabActivity;
import com.wise.wisekit.listener.OnDoubleClickListener;

/**
 * Created by wuruizhi on 2017/8/12.
 */

public class TabBarButton extends FrameLayout {

    private int mIndex;

    private ImageView mImage;
    private TextView mTitle;
    private TextView mNotify;

    private Drawable mSelectedImg;
    private Drawable mUnselectedImg;

    private Context mContext;

    public TabBarButton(Context context) {
        this(context, null);
    }

    public TabBarButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabBarButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tab_bar_button, this, true);
        RelativeLayout container = (RelativeLayout) findViewById(R.id.tab_btn_container);

        mImage = (ImageView) findViewById(R.id.tab_btn_default);
        mTitle = (TextView) findViewById(R.id.tab_btn_title);
        mNotify = (TextView) findViewById(R.id.tab_unread_notify);

        /**双击的判断是有延迟的,为了其他三个按钮click的点击速度，所以区别对待*/
        if (mIndex == 0) {
            OnDoubleClickListener touchListener = new OnDoubleClickListener() {
                @Override
                public void onDoubleClick(View view) {
                    if (mIndex == 0) {
                        ((BaseTabActivity) mContext).chatDoubleListener();
                    }
                }

                @Override
                public void onClick(View view) {
                }
            };
            container.setOnTouchListener(touchListener);
        }

        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseTabActivity) mContext).setFragmentIndicator(mIndex);
            }
        };
        container.setOnClickListener(clickListener);

    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public void setUnselectedImage(Drawable img) {
        this.mUnselectedImg = img;
    }

    public void setSelectedImage(Drawable img) {
        this.mSelectedImg = img;
    }

    private void setSelectedColor(Boolean selected) {
        if (selected) {
            mTitle.setTextColor(getResources().getColor(
                    R.color.colorTabBarSelectedText));
        } else {
            mTitle.setTextColor(getResources().getColor(
                    R.color.colorTabBarNormalText));
        }
    }

    public void setSelectedButton(Boolean selected) {
        setSelectedColor(selected);
        if (selected) {
            mImage.setImageDrawable(mSelectedImg);
        } else {
            mImage.setImageDrawable(mUnselectedImg);
        }
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setUnreadNotify(int unreadNum) {
        if (0 == unreadNum) {
            mNotify.setVisibility(View.INVISIBLE);
            return;
        }

        String notify;
        if (unreadNum > 99) {
            notify = "99+";
        } else {
            notify = Integer.toString(unreadNum);
        }

        mNotify.setText(notify);
        mNotify.setVisibility(View.VISIBLE);
    }
}
