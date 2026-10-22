//
//  SetViewController.m
//  HJBleDemo
//
//  Created by 吴睿智 on 2019/7/23.
//  Copyright © 2019 wuruizhi. All rights reserved.
//

#import "SetViewController.h"
#import "HJConfigInfo.h"
#import "HJFeedback.h"

@interface SetViewController ()

@property (nonatomic, strong) UIButton *asciiBtn;

@end

@implementation SetViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.navigationItem.title = @"设置";
    
    [self initView];
}

- (void)initView
{
    UIView *inputClearBgView = [UIView new];
    [self.contentView addSubview:inputClearBgView];
    [inputClearBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(self.contentView).offset(10);
    }];
    
    UILabel *inputClearLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    inputClearLabel.text = @"是否清除输入框";
    [inputClearBgView addSubview:inputClearLabel];
    [inputClearLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(inputClearBgView).offset(15);
        make.centerY.equalTo(inputClearBgView);
    }];
    
    NSString *title = @"";
    if ([HJConfigInfo shareInstance].isClearInput) {
        title = @"是";
    }
    else {
        title = @"否";
    }
    
    UIButton *inputClearBtn = [self createIconBtn:title];
    [inputClearBtn addTarget:self action:@selector(showInputClearMenu:) forControlEvents:UIControlEventTouchUpInside];
    [inputClearBgView addSubview:inputClearBtn];
    [inputClearBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(inputClearBgView).offset(-15);
        make.centerY.equalTo(inputClearLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@60);
    }];
    inputClearLabel.numberOfLines = 2;
    [inputClearLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(inputClearBtn.mas_left).offset(-8);
    }];
    
    UIView *asciiBgView = [UIView new];
    [self.contentView addSubview:asciiBgView];
    [asciiBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(inputClearBgView.mas_bottom).offset(10);
    }];
    
    UILabel *asciiLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    asciiLabel.text = @"当前字符";
    [asciiBgView addSubview:asciiLabel];
    [asciiLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(asciiBgView).offset(15);
        make.centerY.equalTo(asciiBgView);
    }];
    
    title = @"";
    if ([HJConfigInfo shareInstance].isBleHex) {
        title = @"Hex";
    }
    else {
        title = @"Ascii";
    }
    
    UIButton *asciiBtn = [self createIconBtn:title];
    [asciiBtn addTarget:self action:@selector(showAsciiMenu:) forControlEvents:UIControlEventTouchUpInside];
    _asciiBtn = asciiBtn;
    [asciiBgView addSubview:asciiBtn];
    [asciiBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(asciiBgView).offset(-15);
        make.centerY.equalTo(asciiLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@60);
    }];
    asciiLabel.numberOfLines = 2;
    [asciiLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(asciiBtn.mas_left).offset(-8);
    }];
    
    UIView *modeBgView = [UIView new];
    [self.contentView addSubview:modeBgView];
    [modeBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(asciiBgView.mas_bottom).offset(10);
    }];
    
    UILabel *modeLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    modeLabel.text = @"当前模式";
    [modeBgView addSubview:modeLabel];
    [modeLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(modeBgView).offset(15);
        make.centerY.equalTo(modeBgView);
    }];
    
    if ([HJConfigInfo shareInstance].isBleConfig) {
        title = @"配置模式";
    }
    else {
        title = @"数据模式";
    }
    UIButton *modeBtn = [self createIconBtn:title];
    [modeBtn addTarget:self action:@selector(showModeMenu:) forControlEvents:UIControlEventTouchUpInside];
    [modeBgView addSubview:modeBtn];
    [modeBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(modeBgView).offset(-15);
        make.centerY.equalTo(modeLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@90);
    }];
    modeLabel.numberOfLines = 2;
    [modeLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(modeBtn.mas_left).offset(-8);
    }];
    
    modeBgView.hidden = !_isBleConfig;
    
    UIView *lastView = !_isBleConfig ? asciiBgView :modeBgView;
    
    // 回车
    UIView *addReturnBgView = [UIView new];
    [self.contentView addSubview:addReturnBgView];
    [addReturnBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(lastView.mas_bottom).offset(10);
    }];
    
    UILabel *addReturnLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    addReturnLabel.text = @"是否添加回车";
    [addReturnBgView addSubview:addReturnLabel];
    [addReturnLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(addReturnBgView).offset(15);
        make.centerY.equalTo(addReturnBgView);
    }];
    
    if ([HJConfigInfo shareInstance].isAddReturn) {
        title = @"是";
    }
    else {
        title = @"否";
    }
    
    UIButton *addReturnBtn = [self createIconBtn:title];
    [addReturnBtn addTarget:self action:@selector(showAddReturnMenu:) forControlEvents:UIControlEventTouchUpInside];
    [addReturnBgView addSubview:addReturnBtn];
    [addReturnBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(addReturnBgView).offset(-15);
        make.centerY.equalTo(addReturnLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@60);
    }];
    addReturnLabel.numberOfLines = 2;
    [addReturnLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(addReturnBtn.mas_left).offset(-8);
    }];
    
    // respone 模式
    UIView *responeBgView = [UIView new];
    [self.contentView addSubview:responeBgView];
    [responeBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(addReturnBgView.mas_bottom).offset(10);
    }];
    
    UILabel *responeLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    responeLabel.text = @"Respone模式";
    [responeBgView addSubview:responeLabel];
    [responeLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(responeBgView).offset(15);
        make.centerY.equalTo(responeBgView);
    }];
    
    if ([HJConfigInfo shareInstance].isRespone) {
        title = @"是";
    }
    else {
        title = @"否";
    }
    UIButton *responeBtn = [self createIconBtn:title];
    [responeBtn addTarget:self action:@selector(showResponeMenu:) forControlEvents:UIControlEventTouchUpInside];
    [responeBgView addSubview:responeBtn];
    [responeBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(responeBgView).offset(-15);
        make.centerY.equalTo(responeLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@90);
    }];
    responeLabel.numberOfLines = 2;
    [responeLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(responeBtn.mas_left).offset(-8);
    }];
    
    // 测试数据长度
    UIView *testDataLenBgView = [UIView new];
    [self.contentView addSubview:testDataLenBgView];
    [testDataLenBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(responeBgView.mas_bottom).offset(10);
    }];
    
    UILabel *testDataLenTitleLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    testDataLenTitleLabel.text = @"测试数据总长度";
    [testDataLenBgView addSubview:testDataLenTitleLabel];
    [testDataLenTitleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(testDataLenBgView).offset(15);
        make.centerY.equalTo(testDataLenBgView);
    }];
    
    title = [NSString stringWithFormat:@"%d", [HJConfigInfo shareInstance].dataTotalLen];
    UIButton *testDataLenBtn = [self createIconBtn:title];
    [testDataLenBtn addTarget:self action:@selector(showTestDataLenDialog:) forControlEvents:UIControlEventTouchUpInside];
    [testDataLenBgView addSubview:testDataLenBtn];
    [testDataLenBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(testDataLenBgView).offset(-15);
        make.centerY.equalTo(testDataLenTitleLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@90);
    }];
    testDataLenTitleLabel.numberOfLines = 2;
    [testDataLenTitleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(testDataLenBtn.mas_left).offset(-8);
    }];
    
    // 下发数据时间间隙
    UIView *sendDataGapBgView = [UIView new];
    [self.contentView addSubview:sendDataGapBgView];
    [sendDataGapBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView);
        make.right.equalTo(self.contentView);
        make.height.equalTo(@45);
        make.top.equalTo(testDataLenBgView.mas_bottom).offset(10);
    }];
    
    UILabel *sendDataGapTitleLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    sendDataGapTitleLabel.text = @"下发数据时间间隙(ms)";
    [sendDataGapBgView addSubview:sendDataGapTitleLabel];
    [sendDataGapTitleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(sendDataGapBgView).offset(15);
        make.centerY.equalTo(sendDataGapBgView);
    }];
    
    title = [NSString stringWithFormat:@"%d", [HJConfigInfo shareInstance].sendDataGap];
    UIButton *sendDataGapBtn = [self createIconBtn:title];
    [sendDataGapBtn addTarget:self action:@selector(showSendDataGapDialog:) forControlEvents:UIControlEventTouchUpInside];
    [sendDataGapBgView addSubview:sendDataGapBtn];
    [sendDataGapBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(sendDataGapBgView).offset(-15);
        make.centerY.equalTo(sendDataGapTitleLabel);
        make.height.equalTo(@44);
        make.width.equalTo(@90);
    }];
    sendDataGapTitleLabel.numberOfLines = 2;
    [sendDataGapTitleLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.lessThanOrEqualTo(sendDataGapBtn.mas_left).offset(-8);
    }];
    [sendDataGapBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.bottom.equalTo(self.contentView).offset(-15);
    }];

    
    
}

