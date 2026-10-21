//
//  ScanViewController.m
//  bleDemo
//
//  Created by wurz on 15/4/12.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "ScanViewController.h"
#import "HJBleScanData.h"
#import "BleViewController.h"
#import "HJConfigInfo.h"
#import <Toast/Toast.h>
#import <SVProgressHUD/SVProgressHUD.h>
#import "HJScanTableViewCell.h"
#import "WWAlertView.h"
#import "AppConfigSetViewController.h"


@interface ScanViewController ()

@property(nonatomic,strong) UIActivityIndicatorView *scanIndicatorView; //扫描的菊花

@property(nonatomic,strong) NSMutableArray<HJBleScanData *> *hjBleArray;
@property(nonatomic,strong) WWBluetoothLE *ble;

@property(nonatomic,strong) HJBleScanData *selectedScanData;
@property(nonatomic, strong) NSDate *lastReloadTime;
@property(atomic, strong) NSUUID *connectionSessionToken;
@property(nonatomic, strong) dispatch_group_t connectionWork;

@end

@implementation ScanViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.navigationItem.title = @"扫描列表";
    
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithImage:[UIImage imageNamed:@"info"] style:UIBarButtonItemStylePlain target:self action:@selector(onClickConfigInfo)];
    
    self.view.backgroundColor = [UIColor colorWithRed:239.0/255 green:236.0/255 blue:237.0/255 alpha:1];
    
    _scanIndicatorView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh target:self action:@selector(refresh)];
    
    _hjBleArray = [[NSMutableArray alloc] init];
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    [self setConnectionInProgress:NO];
    _ble = [WWBluetoothLE shareBLE];
    _ble.managerDelegate = self;
    _hjBleArray = [NSMutableArray array];
    [self.tableView reloadData];
    
    if (_ble.loaclState == WWBleLocalStatePowerOn) {
        [self refresh];
    }
    else{
        [_scanIndicatorView stopAnimating];
    }
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    [_ble stopScan];
    if (_connectionSessionToken != nil) {
        [self cancelConnectionWork];
        [SVProgressHUD dismiss];
    }
}

// 配置信息
- (void)onClickConfigInfo
{
//    NSString *appCurVersionNum = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
//    WWAlertView *alertView = [[WWAlertView alloc] initWithTipsTitle:@"版本信息" detail:[NSString stringWithFormat:@"V%@", appCurVersionNum]];
//    alertView.attachedView = self.navigationController.view;
//    [alertView showWithBlock:nil];
    
    AppConfigSetViewController *vc = [[AppConfigSetViewController alloc] init];
    [self.navigationController pushViewController:vc animated:true];
    
    return;
}

#pragma mark -- 刷新列表 
-(void)refresh
{
    _hjBleArray = [NSMutableArray array];
    [self.tableView reloadData];
    switch (_ble.loaclState) {
        case WWBleLocalStatePowerOff:
            [self.view makeToast:@"本地蓝牙已关闭"];
            [_scanIndicatorView stopAnimating];
            break;
        case WWBleLocalStatePowerOn:
            [_ble startScan:false];
            [_scanIndicatorView startAnimating];
            break;
            
        default:
            [self.view makeToast:@"本地设备不支持蓝牙4.0"];
            [_scanIndicatorView stopAnimating];
            break;
    }
}

#pragma mark -- 停止扫描
-(void)stopScan
{
    [_ble stopScan];
    [_scanIndicatorView stopAnimating];
}

