package com.hongjia.hjbledemo.bean;

import java.io.Serializable;

/**
 * 自定义指令数据模型
 */
public class CustomCommand implements Serializable {
    
    /**
     * 指令类型：十六进制
     */
    public static final int TYPE_HEX = 0;
    
    /**
     * 指令类型：ASCII字符
     */
    public static final int TYPE_ASCII = 1;
    
    private String id;          // 唯一标识
    private String name;        // 指令名称
    private String command;     // 指令内容
    private int type;           // 指令类型（TYPE_HEX or TYPE_ASCII）
    private long createTime;    // 创建时间
    
    public CustomCommand() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.createTime = System.currentTimeMillis();
    }
    
    public CustomCommand(String name, String command, int type) {
        this();
        this.name = name;
        this.command = command;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    /**
     * 获取指令类型的字符串描述
     */
    public String getTypeString() {
        return type == TYPE_HEX ? "HEX" : "ASCII";
    }
    
    @Override
    public String toString() {
        return "CustomCommand{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", command='" + command + '\'' +
                ", type=" + type +
                ", createTime=" + createTime +
                '}';
    }
}