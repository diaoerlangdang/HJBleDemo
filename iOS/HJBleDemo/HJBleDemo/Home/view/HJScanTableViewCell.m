
//
//  HJScanTableViewCell.m
//  HJBleDemo
//
//  Created by 吴睿智 on 2020/3/12.
//  Copyright © 2020 wuruizhi. All rights reserved.
//

#import "HJScanTableViewCell.h"
#import "HJUI.h"

@interface HJScanTableViewCell()

// 名称
@property (nonatomic, strong) UILabel *nameLabel;

// mac
@property (nonatomic, strong) UILabel *macLabel;

// rssi
@property (nonatomic, strong) UILabel *rssiLabel;

@property (nonatomic, strong) UILabel *timeLabel;

@end

@implementation HJScanTableViewCell

- (void)awakeFromNib {
    [super awakeFromNib];
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        
        //初始化view
        [self initView];
        
    }
    
    return self;
}

- (void)initView
{
    _nameLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:18]);
    [self.contentView addSubview:_nameLabel];
    [_nameLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView).offset(15);
        make.top.equalTo(self.contentView).offset(10);
        make.right.lessThanOrEqualTo(self.contentView).offset(-110);
    }];
    
    _rssiLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:14]);
    [self.contentView addSubview:_rssiLabel];
    [_rssiLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.right.equalTo(self.contentView).offset(-15);
        make.centerY.equalTo(_nameLabel);
    }];
    
    _timeLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:14]);
    [self.contentView addSubview:_timeLabel];
    [_timeLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView).offset(15);
        make.top.equalTo(_nameLabel.mas_bottom).offset(10);
        make.right.lessThanOrEqualTo(self.contentView).offset(-15);

    }];
    
    _nameLabel.lineBreakMode = NSLineBreakByTruncatingTail;
    _macLabel = HJLabel(HJColor(0x333333), [UIFont systemFontOfSize:15]);
    [self.contentView addSubview:_macLabel];
    [_macLabel mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.contentView).offset(15);
        make.right.equalTo(self.contentView).offset(-15);
        make.top.equalTo(_timeLabel.mas_bottom).offset(6);
        make.bottom.equalTo(self.contentView).offset(-10);
    }];
    
    
}

- (void)setData:(HJBleScanData *)data
{
    _data = data;
    
    _rssiLabel.text = [NSString stringWithFormat:@"RSSI:%@dB",data.RSSI];
    _nameLabel.text = data.peripheral.name;
    
    _timeLabel.text = [NSString stringWithFormat:@"time: %@", data.formatDateTime];
    if (data.mac.length > 0) {
        _macLabel.text = [NSString stringWithFormat:@"mac: %@", data.mac];
    }
    else {
        _macLabel.text = @"";
    }
    
    [self setMode:data.isEasy];
}

- (void)setMode:(BOOL)isEasy
{
    if (isEasy) {
        self.contentView.backgroundColor = HJColor(0x1FA7D3);
        _nameLabel.textColor = HJColor(0xFFFFFF);
        _rssiLabel.textColor = HJColor(0xFFFFFF);
        _timeLabel.textColor = HJColor(0xFFFFFF);
        _macLabel.textColor = HJColor(0xFFFFFF);
    }
    else {
        self.contentView.backgroundColor = HJColor(0xFFFFFF);
        _nameLabel.textColor = HJColor(0x333333);
        _rssiLabel.textColor = HJColor(0x333333);
        _timeLabel.textColor = HJColor(0x333333);
        _macLabel.textColor = HJColor(0x333333);
    }
}

@end
