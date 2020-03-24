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
        _dataSendService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF2"];
        
        // 数据接收服务
        _dataReceiveService = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF1"];
        
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

@end
