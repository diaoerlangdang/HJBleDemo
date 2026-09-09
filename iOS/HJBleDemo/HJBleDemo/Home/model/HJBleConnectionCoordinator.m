#import "HJBleConnectionCoordinator.h"
#import <wiseBle/WiseBle.h>

NSNotificationName const HJBlePeripheralDidDisconnectNotification = @"HJBlePeripheralDidDisconnectNotification";
NSString * const HJBlePeripheralKey = @"peripheral";
NSString * const HJBleSessionTokenKey = @"sessionToken";

typedef NS_ENUM(NSUInteger, HJBleSessionState) {
    HJBleSessionStateConnecting,
    HJBleSessionStateConnected,
    HJBleSessionStateWaitingForDisconnect,
};

@interface HJBleConnectionSession : NSObject
@property (nonatomic, strong) NSUUID *token;
@property (nonatomic, strong) CBPeripheral *peripheral;
@property (nonatomic, copy, nullable) void (^completion)(HJBleConnectionResult result);
@property (nonatomic, assign) NSUInteger attempt;
@property (nonatomic, assign) NSUInteger timerGeneration;
@property (nonatomic, assign) HJBleSessionState state;
@property (nonatomic, assign) BOOL cancellationRequested;
@property (nonatomic, assign) BOOL completionDelivered;
@end

@implementation HJBleConnectionSession
@end

@interface HJBleWiseConnectionTransport : NSObject <HJBleConnectionTransport, WWBluetoothLEConnectDelegate>
@property (nonatomic, weak) id<HJBleConnectionTransportDelegate> delegate;
@property (nonatomic, strong) WWBluetoothLE *ble;
@end

@implementation HJBleWiseConnectionTransport

- (instancetype)init
{
    self = [super init];
    if (self) {
        _ble = [WWBluetoothLE shareBLE];
        _ble.connectDelegate = self;
    }
    return self;
}

- (void)connectPeripheral:(CBPeripheral *)peripheral
{
    self.ble.connectDelegate = self;
    [self.ble connect:peripheral];
}

- (void)disconnectPeripheral:(CBPeripheral *)peripheral
{
    self.ble.connectDelegate = self;
    [self.ble disconnect:peripheral callBack:YES];
}

- (void)ble:(WWBluetoothLE *)ble didConnect:(CBPeripheral *)peripheral result:(BOOL)isSuccess
{
    [self.delegate connectionTransport:self didConnectPeripheral:peripheral success:isSuccess];
}

- (void)ble:(WWBluetoothLE *)ble didDisconnect:(CBPeripheral *)peripheral
{
    [self.delegate connectionTransport:self didDisconnectPeripheral:peripheral];
}

@end

@interface HJBleConnectionCoordinator ()
@property (nonatomic, strong) id<HJBleConnectionTransport> transport;
@property (nonatomic, assign) NSTimeInterval attemptTimeout;
@property (nonatomic, assign) NSTimeInterval cleanupTimeout;
@property (nonatomic, assign) NSUInteger maxAttempts;
@property (nonatomic, strong, nullable) HJBleConnectionSession *session;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSUUID *> *ownedSessions;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSUUID *> *disconnectingSessions;
@property (nonatomic, strong) NSMutableSet<NSString *> *quarantinedPeripheralKeys;
@end

@implementation HJBleConnectionCoordinator

+ (instancetype)sharedCoordinator
{
    static HJBleConnectionCoordinator *coordinator;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        HJBleWiseConnectionTransport *transport = [[HJBleWiseConnectionTransport alloc] init];
        coordinator = [[self alloc] initWithTransport:transport
                                       attemptTimeout:5.0
                                       cleanupTimeout:2.0
                                          maxAttempts:5];
    });
    return coordinator;
}

- (instancetype)initWithTransport:(id<HJBleConnectionTransport>)transport
                    attemptTimeout:(NSTimeInterval)attemptTimeout
                    cleanupTimeout:(NSTimeInterval)cleanupTimeout
                       maxAttempts:(NSUInteger)maxAttempts
{
    self = [super init];
    if (self) {
        _transport = transport;
        _transport.delegate = self;
        _attemptTimeout = attemptTimeout;
        _cleanupTimeout = cleanupTimeout;
        _maxAttempts = MAX(maxAttempts, 1);
        _ownedSessions = [NSMutableDictionary dictionary];
        _disconnectingSessions = [NSMutableDictionary dictionary];
        _quarantinedPeripheralKeys = [NSMutableSet set];
    }
    return self;
}

