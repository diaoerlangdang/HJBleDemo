//
//  BleViewController.m
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "BleViewController.h"
#import "BleSendReceiveTableViewCell.h"
#import <Toast/Toast.h>
#import "SetViewController.h"
#import "HJConfigInfo.h"
#import "HJBleDevicePageLifecycle.h"
#import "HJCustomCommandViewController.h"
#import "HJCustomCommandStore.h"
#import "HJCustomCommandCodec.h"
 
// header view 高度
#define HEADER_VIEW_HEIGHT 85.f

@interface BleViewController ()

@property (nonatomic,strong) CBPeripheral *peripheral;

@property (nonatomic, strong) DXMessageToolBar *chatToolBar;

@property (nonatomic, strong) NSMutableArray<BleSendReceiveData *> *tableData;

@property (nonatomic, strong) WWBluetoothLE *ble;

@property (nonatomic, assign) BOOL isShowHex;

@property (nonatomic, assign) BOOL isRespone;

@property (nonatomic, strong) UIView *headerView;

// 发送字节数
@property (nonatomic, strong) UILabel *sendByteCountLabel;

@property (nonatomic, assign) NSInteger sendByteCount;

// 接收字节数
@property (nonatomic, strong) UILabel *receiveByteCountLabel;

@property (nonatomic, assign) NSInteger receiveByteCount;

// 接收速率
@property (nonatomic, strong) UILabel *receiveRateLabel;

// 平均发送速率
@property (nonatomic, strong) UILabel *averageSendRateLabel;

// 测试
@property (nonatomic, strong) UIButton *testBtn;

// 每秒钟接收的字节数
@property (nonatomic, assign) NSInteger recCountBySecond;

// 接收速率
@property (nonatomic, strong) UILabel *flowControlLabel;

// 定时器
@property (nonatomic, strong) NSTimer *timer;

// 蓝牙是否繁忙
@property (nonatomic, assign) BOOL bBusyBle;

// 正在测试
@property (nonatomic, assign) BOOL isTesting;



@property (nonatomic, assign) BOOL isSending;
@property (nonatomic, strong) dispatch_queue_t sendQueue;
@property (atomic, strong) NSUUID *testIdentifier;
@property (atomic, assign) BOOL connectionPageActive;
@property (nonatomic, assign) BOOL didCleanupConnection;

@end

