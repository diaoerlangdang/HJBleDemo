//
//  SetViewController.m
//  HJBleDemo
//
//  Created by 吴睿智 on 2019/7/23.
//  Copyright © 2019 wuruizhi. All rights reserved.
//

#import "SetViewController.h"
#import "PopoverView.h"
#import "HJConfigInfo.h"

@interface SetViewController ()

@property (nonatomic, strong) IconButton *asciiBtn;

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
    [self.view addSubview:inputClearBgView];
    [inputClearBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view);
        make.right.equalTo(self.view);
        make.height.equalTo(@45);
        make.top.equalTo(self.view).offset(10);
    }];
    
    UILabel *inputClearLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(15)];
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
    
    IconButton *inputClearBtn = [self createIconBtn:title];
    [inputClearBtn addTarget:self action:@selector(showInputClearMenu:) forControlEvents:UIControlEventTouchUpInside];
    [inputClearBgView addSubview:inputClearBtn];
    [inputClearBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(inputClearBgView).offset(-15);
        make.centerY.equalTo(inputClearLabel);
        make.height.equalTo(@30);
        make.width.equalTo(@60);
    }];
    
    UIView *asciiBgView = [UIView new];
    [self.view addSubview:asciiBgView];
    [asciiBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view);
        make.right.equalTo(self.view);
        make.height.equalTo(@45);
        make.top.equalTo(inputClearBgView.mas_bottom).offset(10);
    }];
    
    UILabel *asciiLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(15)];
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
    
    IconButton *asciiBtn = [self createIconBtn:title];
    [asciiBtn addTarget:self action:@selector(showAsciiMenu:) forControlEvents:UIControlEventTouchUpInside];
    _asciiBtn = asciiBtn;
    [asciiBgView addSubview:asciiBtn];
    [asciiBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(asciiBgView).offset(-15);
        make.centerY.equalTo(asciiLabel);
        make.height.equalTo(@30);
        make.width.equalTo(@60);
    }];
    
    UIView *addReturnBgView = [UIView new];
    [self.view addSubview:addReturnBgView];
    [addReturnBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view);
        make.right.equalTo(self.view);
        make.height.equalTo(@45);
        make.top.equalTo(asciiBgView.mas_bottom).offset(10);
    }];
    
    UILabel *addReturnLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(15)];
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
    
    IconButton *addReturnBtn = [self createIconBtn:title];
    [addReturnBtn addTarget:self action:@selector(showAddReturnMenu:) forControlEvents:UIControlEventTouchUpInside];
    [addReturnBgView addSubview:addReturnBtn];
    [addReturnBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(addReturnBgView).offset(-15);
        make.centerY.equalTo(addReturnLabel);
        make.height.equalTo(@30);
        make.width.equalTo(@60);
    }];
    
    UIView *modeBgView = [UIView new];
    [self.view addSubview:modeBgView];
    [modeBgView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view);
        make.right.equalTo(self.view);
        make.height.equalTo(@45);
        make.top.equalTo(addReturnBgView.mas_bottom).offset(10);
    }];
    
    UILabel *modeLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(15)];
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
    IconButton *modeBtn = [self createIconBtn:title];
    [modeBtn addTarget:self action:@selector(showModeMenu:) forControlEvents:UIControlEventTouchUpInside];
    [modeBgView addSubview:modeBtn];
    [modeBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(modeBgView).offset(-15);
        make.centerY.equalTo(modeLabel);
        make.height.equalTo(@30);
        make.width.equalTo(@90);
    }];
    
    modeBgView.hidden = !_isBleConfig;
}

