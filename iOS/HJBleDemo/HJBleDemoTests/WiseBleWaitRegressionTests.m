#import <XCTest/XCTest.h>
#import <wiseBle/WWWaitEvent.h>

@interface WiseBleWaitRegressionTests : XCTestCase
@end

@implementation WiseBleWaitRegressionTests

- (void)testResultArrivingBeforeBlockingIsPreserved
{
    WWWaitEvent *event = [[WWWaitEvent alloc] init];
    XCTAssertTrue([event prepareWait]);
    [event waitOver:WWWaitResultSuccess];

    CFAbsoluteTime started = CFAbsoluteTimeGetCurrent();
    XCTAssertEqual([event waitPrepared:1000], WWWaitResultSuccess);
    XCTAssertLessThan(CFAbsoluteTimeGetCurrent() - started, 0.1);
}

- (void)testWaitCanOnlyBeSettledOnce
{
    WWWaitEvent *event = [[WWWaitEvent alloc] init];
    XCTAssertTrue([event prepareWait]);
    [event waitOver:WWWaitResultFailed];
    [event waitOver:WWWaitResultSuccess];
    XCTAssertEqual([event waitPrepared:100], WWWaitResultFailed);
}

@end
