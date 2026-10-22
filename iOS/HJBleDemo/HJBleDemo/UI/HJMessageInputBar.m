#import "HJMessageInputBar.h"

@interface HJMessageInputBar () <UITextViewDelegate>
@property (nonatomic, strong) UILabel *placeholderLabel;
@property (nonatomic, strong) NSLayoutConstraint *inputHeight;
@end
@implementation HJMessageInputBar
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.backgroundColor = UIColor.whiteColor;
        _inputTextView = [UITextView new];
        _inputTextView.font = [UIFont systemFontOfSize:16];
        _inputTextView.delegate = self;
        _inputTextView.returnKeyType = UIReturnKeySend;
        _inputTextView.layer.borderColor = HJColor(0xcccccc).CGColor;
        _inputTextView.layer.borderWidth = 1;
        _inputTextView.layer.cornerRadius = 6;
        _inputTextView.textContainerInset = UIEdgeInsetsMake(10, 4, 10, 4);
        [self addSubview:_inputTextView];
        _placeholderLabel = HJLabel(UIColor.placeholderTextColor, _inputTextView.font);
        _placeholderLabel.userInteractionEnabled = NO;
        [_inputTextView addSubview:_placeholderLabel];
        [_placeholderLabel mas_makeConstraints:^(MASConstraintMaker *make) {
            make.top.left.equalTo(_inputTextView).offset(10);
        }];
        UIButton *send = [UIButton buttonWithType:UIButtonTypeSystem];
        [send setTitle:@"发送" forState:UIControlStateNormal];
        [send addTarget:self action:@selector(sendText) forControlEvents:UIControlEventTouchUpInside];
        [self addSubview:send];
        [_inputTextView mas_makeConstraints:^(MASConstraintMaker *make) {
            make.left.top.equalTo(self).offset(6);
            make.bottom.equalTo(self).offset(-6);
            make.right.equalTo(send.mas_left).offset(-6);
        }];
        _inputHeight = [_inputTextView.heightAnchor constraintEqualToConstant:44];
        _inputHeight.active = YES;
        [send mas_makeConstraints:^(MASConstraintMaker *make) {
            make.right.equalTo(self).offset(-6);
            make.bottom.equalTo(_inputTextView);
            make.width.mas_equalTo(56);
            make.height.mas_equalTo(44);
        }];
    }
    return self;
}
- (void)setPlaceholder:(NSString *)placeholder {
    _placeholder = [placeholder copy];
    self.placeholderLabel.text = placeholder;
    self.inputTextView.accessibilityLabel = placeholder;
}
- (void)layoutSubviews {
    [super layoutSubviews];
    [self updateInputHeight];
}
- (void)updateInputHeight {
    CGFloat width = CGRectGetWidth(self.inputTextView.bounds);
    if (width <= 0) return;
    CGFloat natural = ceil([self.inputTextView sizeThatFits:CGSizeMake(width, CGFLOAT_MAX)].height);
    CGFloat height = MIN(120, MAX(44, natural));
    if (fabs(self.inputHeight.constant - height) > 0.5) self.inputHeight.constant = height;
    self.inputTextView.scrollEnabled = natural > 120;
    self.placeholderLabel.hidden = self.inputTextView.text.length > 0;
}
- (void)textViewDidChange:(UITextView *)textView { [self updateInputHeight]; }
- (BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    if ([text isEqualToString:@"\n"]) { [self sendText]; return NO; }
    if (self.isHex && text.length) {
        return [[NSPredicate predicateWithFormat:@"SELF MATCHES %@", @"^[0-9A-Fa-f]*$"] evaluateWithObject:text];
    }
    return YES;
}
- (void)sendText { [self.delegate inputBar:self didSendText:self.inputTextView.text]; }
- (void)clearText {
    self.inputTextView.text = @"";
    [self updateInputHeight];
}
@end