// 创建iconbtn
- (UIButton *)createIconBtn:(NSString *)title
{
    UIButton *btn = [[UIButton alloc] init];
    btn.titleLabel.font = [UIFont systemFontOfSize:14];
    btn.titleLabel.adjustsFontSizeToFitWidth = YES;
    btn.titleLabel.minimumScaleFactor = 0.7;
    [btn setTitle:title forState:UIControlStateNormal];
    
    [btn setTitleColor:HJColor(0xFFFFFF) forState:UIControlStateNormal];
    [btn setBackgroundImage:HJColorImage(HJColor(0x649fea)) forState:UIControlStateNormal];
    btn.semanticContentAttribute = UISemanticContentAttributeForceRightToLeft;
    [btn setImage:[UIImage systemImageNamed:@"chevron.down"] forState:UIControlStateNormal];
    btn.tintColor = UIColor.whiteColor;
    btn.layer.cornerRadius = 5;
    btn.layer.masksToBounds = true;
    
    return btn;
}

// UIKit action sheet 在 iPad 上锚定触发按钮，避免缺少 popover 来源。
- (void)showChoices:(NSArray<NSString *> *)choices sender:(UIButton *)sender selected:(void (^)(NSUInteger))selected
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    [choices enumerateObjectsUsingBlock:^(NSString *title, NSUInteger index, BOOL *stop) {
        [alert addAction:[UIAlertAction actionWithTitle:title style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
            [sender setTitle:title forState:UIControlStateNormal];
            selected(index);
        }]];
    }];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    alert.popoverPresentationController.sourceView = sender;
    alert.popoverPresentationController.sourceRect = sender.bounds;
    [self presentViewController:alert animated:YES completion:nil];
}
- (void)showInputClearMenu:(UIButton *)sender {
    [self showChoices:@[@"是", @"否"] sender:sender selected:^(NSUInteger index) {
        HJConfigInfo.shareInstance.isClearInput = index == 0;
    }];
}
- (void)showAsciiMenu:(UIButton *)sender {
    [self showChoices:@[@"ASCII", @"HEX"] sender:sender selected:^(NSUInteger index) {
        HJConfigInfo.shareInstance.isBleHex = index == 1;
    }];
}
- (void)showModeMenu:(UIButton *)sender {
    [self showChoices:@[@"数据模式", @"配置模式"] sender:sender selected:^(NSUInteger index) {
        HJConfigInfo.shareInstance.isBleConfig = index == 1;
        if (index == 1) {
            HJConfigInfo.shareInstance.isBleHex = NO;
            [self.asciiBtn setTitle:@"ASCII" forState:UIControlStateNormal];
        }
    }];
}
- (void)showAddReturnMenu:(UIButton *)sender {
    [self showChoices:@[@"是", @"否"] sender:sender selected:^(NSUInteger index) {
        HJConfigInfo.shareInstance.isAddReturn = index == 0;
    }];
}
- (void)showResponeMenu:(UIButton *)sender {
    [self showChoices:@[@"是", @"否"] sender:sender selected:^(NSUInteger index) {
        HJConfigInfo.shareInstance.isRespone = index == 0;
    }];
}