@implementation BleViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor whiteColor];
    
    _sendByteCount = 0;
    _receiveByteCount = 0;
    _recCountBySecond = 0;
    _isTesting = false;
    _connectionPageActive = YES;
    _sendQueue = dispatch_queue_create("com.hongjia.HJBleDemo.send", DISPATCH_QUEUE_SERIAL);
    
    _peripheral = _scanData.peripheral;
    if (!_scanData.isConfig) {
        [HJConfigInfo shareInstance].isBleConfig = false;
    }
    
    UIBarButtonItem *clearBtnItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemTrash target:self action:@selector(clearData)];

    UIBarButtonItem *setBtnItem = [[UIBarButtonItem alloc] initWithImage:[UIImage imageNamed:@"set"] style:UIBarButtonItemStylePlain target:self action:@selector(setBtnClicked)];
    self.navigationItem.rightBarButtonItems = @[setBtnItem, clearBtnItem];
    
    CGFloat chatToolBarHeight = [DXMessageToolBar defaultHeight];
    _chatToolBar = [[DXMessageToolBar alloc] initWithFrame:CGRectMake(0, self.view.frame.size.height - [DXMessageToolBar defaultHeight], self.view.frame.size.width, chatToolBarHeight)];
    _chatToolBar.backgroundColor = [UIColor whiteColor];
    _chatToolBar.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin;
    _chatToolBar.delegate = self;
    [self.view addSubview:_chatToolBar];
    self.isShowHex = true;
    
    [self setupHeaderView];
    
    [self.tableView mas_remakeConstraints:^(MASConstraintMaker *make) {
        if (@available(iOS 11.0, *)) {
            make.left.equalTo(self.view.mas_safeAreaLayoutGuideLeft);
            make.right.equalTo(self.view.mas_safeAreaLayoutGuideRight);
            make.top.equalTo(_headerView.mas_bottom);
            make.bottom.equalTo(self.view).offset(-5-chatToolBarHeight);
        } else {
            make.left.right.equalTo(self.view);
            make.top.equalTo(_headerView.mas_bottom);
            make.bottom.equalTo(self.view).offset(-5-chatToolBarHeight);
        }
    }];
    
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(keyBoardHidden)];
    [self.view addGestureRecognizer:tap];
    
    _tableData = [[NSMutableArray alloc] init];
    
    _ble = [WWBluetoothLE shareBLE];
    _ble.managerDelegate = self;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(peripheralDidDisconnect:)
                                                 name:WWBLEConnectionDidDisconnectNotification
                                               object:_ble];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(peripheralDidReceiveData:)
                                                 name:WWBLEDidReceiveDataNotification
                                               object:_ble];
    
    _timer = [NSTimer timerWithTimeInterval:1
                                             target:self
                                           selector:@selector(computeRate)
                                           userInfo:nil
                                            repeats:YES];
    [[NSRunLoop currentRunLoop] addTimer:_timer forMode:NSRunLoopCommonModes];
    
    // 定时器停止
    [_timer setFireDate:[NSDate distantFuture]];
    
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    if (!_didCleanupConnection) {
        _ble.managerDelegate = self;
    }
    
    if ([HJConfigInfo shareInstance].isBleConfig) {
        self.navigationItem.title = [NSString stringWithFormat:@"%@-配置",_peripheral.name];
        _headerView.height = CGFLOAT_MIN;
        _headerView.hidden = true;
        
        // 定时器停止
        [_timer setFireDate:[NSDate distantFuture]];
    }
    else {
        self.navigationItem.title = [NSString stringWithFormat:@"%@-数据",_peripheral.name];
        
        _headerView.height = HEADER_VIEW_HEIGHT;
        _headerView.hidden = false;
        
        //启动定时器
        [_timer setFireDate:[NSDate distantPast]];
    }
    
    [self.view endEditing:true];
    [_chatToolBar clearText]; //清空发送的数据
    self.isShowHex = [HJConfigInfo shareInstance].isBleHex;
    self.isRespone = [HJConfigInfo shareInstance].isRespone;
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    
    // 定时器停止
    [_timer setFireDate:[NSDate distantFuture]];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [super viewDidDisappear:animated];
    NSArray *stack = self.navigationController.viewControllers ?: @[];
    if (HJBleDevicePageDidLeaveNavigationStack(stack, self)) {
        [self cleanupConnectionIfNeeded];
    }
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

// 计算速率
- (void)computeRate
{
    NSInteger tmp = _recCountBySecond;
    _recCountBySecond = 0;
    
    [self showRecCountBySecond: tmp];
}

- (void)setupHeaderView
{
    _headerView = [UIView new];
    [self.view addSubview:_headerView];
    [_headerView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.top.equalTo(self.view);
        make.height.equalTo(@(HEADER_VIEW_HEIGHT));
    }];
    
    _sendByteCountLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(9)];
    [_headerView addSubview:_sendByteCountLabel];
    [_sendByteCountLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_headerView).offset(5);
        make.left.equalTo(_headerView).offset(10);
    }];
    
    [self setSendByteCount:0];
    
    _receiveByteCountLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(9)];
    [_headerView addSubview:_receiveByteCountLabel];
    [_receiveByteCountLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_headerView).offset(5);
        make.right.equalTo(_headerView).offset(-10);
    }];
    
    [self setReceiveByteCount:0];
    
    _receiveRateLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(9)];
    [_headerView addSubview:_receiveRateLabel];
    [_receiveRateLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_sendByteCountLabel.mas_bottom).offset(5);
        make.left.equalTo(_headerView).offset(10);
    }];
    
    [self showRecCountBySecond:0];
    
    _averageSendRateLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(9)];
    [_headerView addSubview:_averageSendRateLabel];
    [_averageSendRateLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_receiveRateLabel.mas_bottom).offset(5);
        make.left.equalTo(_headerView).offset(10);
    }];
    
    [self showAverageSendCountBySecond:0];
    
    _flowControlLabel = [UILabel labelWithTextColor:WW_COLOR_HexRGB(0x333333) font:WW_Font(9)];
    [_headerView addSubview:_flowControlLabel];
    [_flowControlLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_receiveByteCountLabel.mas_bottom).offset(5);
        make.right.equalTo(_headerView).offset(-10);
    }];
    _flowControlLabel.text = self.scanData.bFlowControl ? @"串口流控：使能" : @"串口流控：禁止";
    
    _testBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    [_testBtn setTitle:@"开始测试" forState:UIControlStateNormal];
    [_testBtn addTarget:self action:@selector(testBtnClicked) forControlEvents:UIControlEventTouchUpInside];
    [_headerView addSubview:_testBtn];
    
    [_testBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_averageSendRateLabel.mas_bottom).offset(5);
        make.right.equalTo(_headerView).offset(-10);
    }];

    UIButton *customCommandButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [customCommandButton setTitle:@"自定义指令" forState:UIControlStateNormal];
    [customCommandButton addTarget:self action:@selector(customCommandBtnClicked) forControlEvents:UIControlEventTouchUpInside];
    [_headerView addSubview:customCommandButton];
    [customCommandButton mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(_testBtn);
        make.right.equalTo(_testBtn.mas_left).offset(-12);
    }];

}

