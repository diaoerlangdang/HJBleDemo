#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN
// 仅用于跨版本升级；不修改或删除旧 YYCache 的数据库和归档。
FOUNDATION_EXPORT id _Nullable HJReadLegacyCache(NSString *directory, NSString *key, NSError **error);
NS_ASSUME_NONNULL_END
