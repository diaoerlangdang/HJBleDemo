package com.wise.wisekit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.Nullable;

import com.wise.wisekit.R;

public class ProgressBarView extends View {
   private Paint mPaint; // 画笔
   private CircleBarAnim anim; // 动画
   private float progressSweepAngle = 0; // 初始化进度条圆弧扫过的角度

   // 以下是自定义参数
   private int mAnnulusWidth; // 圆环宽度
   private int mProgressWidth; // 进度条宽度
   private int mBorderWidth; // 圆环边框宽度
   private int mAnnulusColor; // 圆环颜色
   private int mAnnulusBorderColor; // 圆环边框颜色
   private int mLoadColor; // 加载进度圆弧扫过的颜色
   private int mProgress = 0; // 当前进度
   private int maxProgress = 100; // 最大进度，默认100
   public int startAngle = -90; // 开始圆点角度

   public ProgressBarView(Context context) {
      this(context, null);
   }

   public ProgressBarView(Context context, @Nullable AttributeSet attrs) {
      this(context, attrs, 0);
   }

   public ProgressBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      // 获取自定义属性
      TypedArray value = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ProgressBarView, defStyleAttr, 0);
      int indexCount = value.getIndexCount();
      for (int i = 0; i < indexCount; i++) {
         int parm = value.getIndex(i);
         if (parm == R.styleable.ProgressBarView_startAngle) {
            startAngle = value.getInt(parm, -90);
         }
         else if (parm == R.styleable.ProgressBarView_annulusWidth) {
            mAnnulusWidth = value.getDimensionPixelSize(parm,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            10,
                            getResources().getDisplayMetrics()));
         }
         else if (parm == R.styleable.ProgressBarView_progressWidth) {
            mProgressWidth = value.getDimensionPixelSize(parm,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            10,
                            getResources().getDisplayMetrics()));
         }
         else if (parm == R.styleable.ProgressBarView_annulusColor) {
            mAnnulusColor = value.getColor(parm, Color.BLACK);
         }
         else if (parm == R.styleable.ProgressBarView_loadColor) {
            mLoadColor = value.getColor(parm, Color.BLACK);
         }
         else if (parm == R.styleable.ProgressBarView_progress) {
            mProgress = value.getInt(parm, 0);
         }
         else if (parm == R.styleable.ProgressBarView_borderColor) {
            mAnnulusBorderColor = value.getColor(parm, Color.BLACK);
         }
         else if (parm == R.styleable.ProgressBarView_borderColor) {
            mBorderWidth = value.getDimensionPixelSize(parm,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            1,
                            getResources().getDisplayMetrics()));
         }
      }
      value.recycle();
      // 动画
      anim = new CircleBarAnim();
      mPaint = new Paint();
   }

   @Override
   protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      // 绘制圆环
      int centre = getWidth() / 2;
      int radius = centre - mAnnulusWidth / 2;

      // 边框没好使？？？？
      mPaint.setAntiAlias(true);
      mPaint.setStrokeWidth(mBorderWidth + mAnnulusWidth); // 设置边框宽度
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setColor(mAnnulusBorderColor); // 设置边框颜色
      // 绘制边框
      canvas.drawCircle(centre, centre, radius + mBorderWidth, mPaint);


      mPaint.setAntiAlias(true);
      mPaint.setStrokeWidth(mAnnulusWidth);
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setColor(mAnnulusColor);
      canvas.drawCircle(centre, centre, radius, mPaint);



      // 画圆弧，进度
      int progressRadius = centre - mAnnulusWidth / 2;
      mPaint.setStyle(Paint.Style.STROKE);
      mPaint.setColor(mLoadColor);
      mPaint.setStrokeWidth(mProgressWidth);
      mPaint.setAntiAlias(true); // 设置抗锯齿
      mPaint.setStrokeCap(Paint.Cap.ROUND); // 设置线条端点为圆角

      RectF ovalStroke = new RectF(centre - progressRadius, centre - progressRadius,
              centre + progressRadius, centre + progressRadius);
      canvas.drawArc(ovalStroke, startAngle, progressSweepAngle, false, mPaint);
   }

   public synchronized void setProgress(int progress, int time) {
      if (progress > maxProgress) {
         progress = maxProgress;
      }
      if (progress < 0) {
         progress = 0;
      }
      anim.setDuration(time);
      this.startAnimation(anim);
      this.mProgress = progress;
   }

   // 动画
   public class CircleBarAnim extends Animation {
      public CircleBarAnim() {

      }

      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
         super.applyTransformation(interpolatedTime, t);
         progressSweepAngle=interpolatedTime*(mProgress * 360 / maxProgress);//这里计算进度条的比例

         postInvalidate();
      }
   }

   public synchronized int getmProgress() {
      return mProgress;
   }
}
