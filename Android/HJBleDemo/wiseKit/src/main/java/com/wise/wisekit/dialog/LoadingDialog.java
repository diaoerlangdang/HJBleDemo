package com.wise.wisekit.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.wise.wisekit.R;

/**
 * Created by tjy on 2017/6/19.
 */
public class LoadingDialog extends Dialog{

    private TextView msgText;

    public LoadingDialog(Context context) {
        super(context);
    }

    public LoadingDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public void setMsgText(TextView msgText) {
        this.msgText = msgText;
    }

    public void setMessage(String text) {
        this.msgText.setText(text);
    }

    public static class Builder{

        private Context context;
        private String message;
        private boolean isShowMessage=true;
        private boolean isCancelable=false;
        private boolean isCancelOutside=false;
        private OnCancelListener cancelListener;


        public Builder(Context context) {
            this.context = context;
        }

        /**
         * 设置提示信息
         * @param message
         * @return
         */

        public Builder setMessage(String message){
            this.message=message;
            return this;
        }

        /**
         * 设置是否显示提示信息
         * @param isShowMessage
         * @return
         */
        public Builder setShowMessage(boolean isShowMessage){
            this.isShowMessage=isShowMessage;
            return this;
        }

        /**
         * 设置是否可以按返回键取消
         * @param isCancelable
         * @return
         */

        public Builder setCancelable(boolean isCancelable){
            this.isCancelable=isCancelable;
            return this;
        }

        /**
         * 设置是否可以取消
         * @param isCancelOutside
         * @return
         */
        public Builder setCancelOutside(boolean isCancelOutside){
            this.isCancelOutside=isCancelOutside;
            return this;
        }

        /**
         * 设置监听取消
         * @param listener
         * @return
         */
        public Builder setOnCancelListener(OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }

        public LoadingDialog create(){

            LayoutInflater inflater = LayoutInflater.from(context);
            View view=inflater.inflate(R.layout.dialog_loading,null);
            LoadingDialog loadingDailog=new LoadingDialog(context,R.style.LoadingDialogStyle);
            TextView msgText= (TextView) view.findViewById(R.id.tipTextView);
            loadingDailog.setMsgText(msgText);
            if(isShowMessage){
                msgText.setText(message);
            }else{
                msgText.setVisibility(View.GONE);
            }
            loadingDailog.setContentView(view);
            loadingDailog.setCancelable(isCancelable);
            loadingDailog.setCanceledOnTouchOutside(isCancelOutside);
            loadingDailog.setOnCancelListener(cancelListener);
            return  loadingDailog;

        }


    }
}
