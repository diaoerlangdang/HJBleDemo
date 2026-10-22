#import "HJUI.h"
@interface UIView (HJFeedback)
- (void)hj_showMessage:(NSString *)message;
@end
@interface HJLoadingView : UIView
- (instancetype)initWithText:(NSString *)text;
@end
