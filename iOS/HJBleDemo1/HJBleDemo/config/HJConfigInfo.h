//
//  HJConfigInfo.h
//  HJBle
//
//  Created by 吴睿智 on 2019/1/6.
//  Copyright © 2019年 wurz. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <wiseBle/WiseBle.h>

NS_ASSUME_NONNULL_BEGIN



@interface HJConfigInfo : NSObject

//默认数据发送服务
@property (nonatomic, strong, readonly) WWCharacteristic *dataDefaultSendService;

//默认数据接收服务
@property (nonatomic, strong, readonly) WWCharacteristic *dataDefaultReceiveService;

//数据发送服务
@property (nonatomic, strong, readonly) WWCharacteristic *dataSendService;

//数据接收服务
@property (nonatomic, strong, readonly) WWCharacteristic *dataReceiveService;

//数据配置服务
@property (nonatomic, strong) WWCharacteristic *configSendService;

//数据配置服务
@property (nonatomic, strong) WWCharacteristic *configReceiveService;

// 是否为配置模式，默认为false
@property (nonatomic, assign) BOOL isBleConfig;

// 是否为Hex模式，默认为true
@property (nonatomic, assign) BOOL isBleHex;

// 是否添加回车，默认false
@property (nonatomic, assign) BOOL isAddReturn;

// 是否清除输入框，默认true
@property (nonatomic, assign) BOOL isClearInput;

// 是否过滤，默认true
@property (nonatomic, assign) BOOL isScanFilter;

// 数据主服务
@property (nonatomic, strong) NSString *dataMainService;

// 数据发送服务
@property (nonatomic, strong) NSString *dataSendSubService;

// 数据通知服务
@property (nonatomic, strong) NSString *dataReceiveSubService;


/**
 * 配置信息单例
 */
+ (instancetype)shareInstance;

@end

NS_ASSUME_NONNULL_END
