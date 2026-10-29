#import "HJBleFlowControlCheck.h"
#import "HJBleDevicePageLifecycle.h"

@implementation HJBleFlowControlCheck

- (BOOL)checkWithBLE:(WWBluetoothLE *)ble
         peripheral:(CBPeripheral *)peripheral
     characteristic:(WWCharacteristic *)characteristic
            session:(NSUUID *)session
               type:(CBCharacteristicWriteType)type
            timeout:(NSUInteger)timeout
              error:(NSError **)error
{
    _state = HJBleFlowControlUnknown;
    _busy = NO;
    NSCondition *condition = [[NSCondition alloc] init];
    __block HJBleFlowControlState state = HJBleFlowControlUnknown;
    __block BOOL busy = NO;
    __block BOOL accepting = YES;
    // 先监听再写入，避免丢失早于写入完成回调的回复。
    id observer = [[NSNotificationCenter defaultCenter] addObserverForName:WWBLEDidReceiveDataNotification
                                                                  object:ble queue:nil usingBlock:^(NSNotification *note) {
        if (!HJBleConnectionEventMatches(peripheral, session, note.userInfo[WWBLEPeripheralKey], note.userInfo[WWBLESessionKey]) ||
            ![characteristic isEqual:note.userInfo[WWBLECharacteristicKey]]) {
            return;
        }
        NSString *reply = [[NSString alloc] initWithData:note.userInfo[WWBLEDataKey] encoding:NSUTF8StringEncoding];
        [condition lock];
        if (accepting) {
            if (state == HJBleFlowControlUnknown) {
                if ([reply isEqualToString:@"<rd_uart_fc=1>"]) { state = HJBleFlowControlEnabled; }
                else if ([reply isEqualToString:@"<rd_uart_fc=0>"]) { state = HJBleFlowControlDisabled; }
            }
            if ([reply isEqualToString:@"<HJ_BLE_BUSY_STOP_SEND>"]) { busy = YES; }
            else if ([reply isEqualToString:@"<HJ_BLE_IDLE_START_SEND>"]) { busy = NO; }
            [condition signal];
        }
        [condition unlock];
    }];

    // 仅等待写入完成，不在库中登记业务响应等待，业务超时不会阻塞后续发送。
    BOOL sent = [ble synchronizedSend:peripheral characteristic:characteristic
                               value:[@"<RD_UART_FC>" dataUsingEncoding:NSUTF8StringEncoding]
                                type:type session:session packetSize:20 time:timeout error:error];
    NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:timeout / 1000.0];
    [condition lock];
    while (sent && state == HJBleFlowControlUnknown && deadline.timeIntervalSinceNow > 0 &&
           [ble isSessionActive:session peripheral:peripheral]) {
        // 取消会使会话立即失效，短间隔检查避免退出页面后仍等待整个查询超时。
        [condition waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:MIN(0.05, deadline.timeIntervalSinceNow)]];
    }
    accepting = NO;
    _state = state;
    _busy = busy;
    [condition unlock];
    [[NSNotificationCenter defaultCenter] removeObserver:observer];

    if (sent && ![ble isSessionActive:session peripheral:peripheral]) {
        if (error != NULL) { *error = [NSError errorWithDomain:WWBLEErrorDomain code:WWBLEErrorDisconnected userInfo:nil]; }
        return NO;
    }
    return sent;
}

@end
