#import <UIKit/UIKit.h>
#import <Masonry/Masonry.h>

static inline UIColor *HJColor(NSUInteger rgb) {
    return [UIColor colorWithRed:((rgb >> 16) & 0xff) / 255.0
                          green:((rgb >> 8) & 0xff) / 255.0
                           blue:(rgb & 0xff) / 255.0 alpha:1];
}

static inline UILabel *HJLabel(UIColor *color, UIFont *font) {
    UILabel *label = [UILabel new];
    label.textColor = color;
    label.font = font;
    return label;
}

static inline UIImage *HJColorImage(UIColor *color) {
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(1, 1)];
    return [renderer imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [color setFill];
        [context fillRect:CGRectMake(0, 0, 1, 1)];
    }];
}