- (void)setSendByteCount:(NSInteger)count
{
    _sendByteCount = count;
    _sendByteCountLabel.text = [NSString stringWithFormat:@"发送字节：%ld Byte", (long)count];
}

- (void)addSendByteCount:(NSInteger)count
{
    [self setSendByteCount:_sendByteCount+count];
}


- (void)setReceiveByteCount:(NSInteger)count
{
    _receiveByteCount = count;
    _receiveByteCountLabel.text = [NSString stringWithFormat:@"接收字节：%ld Byte", (long)count];
}

- (void)addReceiveByteCount:(NSInteger)count
{
    [self setReceiveByteCount:_receiveByteCount+count];
}

- (void)showRecCountBySecond:(NSInteger)recCountBySecond
{
    _receiveRateLabel.text = [NSString stringWithFormat:@"实时速率：%ld B/s", (long)recCountBySecond];
}

- (void)showAverageSendCountBySecond:(NSInteger)count
{
    _averageSendRateLabel.text = [NSString stringWithFormat:@"平均发送速率：%ld B/s", (long)count];
}

// 返回
- (void)leftButtonMethod
{
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)cleanupConnectionIfNeeded
{
    if (_didCleanupConnection) {
        return;
    }
    BOOL shouldDisconnect = _connectionPageActive;
    _didCleanupConnection = YES;
    self.connectionPageActive = NO;
    _isTesting = NO;
    [_timer setFireDate:[NSDate distantFuture]];
    self.testIdentifier = nil;
    [[NSNotificationCenter defaultCenter] removeObserver:self];

    if (_ble.managerDelegate == self) {
        _ble.managerDelegate = nil;
    }
    if (shouldDisconnect) {
        [_ble disconnect:_peripheral session:_scanData.connectionSessionIdentifier error:NULL];
    }
}

// 清除数据
-(void)clearData
{
    [_tableData removeAllObjects];
    
    [self setReceiveByteCount:0];
    [self setSendByteCount:0];
    [self showAverageSendCountBySecond:0];
    
    [self.tableView reloadData];
}

// 点击设置按钮
- (void)setBtnClicked
{
    SetViewController *setVC = [[SetViewController alloc] init];
    setVC.isBleConfig = _scanData.isConfig;
    [self.navigationController pushViewController:setVC animated:true];
}

//是否显示hex输入
- (void)setIsShowHex:(BOOL)isShowHex
{
    _isShowHex = isShowHex;
    
    if (isShowHex) {
        _chatToolBar.isHex = true;
        _chatToolBar.inputTextView.keyboardType = UIKeyboardTypeASCIICapable;//UIKeyboardTypeNumberPad;
        _chatToolBar.inputTextView.placeHolder = @"请输入Hex数据";
    }
    else {
        _chatToolBar.isHex = false;
        _chatToolBar.inputTextView.keyboardType = UIKeyboardTypeDefault;
        _chatToolBar.inputTextView.placeHolder = @"请输入数据";
    }
}

#pragma mark -- UITableViewDataSource
-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _tableData.count;
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *cellIdentify = @"myCell";
    BleSendReceiveTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentify];
    if (!cell) {
        cell = [[BleSendReceiveTableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:cellIdentify];
    }
    cell.bleData = _tableData[indexPath.row];
    //cell.backgroundColor = [UIColor clearColor];
    return cell;
}


