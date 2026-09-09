#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT BOOL HJBleDevicePageDidLeaveNavigationStack(NSArray *navigationStack, id devicePage);
FOUNDATION_EXPORT BOOL HJBleConnectionEventMatches(CBPeripheral *expectedPeripheral,
                                                   NSUUID *expectedSession,
                                                   CBPeripheral *actualPeripheral,
                                                   NSUUID * _Nullable actualSession);

NS_ASSUME_NONNULL_END