// 创建iconbtn
- (IconButton *)createIconBtn:(NSString *)title
{
    IconButton *btn = [[IconButton alloc] init];
    btn.iconStyle = IconButtonStyleIconRight;
    btn.titleLabel.font = WW_Font(14);
    [btn setTitle:title forState:UIControlStateNormal];
    
    [btn setTitleColor:WW_COLOR_HexRGB(0xFFFFFF) forState:UIControlStateNormal];
    [btn setBackgroundImage:[UIImage imageWithColor:WW_COLOR_HexRGB(0x649fea)] forState:UIControlStateNormal];
    [btn setImage:[UIImage imageNamed:@"down_white"] forState:UIControlStateNormal];
    btn.imageEdgeOffsets = UIEdgeOffsetsMake(-8, 0, 0, 0);
    btn.layer.cornerRadius = 5;
    btn.layer.masksToBounds = true;
    
    return btn;
}

//是否清空输入框
- (void)showInputClearMenu:(UIButton *)sender
{
    // 不带图片
    PopoverAction *yesAction = [PopoverAction actionWithTitle:@"是" handler:^(PopoverAction *action) {
        [sender setTitle:@"是" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isClearInput = true;
    }];
    PopoverAction *falseAction = [PopoverAction actionWithTitle:@"否" handler:^(PopoverAction *action) {
        [sender setTitle:@"否" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isClearInput = false;
    }];
    
    PopoverView *popoverView = [PopoverView popoverView];
    popoverView.style = PopoverViewStyleDefault;
    popoverView.hideAfterTouchOutside = true; // 点击外部时不允许隐藏
    
    [popoverView showToView:sender withActions:@[yesAction, falseAction]];
}

//显示字符菜单
- (void)showAsciiMenu:(UIButton *)sender
{
    // 不带图片
    PopoverAction *asciiAction = [PopoverAction actionWithTitle:@"ASCII" handler:^(PopoverAction *action) {
        [sender setTitle:@"ASCII" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isBleHex = false;
    }];
    PopoverAction *hexAction = [PopoverAction actionWithTitle:@"Hex" handler:^(PopoverAction *action) {
        [sender setTitle:@"Hex" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isBleHex = true;
    }];
    
    PopoverView *popoverView = [PopoverView popoverView];
    popoverView.style = PopoverViewStyleDefault;
    popoverView.hideAfterTouchOutside = true; // 点击外部时不允许隐藏
    
    [popoverView showToView:sender withActions:@[asciiAction, hexAction]];
}

//显示模式菜单
- (void)showModeMenu:(UIButton *)sender
{
    // 不带图片
    PopoverAction *dataAction = [PopoverAction actionWithTitle:@"数据模式" handler:^(PopoverAction *action) {
        [sender setTitle:@"数据模式" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isBleConfig = false;
    }];
    PopoverAction *configAction = [PopoverAction actionWithTitle:@"配置模式" handler:^(PopoverAction *action) {
        [sender setTitle:@"配置模式" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isBleConfig = true;
        
        [self.asciiBtn setTitle:@"ASCII" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isBleHex = false;
    }];
    
    PopoverView *popoverView = [PopoverView popoverView];
    popoverView.style = PopoverViewStyleDefault;
    popoverView.hideAfterTouchOutside = true; // 点击外部时不允许隐藏
    
    [popoverView showToView:sender withActions:@[dataAction, configAction]];
}

- (void)showAddReturnMenu:(UIButton *)sender
{
    // 不带图片
    PopoverAction *yesAction = [PopoverAction actionWithTitle:@"是" handler:^(PopoverAction *action) {
        [sender setTitle:@"是" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isAddReturn = true;
    }];
    PopoverAction *falseAction = [PopoverAction actionWithTitle:@"否" handler:^(PopoverAction *action) {
        [sender setTitle:@"否" forState:UIControlStateNormal];
        [HJConfigInfo shareInstance].isAddReturn = false;
    }];
    
    PopoverView *popoverView = [PopoverView popoverView];
    popoverView.style = PopoverViewStyleDefault;
    popoverView.hideAfterTouchOutside = true; // 点击外部时不允许隐藏
    
    [popoverView showToView:sender withActions:@[yesAction, falseAction]];
}

@end
