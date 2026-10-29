#import <XCTest/XCTest.h>
#import <wiseBle/WWBluetoothLE.h>
#import "HJBleDevicePageLifecycle.h"
#import "HJBleFlowControlCheck.h"

@interface WWBluetoothLE (HJBleTesting)
- (instancetype)initWithCentralManager:(CBCentralManager *)centralManager queue:(dispatch_queue_t)queue;
- (void)centralManager:(CBCentralManager *)central
    didFailToConnectPeripheral:(CBPeripheral *)peripheral
                         error:(NSError *)error;
- (void)centralManager:(CBCentralManager *)central
    didDisconnectPeripheral:(CBPeripheral *)peripheral
                       error:(NSError *)error;
@end

@interface HJSDKFakeCharacteristic : NSObject
@property (nonatomic, strong) CBUUID *UUID;
@property (nonatomic, weak) id service;
@property (nonatomic, assign) CBCharacteristicProperties properties;
@property (nonatomic, strong) NSData *value;
@property (nonatomic, assign, getter=isNotifying) BOOL notifying;
@end
@implementation HJSDKFakeCharacteristic
@end

@interface HJSDKFakeService : NSObject
@property (nonatomic, strong) CBUUID *UUID;
@property (nonatomic, copy) NSArray *characteristics;
@end
@implementation HJSDKFakeService
@end

@interface HJSDKFakePeripheral : NSObject
@property (nonatomic, strong) NSUUID *identifier;
@property (nonatomic, assign) CBPeripheralState state;
@property (nonatomic, weak) id<CBPeripheralDelegate> delegate;
@property (nonatomic, strong) dispatch_queue_t queue;
@property (nonatomic, copy) NSArray *services;
@property (nonatomic, strong) NSMutableArray<NSData *> *packets;
@property (nonatomic, copy) void (^writeHandler)(HJSDKFakeCharacteristic *characteristic);
@property (nonatomic, strong) NSError *writeError;
@property (nonatomic, assign) BOOL suppressWriteCompletion;
@end

@implementation HJSDKFakePeripheral
- (void)discoverServices:(NSArray *)services
{
    dispatch_async(self.queue, ^{ [self.delegate peripheral:(id)self didDiscoverServices:nil]; });
}
- (void)discoverCharacteristics:(NSArray *)characteristics forService:(id)service
{
    dispatch_async(self.queue, ^{
        [self.delegate peripheral:(id)self didDiscoverCharacteristicsForService:service error:nil];
    });
}
- (void)setNotifyValue:(BOOL)enable forCharacteristic:(HJSDKFakeCharacteristic *)characteristic
{
    characteristic.notifying = enable;
    dispatch_async(self.queue, ^{
        [self.delegate peripheral:(id)self didUpdateNotificationStateForCharacteristic:(id)characteristic error:nil];
    });
}
- (NSUInteger)maximumWriteValueLengthForType:(CBCharacteristicWriteType)type { return 64; }
- (BOOL)canSendWriteWithoutResponse { return YES; }
- (void)writeValue:(NSData *)data forCharacteristic:(HJSDKFakeCharacteristic *)characteristic type:(CBCharacteristicWriteType)type
{
    [self.packets addObject:data];
    if (self.writeHandler) { self.writeHandler(characteristic); }
    if (type == CBCharacteristicWriteWithResponse && !self.suppressWriteCompletion) {
        dispatch_async(self.queue, ^{
            [self.delegate peripheral:(id)self didWriteValueForCharacteristic:(id)characteristic error:self.writeError];
        });
    }
}
@end

@interface HJSDKFakeCentralManager : NSObject
@property (nonatomic, weak) id<CBCentralManagerDelegate> delegate;
@property (nonatomic, assign) NSUInteger connectCount;
@property (nonatomic, assign) NSUInteger disconnectCount;
@property (nonatomic, copy) void (^disconnectHandler)(CBPeripheral *peripheral);
@property (nonatomic, copy) void (^connectHandler)(CBPeripheral *peripheral);
@end


@implementation HJSDKFakeCentralManager

- (CBManagerState)state
{
    return CBManagerStatePoweredOn;
}

- (void)connectPeripheral:(CBPeripheral *)peripheral options:(NSDictionary *)options
{
    self.connectCount += 1;
    if (self.connectHandler) {
        self.connectHandler(peripheral);
    }
}