- (void)keyBoardHidden
{
    [_chatToolBar endEditing:YES];
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:NO];
}

#pragma mark - DXMessageToolBarDelegate
- (void)inputTextViewWillBeginEditing:(XHMessageTextView *)messageInputTextView{
}

- (void)didChangeFrameToHeight:(CGFloat)toHeight
{
    [UIView animateWithDuration:0.3 animations:^{
        CGRect rect = self.tableView.frame;
        rect.size.height = self.view.frame.size.height - toHeight;
        self.tableView.frame = rect;
    }];
}

- (void)toolBar:(DXMessageToolBar *)toolBar didSendText:(NSString *)text
{
    [self.view endEditing:true];

    if (!text || text.length <= 0 || _isTesting) {
        return;
    }

    if (_isShowHex) {
        if (text.length % 2 != 0) {
            [self.view makeToast:@"数据不为2的倍数"];
            return;
        }
        text = text.uppercaseString;
        if (![WWRegexUtils isValidateByRegex:@"^[A-Fa-f0-9]+$" value:text]) {
            [self.view makeToast:@"输入格式错误"];
            return;
        }
    }

    if (_bBusyBle) {
        [self.view makeToast:@"蓝牙繁忙，请稍后在试"];
        return;
    }

    HJCustomCommand *command = [[HJCustomCommand alloc] initWithName:@"手动输入"
                                                               command:text
                                                                  type:_isShowHex ? HJCustomCommandTypeHex : HJCustomCommandTypeASCII];
    NSString *error = nil;
    NSData *sendData = [HJCustomCommandCodec dataForCommand:command
                                               appendReturn:[HJConfigInfo shareInstance].isAddReturn
                                                      error:&error];
    if (!sendData) {
        [self.view makeToast:error ?: @"输入格式错误"];
        return;
    }
    [self sendPreparedData:sendData logText:text clearInput:[HJConfigInfo shareInstance].isClearInput toolbar:toolBar];
}

- (void)customCommandBtnClicked
{
    HJCustomCommandViewController *commandViewController =
        [[HJCustomCommandViewController alloc] initWithStore:[[HJCustomCommandStore alloc] init]];
    __weak typeof(self) weakSelf = self;
    commandViewController.onCommandExecute = ^(HJCustomCommand *command) {
        __strong typeof(weakSelf) self = weakSelf;
        [self executeCustomCommand:command];
    };
    [self presentViewController:commandViewController animated:YES completion:nil];
}

- (void)executeCustomCommand:(HJCustomCommand *)command
{
    if (_isTesting) {
        [self.view makeToast:@"正在测试中，无法执行指令"];
        return;
    }
    if (_bBusyBle) {
        [self.view makeToast:@"蓝牙繁忙，请稍后再试"];
        return;
    }

    NSString *error = nil;
    NSData *sendData = [HJCustomCommandCodec dataForCommand:command
                                               appendReturn:[HJConfigInfo shareInstance].isAddReturn
                                                      error:&error];
    if (!sendData) {
        [self.view makeToast:error ?: @"指令格式错误"];
        return;
    }
    [self sendPreparedData:sendData logText:command.command clearInput:NO toolbar:nil];
}

