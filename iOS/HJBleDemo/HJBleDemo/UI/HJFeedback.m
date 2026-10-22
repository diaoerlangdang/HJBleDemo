#import "HJFeedback.h"
#import <objc/runtime.h>

@implementation UIView (HJFeedback)
- (void)hj_showMessage:(NSString *)message {
    if (!message.length) return;
    [objc_getAssociatedObject(self, @selector(hj_showMessage:)) removeFromSuperview];
    UILabel *label = HJLabel(UIColor.whiteColor, [UIFont systemFontOfSize:15]);
    label.text = message;
    label.numberOfLines = 0;
    label.textAlignment = NSTextAlignmentCenter;
    label.backgroundColor = [UIColor colorWithWhite:0 alpha:0.8];
    label.layer.cornerRadius = 8;
    label.clipsToBounds = YES;
    [self addSubview:label];
    [label mas_makeConstraints:^(MASConstraintMaker *make) {
        make.center.equalTo(self);
        make.height.greaterThanOrEqualTo(@44);
    }];
    [label.widthAnchor constraintLessThanOrEqualToAnchor:self.safeAreaLayoutGuide.widthAnchor constant:-32].active = YES;
    objc_setAssociatedObject(self, @selector(hj_showMessage:), label, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, message);
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [label removeFromSuperview];
        if (objc_getAssociatedObject(self, @selector(hj_showMessage:)) == label)
            objc_setAssociatedObject(self, @selector(hj_showMessage:), nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    });
}
@end
@implementation HJLoadingView
- (instancetype)initWithText:(NSString *)text {
    if ((self = [super initWithFrame:CGRectZero])) {
        self.backgroundColor = [UIColor colorWithWhite:0 alpha:0.8];
        self.layer.cornerRadius = 10;
        self.userInteractionEnabled = NO; // 扫描页导航栏的“取消”始终可点。
        UIActivityIndicatorView *spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleLarge];
        spinner.color = UIColor.whiteColor;
        [spinner startAnimating];
        UILabel *label = HJLabel(UIColor.whiteColor, [UIFont systemFontOfSize:15]);
        label.text = text;
        label.numberOfLines = 0;
        [self addSubview:spinner];
        [self addSubview:label];
        [spinner mas_makeConstraints:^(MASConstraintMaker *make) {
            make.top.equalTo(self).offset(16);
            make.centerX.equalTo(self);
        }];
        [label mas_makeConstraints:^(MASConstraintMaker *make) {
            make.top.equalTo(spinner.mas_bottom).offset(10);
            make.left.equalTo(self).offset(16);
            make.right.bottom.equalTo(self).offset(-16);
        }];
    }
    return self;
}
@end
