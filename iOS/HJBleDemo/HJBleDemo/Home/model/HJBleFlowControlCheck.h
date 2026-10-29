#import <wiseBle/WiseBle.h>

typedef NS_ENUM(NSInteger, HJBleFlowControlState) {
    HJBleFlowControlUnknown,
    HJBleFlowControlEnabled,
    HJBleFlowControlDisabled,
};

@interface HJBleFlowControlCheck : NSObject
@property (nonatomic, readonly) HJBleFlowControlState state;
@property (nonatomic, readonly) BOOL busy;

// 后台调用；只有写入或连接失败才返回 NO，无有效业务回复仍返回 YES。
- (BOOL)checkWithBLE:(WWBluetoothLE *)ble
         peripheral:(CBPeripheral *)peripheral
     characteristic:(WWCharacteristic *)characteristic
            session:(NSUUID *)session
               type:(CBCharacteristicWriteType)type
            timeout:(NSUInteger)timeout
              error:(NSError **)error;
@end
