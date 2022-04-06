//
//  BleViewController.h
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "WiseBaseKit.h"
#import "DXMessageToolBar.h"
#import "WiseBle.h"
#import "HJBleScanData.h"

@class BleViewController;


@interface BleViewController : WWBaseTableViewController<DXMessageToolBarDelegate,WWBluetoothLEManagerDelegate,WWBluetoothLEDelegate>

@property (nonatomic, strong) HJBleScanData *scanData;


@end
