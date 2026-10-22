#import "HJScrollViewController.h"

@implementation HJScrollViewController
- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = HJColor(0xf8f8f8);
    self.navigationItem.backButtonDisplayMode = UINavigationItemBackButtonDisplayModeMinimal;
    _scrollView = [UIScrollView new];
    _scrollView.keyboardDismissMode = UIScrollViewKeyboardDismissModeInteractive;
    [self.view addSubview:_scrollView];
    _contentView = [UIView new];
    [_scrollView addSubview:_contentView];
    [_scrollView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.view.mas_safeAreaLayoutGuideTop);
        make.left.equalTo(self.view.mas_safeAreaLayoutGuideLeft);
        make.right.equalTo(self.view.mas_safeAreaLayoutGuideRight);
    }];
    // iOS 15+：布局跟随当前窗口的停靠键盘，无需全局键盘管理器。
    [_scrollView.bottomAnchor constraintEqualToAnchor:self.view.keyboardLayoutGuide.topAnchor].active = YES;
    _contentView.translatesAutoresizingMaskIntoConstraints = NO;
    [NSLayoutConstraint activateConstraints:@[
        [_contentView.leadingAnchor constraintEqualToAnchor:_scrollView.contentLayoutGuide.leadingAnchor],
        [_contentView.trailingAnchor constraintEqualToAnchor:_scrollView.contentLayoutGuide.trailingAnchor],
        [_contentView.topAnchor constraintEqualToAnchor:_scrollView.contentLayoutGuide.topAnchor],
        [_contentView.bottomAnchor constraintEqualToAnchor:_scrollView.contentLayoutGuide.bottomAnchor],
        [_contentView.widthAnchor constraintEqualToAnchor:_scrollView.frameLayoutGuide.widthAnchor],
    ]];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(revealEditingField:)
                                                 name:UITextFieldTextDidBeginEditingNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(revealEditingField:)
                                                 name:UITextViewTextDidBeginEditingNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardDidChange:)
                                                 name:UIKeyboardDidChangeFrameNotification object:nil];
}
- (void)revealEditingField:(NSNotification *)notification {
    UIView *field = notification.object;
    if ([field isDescendantOfView:self.contentView]) {
        [self revealFirstResponderInView:field];
    }
}
- (void)keyboardDidChange:(NSNotification *)notification {
    [self.view layoutIfNeeded];
    for (UIView *view in self.contentView.subviews) {
        [self revealFirstResponderInView:view];
    }
}
- (void)revealFirstResponderInView:(UIView *)view {
    if (view.isFirstResponder) {
        CGRect visible = view.bounds;
        if ([view isKindOfClass:UITextView.class]) {
            UITextView *textView = (UITextView *)view;
            if (textView.selectedTextRange) visible = [textView caretRectForPosition:textView.selectedTextRange.end];
        }
        visible = CGRectInset([view convertRect:visible toView:self.contentView], -8, -8);
        [self.scrollView scrollRectToVisible:visible animated:YES];
        return;
    }
    for (UIView *child in view.subviews) [self revealFirstResponderInView:child];
}
- (void)dealloc { [[NSNotificationCenter defaultCenter] removeObserver:self]; }
@end