- (void)sendPreparedData:(NSData *)sendData
                 logText:(NSString *)logText
             clearInput:(BOOL)clearInput
                 toolbar:(DXMessageToolBar *)toolbar
{
    if (!sendData || !_connectionPageActive ||
        ![_ble isSessionActive:_scanData.connectionSessionIdentifier peripheral:_peripheral]) {
        [self.view makeToast:@"蓝牙未连接"];
        return;
    }
    if (_isSending || _isTesting) {
        [self.view makeToast:@"正在发送，请稍后再试"];
        return;
    }

    BOOL isConfig = [HJConfigInfo shareInstance].isBleConfig;
    WWCharacteristic *characteristic = isConfig
        ? [HJConfigInfo shareInstance].configSendService
        : [HJConfigInfo shareInstance].dataSendService;
    CBCharacteristicWriteType type = _isRespone ? CBCharacteristicWriteWithResponse : CBCharacteristicWriteWithoutResponse;
    NSUUID *session = _scanData.connectionSessionIdentifier;
    NSUInteger packetSize = MAX(1, _scanData.sendDataLenMax);
    _isSending = YES;

    __weak typeof(self) weakSelf = self;
    dispatch_async(self.sendQueue, ^{
        __strong typeof(weakSelf) self = weakSelf;
        if (!self) return;

        NSDate *startTime = [NSDate date];
        NSError *error = nil;
        BOOL result = NO;
        if (self.connectionPageActive) {
            result = [self.ble synchronizedSend:self.peripheral
                                characteristic:characteristic
                                         value:sendData
                                          type:type
                                       session:session
                                    packetSize:packetSize
                                          time:2000
                                         error:&error];
        }
        NSTimeInterval interval = [[NSDate date] timeIntervalSinceDate:startTime];

        dispatch_async(dispatch_get_main_queue(), ^{
            self.isSending = NO;
            if (!self.connectionPageActive || ![self.ble isSessionActive:session peripheral:self.peripheral]) {
                return;
            }
            if (!result) {
                [self showSendError:error];
                return;
            }
            if (!isConfig) {
                [self addSendByteCount:sendData.length];
            }

            NSMutableDictionary *dict = [NSMutableDictionary dictionary];
            NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
            formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
            formatter.dateFormat = @"HH:mm:ss SSS";
            dict[@"time"] = [NSString stringWithFormat:@"时间:%@", [formatter stringFromDate:[NSDate date]]];
            dict[@"title"] = @"发送:";
            dict[@"context"] = logText ?: @"";
            [self.tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];

            int rate = interval > 0 ? (int)(sendData.length / interval) : 0;
            [self showAverageSendCountBySecond:rate];
            [self.tableView reloadData];
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:self.tableData.count - 1 inSection:0];
            [self.tableView scrollToRowAtIndexPath:indexPath atScrollPosition:UITableViewScrollPositionTop animated:YES];
            if (clearInput && toolbar) {
                [toolbar clearText];
            }
        });
    });
}

- (void)showSendError:(NSError *)error
{
    NSString *message = @"发送失败";
    if ([error.domain isEqualToString:WWBLEErrorDomain]) {
        switch (error.code) {
            case WWBLEErrorCancelled: return;
            case WWBLEErrorBusy: message = @"蓝牙繁忙，请稍后再试"; break;
            case WWBLEErrorTimedOut: message = @"发送超时，请重新连接后再试"; break;
            case WWBLEErrorUnsupportedOperation: message = @"当前特征不支持此写入方式，请检查设置"; break;
            case WWBLEErrorCharacteristicNotFound: message = @"未找到发送特征，请检查设置"; break;
            default: break;
        }
    }
    [self.view makeToast:message];
}

// 测试
- (void)testBtnClicked
{
    if (_isTesting) {
        self.isTesting = NO;
        self.testIdentifier = nil;
        [_ble cancelWaitingForPeripheral:_peripheral session:_scanData.connectionSessionIdentifier error:NULL];
        [_testBtn setTitle:@"开始测试" forState:UIControlStateNormal];
    } else {
        if (!_connectionPageActive || ![_ble isSessionActive:_scanData.connectionSessionIdentifier peripheral:_peripheral]) {
            [self.view makeToast:@"蓝牙未连接"];
            return;
        }
        if (_isSending || _bBusyBle) {
            [self.view makeToast:@"蓝牙繁忙，请稍后再试"];
            return;
        }
        self.isTesting = YES;
        self.testIdentifier = [NSUUID UUID];
        [_testBtn setTitle:@"停止测试" forState:UIControlStateNormal];
        [self startTest];
    }
}

