#import "SceneDelegate.h"
#import "ScanViewController.h"
#import "HJUI.h"

@implementation SceneDelegate
- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions {
    if (![scene isKindOfClass:UIWindowScene.class]) return;
    self.window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
    self.window.backgroundColor = HJColor(0x649fea);
    self.window.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
    self.window.rootViewController = [[UINavigationController alloc] initWithRootViewController:[ScanViewController new]];
    [self.window makeKeyAndVisible];
}
@end