- (void)cancelPeripheralConnection:(CBPeripheral *)peripheral
{
    self.disconnectCount += 1;
    if (self.disconnectHandler) { self.disconnectHandler(peripheral); }
}

@end


@interface WiseBleConnectionRegressionTests : XCTestCase
@property (nonatomic, strong) HJSDKFakeCentralManager *central;
@property (nonatomic, strong) WWBluetoothLE *ble;
@property (nonatomic, strong) HJSDKFakePeripheral *peripheralA;
@property (nonatomic, strong) HJSDKFakePeripheral *peripheralB;
@property (nonatomic) dispatch_queue_t bleQueue;
@end

@implementation WiseBleConnectionRegressionTests

- (void)setUp
{
    [super setUp];
    self.central = [[HJSDKFakeCentralManager alloc] init];
    self.bleQueue = dispatch_queue_create("com.hongjia.HJBleDemoTests", DISPATCH_QUEUE_SERIAL);
    self.ble = [[WWBluetoothLE alloc] initWithCentralManager:(CBCentralManager *)self.central queue:self.bleQueue];
    self.peripheralA = [[HJSDKFakePeripheral alloc] init];
    self.peripheralA.identifier = [NSUUID UUID];
    self.peripheralA.state = CBPeripheralStateDisconnected;
    self.peripheralB = [[HJSDKFakePeripheral alloc] init];
    self.peripheralB.identifier = [NSUUID UUID];
    self.peripheralB.state = CBPeripheralStateDisconnected;
}

- (void)testSystemFailureBeforeWaitBlocksFinishesImmediately
{
    __weak typeof(self) weakSelf = self;
    self.central.connectHandler = ^(CBPeripheral *peripheral) {
        __strong typeof(weakSelf) self = weakSelf;
        [self.ble centralManager:(CBCentralManager *)self.central
            didFailToConnectPeripheral:peripheral
                                 error:nil];
    };
    XCTestExpectation *finished = [self expectationWithDescription:@"failed immediately"];
    CFAbsoluteTime started = CFAbsoluteTimeGetCurrent();
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        BOOL result = [self.ble synchronizedConnect:(CBPeripheral *)self.peripheralA time:1000];
        XCTAssertFalse(result);
        XCTAssertLessThan(CFAbsoluteTimeGetCurrent() - started, 0.2);
        [finished fulfill];
    });
    [self waitForExpectations:@[finished] timeout:0.5];
}

- (void)testDisconnectingADoesNotFinishBConnectionWait
{
    XCTestExpectation *started = [self expectationWithDescription:@"B started"];
    self.central.connectHandler = ^(CBPeripheral *peripheral) {
        [started fulfill];
    };
    XCTestExpectation *finished = [self expectationWithDescription:@"B finished"];
    __block BOOL didFinish = NO;
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [self.ble synchronizedConnect:(CBPeripheral *)self.peripheralB time:1000];
        didFinish = YES;
        [finished fulfill];
    });
    [self waitForExpectations:@[started] timeout:0.2];

    dispatch_sync(self.bleQueue, ^{
        [self.ble centralManager:(CBCentralManager *)self.central
             didDisconnectPeripheral:(CBPeripheral *)self.peripheralA
                                error:nil];
    });
    XCTestExpectation *briefWait = [self expectationWithDescription:@"B remains waiting"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 50 * NSEC_PER_MSEC), dispatch_get_main_queue(), ^{
        XCTAssertFalse(didFinish);
        [briefWait fulfill];
    });
    [self waitForExpectations:@[briefWait] timeout:0.2];

    dispatch_sync(self.bleQueue, ^{
        [self.ble centralManager:(CBCentralManager *)self.central
            didFailToConnectPeripheral:(CBPeripheral *)self.peripheralB
                                 error:nil];
    });
    [self waitForExpectations:@[finished] timeout:0.2];
}

