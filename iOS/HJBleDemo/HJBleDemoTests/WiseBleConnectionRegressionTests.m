#import <XCTest/XCTest.h>
#import <wiseBle/WWBluetoothLE.h>

@interface WWBluetoothLE (HJBleTesting)
- (instancetype)initWithCentralManager:(CBCentralManager *)centralManager queue:(dispatch_queue_t)queue;
- (void)centralManager:(CBCentralManager *)central
    didFailToConnectPeripheral:(CBPeripheral *)peripheral
                         error:(NSError *)error;
- (void)centralManager:(CBCentralManager *)central
    didDisconnectPeripheral:(CBPeripheral *)peripheral
                       error:(NSError *)error;
@end

@interface HJSDKFakePeripheral : NSObject
@property (nonatomic, strong) NSUUID *identifier;
@property (nonatomic, assign) CBPeripheralState state;
@end

@implementation HJSDKFakePeripheral
@end

@interface HJSDKFakeCentralManager : NSObject
@property (nonatomic, weak) id delegate;
@property (nonatomic, assign) NSUInteger connectCount;
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

@end
