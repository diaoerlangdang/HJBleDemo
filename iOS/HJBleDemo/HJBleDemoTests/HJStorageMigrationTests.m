#import <XCTest/XCTest.h>
#import <sqlite3.h>
#import "HJSettingsStore.h"
#import "HJCustomCommandStore.h"
#import "HJCustomCommand.h"

@interface HJStorageMigrationTests : XCTestCase
@property (nonatomic, copy) NSString *directory;
@property (nonatomic, copy) NSString *suite;
@property (nonatomic, strong) NSUserDefaults *defaults;
@end
@implementation HJStorageMigrationTests
- (void)setUp {
    self.directory = [NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString];
    [[NSFileManager defaultManager] createDirectoryAtPath:self.directory withIntermediateDirectories:YES attributes:nil error:NULL];
    self.suite = [@"HJMigrationTests." stringByAppendingString:NSUUID.UUID.UUIDString];
    self.defaults = [[NSUserDefaults alloc] initWithSuiteName:self.suite];
}
- (void)tearDown {
    [[NSFileManager defaultManager] removeItemAtPath:self.directory error:NULL];
    [self.defaults removePersistentDomainForName:self.suite];
}
- (void)writeArchive:(NSData *)archive key:(NSString *)key external:(BOOL)external {
    sqlite3 *db;
    XCTAssertEqual(sqlite3_open([[self.directory stringByAppendingPathComponent:@"manifest.sqlite"] fileSystemRepresentation], &db), SQLITE_OK);
    XCTAssertEqual(sqlite3_exec(db, "CREATE TABLE IF NOT EXISTS manifest (key TEXT PRIMARY KEY, filename TEXT, inline_data BLOB)", NULL, NULL, NULL), SQLITE_OK);
    sqlite3_stmt *stmt;
    XCTAssertEqual(sqlite3_prepare_v2(db, "INSERT OR REPLACE INTO manifest VALUES (?, ?, ?)", -1, &stmt, NULL), SQLITE_OK);
    sqlite3_bind_text(stmt, 1, key.UTF8String, -1, SQLITE_TRANSIENT);
    if (external) {
        NSString *data = [self.directory stringByAppendingPathComponent:@"data"];
        [[NSFileManager defaultManager] createDirectoryAtPath:data withIntermediateDirectories:YES attributes:nil error:NULL];
        XCTAssertTrue([archive writeToFile:[data stringByAppendingPathComponent:key] atomically:YES]);
        sqlite3_bind_text(stmt, 2, key.UTF8String, -1, SQLITE_TRANSIENT);
    } else sqlite3_bind_blob(stmt, 3, archive.bytes, (int)archive.length, SQLITE_TRANSIENT);
    XCTAssertEqual(sqlite3_step(stmt), SQLITE_DONE);
    sqlite3_finalize(stmt);
    sqlite3_close(db);
}
- (void)writeLegacy:(id)value key:(NSString *)key external:(BOOL)external {
    [self writeArchive:[NSKeyedArchiver archivedDataWithRootObject:value requiringSecureCoding:NO error:NULL] key:key external:external];
}
- (NSArray *)legacyCommands {
    return @[@{@"id":@"old-1", @"name":@"查询", @"command":@"FF01", @"type":@0, @"createTime":@12345},
             @{@"id":@"old-2", @"name":@"状态", @"command":@"status", @"type":@1, @"createTime":@23456}];
}
- (void)testSettingsMigrateAllKeysAndKeepNewValues {
    NSDictionary *old = @{@"isBleHex":@NO, @"isClearInput":@YES, @"isAddReturn":@YES, @"isScanFilter":@NO,
                          @"isRespone":@YES, @"dataTotalLen":@512, @"sendDataGap":@30,
                          @"dataMainService":@"AB00", @"dataSendSubService":@"AB01", @"dataReceiveSubService":@"AB02"};
    for (NSString *key in old) [self writeLegacy:old[key] key:key external:NO];
    HJSettingsStore *store = [[HJSettingsStore alloc] initWithDefaults:self.defaults legacyDirectory:self.directory];
    for (NSString *key in old) XCTAssertEqualObjects([store objectForKey:key], old[key]);
    [store setObject:@YES forKey:@"isBleHex"];
    store = [[HJSettingsStore alloc] initWithDefaults:self.defaults legacyDirectory:self.directory];
    XCTAssertEqualObjects([store objectForKey:@"isBleHex"], @YES);
}
- (void)testSettingsRetryAfterUnreadableArchive {
    [self writeArchive:[@"broken" dataUsingEncoding:NSUTF8StringEncoding] key:@"isBleHex" external:NO];
    HJSettingsStore *store = [[HJSettingsStore alloc] initWithDefaults:self.defaults legacyDirectory:self.directory];
    XCTAssertNil([store objectForKey:@"isBleHex"]);
    [self writeLegacy:@NO key:@"isBleHex" external:YES];
    XCTAssertEqualObjects([store objectForKey:@"isBleHex"], @NO);
}
- (void)checkCommandMigrationExternal:(BOOL)external {
    [self writeLegacy:self.legacyCommands key:@"commands.v1" external:external];
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    NSArray *commands = [store allCommands];
    XCTAssertEqual(commands.count, 2U);
    XCTAssertNil(store.lastError);
    for (NSUInteger i = 0; i < commands.count; i++) XCTAssertEqualObjects([commands[i] dictionaryRepresentation], self.legacyCommands[i]);
    XCTAssertTrue([store deleteCommandWithID:@"old-1"]);
    store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    XCTAssertEqual(store.allCommands.count, 1U);
    XCTAssertEqualObjects(store.allCommands.firstObject.commandID, @"old-2");
}
- (void)testInlineLegacyCommandsKeepFieldsAndOrder { [self checkCommandMigrationExternal:NO]; }
- (void)testExternalLegacyCommandsKeepFieldsAndOrder { [self checkCommandMigrationExternal:YES]; }
- (void)testNewInstallCRUDAndReopen {
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    XCTAssertEqual(store.allCommands.count, 0U);
    HJCustomCommand *cmd = [[HJCustomCommand alloc] initWithName:@"查询" command:@"FF" type:0];
    XCTAssertTrue([store addCommand:cmd]);
    cmd.name = @"新名称";
    XCTAssertTrue([store updateCommand:cmd]);
    store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    XCTAssertEqualObjects(store.allCommands.firstObject.name, @"新名称");
    XCTAssertTrue([store clearAllCommands]);
    XCTAssertEqual(store.allCommands.count, 0U);
}
- (void)testCorruptJSONBlocksMutationsAndPreservesBytes {
    NSString *path = [self.directory stringByAppendingPathComponent:@"commands.v1.json"];
    NSData *broken = [@"broken" dataUsingEncoding:NSUTF8StringEncoding];
    [broken writeToFile:path atomically:YES];
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    HJCustomCommand *cmd = [[HJCustomCommand alloc] initWithName:@"查询" command:@"FF" type:0];
    XCTAssertFalse([store addCommand:cmd]);
    XCTAssertFalse([store updateCommand:cmd]);
    XCTAssertFalse([store deleteCommandWithID:@"old-1"]);
    XCTAssertFalse([store clearAllCommands]);
    XCTAssertNotNil(store.lastError);
    XCTAssertEqualObjects([NSData dataWithContentsOfFile:path], broken);
}
- (void)testInvalidLegacyListIsNotPartiallyMigrated {
    [self writeLegacy:@[self.legacyCommands[0], @{@"name":@"bad"}] key:@"commands.v1" external:NO];
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    XCTAssertEqual(store.allCommands.count, 0U);
    XCTAssertNotNil(store.lastError);
    XCTAssertFalse([[NSFileManager defaultManager] fileExistsAtPath:[self.directory stringByAppendingPathComponent:@"commands.v1.json"]]);
    [self writeLegacy:self.legacyCommands key:@"commands.v1" external:NO];
    XCTAssertEqual(store.allCommands.count, 2U);
}
- (void)testMigrationFailureKeepsLegacyAndCanRetry {
    [self writeLegacy:self.legacyCommands key:@"commands.v1" external:NO];
    NSString *path = [self.directory stringByAppendingPathComponent:@"commands.v1.json"];
    [[NSFileManager defaultManager] createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:NULL];
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    XCTAssertEqual(store.allCommands.count, 0U);
    XCTAssertNotNil(store.lastError);
    XCTAssertTrue([[NSFileManager defaultManager] fileExistsAtPath:[self.directory stringByAppendingPathComponent:@"manifest.sqlite"]]);
    [[NSFileManager defaultManager] removeItemAtPath:path error:NULL];
    XCTAssertEqual(store.allCommands.count, 2U);
}
- (void)testSaveReportsFileSystemFailure {
    [[NSFileManager defaultManager] removeItemAtPath:self.directory error:NULL];
    [@"occupied" writeToFile:self.directory atomically:YES encoding:NSUTF8StringEncoding error:NULL];
    HJCustomCommandStore *store = [[HJCustomCommandStore alloc] initWithDirectory:self.directory];
    HJCustomCommand *cmd = [[HJCustomCommand alloc] initWithName:@"查询" command:@"FF" type:0];
    XCTAssertFalse([store addCommand:cmd]);
    XCTAssertNotNil(store.lastError);
}
@end
