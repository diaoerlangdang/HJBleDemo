//
//  BleSendReceiveData.m
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "BleSendReceiveData.h"

@implementation BleSendReceiveData

-(id)initWithDictionary:(NSDictionary *)dict
{
    self = [super init];
    if (self) {
        _time = dict[@"time"];
        _context = dict[@"context"];
        _title = dict[@"title"];
    }
    
    return self;
}

//实例化
+(id)BleSendReceiveDataWithDictionary:(NSDictionary *)dict
{
    return [[BleSendReceiveData alloc] initWithDictionary:dict];
}

@end
