//
//  HJScanTableViewCell.h
//  HJBleDemo
//
//  Created by 吴睿智 on 2020/3/12.
//  Copyright © 2020 wuruizhi. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "HJBleScanData.h"


NS_ASSUME_NONNULL_BEGIN

@interface HJScanTableViewCell : UITableViewCell

@property (nonatomic, strong) HJBleScanData *data;

@end

NS_ASSUME_NONNULL_END
