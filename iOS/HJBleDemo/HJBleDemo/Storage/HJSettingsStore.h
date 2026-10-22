#import <Foundation/Foundation.h>

@interface HJSettingsStore : NSObject
- (instancetype)initWithDefaults:(NSUserDefaults *)defaults legacyDirectory:(NSString *)directory;
- (id)objectForKey:(NSString *)key;
- (void)setObject:(id)object forKey:(NSString *)key;
@end
