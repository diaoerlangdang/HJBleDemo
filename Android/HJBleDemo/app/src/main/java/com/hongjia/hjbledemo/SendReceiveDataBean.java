package com.hongjia.hjbledemo;

import java.util.Date;

public class SendReceiveDataBean {

    // 其他类型
    public static final int DataTypeOther = 0;
    // 发送类型
    public static final int DataTypeSend = 1;
    // 接收类型
    public static final int DataTypeReceive = 2;
    // 失败类型
    public static final int DataTypeFailed = 3;

    // 数据类型
    private int dataType;

    // 时间戳
    private long timeStamp;

    // 数据内容
    private String dataInfo;

    public SendReceiveDataBean(int dataType, String dataInfo) {
        this.dataType = dataType;
        this.dataInfo = dataInfo;
        this.timeStamp = new Date().getTime();
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getDataInfo() {
        return dataInfo;
    }

    public void setDataInfo(String dataInfo) {
        this.dataInfo = dataInfo;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