// 只替代系统蓝牙对象，初始化、等待、分包和会话管理使用 Pod 中的真实实现。
- (void)prepareDevice
{
    HJSDKFakeService *service = [[HJSDKFakeService alloc] init];
    service.UUID = [CBUUID UUIDWithString:@"FFF0"];
    NSMutableArray *characteristics = [NSMutableArray array];
    for (NSString *uuid in @[@"FFF1", @"FFF2", @"FFF3"]) {
        HJSDKFakeCharacteristic *characteristic = [[HJSDKFakeCharacteristic alloc] init];
        characteristic.UUID = [CBUUID UUIDWithString:uuid];
        characteristic.service = service;
        characteristic.properties = CBCharacteristicPropertyNotify | CBCharacteristicPropertyWrite |
            CBCharacteristicPropertyWriteWithoutResponse;
        [characteristics addObject:characteristic];
    }
    service.characteristics = characteristics;
    self.peripheralA.services = @[service];
    self.peripheralA.queue = self.bleQueue;
    self.peripheralA.packets = [NSMutableArray array];
    __weak typeof(self) weakSelf = self;
    self.central.connectHandler = ^(CBPeripheral *peripheral) {
        __strong typeof(weakSelf) self = weakSelf;
        ((HJSDKFakePeripheral *)peripheral).state = CBPeripheralStateConnected;
        [self.central.delegate centralManager:(id)self.central didConnectPeripheral:peripheral];
    };
    self.central.disconnectHandler = ^(CBPeripheral *peripheral) {
        __strong typeof(weakSelf) self = weakSelf;
        ((HJSDKFakePeripheral *)peripheral).state = CBPeripheralStateDisconnected;
        [self.central.delegate centralManager:(id)self.central didDisconnectPeripheral:peripheral error:nil];
    };
}

- (void)testSequentialInitializationAndSendUseExplicitSessionAndCharacteristics
{
    [self prepareDevice];
    NSUUID *session = [NSUUID UUID];
    WWCharacteristic *config = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
    WWCharacteristic *receive = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF1"];
    WWCharacteristic *send = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF2"];
    NSData *report = [@"device report" dataUsingEncoding:NSUTF8StringEncoding];
    XCTestExpectation *received = [self expectationForNotification:WWBLEDidReceiveDataNotification
                                                           object:self.ble handler:^BOOL(NSNotification *note) {
        XCTAssertTrue(HJBleConnectionEventMatches((id)self.peripheralA, session,
                                                note.userInfo[WWBLEPeripheralKey], note.userInfo[WWBLESessionKey]));
        XCTAssertEqualObjects(note.userInfo[WWBLECharacteristicKey], receive);
        XCTAssertEqualObjects(note.userInfo[WWBLEDataKey], report);
        return YES;
    }];
    __weak typeof(self) weakSelf = self;
    self.peripheralA.writeHandler = ^(HJSDKFakeCharacteristic *characteristic) {
        if ([characteristic.UUID isEqual:[CBUUID UUIDWithString:@"FFF3"]]) {
            __strong typeof(weakSelf) self = weakSelf;
            characteristic.value = [@"<rd_uart_fc=1>" dataUsingEncoding:NSUTF8StringEncoding];
            [self.peripheralA.delegate peripheral:(id)self.peripheralA
                 didUpdateValueForCharacteristic:(id)characteristic error:nil];
        }
    };
    XCTestExpectation *finished = [self expectationWithDescription:@"sequential initialization and send"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error = nil;
        CBPeripheral *peripheral = (id)self.peripheralA;
        XCTAssertTrue([self.ble synchronizedConnect:peripheral session:session time:500 maxAttempts:1 cleanupTime:200 error:&error]);
        XCTAssertTrue([self.ble synchronizedSetNotify:peripheral characteristic:config enable:YES session:session time:500 error:&error]);
        NSData *response = [self.ble sendReceive:peripheral characteristic:config
                                         value:[@"<RD_UART_FC>" dataUsingEncoding:NSUTF8StringEncoding]
                                          type:CBCharacteristicWriteWithResponse responseCharacteristic:config
                                       session:session packetSize:20 time:500 error:&error];
        XCTAssertEqualObjects(response, [@"<rd_uart_fc=1>" dataUsingEncoding:NSUTF8StringEncoding]);
        XCTAssertTrue([self.ble synchronizedSetNotify:peripheral characteristic:receive enable:YES session:session time:500 error:&error]);
        XCTAssertTrue([self.ble synchronizedSend:peripheral characteristic:send value:[NSMutableData dataWithLength:130]
                                          type:CBCharacteristicWriteWithoutResponse session:session packetSize:64 time:500 error:&error]);
        XCTAssertNil(error);
        XCTAssertNil(self.ble.commonResponeNotifyCharacteristic);
        XCTAssertEqual(self.ble.nGroupSendDataLen, 20);
        XCTAssertEqual(self.peripheralA.packets.count, 4);
        XCTAssertEqual(self.peripheralA.packets[1].length, 64);
        XCTAssertEqual(self.peripheralA.packets[2].length, 64);
        XCTAssertEqual(self.peripheralA.packets[3].length, 2);
        dispatch_sync(self.bleQueue, ^{
            HJSDKFakeService *service = self.peripheralA.services.firstObject;
            HJSDKFakeCharacteristic *characteristic = service.characteristics.firstObject;
            characteristic.value = report;
            [self.peripheralA.delegate peripheral:peripheral didUpdateValueForCharacteristic:(id)characteristic error:nil];
        });
        [finished fulfill];
    });
    [self waitForExpectations:@[received, finished] timeout:2];
}

