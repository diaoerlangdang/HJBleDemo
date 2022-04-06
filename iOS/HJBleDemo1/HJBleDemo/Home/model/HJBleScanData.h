//
//  HJBle.h
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import <CoreBluetooth/CoreBluetooth.h>

@interface HJBleScanData : NSObject

@property(nonatomic,strong) CBPeripheral *peripheral;
@property(nonatomic,strong) NSDictionary *advertisementData;
@property(nonatomic,strong) NSNumber    *RSSI;
// 是否为简易模式
@property (nonatomic, assign) BOOL isEasy;
// 是否支持配置
@property (nonatomic, assign) BOOL isConfig;
// 是否支持流控
@property (nonatomic, assign) BOOL bFlowControl;
// 最大发送数据长度
@property (nonatomic, assign) NSInteger sendDataLenMax;

// mac 地址
@property (nonatomic, strong) NSString *mac;

//时间
@property (nonatomic, strong) NSDate *datetime;


/**
 格式化时间

 @return 字符串
 */
- (NSString *)formatDateTime;


@end
