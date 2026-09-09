#import <XCTest/XCTest.h>
#import "HJBleConnectionCoordinator.h"

@interface HJFakePeripheral : NSObject
@property (nonatomic, strong) NSUUID *identifier;
@end

@implementation HJFakePeripheral
@end

@interface HJFakeConnectionTransport : NSObject <HJBleConnectionTransport>
@property (nonatomic, weak) id<HJBleConnectionTransportDelegate> delegate;
@property (nonatomic, strong) NSMutableArray *connectedPeripherals;
@property (nonatomic, strong) NSMutableArray *disconnectedPeripherals;
@end

@implementation HJFakeConnectionTransport

- (instancetype)init
{
    self = [super init];
    if (self) {
        _connectedPeripherals = [NSMutableArray array];
        _disconnectedPeripherals = [NSMutableArray array];
    }
    return self;
}

- (void)connectPeripheral:(CBPeripheral *)peripheral
{
    [self.connectedPeripherals addObject:peripheral];
}

- (void)disconnectPeripheral:(CBPeripheral *)peripheral
{
    [self.disconnectedPeripherals addObject:peripheral];
}

- (void)finishConnect:(CBPeripheral *)peripheral success:(BOOL)success
{
    [self.delegate connectionTransport:self didConnectPeripheral:peripheral success:success];
}

- (void)finishDisconnect:(CBPeripheral *)peripheral
{
    [self.delegate connectionTransport:self didDisconnectPeripheral:peripheral];
}

@end

@interface HJBleConnectionCoordinatorTests : XCTestCase
@property (nonatomic, strong) HJFakeConnectionTransport *transport;
@property (nonatomic, strong) HJBleConnectionCoordinator *coordinator;
@property (nonatomic, strong) HJFakePeripheral *peripheralA;
@property (nonatomic, strong) HJFakePeripheral *peripheralB;
@end

@implementation HJBleConnectionCoordinatorTests

- (void)setUp
{
    [super setUp];
    self.transport = [[HJFakeConnectionTransport alloc] init];
    self.coordinator = [[HJBleConnectionCoordinator alloc] initWithTransport:self.transport
                                                               attemptTimeout:0.02
                                                               cleanupTimeout:0.05
                                                                   maxAttempts:2];
    self.peripheralA = [[HJFakePeripheral alloc] init];
    self.peripheralA.identifier = [NSUUID UUID];
    self.peripheralB = [[HJFakePeripheral alloc] init];
    self.peripheralB.identifier = [NSUUID UUID];
}

- (void)testSuccessfulConnectionCanBeTransferredWithoutDisconnect
{
    XCTestExpectation *completed = [self expectationWithDescription:@"connected"];
    NSUUID *token = [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultSuccess);
        [completed fulfill];
    }];

    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:YES];
    [self waitForExpectations:@[completed] timeout:0.2];
    XCTAssertTrue([self.coordinator isSessionActive:token peripheral:(CBPeripheral *)self.peripheralA]);

    [self.coordinator transferSession:token];
    XCTAssertEqual(self.transport.disconnectedPeripherals.count, 0u);
    XCTAssertFalse([self.coordinator isSessionActive:token peripheral:(CBPeripheral *)self.peripheralA]);
}

- (void)testSystemFailureFinishesImmediatelyAfterLastAttempt
{
    XCTestExpectation *completed = [self expectationWithDescription:@"failed"];
    CFAbsoluteTime started = CFAbsoluteTimeGetCurrent();
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultFailed);
        XCTAssertLessThan(CFAbsoluteTimeGetCurrent() - started, 0.1);
        [completed fulfill];
    }];

    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:NO];
    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:NO];
    [self waitForExpectations:@[completed] timeout:0.2];
}

- (void)testTimeoutWaitsForDisconnectBeforeRetry
{
    XCTestExpectation *timeoutStartedCleanup = [self expectationWithDescription:@"cleanup"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 35 * NSEC_PER_MSEC), dispatch_get_main_queue(), ^{
        XCTAssertEqual(self.transport.connectedPeripherals.count, 1u);
        XCTAssertEqual(self.transport.disconnectedPeripherals.count, 1u);
        [timeoutStartedCleanup fulfill];
    });
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:nil];
    [self waitForExpectations:@[timeoutStartedCleanup] timeout:0.2];

    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    XCTAssertEqual(self.transport.connectedPeripherals.count, 2u);
}

