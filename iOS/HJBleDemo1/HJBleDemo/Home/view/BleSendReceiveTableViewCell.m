//
//  BleSendReceiveTableViewCell.m
//  bleDemo
//
//  Created by wurz on 15/4/14.
//  Copyright (c) 2015年 wurz. All rights reserved.
//

#import "BleSendReceiveTableViewCell.h"
#import <WiseBaseKit/WiseBaseKit.h>

@interface BleSendReceiveTableViewCell()

@property (nonatomic, strong) UILabel *titleLab;
@property (nonatomic, strong) UILabel *timeLab;
@property (nonatomic, strong) UILabel *contextLab;

@end

@implementation BleSendReceiveTableViewCell



-(id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    
    if (self) {
        // Initialization code
        [self initSubview];
    }
    return self;
}

#pragma mark 初始化视图
- (void)initSubview
{
    //时间
    _timeLab = [[UILabel alloc] init];
    _timeLab.font = [UIFont systemFontOfSize:14];
    [self addSubview:_timeLab];
    [_timeLab mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self).offset(-10);
        make.top.equalTo(self).offset(10);
    }];
    
    //标题
    _titleLab = [[UILabel alloc] init];
    _titleLab.font = [UIFont systemFontOfSize:14];
    [self addSubview:_titleLab];
    [_titleLab mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self).offset(10);
        make.top.equalTo(self).offset(10);
    }];
    
    //内容
    _contextLab = [[UILabel alloc] init];
    _contextLab.font = [UIFont systemFontOfSize:12];
    _contextLab.numberOfLines = 0;//多行
    [self addSubview:_contextLab];
    [_contextLab mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self).offset(10);
        make.right.equalTo(self).offset(-10);
        make.top.equalTo(self.titleLab.mas_bottom).offset(10);
        make.bottom.equalTo(self).offset(-10);
    }];
}

-(void)setBleData:(BleSendReceiveData *)data
{
    _titleLab.text = data.title;
    _timeLab.text = data.time;
    _contextLab.text = data.context;
    
    if ([data.title isEqualToString:@"发送:"]) {
        _titleLab.textColor = [UIColor blueColor];
        _contextLab.textColor = [UIColor blueColor];
        _timeLab.textColor = [UIColor blueColor];
    }
    else if ([data.title isEqualToString:@"接收:"]) {
        _titleLab.textColor = [UIColor redColor];
        _contextLab.textColor = [UIColor redColor];
        _timeLab.textColor = [UIColor redColor];
    }
    else {
        _titleLab.textColor = [UIColor blackColor];
        _contextLab.textColor = [UIColor blackColor];
        _timeLab.textColor = [UIColor blackColor];
    }
    
}

- (void)awakeFromNib {
    // Initialization code
    [super awakeFromNib];
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

@end
