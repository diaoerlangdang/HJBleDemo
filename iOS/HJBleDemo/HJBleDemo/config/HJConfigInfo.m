//
//  HJConfigInfo.m
//  HJBle
//
//  Created by 吴睿智 on 2019/1/6.
//  Copyright © 2019年 wurz. All rights reserved.
//

#import "HJConfigInfo.h"
#import <YYCache/YYCache.h>

@interface HJConfigInfo()

@property (nonatomic, strong) YYCache *cache;

@end

@implementation HJConfigInfo

/**
 * 配置信息单例
 */
+ (instancetype)shareInstance
{
    static HJConfigInfo *g_configInfo;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        g_configInfo = [[HJConfigInfo alloc] init];
    });
    return g_configInfo;
}


- (id)copyWithZone:(struct _NSZone *)zone
{
    return [HJConfigInfo shareInstance] ;
}

- (instancetype)init {
    
    self = [super init];
    
    if (self) {
        
        _cache = [YYCache cacheWithName:@"bleCache"];
        
        // 数据发送服务
        _dataDefaultSendService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF2"];
        
        // 数据接收服务
        _dataDefaultReceiveService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF1"];
        
        
//        // 数据发送服务
//        _dataSendService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF2"];
//        
//        // 数据接收服务
//        _dataReceiveService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF1"];
        
        
        // 配置发送服务
        _configSendService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
        
        // 配置接收服务
        _configReceiveService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
        
        _isBleConfig = false;
    }
    
    return self;
}

- (BOOL)isBleHex
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"isBleHex"];
    if (tmp == nil) {
        return true;
    }
    return tmp.boolValue;
}

- (void)setIsBleHex:(BOOL)isBleHex
{
    [_cache setObject:@(isBleHex) forKey:@"isBleHex"];
}

- (BOOL)isClearInput
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"isClearInput"];
    if (tmp == nil) {
        return false;
    }
    return tmp.boolValue;
}

- (void)setIsClearInput:(BOOL)isClearInput
{
    [_cache setObject:@(isClearInput) forKey:@"isClearInput"];
}

- (BOOL)isAddReturn
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"isAddReturn"];
    if (tmp == nil) {
        return false;
    }
    return tmp.boolValue;
}

- (void)setIsAddReturn:(BOOL)isAddReturn
{
    [_cache setObject:@(isAddReturn) forKey:@"isAddReturn"];
}

- (BOOL)isScanFilter
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"isScanFilter"];
    if (tmp == nil) {
        return true;
    }
    return tmp.boolValue;
}

- (void)setIsScanFilter:(BOOL)isScanFilter
{
    [_cache setObject:@(isScanFilter) forKey:@"isScanFilter"];
}

- (NSString *)dataMainService
{
    NSString *tmp = (NSString *)[_cache objectForKey:@"dataMainService"];
    if (tmp == nil) {
        return _dataDefaultSendService.serviceID;
    }
    return tmp;
}

- (void)setDataMainService:(NSString *)dataMainService
{
    [_cache setObject:dataMainService forKey:@"dataMainService"];
}

- (NSString *)dataSendSubService
{
    NSString *tmp = (NSString *)[_cache objectForKey:@"dataSendSubService"];
    if (tmp == nil) {
        return _dataDefaultSendService.characteristicID;
    }
    return tmp;
}

- (void)setDataSendSubService:(NSString *)dataSendSubService
{
    [_cache setObject:dataSendSubService forKey:@"dataSendSubService"];
}

- (NSString *)dataReceiveSubService
{
    NSString *tmp = (NSString *)[_cache objectForKey:@"dataReceiveSubService"];
    if (tmp == nil) {
        return _dataDefaultReceiveService.characteristicID;
    }
    return tmp;
}

- (void)setDataReceiveSubService:(NSString *)dataReceiveSubService
{
    [_cache setObject:dataReceiveSubService forKey:@"dataReceiveSubService"];
}

- (WWCharacteristic *)dataSendService
{
    return [[WWCharacteristic alloc] initWithServiceID:self.dataMainService characteristicID:self.dataSendSubService];
}

- (WWCharacteristic *)dataReceiveService
{
    return [[WWCharacteristic alloc] initWithServiceID:self.dataMainService characteristicID:self.dataReceiveSubService];
}


- (BOOL)isRespone
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"isRespone"];
    if (tmp == nil) {
        return false;
    }
    return tmp.boolValue;
}

- (void)setIsRespone:(BOOL)isRespone
{
    [_cache setObject:@(isRespone) forKey:@"isRespone"];
}

- (int)dataTotalLen
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"dataTotalLen"];
    if (tmp == nil) {
        return 100;
    }
    return tmp.intValue;
}

- (void)setDataTotalLen:(int)len
{
    [_cache setObject:@(len) forKey:@"dataTotalLen"];
}

- (int)sendDataGap
{
    NSNumber *tmp = (NSNumber *)[_cache objectForKey:@"sendDataGap"];
    if (tmp == nil) {
        return 20;
    }
    return tmp.intValue;
}

- (void)setSendDataGap:(int)gap
{
    [_cache setObject:@(gap) forKey:@"sendDataGap"];
}



@end
