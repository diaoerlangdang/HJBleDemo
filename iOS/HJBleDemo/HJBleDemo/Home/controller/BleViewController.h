//
//  BleViewController.h
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "HJTableViewController.h"
#import "HJUI.h"
#import "HJMessageInputBar.h"
#import "WiseBle.h"
#import "HJBleScanData.h"

@class BleViewController;


@interface BleViewController : HJTableViewController<HJMessageInputBarDelegate,WWBluetoothLEManagerDelegate>

@property (nonatomic, strong) HJBleScanData *scanData;


@end
