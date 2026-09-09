#import <XCTest/XCTest.h>
#import "HJBleDevicePageLifecycle.h"

@interface HJLifecycleFakePeripheral : NSObject
@property (nonatomic, strong) NSUUID *identifier;
@end

@implementation HJLifecycleFakePeripheral
@end

@interface HJBleDevicePageLifecycleTests : XCTestCase
@end

@implementation HJBleDevicePageLifecycleTests

- (void)testButtonBackAndCompletedSwipeLeaveTheStack
{
    NSObject *page = [[NSObject alloc] init];
    XCTAssertTrue(HJBleDevicePageDidLeaveNavigationStack(@[], page));
}

- (void)testPushingSettingsAndCancelledSwipeKeepThePage
{
    NSObject *page = [[NSObject alloc] init];
    NSObject *settings = [[NSObject alloc] init];
    XCTAssertFalse(HJBleDevicePageDidLeaveNavigationStack(@[page, settings], page));
    XCTAssertFalse(HJBleDevicePageDidLeaveNavigationStack(@[page], page));
}

- (void)testConnectionEventsRequireTheCurrentDeviceAndSession
{
    HJLifecycleFakePeripheral *peripheralA = [[HJLifecycleFakePeripheral alloc] init];
    peripheralA.identifier = [NSUUID UUID];
    HJLifecycleFakePeripheral *peripheralB = [[HJLifecycleFakePeripheral alloc] init];
    peripheralB.identifier = [NSUUID UUID];
    NSUUID *currentSession = [NSUUID UUID];

    XCTAssertTrue(HJBleConnectionEventMatches((CBPeripheral *)peripheralB,
                                              currentSession,
                                              (CBPeripheral *)peripheralB,
                                              currentSession));
    XCTAssertFalse(HJBleConnectionEventMatches((CBPeripheral *)peripheralB,
                                               currentSession,
                                               (CBPeripheral *)peripheralA,
                                               currentSession));
    XCTAssertFalse(HJBleConnectionEventMatches((CBPeripheral *)peripheralB,
                                               currentSession,
                                               (CBPeripheral *)peripheralB,
                                               [NSUUID UUID]));
}

@end