- (void)startTest
{
    [self showAverageSendCountBySecond:0];
    NSUUID *testIdentifier = self.testIdentifier;
    NSUUID *session = _scanData.connectionSessionIdentifier;
    NSUInteger packetSize = MAX(1, _scanData.sendDataLenMax);
    NSInteger totalLen = MAX(0, [HJConfigInfo shareInstance].dataTotalLen);
    int gap = MAX(0, [HJConfigInfo shareInstance].sendDataGap);
    WWCharacteristic *characteristic = [HJConfigInfo shareInstance].dataSendService;
    CBCharacteristicWriteType type = _isRespone ? CBCharacteristicWriteWithResponse : CBCharacteristicWriteWithoutResponse;
    __weak typeof(self) weakSelf = self;
    dispatch_async(self.sendQueue, ^{
        __strong typeof(weakSelf) self = weakSelf;
        if (!self) return;
        NSDate *startTime = [NSDate date];
        NSUInteger sentLength = 0;
        NSError *error = nil;
        while (sentLength < totalLen && self.connectionPageActive &&
               [self.testIdentifier isEqual:testIdentifier]) {
            NSUInteger length = MIN(packetSize, totalLen - sentLength);
            NSData *data = [NSMutableData dataWithLength:length];
            if (![self.ble synchronizedSend:self.peripheral
                            characteristic:characteristic
                                     value:data
                                      type:type
                                   session:session
                                packetSize:packetSize
                                      time:2000
                                     error:&error]) {
                break;
            }
            sentLength += length;
            dispatch_async(dispatch_get_main_queue(), ^{
                if (self.connectionPageActive) {
                    [self addSendByteCount:length];
                }
            });
            // 分段等待，停止测试或退出页面后不再等待整个发送间隔。
            for (int remaining = gap; remaining > 0 && self.connectionPageActive &&
                 [self.testIdentifier isEqual:testIdentifier]; remaining -= MIN(remaining, 50)) {
                usleep(1000 * MIN(remaining, 50));
            }
        }
        NSTimeInterval interval = [[NSDate date] timeIntervalSinceDate:startTime];
        dispatch_async(dispatch_get_main_queue(), ^{
            if (!self.connectionPageActive || ![self.testIdentifier isEqual:testIdentifier]) {
                return;
            }
            if (error != nil) {
                [self showSendError:error];
            }
            [self showAverageSendCountBySecond:interval > 0 ? (int)(sentLength / interval) : 0];
            self.isTesting = NO;
            self.testIdentifier = nil;
            [self.testBtn setTitle:@"开始测试" forState:UIControlStateNormal];
        });
    });
}

#pragma mark -- WWBluetoothLEManagerDelegate

/**
 *  蓝牙状态，仅在本地蓝牙状态为开启时, 即WWBleLocalStatePowerOn，其他函数方可使用
 *
 *  @param ble     蓝牙
 *  @param state   当前蓝牙的状态
 *
 */
- (void)ble:(WWBluetoothLE *)ble didLocalState:(WWBleLocalState)state
{
    if (state != WWBleLocalStatePowerOn) {
        NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
        NSDate* now = [NSDate date];
        NSDateFormatter* fmt = [[NSDateFormatter alloc] init];
        fmt.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
        fmt.dateFormat = @"HH:mm:ss";
        NSString* dateString = [NSString stringWithFormat:@"时间:%@",[fmt stringFromDate:now]];
        [dict setValue:dateString forKey:@"time"];
        [dict setValue:@"其他:" forKey:@"title"];
        [dict setValue:@"蓝牙已关闭" forKey:@"context"];
        
        [_tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];
        [self.tableView reloadData];
    }
}

#pragma mark -- WWBluetoothLEDelegate

- (void)peripheralDidDisconnect:(NSNotification *)notification
{
    CBPeripheral *peripheral = notification.userInfo[WWBLEPeripheralKey];
    NSUUID *token = notification.userInfo[WWBLESessionKey];
    if (!_connectionPageActive ||
        !HJBleConnectionEventMatches(_peripheral, _scanData.connectionSessionIdentifier, peripheral, token)) {
        return;
    }
    self.connectionPageActive = NO;
    self.isTesting = NO;
    [_testBtn setTitle:@"开始测试" forState:UIControlStateNormal];
    [_timer setFireDate:[NSDate distantFuture]];
    self.testIdentifier = nil;
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    NSDate* now = [NSDate date];
    NSDateFormatter* fmt = [[NSDateFormatter alloc] init];
    fmt.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
    fmt.dateFormat = @"HH:mm:ss";
    NSString* dateString = [NSString stringWithFormat:@"时间:%@",[fmt stringFromDate:now]];
    [dict setValue:dateString forKey:@"time"];
    [dict setValue:@"其他:" forKey:@"title"];
    [dict setValue:@"蓝牙已断开" forKey:@"context"];
    
    [_tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];
    [self.tableView reloadData];
}

