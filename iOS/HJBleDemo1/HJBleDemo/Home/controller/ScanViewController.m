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
    _ble = [WWBluetoothLE shareBLE];
    _ble.managerDelegate = self;
    _ble.bleDelegate = self;
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
    
    [self stopScan];
    
    _selectedScanData = _hjBleArray[indexPath.row];

    @weakify(self)
    [SVProgressHUD showWithStatus:@"正在连接中..."];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        
        @strongify(self)
        BOOL bResult = false;
        // 连接重复5次
        for (int i=0; i<5; i++) {
            bResult = [self.ble synchronizedConnect:self.selectedScanData.peripheral time:5000];
            if (bResult) {
                break;
            }
            
            [self.ble disconnect:self.selectedScanData.peripheral];
            sleep(0.5);
        }
        
        if (bResult) {
            
            // 是否为流控模式
            self.selectedScanData.bFlowControl = false;
            
            // 打开配置通知
            if (self.selectedScanData.isConfig) {
                
                bResult = [self.ble synchronizedOpenNofity:self.selectedScanData.peripheral characteristic:[HJConfigInfo shareInstance].configReceiveService time:5000];
                
                // 打开失败
                if (!bResult) {
                    
                    dispatch_async(dispatch_get_main_queue(), ^{
                        [SVProgressHUD dismiss];
                        [self.ble disconnect:self.selectedScanData.peripheral];
                        [self.view makeToast:@"连接失败"];
                    });
                    return ;
                }
                
                // 读取流控信息
                NSData *sendData = [NSData unicodeToUtf8:@"<RD_UART_FC>"];
                self.ble.commonResponeNotifyCharacteristic = [HJConfigInfo shareInstance].configReceiveService;
                NSData *recvData = [self.ble sendReceive:self.selectedScanData.peripheral characteristic:[HJConfigInfo shareInstance].configReceiveService value:sendData time:5000];
                if (recvData != nil) {
                    NSString *recvStr = [NSString utf8ToUnicode:recvData];
                    self.selectedScanData.bFlowControl = [recvStr isEqualToString:@"<rd_uart_fc=1>"];
                }
                
            }
            
            bResult = [self.ble synchronizedOpenNofity:self.selectedScanData.peripheral characteristic:[HJConfigInfo shareInstance].dataReceiveService time:5000];
        }
        else {
            
            dispatch_async(dispatch_get_main_queue(), ^{
                [SVProgressHUD dismiss];
                [self.ble disconnect:self.selectedScanData.peripheral];
                [self.view makeToast:@"连接超时"];
            });
            
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            [SVProgressHUD dismiss];
            if (bResult) {
                self.ble.nGroupSendDataLen = self.selectedScanData.sendDataLenMax;
                BleViewController *bleView = [[BleViewController alloc] init];
                bleView.scanData = self.selectedScanData;
                [self.navigationController pushViewController:bleView animated:YES];
            }
            else {
                [self.ble disconnect:self.selectedScanData.peripheral];
                [self.view makeToast:@"连接超时"];
            }
        });
    });    
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
            sendDataLenMax = 20;
        }
        else if (arr.count == 4) {
            
            isConfig = true;
            sendDataLenMax = 160;
            
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
    
    [self.tableView reloadData];
}

#pragma mark -- WWBluetoothLEDelegate

- (void)ble:(WWBluetoothLE *)ble didDisconnect:(CBPeripheral *)peripheral
{
    
}

@end
