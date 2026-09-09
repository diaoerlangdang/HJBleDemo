#import <CoreBluetooth/CoreBluetooth.h>
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

extern NSNotificationName const HJBlePeripheralDidDisconnectNotification;
extern NSString * const HJBlePeripheralKey;
extern NSString * const HJBleSessionTokenKey;

typedef NS_ENUM(NSUInteger, HJBleConnectionResult) {
    HJBleConnectionResultSuccess,
    HJBleConnectionResultFailed,
    HJBleConnectionResultTimedOut,
    HJBleConnectionResultCancelled,
};

@protocol HJBleConnectionTransport;

@protocol HJBleConnectionTransportDelegate <NSObject>
- (void)connectionTransport:(id<HJBleConnectionTransport>)transport
       didConnectPeripheral:(CBPeripheral *)peripheral
                    success:(BOOL)success;
- (void)connectionTransport:(id<HJBleConnectionTransport>)transport
    didDisconnectPeripheral:(CBPeripheral *)peripheral;
@end

@protocol HJBleConnectionTransport <NSObject>
@property (nonatomic, weak) id<HJBleConnectionTransportDelegate> delegate;
- (void)connectPeripheral:(CBPeripheral *)peripheral;
- (void)disconnectPeripheral:(CBPeripheral *)peripheral;
@end

@interface HJBleConnectionCoordinator : NSObject <HJBleConnectionTransportDelegate>

+ (instancetype)sharedCoordinator;

- (instancetype)initWithTransport:(id<HJBleConnectionTransport>)transport
                    attemptTimeout:(NSTimeInterval)attemptTimeout
                    cleanupTimeout:(NSTimeInterval)cleanupTimeout
                       maxAttempts:(NSUInteger)maxAttempts NS_DESIGNATED_INITIALIZER;
- (instancetype)init NS_UNAVAILABLE;

- (NSUUID *)connectPeripheral:(CBPeripheral *)peripheral
                   completion:(void (^ _Nullable)(HJBleConnectionResult result))completion;
- (BOOL)isSessionActive:(NSUUID *)token peripheral:(CBPeripheral *)peripheral;
- (void)cancelSession:(NSUUID *)token;
- (void)transferSession:(NSUUID *)token;
- (void)disconnectPeripheral:(CBPeripheral *)peripheral session:(nullable NSUUID *)token;

@end

NS_ASSUME_NONNULL_END
