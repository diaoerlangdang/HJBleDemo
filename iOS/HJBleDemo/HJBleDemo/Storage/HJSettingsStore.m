#import "HJSettingsStore.h"
#import "HJLegacyCacheReader.h"

@interface HJSettingsStore ()
@property (nonatomic, strong) NSUserDefaults *defaults;
@property (nonatomic, copy) NSString *legacyDirectory;
@end

@implementation HJSettingsStore
- (instancetype)initWithDefaults:(NSUserDefaults *)defaults legacyDirectory:(NSString *)directory {
    if ((self = [super init])) {
        _defaults = defaults;
        _legacyDirectory = [directory copy];
    }
    return self;
}
- (id)objectForKey:(NSString *)key {
    NSString *newKey = [@"HJSettings." stringByAppendingString:key];
    id value = [self.defaults objectForKey:newKey];
    if (value) return value;
    NSError *error = nil;
    value = HJReadLegacyCache(self.legacyDirectory, key, &error);
    Class expected = [key hasSuffix:@"Service"] ? NSString.class : NSNumber.class;
    if (!error && [value isKindOfClass:expected]) {
        [self.defaults setObject:value forKey:newKey];
        return value;
    }
    // 不写默认值作为迁移结果，旧文件暂时无法读取时下次仍可重试。
    return nil;
}
- (void)setObject:(id)object forKey:(NSString *)key {
    [self.defaults setObject:object forKey:[@"HJSettings." stringByAppendingString:key]];
}
@end
