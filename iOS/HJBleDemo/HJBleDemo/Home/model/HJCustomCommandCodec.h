#import <Foundation/Foundation.h>
#import "HJCustomCommand.h"

NS_ASSUME_NONNULL_BEGIN

@interface HJCustomCommandCodec : NSObject

+ (nullable NSData *)dataForCommand:(HJCustomCommand *)command
                        appendReturn:(BOOL)appendReturn
                               error:(NSString * _Nullable * _Nullable)error;

@end

NS_ASSUME_NONNULL_END
