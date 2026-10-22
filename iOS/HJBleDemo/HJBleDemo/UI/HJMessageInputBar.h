#import "HJUI.h"
@class HJMessageInputBar;
@protocol HJMessageInputBarDelegate <NSObject>
- (void)inputBar:(HJMessageInputBar *)bar didSendText:(NSString *)text;
@end
@interface HJMessageInputBar : UIView
@property (nonatomic, weak) id<HJMessageInputBarDelegate> delegate;
@property (nonatomic, strong, readonly) UITextView *inputTextView;
@property (nonatomic, copy) NSString *placeholder;
@property (nonatomic, assign) BOOL isHex;
- (void)clearText;
@end
