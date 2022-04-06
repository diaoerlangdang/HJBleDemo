//
//  BleServiceConfigViewController.m
//  HJBleDemo
//
//  Created by wuruizhi on 2022/3/30.
//  Copyright © 2022 wuruizhi. All rights reserved.
//

#import "BleServiceConfigViewController.h"
#import "HJConfigInfo.h"
#import <Toast/Toast.h>

@interface BleServiceConfigViewController ()

//主服务
@property (nonatomic, strong) WWTextField *mainServiceTF;

// 通知服务
@property (nonatomic, strong) WWTextField *notifyServiceTF;

// 发送服务
@property (nonatomic, strong) WWTextField *sendServiceTF;

@end

@implementation BleServiceConfigViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.navigationItem.title = @"服务设置";
    
    [self initView];
    
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"保存" style:UIBarButtonItemStylePlain target:self action: @selector(save)];
    
    [self updateData];
}


- (void)initView {
    
    _mainServiceTF = [[WWTextField alloc] init];
    _mainServiceTF.placeholder = @"请输入主服务";
    UIView *mainServieView = [self createItemView:@"主服务：" textField:_mainServiceTF];
    
    [self.contentView addSubview:mainServieView];
    [mainServieView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.contentView);
        make.left.right.equalTo(self.contentView);
    }];
    
    _notifyServiceTF = [[WWTextField alloc] init];
    _notifyServiceTF.placeholder = @"请输入通知服务";
    UIView *notifyServiceView = [self createItemView:@"通知服务：" textField:_notifyServiceTF];
    [self.contentView addSubview:notifyServiceView];
    [notifyServiceView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(mainServieView.mas_bottom);
        make.left.right.equalTo(self.contentView);
    }];
    
    _sendServiceTF = [[WWTextField alloc] init];
    _sendServiceTF.placeholder = @"请输入发送服务";
    UIView *sendServiceView = [self createItemView:@"发送服务：" textField:_sendServiceTF];
    [self.contentView addSubview:sendServiceView];
    [sendServiceView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(notifyServiceView.mas_bottom);
        make.left.right.equalTo(self.contentView);
    }];
    
    UILabel *tipsLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0xf20c00) font:WW_Font(12)];
    [self.contentView addSubview:tipsLabel];
    tipsLabel.numberOfLines = 0;
    tipsLabel.text = @"* 输入的格式为xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\n*若服务格式为0000xxxx-0000-1000-8000-00805f9b34fb的数据，请输入xxxx两字节服务";
    [tipsLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView).offset(15);
        make.right.equalTo(self.contentView).offset(-15);
        make.top.equalTo(sendServiceView.mas_bottom).offset(15);
    }];
    
    UIButton *recoveryBtn = [[UIButton alloc] init];
    [recoveryBtn setTitle:@"恢复默认" forState:UIControlStateNormal];
    [recoveryBtn setBackgroundImage:[UIImage imageWithColor:WW_COLOR_HexRGB(0xa0a0a0)] forState:UIControlStateNormal];
    [recoveryBtn setBackgroundImage:[UIImage imageWithColor:WW_COLOR_HexRGB(0x808080)] forState:UIControlStateHighlighted];
    recoveryBtn.layer.cornerRadius = 5;
    recoveryBtn.layer.masksToBounds = true;
    [recoveryBtn addTarget:self action:@selector(onClickRecoveryBtn) forControlEvents:UIControlEventTouchUpInside];
    [self.contentView addSubview:recoveryBtn];
    [recoveryBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerX.equalTo(self.contentView);
        make.width.equalTo(@150);
        make.height.equalTo(@45);
        make.top.equalTo(tipsLabel.mas_bottom).offset(30);
        make.bottom.equalTo(self.contentView).offset(-15);
    }];
    
}

- (UIView *)createItemView: (NSString *)title textField:(WWTextField *) textField {
    
    UIView *view = [UIView new];
    
    UILabel *titleLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x666666) font:WW_Font(15)];
    titleLabel.text = title;
    [view addSubview:titleLabel];
    [titleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(view).offset(15);
        make.top.equalTo(view).offset(10);
    }];
    
    [view addSubview:textField];
    textField.textColor = WW_COLOR_HexRGB(0x333333);
    textField.layer.borderColor = WW_COLOR_HexRGB(0xc0c0c0).CGColor;
    textField.layer.borderWidth = 1;
    textField.layer.cornerRadius = 5;
    [textField mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(view).offset(15);
        make.right.equalTo(view).offset(-15);
        make.top.equalTo(titleLabel.mas_bottom).offset(5);
        make.bottom.equalTo(view).offset(-10);
        make.height.equalTo(@35);
    }];
    
    return view;
}

// 恢复默认
- (void)onClickRecoveryBtn {
    HJConfigInfo *configInfo = [HJConfigInfo shareInstance];
    configInfo.dataMainService = configInfo.dataDefaultSendService.serviceID;
    configInfo.dataSendSubService = configInfo.dataDefaultSendService.characteristicID;
    configInfo.dataReceiveSubService = configInfo.dataDefaultReceiveService.characteristicID;
    
    [self updateData];
}

- (BOOL) verfiyService:(NSString *)serviceId {
    NSString *regex1 = @"^([0-9a-fA-F]{8})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{12})$";
    NSPredicate *pre1 = [NSPredicate predicateWithFormat:@"SELF MATCHES %@",regex1];
    
    NSString *regex2 = @"^([0-9a-fA-F]{4})$";
    NSPredicate *pre2 = [NSPredicate predicateWithFormat:@"SELF MATCHES %@",regex2];
    return [pre1 evaluateWithObject:serviceId] || [pre2 evaluateWithObject:serviceId];
}

// 保存
- (void)save {
    [self.view endEditing:true];
    
    NSString *mainService = _mainServiceTF.text;
    NSString *sendService = _sendServiceTF.text;
    NSString *notifyService = _notifyServiceTF.text;
    
    if (![self verfiyService:mainService]) {
        [self.view makeToast:@"主服务格式不正确"];
        return;
    }
    
    if (![self verfiyService:sendService]) {
        [self.view makeToast:@"发送服务格式不正确"];
        return;
    }
    
    if (![self verfiyService:notifyService]) {
        [self.view makeToast:@"通知服务格式不正确"];
        return;
    }
    
    [HJConfigInfo shareInstance].dataMainService = mainService;
    [HJConfigInfo shareInstance].dataSendSubService = sendService;
    [HJConfigInfo shareInstance].dataReceiveSubService = notifyService;
    
    [self.navigationController.view makeToast:@"保存成功"];
    
    [self.navigationController popViewControllerAnimated:true];
}

- (void)updateData {
    _mainServiceTF.text = [HJConfigInfo shareInstance].dataMainService;
    _sendServiceTF.text = [HJConfigInfo shareInstance].dataSendSubService;
    _notifyServiceTF.text = [HJConfigInfo shareInstance].dataReceiveSubService;
}

@end
