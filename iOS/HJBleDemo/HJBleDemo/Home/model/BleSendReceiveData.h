//
//  BleSendReceiveData.h
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface BleSendReceiveData : NSObject

//实例化
+(id)BleSendReceiveDataWithDictionary:(NSDictionary *)dict;

@property(nonatomic,readonly) NSString *time;
@property(nonatomic,readonly) NSString *title;
@property(nonatomic,readonly) NSString *context;

@end
