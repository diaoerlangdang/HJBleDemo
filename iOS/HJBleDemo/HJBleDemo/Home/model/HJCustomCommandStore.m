#import "HJCustomCommandStore.h"
#import "HJCustomCommand.h"
#import <YYCache/YYCache.h>

static NSString * const HJCustomCommandCacheKey = @"commands.v1";
static NSString * const HJCustomCommandIDKey = @"id";

@interface HJCustomCommandStore ()
@property (nonatomic, strong) YYCache *cache;
@end

@implementation HJCustomCommandStore

- (instancetype)init
{
    self = [super init];
    if (self) {
        NSString *applicationSupport = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory,
                                                                              NSUserDomainMask,
                                                                              YES) firstObject];
        NSString *directory = [applicationSupport stringByAppendingPathComponent:@"HJBleDemo/CustomCommands"];
        [[NSFileManager defaultManager] createDirectoryAtPath:directory
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:nil];
        _cache = [YYCache cacheWithPath:directory];
    }
    return self;
}

- (NSArray<HJCustomCommand *> *)allCommands
{
    NSArray *storedCommands = (NSArray *)[self.cache objectForKey:HJCustomCommandCacheKey];
    if (![storedCommands isKindOfClass:[NSArray class]]) {
        return @[];
    }

    NSMutableArray<HJCustomCommand *> *commands = [NSMutableArray arrayWithCapacity:storedCommands.count];
    for (NSDictionary *dictionary in storedCommands) {
        HJCustomCommand *command = [[HJCustomCommand alloc] initWithDictionary:dictionary];
        if (command) {
            [commands addObject:command];
        }
    }
    return [commands copy];
}

- (BOOL)addCommand:(HJCustomCommand *)command
{
    if (![self isValidCommand:command]) {
        return NO;
    }

    NSMutableArray *commands = [self storedDictionaries].mutableCopy;
    [commands addObject:command.dictionaryRepresentation];
    return [self saveDictionaries:commands];
}

- (BOOL)updateCommand:(HJCustomCommand *)command
{
    if (![self isValidCommand:command] || command.commandID.length == 0) {
        return NO;
    }

    NSMutableArray *commands = [self storedDictionaries].mutableCopy;
    for (NSUInteger index = 0; index < commands.count; index++) {
        NSDictionary *dictionary = commands[index];
        if ([dictionary[HJCustomCommandIDKey] isKindOfClass:[NSString class]] &&
            [dictionary[HJCustomCommandIDKey] isEqualToString:command.commandID]) {
            commands[index] = command.dictionaryRepresentation;
            return [self saveDictionaries:commands];
        }
    }
    return NO;
}

- (BOOL)deleteCommandWithID:(NSString *)commandID
{
    if (commandID.length == 0) {
        return NO;
    }

    NSMutableArray *commands = [self storedDictionaries].mutableCopy;
    for (NSUInteger index = 0; index < commands.count; index++) {
        NSDictionary *dictionary = commands[index];
        if ([dictionary[HJCustomCommandIDKey] isKindOfClass:[NSString class]] &&
            [dictionary[HJCustomCommandIDKey] isEqualToString:commandID]) {
            [commands removeObjectAtIndex:index];
            return [self saveDictionaries:commands];
        }
    }
    return NO;
}

- (BOOL)clearAllCommands
{
    return [self saveDictionaries:@[]];
}

- (BOOL)isValidCommand:(HJCustomCommand *)command
{
    return command != nil &&
           command.name.length > 0 &&
           command.command.length > 0;
}

- (BOOL)saveDictionaries:(NSArray<NSDictionary *> *)commands
{
    if (!self.cache) {
        return NO;
    }
    [self.cache setObject:commands forKey:HJCustomCommandCacheKey];
    return YES;
}

- (NSArray<NSDictionary *> *)storedDictionaries
{
    NSArray *storedCommands = (NSArray *)[self.cache objectForKey:HJCustomCommandCacheKey];
    return [storedCommands isKindOfClass:[NSArray class]] ? storedCommands : @[];
}

@end