#pragma mark -- UITableViewDelegete
-(UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section
{
    UIView *view = [[UIView alloc] init];
    view.frame = CGRectMake(0, 0, tableView.frame.size.width, 30);
    //view.backgroundColor = [UIColor clearColor];
    
    UILabel *label = [[UILabel alloc] init];
    label.frame = CGRectMake(10, 0, 70, 30);
    label.text = @"蓝牙列表";
    
    _scanIndicatorView.frame = CGRectMake(label.frame.size.width+10, 0, 40, 30);
    
    [view addSubview:label];
    [view addSubview:_scanIndicatorView];
    //[_scanIndicatorView startAnimating];
    
    return view;
}

-(CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    return 30;
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    if (_connectionSessionToken != nil) {
        return;
    }

    [self stopScan];
    HJBleScanData *scanData = _hjBleArray[indexPath.row];
    _selectedScanData = scanData;
    [self setConnectionInProgress:YES];
    [SVProgressHUD showWithStatus:@"正在连接中..."];

    self.connectionSessionToken = [NSUUID UUID];
    [self initializeScanData:scanData session:self.connectionSessionToken];
}

- (void)initializeScanData:(HJBleScanData *)scanData session:(NSUUID *)token
{
    WWCharacteristic *configCharacteristic = [HJConfigInfo shareInstance].configReceiveService;
    WWCharacteristic *dataCharacteristic = [HJConfigInfo shareInstance].dataReceiveService;
    self.connectionWork = dispatch_group_create();
    __weak typeof(self) weakSelf = self;
    dispatch_group_async(self.connectionWork, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        __strong typeof(weakSelf) self = weakSelf;
        // 取消可能发生在后台任务真正启动之前，此时不能再发起连接。
        if (self == nil || ![self.connectionSessionToken isEqual:token]) {
            return;
        }

        NSError *error = nil;
        NSString *failureMessage = @"连接失败";
        BOOL initialized = [self.ble synchronizedConnect:scanData.peripheral
                                                session:token
                                                   time:5000
                                            maxAttempts:5
                                            cleanupTime:2000
                                                  error:&error];
        if (initialized && [self.connectionSessionToken isEqual:token]) {
            failureMessage = @"打开通知失败";
            scanData.bFlowControl = NO;
            CBCharacteristicProperties properties = [self.ble getCharacteristicProperties:scanData.peripheral
                                                                            characteristic:configCharacteristic];
            scanData.isConfig = (properties & CBCharacteristicPropertyNotify) &&
                ((properties & CBCharacteristicPropertyWrite) ||
                 (properties & CBCharacteristicPropertyWriteWithoutResponse));

            if (scanData.isConfig) {
                initialized = [self.ble synchronizedSetNotify:scanData.peripheral
                                              characteristic:configCharacteristic
                                                      enable:YES
                                                     session:token
                                                        time:5000
                                                       error:&error];
                if (initialized && [self.connectionSessionToken isEqual:token]) {
                    CBCharacteristicWriteType type = (properties & CBCharacteristicPropertyWrite)
                        ? CBCharacteristicWriteWithResponse : CBCharacteristicWriteWithoutResponse;
                    NSData *recvData = [self.ble sendReceive:scanData.peripheral
                                            characteristic:configCharacteristic
                                                     value:[NSData unicodeToUtf8:@"<RD_UART_FC>"]
                                                      type:type
                                    responseCharacteristic:configCharacteristic
                                                   session:token
                                                packetSize:20
                                                      time:5000
                                                     error:&error];
                    // 请求失败后停止初始化，避免继续使用仍隔离着迟到响应的连接。
                    initialized = recvData != nil;
                    if (recvData != nil) {
                        scanData.bFlowControl = [[NSString utf8ToUnicode:recvData] isEqualToString:@"<rd_uart_fc=1>"];
                    }
                    failureMessage = @"查询流控失败";
                }
            }

            if (initialized && [self.connectionSessionToken isEqual:token]) {
                scanData.sendDataLenMax = MAX(1, [scanData.peripheral maximumWriteValueLengthForType:CBCharacteristicWriteWithoutResponse]);
                failureMessage = @"打开通知失败";
                initialized = [self.ble synchronizedSetNotify:scanData.peripheral
                                              characteristic:dataCharacteristic
                                                      enable:YES
                                                     session:token
                                                        time:5000
                                                       error:&error];
            }
        }

        // 同时覆盖任务启动与取消交错的情况；旧任务只清理自己拥有的会话。
        if (!initialized || ![self.connectionSessionToken isEqual:token]) {
            [self.ble disconnect:scanData.peripheral session:token error:NULL];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            [self finishInitializationForScanData:scanData session:token success:initialized
                                           error:error failureMessage:failureMessage];
        });
    });
}

- (void)setConnectionInProgress:(BOOL)inProgress
{
    if (inProgress) {
        self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"取消"
                                                                                  style:UIBarButtonItemStylePlain
                                                                                 target:self
                                                                                 action:@selector(cancelConnection)];
    }
    else {
        self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh
                                                                                               target:self
                                                                                               action:@selector(refresh)];
    }
}