- (void)peripheralDidReceiveData:(NSNotification *)notification
{
    CBPeripheral *peripheral = notification.userInfo[WWBLEPeripheralKey];
    NSUUID *session = notification.userInfo[WWBLESessionKey];
    if (!_connectionPageActive ||
        !HJBleConnectionEventMatches(_peripheral, _scanData.connectionSessionIdentifier, peripheral, session)) {
        return;
    }
    WWCharacteristic *characteristic = notification.userInfo[WWBLECharacteristicKey];
    NSData *data = notification.userInfo[WWBLEDataKey];
    // 配置服务
    if ([characteristic isEqual:[HJConfigInfo shareInstance].configReceiveService]) {
        NSString *recvStr = [NSString utf8ToUnicode:data];
        if ([recvStr isEqualToString:@"<HJ_BLE_BUSY_STOP_SEND>"]) {
            _bBusyBle = true;
            NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
            NSDate* now = [NSDate date];
            NSDateFormatter* fmt = [[NSDateFormatter alloc] init];
            fmt.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
            fmt.dateFormat = @"HH:mm:ss";
            NSString* dateString = [NSString stringWithFormat:@"时间:%@",[fmt stringFromDate:now]];
            [dict setValue:dateString forKey:@"time"];
            [dict setValue:@"其他:" forKey:@"title"];
            [dict setValue:@"蓝牙设备繁忙" forKey:@"context"];
            
            [_tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];
            [self.tableView reloadData];
        }
        else if ([recvStr isEqualToString:@"<HJ_BLE_IDLE_START_SEND>"]) {
            _bBusyBle = false;
            NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
            NSDate* now = [NSDate date];
            NSDateFormatter* fmt = [[NSDateFormatter alloc] init];
            fmt.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
            fmt.dateFormat = @"HH:mm:ss";
            NSString* dateString = [NSString stringWithFormat:@"时间:%@",[fmt stringFromDate:now]];
            [dict setValue:dateString forKey:@"time"];
            [dict setValue:@"其他:" forKey:@"title"];
            [dict setValue:@"蓝牙设备空闲" forKey:@"context"];
            
            [_tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];
            [self.tableView reloadData];
        }
    }
    
    
    WWCharacteristic *currentChara;
    if ([HJConfigInfo shareInstance].isBleConfig) {
        currentChara = [HJConfigInfo shareInstance].configReceiveService;
    }
    else {
        currentChara = [HJConfigInfo shareInstance].dataReceiveService;
    }
    
    // 不接收不是当前模式的回调
    if (![characteristic isEqual:currentChara]) {
        return;
    }
    
    // 数据模式下统计
    if (![HJConfigInfo shareInstance].isBleConfig) {
        [self addReceiveByteCount:data.length];
        _recCountBySecond += data.length;
    }
    
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    NSDate* now = [NSDate date];
    NSDateFormatter* fmt = [[NSDateFormatter alloc] init];
    fmt.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"zh_CN"];
    fmt.dateFormat = @"HH:mm:ss";
    NSString* dateString = [NSString stringWithFormat:@"时间:%@",[fmt stringFromDate:now]];
    [dict setValue:dateString forKey:@"time"];
    [dict setValue:@"接收:" forKey:@"title"];
    if (_isShowHex) {
        [dict setValue:[NSString hexToString:data space:NO].uppercaseString forKey:@"context"];
    }
    else {
        [dict setValue:[NSString utf8ToUnicode:data] forKey:@"context"];
    }
    
//    [dict setValue:[NSString utf8ToUnicode:data] forKey:@"context"];
    
    [_tableData addObject:[BleSendReceiveData BleSendReceiveDataWithDictionary:dict]];
    [self.tableView reloadData];
    
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(delayScrollToBottom) object:nil];
    
    [self performSelector:@selector(delayScrollToBottom) withObject:nil afterDelay:0.2];
}

// 延迟滑动到底部，避免卡顿
- (void)delayScrollToBottom
{
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:_tableData.count-1 inSection:0];
    [self.tableView scrollToRowAtIndexPath:indexPath atScrollPosition:UITableViewScrollPositionTop animated:YES];
}


@end
