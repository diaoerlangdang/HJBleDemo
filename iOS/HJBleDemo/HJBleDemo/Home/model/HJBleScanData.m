//
//  HJBle.m
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "HJBleScanData.h"

@implementation HJBleScanData

/**
 格式化时间
 
 @return 字符串
 */
- (NSString *)formatDateTime
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    
    dateFormatter.dateFormat = [NSString stringWithFormat:@"yyyy-MM-dd HH:mm:ss"];
    
    return [dateFormatter stringFromDate:self.datetime];
}

@end