- (NSUUID *)connectPeripheral:(CBPeripheral *)peripheral
                   completion:(void (^)(HJBleConnectionResult))completion
{
    NSParameterAssert([NSThread isMainThread]);
    NSUUID *token = [NSUUID UUID];
    NSString *key = peripheral == nil ? nil : [self keyForPeripheral:peripheral];
    if (peripheral == nil || self.session != nil || self.disconnectingSessions[key] != nil ||
        [self.quarantinedPeripheralKeys containsObject:key]) {
        if (completion) {
            dispatch_async(dispatch_get_main_queue(), ^{
                completion(HJBleConnectionResultFailed);
            });
        }
        return token;
    }

    HJBleConnectionSession *session = [[HJBleConnectionSession alloc] init];
    session.token = token;
    session.peripheral = peripheral;
    session.completion = completion;
    self.session = session;
    [self startAttemptForSession:session];
    return token;
}

- (BOOL)isSessionActive:(NSUUID *)token peripheral:(CBPeripheral *)peripheral
{
    if (![NSThread isMainThread]) {
        __block BOOL active = NO;
        dispatch_sync(dispatch_get_main_queue(), ^{
            active = [self isSessionActive:token peripheral:peripheral];
        });
        return active;
    }
    return self.session != nil &&
        [self.session.token isEqual:token] &&
        [self isPeripheral:peripheral equalTo:self.session.peripheral] &&
        !self.session.cancellationRequested;
}

- (void)cancelSession:(NSUUID *)token
{
    NSParameterAssert([NSThread isMainThread]);
    HJBleConnectionSession *session = self.session;
    if (session == nil || ![session.token isEqual:token]) {
        return;
    }

    session.cancellationRequested = YES;
    [self deliverResult:HJBleConnectionResultCancelled forSession:session];
    if (session.state != HJBleSessionStateWaitingForDisconnect) {
        [self beginDisconnectForSession:session];
    }
}

- (void)transferSession:(NSUUID *)token
{
    NSParameterAssert([NSThread isMainThread]);
    HJBleConnectionSession *session = self.session;
    if (session == nil || session.state != HJBleSessionStateConnected ||
        ![session.token isEqual:token]) {
        return;
    }

    self.ownedSessions[[self keyForPeripheral:session.peripheral]] = session.token;
    session.timerGeneration += 1;
    self.session = nil;
}

- (void)disconnectPeripheral:(CBPeripheral *)peripheral session:(NSUUID *)token
{
    NSParameterAssert([NSThread isMainThread]);
    if (peripheral == nil) {
        return;
    }
    NSString *key = [self keyForPeripheral:peripheral];
    NSUUID *ownedToken = self.ownedSessions[key];
    NSUUID *disconnectingToken = self.disconnectingSessions[key];
    HJBleConnectionSession *currentSession = self.session;
    BOOL matchesCurrentPeripheral = currentSession != nil &&
        [self isPeripheral:peripheral equalTo:currentSession.peripheral];
    if ((token != nil && ownedToken != nil && ![ownedToken isEqual:token]) ||
        (token != nil && disconnectingToken != nil && ![disconnectingToken isEqual:token]) ||
        (matchesCurrentPeripheral && ![currentSession.token isEqual:token])) {
        return;
    }
    NSUUID *disconnectToken = ownedToken ?: token ?: [NSUUID UUID];
    [self.ownedSessions removeObjectForKey:key];
    self.disconnectingSessions[key] = disconnectToken;
    [self.transport disconnectPeripheral:peripheral];
}

- (void)startAttemptForSession:(HJBleConnectionSession *)session
{
    if (self.session != session || session.cancellationRequested) {
        return;
    }
    session.attempt += 1;
    session.state = HJBleSessionStateConnecting;
    session.timerGeneration += 1;
    NSUInteger generation = session.timerGeneration;
    [self.transport connectPeripheral:session.peripheral];

    __weak typeof(self) weakSelf = self;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(self.attemptTimeout * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        __strong typeof(weakSelf) self = weakSelf;
        if (self.session == session && session.state == HJBleSessionStateConnecting &&
            session.timerGeneration == generation) {
            [self beginDisconnectForSession:session];
        }
    });
}

