#import "HJCustomCommand.h"

static NSString * const HJCustomCommandIDKey = @"id";
static NSString * const HJCustomCommandNameKey = @"name";
static NSString * const HJCustomCommandContentKey = @"command";
static NSString * const HJCustomCommandTypeKey = @"type";
static NSString * const HJCustomCommandCreateTimeKey = @"createTime";

@implementation HJCustomCommand

- (instancetype)init
{
    self = [super init];
    if (self) {
        long long now = (long long)([[NSDate date] timeIntervalSince1970] * 1000.0);
        _commandID = [NSString stringWithFormat:@"%lld", now];
        _createTime = now;
        _type = HJCustomCommandTypeHex;
    }
    return self;
}

- (instancetype)initWithName:(NSString *)name
                      command:(NSString *)command
                         type:(HJCustomCommandType)type
{
    self = [self init];
    if (self) {
        _name = [name copy];
        _command = [command copy];
        _type = type;
    }
    return self;
}

- (instancetype)initWithDictionary:(NSDictionary *)dictionary
{
    if (![dictionary isKindOfClass:[NSDictionary class]]) {
        return nil;
    }

    NSString *name = dictionary[HJCustomCommandNameKey];
    NSString *command = dictionary[HJCustomCommandContentKey];
    NSNumber *type = dictionary[HJCustomCommandTypeKey];
    if (![name isKindOfClass:[NSString class]] ||
        ![command isKindOfClass:[NSString class]] ||
        ![type isKindOfClass:[NSNumber class]]) {
        return nil;
    }

    self = [self initWithName:name command:command type:type.integerValue];
    if (self) {
        NSString *commandID = dictionary[HJCustomCommandIDKey];
        NSNumber *createTime = dictionary[HJCustomCommandCreateTimeKey];
        if ([commandID isKindOfClass:[NSString class]] && commandID.length > 0) {
            _commandID = [commandID copy];
        }
        if ([createTime isKindOfClass:[NSNumber class]]) {
            _createTime = createTime.longLongValue;
        }
    }
    return self;
}

- (NSDictionary *)dictionaryRepresentation
{
    return @{
        HJCustomCommandIDKey: self.commandID ?: @"",
        HJCustomCommandNameKey: self.name ?: @"",
        HJCustomCommandContentKey: self.command ?: @"",
        HJCustomCommandTypeKey: @(self.type),
        HJCustomCommandCreateTimeKey: @(self.createTime),
    };
}

- (NSString *)typeString
{
    return self.type == HJCustomCommandTypeHex ? @"HEX" : @"ASCII";
}

@end
