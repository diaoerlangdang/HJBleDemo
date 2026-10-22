#import "HJLegacyCacheReader.h"
#import <sqlite3.h>

id HJReadLegacyCache(NSString *directory, NSString *key, NSError **error) {
    NSString *path = [directory stringByAppendingPathComponent:@"manifest.sqlite"];
    if (![[NSFileManager defaultManager] fileExistsAtPath:path]) return nil;
    sqlite3 *database = NULL;
    sqlite3_stmt *statement = NULL;
    NSData *archive = nil;
    int result = sqlite3_open_v2(path.fileSystemRepresentation, &database, SQLITE_OPEN_READONLY, NULL);
    if (result == SQLITE_OK) {
        result = sqlite3_prepare_v2(database, "SELECT filename, inline_data FROM manifest WHERE key = ?", -1, &statement, NULL);
    }
    if (result == SQLITE_OK) {
        sqlite3_bind_text(statement, 1, key.UTF8String, -1, SQLITE_TRANSIENT);
        result = sqlite3_step(statement);
        if (result == SQLITE_ROW) {
            const char *filename = (const char *)sqlite3_column_text(statement, 0);
            if (filename && strlen(filename) > 0) {
                NSString *name = [NSString stringWithUTF8String:filename];
                if ([name isEqualToString:name.lastPathComponent]) {
                    NSString *file = [[directory stringByAppendingPathComponent:@"data"] stringByAppendingPathComponent:name];
                    archive = [NSData dataWithContentsOfFile:file options:0 error:error];
                }
            } else {
                const void *bytes = sqlite3_column_blob(statement, 1);
                int length = sqlite3_column_bytes(statement, 1);
                if (bytes && length > 0) archive = [NSData dataWithBytes:bytes length:length];
            }
        }
    }
    if (statement) sqlite3_finalize(statement);
    if (database) sqlite3_close(database);
    if (result == SQLITE_DONE) return nil; // 键不存在。
    if (result != SQLITE_ROW || !archive.length) {
        if (error && !*error) *error = [NSError errorWithDomain:@"HJLegacyCache" code:result
                                                     userInfo:@{NSLocalizedDescriptionKey:@"旧数据读取失败，原文件已保留"}];
        return nil;
    }
    @try {
        NSKeyedUnarchiver *decoder = [[NSKeyedUnarchiver alloc] initForReadingFromData:archive error:error];
        // 旧 YYCache 使用非 secure 的 Foundation 归档，限定可解档的类型。
        decoder.requiresSecureCoding = NO;
        NSSet *classes = [NSSet setWithObjects:NSArray.class, NSDictionary.class, NSString.class,
                          NSNumber.class, NSData.class, NSDate.class, NSNull.class, nil];
        id value = [decoder decodeObjectOfClasses:classes forKey:NSKeyedArchiveRootObjectKey];
        [decoder finishDecoding];
        if (!value && error && !*error) *error = decoder.error ?: [NSError errorWithDomain:@"HJLegacyCache" code:-1 userInfo:nil];
        return value;
    } @catch (NSException *exception) {
        if (error) *error = [NSError errorWithDomain:@"HJLegacyCache" code:-1
                                           userInfo:@{NSLocalizedDescriptionKey:@"旧数据解档失败，原文件已保留"}];
        return nil;
    }
}