- (void)beginDisconnectForSession:(HJBleConnectionSession *)session
{
    session.state = HJBleSessionStateWaitingForDisconnect;
    session.timerGeneration += 1;
    NSUInteger generation = session.timerGeneration;
    [self.transport disconnectPeripheral:session.peripheral];

    __weak typeof(self) weakSelf = self;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(self.cleanupTimeout * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        __strong typeof(weakSelf) self = weakSelf;
        if (self.session == session && session.state == HJBleSessionStateWaitingForDisconnect &&
            session.timerGeneration == generation) {
            [self.quarantinedPeripheralKeys addObject:[self keyForPeripheral:session.peripheral]];
            HJBleConnectionResult result = session.cancellationRequested
                ? HJBleConnectionResultCancelled : HJBleConnectionResultTimedOut;
            [self finishSession:session result:result];
        }
    });
}

- (void)connectionTransport:(id<HJBleConnectionTransport>)transport
       didConnectPeripheral:(CBPeripheral *)peripheral
                    success:(BOOL)success
{
    NSParameterAssert([NSThread isMainThread]);
    HJBleConnectionSession *session = self.session;
    if (session == nil || session.state != HJBleSessionStateConnecting ||
        ![self isPeripheral:peripheral equalTo:session.peripheral]) {
        return;
    }

    session.timerGeneration += 1;
    if (success) {
        session.state = HJBleSessionStateConnected;
        [self deliverResult:HJBleConnectionResultSuccess forSession:session];
    }
    else if (session.attempt < self.maxAttempts) {
        [self startAttemptForSession:session];
    }
    else {
        [self finishSession:session result:HJBleConnectionResultFailed];
    }
}

- (void)connectionTransport:(id<HJBleConnectionTransport>)transport
    didDisconnectPeripheral:(CBPeripheral *)peripheral
{
    NSParameterAssert([NSThread isMainThread]);
    NSString *key = [self keyForPeripheral:peripheral];
    [self.quarantinedPeripheralKeys removeObject:key];
    HJBleConnectionSession *session = self.session;
    NSUUID *token = self.disconnectingSessions[key] ?: self.ownedSessions[key];

    if (session != nil && [self isPeripheral:peripheral equalTo:session.peripheral]) {
        token = session.token;
        session.timerGeneration += 1;
        if (session.state == HJBleSessionStateWaitingForDisconnect) {
            if (session.cancellationRequested) {
                [self finishSession:session result:HJBleConnectionResultCancelled];
            }
            else if (session.attempt < self.maxAttempts) {
                [self startAttemptForSession:session];
            }
            else {
                [self finishSession:session result:HJBleConnectionResultTimedOut];
            }
        }
        else if (session.state == HJBleSessionStateConnected) {
            self.session = nil;
        }
    }

    [self.ownedSessions removeObjectForKey:key];
    [self.disconnectingSessions removeObjectForKey:key];
    NSMutableDictionary *userInfo = [NSMutableDictionary dictionaryWithObject:peripheral
                                                                        forKey:HJBlePeripheralKey];
    if (token != nil) {
        userInfo[HJBleSessionTokenKey] = token;
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:HJBlePeripheralDidDisconnectNotification
                                                        object:self
                                                      userInfo:userInfo];
}

- (void)finishSession:(HJBleConnectionSession *)session result:(HJBleConnectionResult)result
{
    if (self.session != session) {
        return;
    }
    session.timerGeneration += 1;
    [self deliverResult:result forSession:session];
    self.session = nil;
}

- (void)deliverResult:(HJBleConnectionResult)result forSession:(HJBleConnectionSession *)session
{
    if (session.completionDelivered) {
        return;
    }
    session.completionDelivered = YES;
    if (session.completion) {
        session.completion(result);
    }
}

- (NSString *)keyForPeripheral:(CBPeripheral *)peripheral
{
    return peripheral.identifier.UUIDString ?: [NSString stringWithFormat:@"%p", peripheral];
}

- (BOOL)isPeripheral:(CBPeripheral *)first equalTo:(CBPeripheral *)second
{
    if (first == second) {
        return YES;
    }
    return [first.identifier isEqual:second.identifier];
}

@end