- (void)testCleanupTimeoutQuarantinesSameDeviceUntilTerminalCallback
{
    XCTestExpectation *timedOut = [self expectationWithDescription:@"cleanup bounded"];
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultTimedOut);
        [timedOut fulfill];
    }];
    [self waitForExpectations:@[timedOut] timeout:0.2];

    XCTestExpectation *rejected = [self expectationWithDescription:@"same device rejected"];
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultFailed);
        [rejected fulfill];
    }];
    [self waitForExpectations:@[rejected] timeout:0.2];
    XCTAssertEqual(self.transport.connectedPeripherals.count, 1u);

    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:nil];
    XCTAssertEqual(self.transport.connectedPeripherals.count, 2u);
}

- (void)testCancelPreventsRetryAndSettlesOnlyThatSession
{
    XCTestExpectation *cancelled = [self expectationWithDescription:@"cancelled"];
    NSUUID *token = [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultCancelled);
        [cancelled fulfill];
    }];

    [self.coordinator cancelSession:token];
    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    [self waitForExpectations:@[cancelled] timeout:0.2];
    XCTAssertEqual(self.transport.connectedPeripherals.count, 1u);
}

- (void)testLateCallbackFromAIsIgnoredWhileBConnects
{
    XCTestExpectation *cancelledA = [self expectationWithDescription:@"A cancelled"];
    XCTestExpectation *connectedB = [self expectationWithDescription:@"B connected"];
    NSUUID *tokenA = [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultCancelled);
        [cancelledA fulfill];
    }];
    [self.coordinator cancelSession:tokenA];
    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    [self waitForExpectations:@[cancelledA] timeout:0.2];

    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralB completion:^(HJBleConnectionResult result) {
        XCTAssertEqual(result, HJBleConnectionResultSuccess);
        [connectedB fulfill];
    }];
    XCTAssertFalse([self.coordinator isSessionActive:tokenA peripheral:(CBPeripheral *)self.peripheralA]);
    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:YES];
    XCTAssertEqual(self.transport.disconnectedPeripherals.count, 1u);
    [self.transport finishConnect:(CBPeripheral *)self.peripheralB success:YES];
    [self waitForExpectations:@[connectedB] timeout:0.2];
}

- (void)testTransferredConnectionDisconnectUsesItsSessionToken
{
    XCTestExpectation *connected = [self expectationWithDescription:@"connected"];
    NSUUID *token = [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        [connected fulfill];
    }];
    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:YES];
    [self waitForExpectations:@[connected] timeout:0.2];
    [self.coordinator transferSession:token];

    XCTestExpectation *disconnected = [self expectationForNotification:HJBlePeripheralDidDisconnectNotification
                                                                  object:self.coordinator
                                                                 handler:^BOOL(NSNotification *note) {
        return [note.userInfo[HJBleSessionTokenKey] isEqual:token];
    }];
    [self.coordinator disconnectPeripheral:(CBPeripheral *)self.peripheralA session:token];
    XCTAssertEqualObjects(self.transport.disconnectedPeripherals.lastObject, self.peripheralA);
    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    [self waitForExpectations:@[disconnected] timeout:0.2];
}

- (void)testOldPageCleanupCannotDisconnectNewSessionForSameDevice
{
    XCTestExpectation *firstConnected = [self expectationWithDescription:@"first connected"];
    NSUUID *oldToken = [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        [firstConnected fulfill];
    }];
    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:YES];
    [self waitForExpectations:@[firstConnected] timeout:0.2];
    [self.coordinator transferSession:oldToken];
    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];

    XCTestExpectation *secondConnected = [self expectationWithDescription:@"second connected"];
    [self.coordinator connectPeripheral:(CBPeripheral *)self.peripheralA completion:^(HJBleConnectionResult result) {
        [secondConnected fulfill];
    }];
    [self.transport finishConnect:(CBPeripheral *)self.peripheralA success:YES];
    [self waitForExpectations:@[secondConnected] timeout:0.2];

    [self.coordinator disconnectPeripheral:(CBPeripheral *)self.peripheralA session:oldToken];
    XCTAssertEqual(self.transport.disconnectedPeripherals.count, 0u);
}

- (void)testUnexpectedDisconnectNotificationContainsItsPeripheral
{
    XCTestExpectation *notification = [self expectationForNotification:HJBlePeripheralDidDisconnectNotification
                                                                  object:self.coordinator
                                                                 handler:^BOOL(NSNotification *note) {
        return note.userInfo[HJBlePeripheralKey] == self.peripheralA;
    }];
    [self.transport finishDisconnect:(CBPeripheral *)self.peripheralA];
    [self waitForExpectations:@[notification] timeout:0.2];
}

@end