- (void)checkFlowControlReplies:(NSArray<NSString *> *)replies
                          type:(CBCharacteristicWriteType)type
                         state:(HJBleFlowControlState)state
                          busy:(BOOL)busy
{
    [self prepareDevice];
    NSUUID *session = [NSUUID UUID];
    WWCharacteristic *config = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
    WWCharacteristic *send = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF2"];
    __weak typeof(self) weakSelf = self;
    self.peripheralA.writeHandler = ^(HJSDKFakeCharacteristic *characteristic) {
        __strong typeof(weakSelf) self = weakSelf;
        if (![characteristic.UUID isEqual:[CBUUID UUIDWithString:@"FFF3"]]) { return; }
        for (NSString *reply in replies) {
            characteristic.value = [reply dataUsingEncoding:NSUTF8StringEncoding];
            [self.peripheralA.delegate peripheral:(id)self.peripheralA didUpdateValueForCharacteristic:(id)characteristic error:nil];
        }
    };
    XCTestExpectation *finished = [self expectationWithDescription:@"flow check and subsequent send"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error = nil;
        CBPeripheral *peripheral = (id)self.peripheralA;
        XCTAssertTrue([self.ble synchronizedConnect:peripheral session:session time:500 maxAttempts:1 cleanupTime:200 error:&error]);
        XCTAssertTrue([self.ble synchronizedSetNotify:peripheral characteristic:config enable:YES session:session time:500 error:&error]);
        HJBleFlowControlCheck *check = [[HJBleFlowControlCheck alloc] init];
        XCTAssertTrue([check checkWithBLE:self.ble peripheral:peripheral characteristic:config session:session type:type timeout:150 error:&error]);
        XCTAssertEqual(check.state, state);
        XCTAssertEqual(check.busy, busy);
        XCTAssertNil(error);
        // 无回复时仍须真实调用 Pod 发送成功，防止只保持了表面上的连接。
        XCTAssertTrue([self.ble synchronizedSend:peripheral characteristic:send value:[NSData dataWithBytes:"ok" length:2]
                                          type:CBCharacteristicWriteWithResponse session:session packetSize:20 time:500 error:&error]);
        XCTAssertTrue([self.ble isSessionActive:session peripheral:peripheral]);
        XCTAssertEqual(self.central.disconnectCount, 0U);
        XCTAssertEqual(self.peripheralA.packets.count, 2U);
        XCTAssertEqualObjects(self.peripheralA.packets.firstObject, [@"<RD_UART_FC>" dataUsingEncoding:NSUTF8StringEncoding]);
        [finished fulfill];
    });
    [self waitForExpectations:@[finished] timeout:2];
}

- (void)testFlowCheckWithoutReplyAllowsSubsequentSend
{
    [self checkFlowControlReplies:@[] type:CBCharacteristicWriteWithResponse state:HJBleFlowControlUnknown busy:NO];
}

- (void)testFlowCheckWithoutResponseWriteAllowsSubsequentSend
{
    [self checkFlowControlReplies:@[] type:CBCharacteristicWriteWithoutResponse state:HJBleFlowControlUnknown busy:NO];
}

- (void)testFlowCheckUnknownReplyDoesNotMeanDisabled
{
    [self checkFlowControlReplies:@[@"unsupported"] type:CBCharacteristicWriteWithResponse state:HJBleFlowControlUnknown busy:NO];
}

