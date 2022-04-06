/************************************************************
  *  * EaseMob CONFIDENTIAL 
  * __________________ 
  * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved. 
  *  
  * NOTICE: All information contained herein is, and remains 
  * the property of EaseMob Technologies.
  * Dissemination of this information or reproduction of this material 
  * is strictly forbidden unless prior written permission is obtained
  * from EaseMob Technologies.
  */

#import "FacialView.h"

@interface FacialView ()

@end

@implementation FacialView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        //_faces = [Emoji allEmoji];
        _faces = [[NSArray alloc]initWithObjects:@"A",@"B",@"C",@"D",@"E",@"F", nil];
    }
    return self;
}


//给faces设置位置
-(void)loadFacialView:(int)page size:(CGSize)size
{
	int maxRow = 3;
    int maxCol = 3;
    CGFloat itemWidth = (self.frame.size.width-maxCol+1) / maxCol;
    CGFloat itemHeight = (self.frame.size.height-maxRow+1) / maxRow;
    
    UIButton *deleteButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [deleteButton setBackgroundColor:[UIColor whiteColor]];
    [deleteButton setFrame:CGRectMake((maxCol - 1) * (itemWidth+1), (maxRow - 1) * (itemHeight+1), itemWidth, itemHeight)];
    [deleteButton setImage:[UIImage imageNamed:@"netAddressBackspace"] forState:UIControlStateNormal];
    deleteButton.tag = 10000;
    [deleteButton addTarget:self action:@selector(selected:) forControlEvents:UIControlEventTouchUpInside];
    [self addSubview:deleteButton];
    
    //空白
    UILabel *space = [[UILabel alloc] init];
    space.backgroundColor = [UIColor grayColor];
    [space setFrame:CGRectMake((maxCol - 2) * (itemWidth+1), (maxRow - 1) * (itemHeight+1), itemWidth, itemHeight)];
    [self addSubview:space];
    
    space = [[UILabel alloc] init];
    space.backgroundColor = [UIColor grayColor];
    [space setFrame:CGRectMake((maxCol - 3) * (itemWidth+1), (maxRow - 1) * (itemHeight+1), itemWidth, itemHeight)];
    [self addSubview:space];
    
    
    for (int row = 0; row < maxRow; row++) {
        for (int col = 0; col < maxCol; col++) {
            int index = row * maxCol + col;
            if (index < [_faces count]) {
                UIButton *button = [UIButton buttonWithType:UIButtonTypeCustom];
                [button setBackgroundColor:[UIColor whiteColor]];
                [button setFrame:CGRectMake(col * (itemWidth+1), row * (itemHeight+1), itemWidth, itemHeight)];
                [button.titleLabel setFont:[UIFont fontWithName:@"AppleColorEmoji" size:29.0]];
                [button setTitle: [_faces objectAtIndex:(row * maxCol + col)] forState:UIControlStateNormal];
                [button setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
                button.tag = row * maxCol + col;
                [button addTarget:self action:@selector(selected:) forControlEvents:UIControlEventTouchUpInside];
                [self addSubview:button];
            }
            else{
                break;
            }
        }
    }
}


-(void)selected:(UIButton*)bt
{
    if (bt.tag == 10000 && _delegate) {
        [_delegate deleteSelected:nil];
    }else{
        NSString *str = [_faces objectAtIndex:bt.tag];
        if (_delegate) {
            [_delegate selectedFacialView:str];
        }
    }
}

- (void)sendAction:(id)sender
{
    if (_delegate) {
        [_delegate sendFace];
    }
}

@end
