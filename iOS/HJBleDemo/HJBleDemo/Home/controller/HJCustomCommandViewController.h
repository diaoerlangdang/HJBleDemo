#import <UIKit/UIKit.h>

@class HJCustomCommand;
@class HJCustomCommandStore;

NS_ASSUME_NONNULL_BEGIN

typedef void (^HJCustomCommandExecuteHandler)(HJCustomCommand *command);

@interface HJCustomCommandViewController : UIViewController

@property (nonatomic, copy, nullable) HJCustomCommandExecuteHandler onCommandExecute;

- (instancetype)initWithStore:(HJCustomCommandStore *)store;

@end

NS_ASSUME_NONNULL_END