- (void)cancelConnectionWork
{
    NSUUID *token = self.connectionSessionToken;
    if (token == nil) {
        return;
    }
    CBPeripheral *peripheral = self.selectedScanData.peripheral;
    WWBluetoothLE *ble = self.ble;
    dispatch_group_t work = self.connectionWork;
    BOOL registered = [ble isSessionActive:token peripheral:peripheral];
    self.connectionSessionToken = nil;
    self.connectionWork = nil;
    [ble disconnect:peripheral session:token error:NULL];

    // 库只能取消已登记的连接。若后台任务正要登记，补发取消直至该任务结束。
    if (!registered && work != nil && dispatch_group_wait(work, DISPATCH_TIME_NOW) != 0) {
        dispatch_source_t cancellation = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, dispatch_get_main_queue());
        dispatch_source_set_timer(cancellation, DISPATCH_TIME_NOW, 50 * NSEC_PER_MSEC, 5 * NSEC_PER_MSEC);
        dispatch_source_set_event_handler(cancellation, ^{
            [ble disconnect:peripheral session:token error:NULL];
        });
        dispatch_group_notify(work, dispatch_get_main_queue(), ^{
            dispatch_source_cancel(cancellation);
        });
        dispatch_resume(cancellation);
    }
}

- (void)cancelConnection
{
    NSUUID *token = self.connectionSessionToken;
    if (token == nil) {
        return;
    }
    [self cancelConnectionWork];
    [self setConnectionInProgress:NO];
    [SVProgressHUD dismiss];
    if (self.navigationController.topViewController == self &&
        self.ble.loaclState == WWBleLocalStatePowerOn) {
        [self refresh];
    }
}

- (void)finishInitializationForScanData:(HJBleScanData *)scanData
                                session:(NSUUID *)token
                                success:(BOOL)success
                                  error:(NSError *)error
                         failureMessage:(NSString *)failureMessage
{
    if (![self.connectionSessionToken isEqual:token] || self.selectedScanData != scanData) {
        return;
    }

    BOOL active = [self.ble isSessionActive:token peripheral:scanData.peripheral] &&
        scanData.peripheral.state == CBPeripheralStateConnected;
    self.connectionWork = nil;
    [SVProgressHUD dismiss];
    if (!success || !active) {
        [self.ble disconnect:scanData.peripheral session:token error:NULL];
        self.connectionSessionToken = nil;
        [self setConnectionInProgress:NO];
        if (self.navigationController.topViewController == self) {
            if ([error.domain isEqualToString:WWBLEErrorDomain]) {
                switch (error.code) {
                    case WWBLEErrorCancelled: return;
                    case WWBLEErrorTimedOut: failureMessage = [failureMessage stringByAppendingString:@"：操作超时，请重试"]; break;
                    case WWBLEErrorBusy: failureMessage = @"设备忙，请稍后重试"; break;
                    case WWBLEErrorCleanupPending: failureMessage = @"正在清理连接，请稍后重试"; break;
                    case WWBLEErrorUnauthorized: failureMessage = @"请在系统设置中允许蓝牙权限"; break;
                    case WWBLEErrorBluetoothUnavailable: failureMessage = @"蓝牙不可用，请检查系统蓝牙"; break;
                    case WWBLEErrorDisconnected: failureMessage = @"蓝牙已断开"; break;
                    case WWBLEErrorCharacteristicNotFound: failureMessage = @"未找到配置的服务或特征，请检查设置"; break;
                    default: break;
                }
            }
            [self.view makeToast:failureMessage];
        }
        return;
    }

    scanData.connectionSessionIdentifier = token;
    self.connectionSessionToken = nil;
    [self setConnectionInProgress:NO];

    BleViewController *bleView = [[BleViewController alloc] init];
    bleView.scanData = scanData;
    [self.navigationController pushViewController:bleView animated:YES];
}

#pragma mark -- UITableViewDataSource
-(NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _hjBleArray.count;
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *cellIdentify = @"Cell";
    
    HJScanTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentify];
    if (!cell) {
        cell = [[HJScanTableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:cellIdentify];
    }
    
    HJBleScanData *scanData = _hjBleArray[indexPath.row];
    cell.data = scanData;
    return cell;
}

//翻转
- (NSData *)reversalData:(NSData *)data
{
    Byte *b = malloc(data.length);
    Byte *p = (Byte *)(data.bytes);
    for (int i=0; i<data.length; i++) {
        b[i] = p[data.length-1-i];
    }
    
    NSData *temp = [NSData dataWithBytes:b length:data.length];
    free(b);
    b = NULL;
    
    return temp;
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
    if (state == WWBleLocalStatePowerOn) {
        [self refresh]; //刷新扫描列表
    }
    else{
        [self stopScan];
    }
}

