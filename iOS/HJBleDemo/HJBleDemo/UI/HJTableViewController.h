#import "HJUI.h"

@interface HJTableViewController : UIViewController <UITableViewDataSource, UITableViewDelegate>
@property (nonatomic, strong, readonly) UITableView *tableView;
@end
