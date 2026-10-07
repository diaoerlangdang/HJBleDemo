#import <Foundation/Foundation.h>

@class HJCustomCommand;

NS_ASSUME_NONNULL_BEGIN

@interface HJCustomCommandStore : NSObject

- (NSArray<HJCustomCommand *> *)allCommands;
- (BOOL)addCommand:(HJCustomCommand *)command;
- (BOOL)updateCommand:(HJCustomCommand *)command;
- (BOOL)deleteCommandWithID:(NSString *)commandID;
- (BOOL)clearAllCommands;

@end

NS_ASSUME_NONNULL_END
