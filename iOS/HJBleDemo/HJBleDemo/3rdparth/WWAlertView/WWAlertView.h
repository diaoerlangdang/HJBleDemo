//
//  WWAlertView.h
//  JZBCommonViewDemo
//
//  Created by 吴睿智 on 2020/3/4.
//  Copyright © 2020 wuruizhi. All rights reserved.
//

#import <MMPopupView/MMPopupView.h>


typedef void(^WWPopupInputHandler)(NSString *text);

@interface WWAlertView : MMPopupView

@property (nonatomic, assign) NSUInteger maxInputLength;    // default is 0. Means no length limit.

@property(nonatomic) UIKeyboardType keyboardType;                         // default is UIKeyboardTypeDefault

@property (nonatomic, strong) NSString *inputText;

- (instancetype) initWithInputTitle:(NSString*)title
                             detail:(NSString*)detail
                        placeholder:(NSString*)inputPlaceholder
                            handler:(WWPopupInputHandler)inputHandler;

- (instancetype) initWithTipsTitle:(NSString*)title
                               detail:(NSString*)detail;

- (instancetype) initWithConfirmTitle:(NSString*)title detail:(NSString*)detail sureBlock:(MMPopupBlock)sureBlock cancelBlock:(MMPopupBlock)cancelBlock;

- (instancetype) initWithTitle:(NSString*)title
                        detail:(NSString*)detail
                         items:(NSArray*)items;

@end

/**
 *  Global Configuration of WWAlertView.
 */
@interface WWAlertViewConfig : NSObject

+ (WWAlertViewConfig*) globalConfig;

@property (nonatomic, assign) CGFloat width;                // Default is 275.
@property (nonatomic, assign) CGFloat buttonHeight;         // Default is 50.
@property (nonatomic, assign) CGFloat innerMargin;          // Default is 25.
@property (nonatomic, assign) CGFloat cornerRadius;         // Default is 5.

@property (nonatomic, assign) CGFloat titleFontSize;        // Default is 18.
@property (nonatomic, assign) CGFloat detailFontSize;       // Default is 14.
@property (nonatomic, assign) CGFloat buttonFontSize;       // Default is 17.

@property (nonatomic, strong) UIColor *backgroundColor;     // Default is #FFFFFF.
@property (nonatomic, strong) UIColor *titleColor;          // Default is #333333.
@property (nonatomic, strong) UIColor *detailColor;         // Default is #333333.
@property (nonatomic, strong) UIColor *splitColor;          // Default is #CCCCCC.

@property (nonatomic, strong) UIColor *itemNormalColor;     // Default is #333333. effect with MMItemTypeNormal
@property (nonatomic, strong) UIColor *itemHighlightColor;  // Default is #1B70FF. effect with MMItemTypeHighlight
@property (nonatomic, strong) UIColor *itemPressedColor;    // Default is #EFEDE7.

@property (nonatomic, strong) NSString *defaultTextOK;      // Default is "好".
@property (nonatomic, strong) NSString *defaultTextCancel;  // Default is "取消".
@property (nonatomic, strong) NSString *defaultTextConfirm; // Default is "确定".

@end
