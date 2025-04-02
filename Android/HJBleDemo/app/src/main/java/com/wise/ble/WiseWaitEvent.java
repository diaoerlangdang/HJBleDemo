package com.wise.ble;

public class WiseWaitEvent {

   public final static int ERROR_FAILED = 3;
   public final static int ERROR_TIME_OUT = 4;
   public final static int WillWaitting = 2;
   public final static int Waitting = 1;
   public final static int SUCCESS = 0;

   private volatile int mResult = SUCCESS;

   private volatile boolean ready = false; // 如果是true，则表示是被唤醒

   public void init() {

      // 防止等待之前先成功，所以在使用前先init，如果还没有waitSignal就调用setSignal，则会立即成功
      mResult = WillWaitting;
      ready = false;
   }

   public synchronized void setSignal(int result) {
      ready = true;
      mResult = result;
      notify();
   }

   public synchronized int waitSignal(long mills) {

      // 根据时间来判断是否超时
      long begin = System.currentTimeMillis();
      long rest = mills;
      if (rest == 0) {
         try {
            wait();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         return mResult;
      } else {
         while (!ready && rest > 0) { // 如果被唤醒（ready为true），或超时（rest <= 0）则结束循环
            try {
               wait(rest);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
            rest = mills - (System.currentTimeMillis() - begin); // 计算剩余时间
         }
         // 超时
         if (!ready) {
            mResult = ERROR_TIME_OUT;
         }
         return mResult;
      }
   }

   //获取等待状态
   int getWaitStatus() {
      return mResult;
   }
}