- (void)testFlowCheckBusyReportDoesNotConsumeEnabledReply
{
    [self checkFlowControlReplies:@[@"<HJ_BLE_BUSY_STOP_SEND>", @"<rd_uart_fc=1>"]
                            type:CBCharacteristicWriteWithResponse state:HJBleFlowControlEnabled busy:YES];
}

- (void)testFlowCheckIdleReportRestoresSendingAndDisabledReplyIsRecognized
{
    [self checkFlowControlReplies:@[@"<HJ_BLE_BUSY_STOP_SEND>", @"<HJ_BLE_IDLE_START_SEND>", @"<rd_uart_fc=0>"]
                            type:CBCharacteristicWriteWithResponse state:HJBleFlowControlDisabled busy:NO];
}

- (void)testFlowCheckIgnoresWrongDeviceSessionAndCharacteristic
{
    [self prepareDevice];
    NSUUID *session = [NSUUID UUID];
    WWCharacteristic *config = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
    __weak typeof(self) weakSelf = self;
    self.peripheralA.writeHandler = ^(HJSDKFakeCharacteristic *characteristic) {
        __strong typeof(weakSelf) self = weakSelf;
        dispatch_async(dispatch_get_main_queue(), ^{
            for (NSUInteger i = 0; i < 3; i++) {
                NSDictionary *info = @{WWBLEPeripheralKey: i == 0 ? (id)self.peripheralB : (id)self.peripheralA,
                                       WWBLESessionKey: i == 1 ? [NSUUID UUID] : session,
                                       WWBLECharacteristicKey: i == 2 ? [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF1"] : config,
                                       WWBLEDataKey:[@"<rd_uart_fc=1>" dataUsingEncoding:NSUTF8StringEncoding]};
                [[NSNotificationCenter defaultCenter] postNotificationName:WWBLEDidReceiveDataNotification object:self.ble userInfo:info];
            }
        });
    };
    XCTestExpectation *finished = [self expectationWithDescription:@"unrelated notifications ignored"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        XCTAssertTrue([self.ble synchronizedConnect:(id)self.peripheralA session:session time:500 maxAttempts:1 cleanupTime:200 error:NULL]);
        HJBleFlowControlCheck *check = [[HJBleFlowControlCheck alloc] init];
        XCTAssertTrue([check checkWithBLE:self.ble peripheral:(id)self.peripheralA characteristic:config session:session
                                   type:CBCharacteristicWriteWithResponse timeout:150 error:NULL]);
        XCTAssertEqual(check.state, HJBleFlowControlUnknown);
        [finished fulfill];
    });
    [self waitForExpectations:@[finished] timeout:2];
}

- (void)testFlowCheckWriteFailureAndWriteTimeoutRemainFailures
{
    for (NSNumber *timeout in @[@NO, @YES]) {
        [self prepareDevice];
        self.peripheralA.writeError = [NSError errorWithDomain:CBATTErrorDomain code:CBATTErrorWriteNotPermitted userInfo:nil];
        self.peripheralA.suppressWriteCompletion = timeout.boolValue;
        NSUUID *session = [NSUUID UUID];
        WWCharacteristic *config = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
        XCTestExpectation *finished = [self expectationWithDescription:@"write failure remains fatal"];
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            XCTAssertTrue([self.ble synchronizedConnect:(id)self.peripheralA session:session time:500 maxAttempts:1 cleanupTime:200 error:NULL]);
            HJBleFlowControlCheck *check = [[HJBleFlowControlCheck alloc] init];
            NSError *error = nil;
            XCTAssertFalse([check checkWithBLE:self.ble peripheral:(id)self.peripheralA characteristic:config session:session
                                        type:CBCharacteristicWriteWithResponse timeout:150 error:&error]);
            XCTAssertEqual(error.code, timeout.boolValue ? WWBLEErrorTimedOut : WWBLEErrorOperationFailed);
            XCTAssertEqual(check.state, HJBleFlowControlUnknown);
            [self.ble disconnect:(id)self.peripheralA session:session error:NULL];
            [finished fulfill];
        });
        [self waitForExpectations:@[finished] timeout:2];
    }
}

