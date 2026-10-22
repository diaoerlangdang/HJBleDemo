#import <Foundation/Foundation.h>

@class HJCustomCommand;

NS_ASSUME_NONNULL_BEGIN

@interface HJCustomCommandStore : NSObject

@property (nonatomic, strong, readonly, nullable) NSError *lastError;
- (instancetype)initWithDirectory:(NSString *)directory;
- (NSArray<HJCustomCommand *> *)allCommands;
- (BOOL)addCommand:(HJCustomCommand *)command;
- (BOOL)updateCommand:(HJCustomCommand *)command;
- (BOOL)deleteCommandWithID:(NSString *)commandID;
- (BOOL)clearAllCommands;

@end

NS_ASSUME_NONNULL_END
