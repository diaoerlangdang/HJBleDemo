#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, HJCustomCommandType) {
    HJCustomCommandTypeHex = 0,
    HJCustomCommandTypeASCII = 1,
};

@interface HJCustomCommand : NSObject

@property (nonatomic, copy) NSString *commandID;
@property (nonatomic, copy) NSString *name;
@property (nonatomic, copy) NSString *command;
@property (nonatomic, assign) HJCustomCommandType type;
@property (nonatomic, assign) long long createTime;

- (instancetype)initWithName:(NSString *)name
                      command:(NSString *)command
                         type:(HJCustomCommandType)type;
- (instancetype)initWithDictionary:(NSDictionary *)dictionary;
- (NSDictionary *)dictionaryRepresentation;

- (NSString *)typeString;

@end

NS_ASSUME_NONNULL_END
