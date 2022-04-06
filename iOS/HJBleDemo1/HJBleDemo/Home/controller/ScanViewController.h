//
//  ScanViewController.h
//  bleDemo
//
//  Created by wurz on 15/4/12.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "WiseBle.h"
#import "BleViewController.h"

@interface ScanViewController : WWBaseTableViewController<WWBluetoothLEManagerDelegate, WWBluetoothLEDelegate>

@end
