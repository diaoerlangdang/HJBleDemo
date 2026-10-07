#import "HJCustomCommandCodec.h"

@implementation HJCustomCommandCodec

+ (NSData *)dataForCommand:(HJCustomCommand *)command
               appendReturn:(BOOL)appendReturn
                      error:(NSString **)error
{
    if (command.command.length == 0) {
        if (error) *error = @"指令内容不能为空";
        return nil;
    }

    if (command.type == HJCustomCommandTypeASCII) {
        NSString *text = command.command;
        if (appendReturn) {
            text = [text stringByAppendingString:@"\r\n"];
        }
        return [text dataUsingEncoding:NSUTF8StringEncoding];
    }

    NSString *hex = [command.command stringByReplacingOccurrencesOfString:@" " withString:@""];
    hex = [hex stringByReplacingOccurrencesOfString:@"-" withString:@""];
    hex = [hex stringByReplacingOccurrencesOfString:@":" withString:@""];
    hex = [hex stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    hex = [hex stringByReplacingOccurrencesOfString:@"\r" withString:@""];

    if (hex.length == 0) {
        if (error) *error = @"HEX指令不能为空";
        return nil;
    }
    if (hex.length % 2 != 0) {
        if (error) *error = @"HEX指令长度必须为偶数";
        return nil;
    }

    NSMutableData *data = [NSMutableData dataWithCapacity:hex.length / 2 + (appendReturn ? 2 : 0)];
    for (NSUInteger index = 0; index < hex.length; index += 2) {
        unichar high = [hex characterAtIndex:index];
        unichar low = [hex characterAtIndex:index + 1];
        int highValue = [self hexValue:high];
        int lowValue = [self hexValue:low];
        if (highValue < 0 || lowValue < 0) {
            if (error) *error = @"HEX指令只能包含0-9和A-F";
            return nil;
        }
        uint8_t byte = (uint8_t)((highValue << 4) | lowValue);
        [data appendBytes:&byte length:1];
    }

    if (appendReturn) {
        const uint8_t returnBytes[] = {0x0D, 0x0A};
        [data appendBytes:returnBytes length:sizeof(returnBytes)];
    }
    return data;
}

+ (int)hexValue:(unichar)character
{
    if (character >= '0' && character <= '9') return (int)(character - '0');
    if (character >= 'A' && character <= 'F') return (int)(character - 'A' + 10);
    if (character >= 'a' && character <= 'f') return (int)(character - 'a' + 10);
    return -1;
}

@end
