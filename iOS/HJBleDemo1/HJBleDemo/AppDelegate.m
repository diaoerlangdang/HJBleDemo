//
//  AppDelegate.m
//  HJBleDemo
//
//  Created by 吴睿智 on 2019/7/21.
//  Copyright © 2019 wuruizhi. All rights reserved.
//

#import "AppDelegate.h"
#import "WiseBaseKit.h"
#import "WiseBle.h"
#import <IQKeyboardManager/IQKeyboardManager.h>
#import <Toast/Toast.h>
#import "ScanViewController.h"
#import <SVProgressHUD/SVProgressHUD.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
    [self initWindows];
    
    return YES;
}

- (void)initWindows
{
    [SVProgressHUD appearance].defaultStyle = SVProgressHUDStyleDark;
    //设置toast位置
    [CSToastManager setDefaultPosition:CSToastPositionCenter];
    
    //设置默认背景颜色
    WWBaseViewController.defaultBgColor = WW_COLOR_HexRGB(0xf8f8f8);
    WWBaseTableViewController.defaultTableViewBgColor = WW_COLOR_HexRGB(0xf8f8f8);
    
    //设置默认状态栏风格
    WWBaseViewController.defaultStatusBarStyle = UIStatusBarStyleLightContent;
    
    //设置NavigationBar
    [UINavigationBar appearance].backgroundColor = WW_COLOR_HexRGB(0x649fea);
    [[UINavigationBar appearance] setBackgroundImage:[UIImage imageWithColor:WW_COLOR_HexRGB(0x649fea)] forBarMetrics:UIBarMetricsDefault];
    [UINavigationBar appearance].barTintColor = WW_COLOR_HexRGB(0x649fea);
    [UINavigationBar appearance].tintColor = [UIColor whiteColor];
    NSDictionary *attr = @{NSFontAttributeName:WW_Font(18), NSForegroundColorAttributeName:WW_COLOR_HexRGB(0xFFFFFF)};
    [[UINavigationBar appearance] setTitleTextAttributes:attr];
    [[UINavigationBar appearance] setShadowImage:[UIImage new]];
    
    //1.创建Window
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.window.backgroundColor = WW_COLOR_HexRGB(0x649fea);
    
    if (@available(iOS 13.0, *)) {
        self.window.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
    } else {
        // Fallback on earlier versions
    }
    
    //首页
    WWNavigationController *homeNav = [[WWNavigationController alloc] initWithRootViewController:[[ScanViewController alloc] init]];
    
    //设置控制器为Window的根控制器
    self.window.rootViewController = homeNav;
    
    //2.设置Window为主窗口并显示出来
    [self.window makeKeyAndVisible];
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


@end
