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
@property(nonatomic, strong) UISwitch *flowControlSwitch;

@end

@implementation AppConfigSetViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.navigationItem.title = @"软件设置";
    
    [self initView];
    
    [_filterSwitch addTarget:self action:@selector(onChangeFilter) forControlEvents:UIControlEventValueChanged];
    
    _filterSwitch.on = [HJConfigInfo shareInstance].isScanFilter;
    [_flowControlSwitch addTarget:self action:@selector(onChangeFlowControl) forControlEvents:UIControlEventValueChanged];
    _flowControlSwitch.on = [HJConfigInfo shareInstance].isFlowControlCheck;
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

    // 流控检查
    UIView *flowControlView = [self createItemView:@"流控检查"];
    _flowControlSwitch = [[UISwitch alloc] init];
    _flowControlSwitch.accessibilityLabel = @"流控检查";
    _flowControlSwitch.accessibilityHint = @"连接时查询串口流控状态，下次连接生效";
    [flowControlView addSubview:_flowControlSwitch];
    [_flowControlSwitch mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.equalTo(flowControlView);
        make.right.equalTo(flowControlView).offset(-15);
    }];
    [self.contentView addSubview:flowControlView];
    [flowControlView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.contentView);
        make.top.equalTo(lastView.mas_bottom).offset(10);
        make.height.equalTo(@(VIEW_HEIGHT));
    }];
    UILabel *flowControlHint = HJLabel(HJColor(0x666666), [UIFont systemFontOfSize:12]);
    flowControlHint.text = @"连接时查询串口流控状态，下次连接生效";
    flowControlHint.numberOfLines = 0;
    [self.contentView addSubview:flowControlHint];
    [flowControlHint mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView).offset(15);
        make.right.equalTo(self.contentView).offset(-15);
        make.top.equalTo(flowControlView.mas_bottom).offset(5);
    }];
    lastView = flowControlHint;
    
    // 数据服务
    UIView *dataServiceView = [self createItemView:@"数据服务"];
    [dataServiceView addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(gotoServiceConfig)]];
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
    UILabel *versionLabel = HJLabel(HJColor(0x666666), [UIFont systemFontOfSize:15]);
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
    
    lastView = versionView;
    
    [lastView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.bottom.equalTo(self.contentView).offset(-15);
    }];
    
    
}

- (UIView *)createItemView:(NSString *)title {
    
    UIView *view = [UIView new];
    view.backgroundColor = HJColor(0xececec);
    
    UILabel *titleLabel = HJLabel(HJColor(0x666666), [UIFont systemFontOfSize:15]);
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

- (void)onChangeFlowControl
{
    [HJConfigInfo shareInstance].isFlowControlCheck = _flowControlSwitch.on;
}






@end