- (void)testFlowCheckCancellationDoesNotWaitForResponseTimeout
{
    [self prepareDevice];
    NSUUID *session = [NSUUID UUID];
    WWCharacteristic *config = [[WWCharacteristic alloc] initWithServiceID:@"FFF0" characteristicID:@"FFF3"];
    XCTestExpectation *written = [self expectationWithDescription:@"query written"];
    self.peripheralA.writeHandler = ^(HJSDKFakeCharacteristic *characteristic) { [written fulfill]; };
    XCTestExpectation *finished = [self expectationWithDescription:@"cancelled without waiting five seconds"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        XCTAssertTrue([self.ble synchronizedConnect:(id)self.peripheralA session:session time:500 maxAttempts:1 cleanupTime:200 error:NULL]);
        HJBleFlowControlCheck *check = [[HJBleFlowControlCheck alloc] init];
        XCTAssertFalse([check checkWithBLE:self.ble peripheral:(id)self.peripheralA characteristic:config session:session
                                    type:CBCharacteristicWriteWithResponse timeout:5000 error:NULL]);
        [finished fulfill];
    });
    [self waitForExpectations:@[written] timeout:1];
    [self.ble disconnect:(id)self.peripheralA session:session error:NULL];
    [self waitForExpectations:@[finished] timeout:0.5];
}

- (void)testPowerLossCleanupAndReconnectKeepOldPageFromDisconnectingNewSession
{
    [self prepareDevice];
    NSUUID *oldSession = [NSUUID UUID];
    NSUUID *newSession = [NSUUID UUID];
    XCTestExpectation *disconnected = [self expectationForNotification:WWBLEConnectionDidDisconnectNotification
                                                               object:self.ble handler:^BOOL(NSNotification *note) {
        return HJBleConnectionEventMatches((id)self.peripheralA, oldSession,
                                          note.userInfo[WWBLEPeripheralKey], note.userInfo[WWBLESessionKey]);
    }];
    XCTestExpectation *finished = [self expectationWithDescription:@"reconnected after power loss"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error = nil;
        CBPeripheral *peripheral = (id)self.peripheralA;
        XCTAssertTrue([self.ble synchronizedConnect:peripheral session:oldSession time:500 maxAttempts:1 cleanupTime:200 error:&error]);
        dispatch_sync(self.bleQueue, ^{
            self.peripheralA.state = CBPeripheralStateDisconnected;
            [self.central.delegate centralManager:(id)self.central didDisconnectPeripheral:peripheral error:nil];
        });
        XCTAssertTrue([self.ble disconnect:peripheral session:oldSession error:&error]);
        XCTAssertTrue([self.ble disconnect:peripheral session:oldSession error:&error]);
        XCTAssertTrue([self.ble synchronizedConnect:peripheral session:newSession time:500 maxAttempts:1 cleanupTime:200 error:&error]);
        XCTAssertFalse([self.ble disconnect:peripheral session:oldSession error:&error]);
        XCTAssertEqual(error.code, WWBLEErrorStaleSession);
        XCTAssertTrue([self.ble isSessionActive:newSession peripheral:peripheral]);
        XCTAssertEqual(self.central.disconnectCount, 0);
        XCTAssertEqual(self.central.connectCount, 2);
        [finished fulfill];
    });
    [self waitForExpectations:@[disconnected, finished] timeout:2];
}

- (void)testCancelConnectingSessionWakesSynchronousCallWithoutRetry
{
    [self prepareDevice];
    NSUUID *session = [NSUUID UUID];
    XCTestExpectation *started = [self expectationWithDescription:@"connecting"];
    self.central.connectHandler = ^(CBPeripheral *peripheral) { [started fulfill]; };
    XCTestExpectation *finished = [self expectationWithDescription:@"cancelled"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSError *error = nil;
        XCTAssertFalse([self.ble synchronizedConnect:(id)self.peripheralA session:session
                                               time:500 maxAttempts:5 cleanupTime:200 error:&error]);
        XCTAssertEqual(error.code, WWBLEErrorCancelled);
        XCTAssertEqual(self.central.connectCount, 1);
        [finished fulfill];
    });
    [self waitForExpectations:@[started] timeout:1];
    XCTAssertTrue([self.ble disconnect:(id)self.peripheralA session:session error:NULL]);
    [self waitForExpectations:@[finished] timeout:1];
}

@end