- (void)showTestDataLenDialog:(UIButton *)sender {
    
    [self showDialog:@"测试数据总长度" handler:^(NSString *text) {
        if ([self isStringAnInteger:text]) {
            [sender setTitle:text forState:UIControlStateNormal];
            [[HJConfigInfo shareInstance] setDataTotalLen:[self stringToInt:text]];
        } else {
            [self.view hj_showMessage: @"输入格式异常"];
        }
    }];
        
}

- (void)showSendDataGapDialog:(UIButton *)sender {
    
    [self showDialog:@"下发数据时间间隙(ms)" handler:^(NSString *text) {
        if ([self isStringAnInteger:text]) {
            [sender setTitle:text forState:UIControlStateNormal];
            [[HJConfigInfo shareInstance] setSendDataGap:[self stringToInt:text]];
        } else {
            [self.view hj_showMessage: @"输入格式异常"];
        }
    }];
        
}

- (BOOL)isStringAnInteger: (NSString *)string {
    NSScanner *scanner = [NSScanner scannerWithString:string];
    int value;
    return [scanner scanInt:&value] && [scanner isAtEnd];
}

- (int)stringToInt: (NSString *)string {
    NSScanner *scanner = [NSScanner scannerWithString:string];
    int value = 0;
    [scanner scanInt:&value];
    return value;
}


// 显示输入框
- (void)showDialog:(NSString *)title handler: (void (^ __nullable)(NSString *text))handler
{
    // 创建UIAlertController
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:title
                                                                             message:nil
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    
    // 添加文本输入框
    [alertController addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        // 可以在这里配置textField的属性，例如placeholder等
        textField.placeholder = [NSString stringWithFormat: @"请输入%@", title];
        textField.keyboardType = UIKeyboardTypeNumberPad;
    }];
    
    // 添加确定按钮
    UIAlertAction *okAction = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        // 用户点击确定按钮后的操作
        UITextField *textField = alertController.textFields.firstObject;
        if (textField && handler) {
            handler(textField.text);
        }
    }];
    
    // 添加取消按钮
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil];
    
    // 将动作按钮添加到UIAlertController
    [alertController addAction:okAction];
    [alertController addAction:cancelAction];
    
    [self presentViewController:alertController animated:YES completion:nil];
}



@end
