#import "HJCustomCommandStore.h"
#import "HJCustomCommand.h"
#import "HJLegacyCacheReader.h"

static NSString * const HJCustomCommandCacheKey = @"commands.v1";
static NSString * const HJCustomCommandIDKey = @"id";

@interface HJCustomCommandStore ()
@property (nonatomic, copy) NSString *directory;
@property (nonatomic, copy) NSString *jsonPath;
@property (nonatomic, strong, readwrite) NSError *lastError;
@end

@implementation HJCustomCommandStore

- (instancetype)init
{
    NSString *support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES).firstObject;
    return [self initWithDirectory:[support stringByAppendingPathComponent:@"HJBleDemo/CustomCommands"]];
}

- (instancetype)initWithDirectory:(NSString *)directory
{
    if ((self = [super init])) {
        _directory = [directory copy];
        _jsonPath = [directory stringByAppendingPathComponent:@"commands.v1.json"];
    }
    return self;
}

- (NSArray<HJCustomCommand *> *)allCommands
{
    NSArray *storedCommands = [self storedDictionaries];
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
    if (!commands) return NO;
    [commands addObject:command.dictionaryRepresentation];
    return [self saveDictionaries:commands];
}

- (BOOL)updateCommand:(HJCustomCommand *)command
{
    if (![self isValidCommand:command] || command.commandID.length == 0) {
        return NO;
    }

    NSMutableArray *commands = [self storedDictionaries].mutableCopy;
    if (!commands) return NO;
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
    if (!commands) return NO;
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
    if (![self storedDictionaries]) return NO;
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
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:commands options:0 error:&error];
    BOOL saved = data != nil && [[NSFileManager defaultManager] createDirectoryAtPath:self.directory
                            withIntermediateDirectories:YES attributes:nil error:&error] &&
        [data writeToFile:self.jsonPath options:NSDataWritingAtomic error:&error];
    self.lastError = error;
    return saved;
}

- (NSArray<NSDictionary *> *)storedDictionaries
{
    self.lastError = nil;
    NSError *error = nil;
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:self.jsonPath];
    id value;
    if (exists) {
        NSData *data = [NSData dataWithContentsOfFile:self.jsonPath options:0 error:&error];
        if (data) value = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    } else {
        value = HJReadLegacyCache(self.directory, HJCustomCommandCacheKey, &error);
        if (!value && !error) return @[];
    }
    BOOL valid = !error && [value isKindOfClass:NSArray.class];
    if (valid) {
        for (id dictionary in value) {
            if (![[HJCustomCommand alloc] initWithDictionary:dictionary]) {
                valid = NO;
                break;
            }
        }
    }
    if (!valid) {
        self.lastError = error ?: [NSError errorWithDomain:@"HJCustomCommandStore" code:1
                       userInfo:@{NSLocalizedDescriptionKey:@"指令数据读取失败，原文件已保留"}];
        return nil; // 禁止以空列表覆盖不可读的数据。
    }
    if (!exists && ![self saveDictionaries:value]) return nil;
    return value;
}

@end
