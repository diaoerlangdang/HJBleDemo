//
//  AppConfigSetViewController.m
//  HJBleDemo
//
//  Created by wuruizhi on 2022/3/30.
//  Copyright © 2022 wuruizhi. All rights reserved.
//

#import "AppConfigSetViewController.h"
#import "HJConfigInfo.h"
#import "BleServiceConfigViewController.h"


@interface AppConfigSetViewController ()

// 扫描过滤
@property(nonatomic, strong) UISwitch *filterSwitch;

@end

@implementation AppConfigSetViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.navigationItem.title = @"软件设置";
    
    [self initView];
    
    [_filterSwitch addTarget:self action:@selector(onChangeFilter) forControlEvents:UIControlEventValueChanged];
    
    _filterSwitch.on = [HJConfigInfo shareInstance].isScanFilter;
}

- (void)initView {
    
    UIView *lastView = nil;
    
    CGFloat VIEW_HEIGHT = 50;
    
    // 扫描过滤
    UIView *filterView = [self createItemView:@"扫描过滤"];
    _filterSwitch = [[UISwitch alloc] init];
    [filterView addSubview:_filterSwitch];
    [_filterSwitch mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.equalTo(filterView);
        make.right.equalTo(filterView).offset(-15);
    }];
    [self.contentView addSubview:filterView];
    [filterView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.contentView);
        make.top.equalTo(self.contentView).offset(10);
        make.height.equalTo(@(VIEW_HEIGHT));
    }];
    
    lastView = filterView;
    
    // 数据服务
    UIView *dataServiceView = [self createItemView:@"数据服务"];
    [dataServiceView addTapAction:self selector:@selector(gotoServiceConfig)];
    UIImageView *rightImgView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"right_gray"]];
    [dataServiceView addSubview:rightImgView];
    [rightImgView mas_makeConstraints:^(MASConstraintMaker *make) {
            make.centerY.equalTo(dataServiceView);
            make.right.equalTo(dataServiceView).offset(-10);
    }];
    [self.contentView addSubview:dataServiceView];
    [dataServiceView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.contentView);
        make.top.equalTo(lastView.mas_bottom).offset(10);
        make.height.equalTo(@(VIEW_HEIGHT));
    }];
    
    lastView = dataServiceView;
    
    // 版本信息
    UIView *versionView = [self createItemView:@"版本信息"];
    UILabel *versionLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x666666) font:WW_Font(15)];
    NSString *appCurVersionNum = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
    versionLabel.text = [NSString stringWithFormat:@"V%@", appCurVersionNum];
    [versionView addSubview:versionLabel];
    [versionLabel mas_makeConstraints:^(MASConstraintMaker *make) {
            make.centerY.equalTo(versionView);
            make.right.equalTo(versionView).offset(-10);
    }];
    [self.contentView addSubview:versionView];
    [versionView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.contentView);
        make.top.equalTo(lastView.mas_bottom).offset(10);
        make.height.equalTo(@(VIEW_HEIGHT));
    }];
    
    lastView = versionLabel;
    
    [lastView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.bottom.equalTo(self.contentView).offset(-15);
    }];
    
    
}

- (UIView *)createItemView:(NSString *)title {
    
    UIView *view = [UIView new];
    view.backgroundColor = WW_COLOR_HexRGB(0xececec);
    
    UILabel *titleLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x666666) font:WW_Font(15)];
    [view addSubview:titleLabel];
    [titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(view).offset(15);
        make.centerY.equalTo(view);
    }];
    titleLabel.text = title;
    return view;
}

- (void)onChangeFilter
{
    [HJConfigInfo shareInstance].isScanFilter = _filterSwitch.on;
}

- (void)gotoServiceConfig {
    
    BleServiceConfigViewController *vc = [[BleServiceConfigViewController alloc] init];
    [self.navigationController pushViewController:vc animated:true];

}






@end