/**
 *  扫描函数回调
 *
 *  @param ble                  蓝牙
 *  @param peripheral           扫描到的蓝牙设备
 *  @param advertisementData    广播数据
 *  @param rssi                 rssi值
 *
 */
- (void)ble:(WWBluetoothLE *)ble didScan:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData rssi:(NSNumber *)rssi
{
    
    //没有名字的不显示
    if (peripheral.name == nil || [peripheral.name isEqualToString:@""]){
        return ;
    }
    
    
    
    BOOL isEasy = false;
    BOOL isConfig = false;
    NSInteger sendDataLenMax = 20;
    NSString *mac = @"";
    
    if ([HJConfigInfo shareInstance].isScanFilter) {
        NSArray<CBUUID *> *arr = [advertisementData valueForKey:@"kCBAdvDataServiceUUIDs"];
        
        // 老设备，不支持简易模式 和 配置模式
        if (arr.count == 1) {
            CBUUID *serviceUUID1 = arr[0];
            if (![[NSString hexToString:serviceUUID1.data space:false] isEqualToString:@"6958"]) {
                return;
            }
            
            isEasy = false;
            isConfig = false;
//            sendDataLenMax = 20;
        }
        else if (arr.count == 4) {
            
            isConfig = true;
//            sendDataLenMax = 160;
            
            CBUUID *serviceUUID1 = arr[0];
            // 不支持简易模式
            if ([[NSString hexToString:serviceUUID1.data space:false] isEqualToString:@"6958"]) {
                isEasy = false;
                
                mac = [NSString stringWithFormat:@"%@%@%@", [NSString hexToString:[self reversalData:arr[1].data] space:true], [NSString hexToString:[self reversalData:arr[2].data] space:true], [NSString hexToString:[self reversalData:arr[3].data] space:true]];
                mac = [mac stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
                mac = [mac stringByReplacingOccurrencesOfString:@" " withString:@":"].uppercaseString;
            }
            // 支持简易模式
            else if ([[NSString hexToString:serviceUUID1.data space:false] isEqualToString:@"6959"]) {
                isEasy = true;
                
                mac = [NSString stringWithFormat:@"%@%@%@", [NSString hexToString:[self reversalData:arr[1].data] space:true], [NSString hexToString:[self reversalData:arr[2].data] space:true], [NSString hexToString:[self reversalData:arr[3].data] space:true]];
                mac = [mac stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
                mac = [mac stringByReplacingOccurrencesOfString:@" " withString:@":"].uppercaseString;
            }
            else {
                return;
            }
            
        }
        else {
            return;
        }
        
        NSLog(@"广播数据%@", arr);
    }
    
    BOOL isAdd = YES;
    for (int i=0; i<_hjBleArray.count; i++) {
        HJBleScanData *temp = _hjBleArray[i];
        
        //uuid相同，则更新
        if ([temp.peripheral.identifier.UUIDString isEqualToString:peripheral.identifier.UUIDString] ) {
            
            isAdd = NO;
//            temp.peripheral = peripheral;
            temp.advertisementData = advertisementData;
            if (rssi.integerValue < 0) {
                temp.RSSI = rssi;
            }
            temp.datetime = [NSDate new];
        }
    }
    
    
    //添加数据
    if (isAdd) {
        
        HJBleScanData *scanData = [[HJBleScanData alloc] init];
        
        scanData.peripheral = peripheral;
        scanData.advertisementData = advertisementData;
        scanData.RSSI = rssi;
        scanData.datetime = [NSDate new];
        scanData.isEasy = isEasy;
        scanData.isConfig = isConfig;
        scanData.sendDataLenMax = sendDataLenMax;
        scanData.mac = mac;
        
        [_hjBleArray addObject:scanData];
    }
    
    [self throttleReloadData];
}

- (void)throttleReloadData
{
    NSTimeInterval interval = 0.3;
    NSDate *now = [NSDate date];
    if (_lastReloadTime == nil || [now timeIntervalSinceDate:_lastReloadTime] >= interval) {
        _lastReloadTime = now;
        [self.tableView reloadData];
    }
}

@end
