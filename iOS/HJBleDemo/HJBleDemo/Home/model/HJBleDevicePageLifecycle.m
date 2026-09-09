#import "HJBleDevicePageLifecycle.h"

BOOL HJBleDevicePageDidLeaveNavigationStack(NSArray *navigationStack, id devicePage)
{
    return ![navigationStack containsObject:devicePage];
}

BOOL HJBleConnectionEventMatches(CBPeripheral *expectedPeripheral,
                                 NSUUID *expectedSession,
                                 CBPeripheral *actualPeripheral,
                                 NSUUID *actualSession)
{
    if (expectedPeripheral == nil || actualPeripheral == nil ||
        (actualSession != nil && ![expectedSession isEqual:actualSession])) {
        return NO;
    }
    return expectedPeripheral == actualPeripheral ||
        [expectedPeripheral.identifier isEqual:actualPeripheral.identifier];
}
