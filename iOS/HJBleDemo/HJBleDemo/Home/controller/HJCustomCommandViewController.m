#import "HJCustomCommandViewController.h"
#import "HJCustomCommand.h"
#import "HJCustomCommandStore.h"
#import "HJCustomCommandCodec.h"
#import "HJScrollViewController.h"
#import "HJFeedback.h"

@interface HJCustomCommandEditorViewController : HJScrollViewController

- (instancetype)initWithCommand:(HJCustomCommand * _Nullable)command
                      completion:(BOOL (^)(HJCustomCommand *command))completion;

@end

@interface HJCustomCommandCell : UITableViewCell

@property (nonatomic, copy) void (^editHandler)(void);
@property (nonatomic, copy) void (^deleteHandler)(void);

- (void)configureWithCommand:(HJCustomCommand *)command;

@end

@interface HJCustomCommandViewController () <UITableViewDataSource, UITableViewDelegate>

@property (nonatomic, strong) HJCustomCommandStore *store;
@property (nonatomic, strong) NSMutableArray<HJCustomCommand *> *commands;
@property (nonatomic, strong) UITableView *tableView;

@end

@implementation HJCustomCommandViewController

- (instancetype)initWithStore:(HJCustomCommandStore *)store
{
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        _store = store ?: [[HJCustomCommandStore alloc] init];
        _commands = [[self.store allCommands] mutableCopy] ?: [NSMutableArray array];
        self.modalPresentationStyle = UIModalPresentationOverFullScreen;
        self.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.5];

    UIButton *dismissButton = [UIButton buttonWithType:UIButtonTypeCustom];
    dismissButton.translatesAutoresizingMaskIntoConstraints = NO;
    dismissButton.accessibilityLabel = @"关闭自定义指令";
    [dismissButton addTarget:self action:@selector(close) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:dismissButton];
    [NSLayoutConstraint activateConstraints:@[
        [dismissButton.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [dismissButton.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [dismissButton.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [dismissButton.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
    ]];

    UIView *panel = [[UIView alloc] initWithFrame:CGRectZero];
    panel.translatesAutoresizingMaskIntoConstraints = NO;
    panel.backgroundColor = [UIColor whiteColor];
    [self.view addSubview:panel];
    [NSLayoutConstraint activateConstraints:@[
        [panel.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [panel.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [panel.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
        [panel.widthAnchor constraintLessThanOrEqualToAnchor:self.view.widthAnchor multiplier:0.92],
        [panel.widthAnchor constraintLessThanOrEqualToConstant:520.0],
    ]];

    NSLayoutConstraint *preferredWidth = [panel.widthAnchor constraintEqualToAnchor:self.view.widthAnchor multiplier:0.85];
    preferredWidth.priority = UILayoutPriorityDefaultHigh;
    preferredWidth.active = YES;

    UILabel *titleLabel = [[UILabel alloc] initWithFrame:CGRectZero];
    titleLabel.translatesAutoresizingMaskIntoConstraints = NO;
    titleLabel.text = @"自定义指令";
    titleLabel.font = [UIFont boldSystemFontOfSize:19.0];
    titleLabel.textColor = [UIColor darkTextColor];
    [panel addSubview:titleLabel];

    UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeSystem];
    closeButton.translatesAutoresizingMaskIntoConstraints = NO;
    [closeButton setTitle:@"关闭" forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(close) forControlEvents:UIControlEventTouchUpInside];
    [panel addSubview:closeButton];

    UIButton *addButton = [UIButton buttonWithType:UIButtonTypeSystem];
    addButton.translatesAutoresizingMaskIntoConstraints = NO;
    [addButton setTitle:@"添加指令" forState:UIControlStateNormal];
    [addButton addTarget:self action:@selector(addCommand) forControlEvents:UIControlEventTouchUpInside];
    [panel addSubview:addButton];

    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.estimatedRowHeight = 90.0;
    self.tableView.tableFooterView = [UIView new];
    [self.tableView registerClass:[HJCustomCommandCell class] forCellReuseIdentifier:@"HJCustomCommandCell"];
    [panel addSubview:self.tableView];

    UILayoutGuide *safeArea = panel.safeAreaLayoutGuide;
    [NSLayoutConstraint activateConstraints:@[
        [titleLabel.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor constant:16.0],
        [titleLabel.topAnchor constraintEqualToAnchor:safeArea.topAnchor constant:14.0],
        [titleLabel.heightAnchor constraintEqualToConstant:24.0],
        [closeButton.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor constant:-16.0],
        [closeButton.centerYAnchor constraintEqualToAnchor:titleLabel.centerYAnchor],
        [closeButton.widthAnchor constraintGreaterThanOrEqualToConstant:44.0],
        [closeButton.heightAnchor constraintGreaterThanOrEqualToConstant:44.0],
        [titleLabel.trailingAnchor constraintLessThanOrEqualToAnchor:closeButton.leadingAnchor constant:-8.0],
        [addButton.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor constant:16.0],
        [addButton.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor constant:-16.0],
        [addButton.topAnchor constraintEqualToAnchor:titleLabel.bottomAnchor constant:14.0],
        [addButton.heightAnchor constraintEqualToConstant:44.0],
        [self.tableView.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor],
        [self.tableView.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor],
        [self.tableView.topAnchor constraintEqualToAnchor:addButton.bottomAnchor constant:10.0],
        [self.tableView.bottomAnchor constraintEqualToAnchor:safeArea.bottomAnchor],
    ]];
    if (self.store.lastError) {
        UILabel *errorLabel = HJLabel(UIColor.secondaryLabelColor, [UIFont systemFontOfSize:15]);
        errorLabel.text = @"指令数据读取失败，原文件已保留。请重试或联系开发者。";
        errorLabel.numberOfLines = 0;
        errorLabel.textAlignment = NSTextAlignmentCenter;
        self.tableView.backgroundView = errorLabel;
    }

}

- (void)close
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)addCommand
{
    [self presentEditorForCommand:nil];
}

- (void)presentEditorForCommand:(HJCustomCommand *)command
{
    __weak typeof(self) weakSelf = self;
    HJCustomCommandEditorViewController *editor =
        [[HJCustomCommandEditorViewController alloc] initWithCommand:command
                                                            completion:^(HJCustomCommand *updatedCommand) {
        __strong typeof(weakSelf) self = weakSelf;
        if (!self) return NO;
        BOOL saved = command ? [self.store updateCommand:updatedCommand] : [self.store addCommand:updatedCommand];
        if (saved) {
            self.commands = [[self.store allCommands] mutableCopy];
            self.tableView.backgroundView = nil;
            [self.tableView reloadData];
            [self dismissViewControllerAnimated:YES completion:nil];
        }
        return saved;
    }];
    UINavigationController *navigationController = [[UINavigationController alloc] initWithRootViewController:editor];
    navigationController.modalPresentationStyle = UIModalPresentationFormSheet;
    [self presentViewController:navigationController animated:YES completion:nil];
}

- (void)deleteCommand:(HJCustomCommand *)command
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"删除指令"
                                                                       message:@"确定删除这个自定义指令吗？"
                                                                preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:nil]];
    __weak typeof(self) weakSelf = self;
    [alert addAction:[UIAlertAction actionWithTitle:@"删除" style:UIAlertActionStyleDestructive handler:^(UIAlertAction *action) {
        __strong typeof(weakSelf) self = weakSelf;
        if ([self.store deleteCommandWithID:command.commandID]) {
            self.commands = [[self.store allCommands] mutableCopy];
            self.tableView.backgroundView = nil;
            [self.tableView reloadData];
        } else {
            [self.view hj_showMessage:@"删除失败，原指令已保留"];
        }
    }]];
    [self presentViewController:alert animated:YES completion:nil];
}

#pragma mark - UITableViewDataSource

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return self.commands.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    HJCustomCommandCell *cell = [tableView dequeueReusableCellWithIdentifier:@"HJCustomCommandCell" forIndexPath:indexPath];
    HJCustomCommand *command = self.commands[indexPath.row];
    [cell configureWithCommand:command];
    __weak typeof(self) weakSelf = self;
    cell.editHandler = ^{
        __strong typeof(weakSelf) self = weakSelf;
        [self presentEditorForCommand:command];
    };
    cell.deleteHandler = ^{
        __strong typeof(weakSelf) self = weakSelf;
        [self deleteCommand:command];
    };
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    HJCustomCommand *command = self.commands[indexPath.row];
    if (self.onCommandExecute) {
        self.onCommandExecute(command);
    }
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end

@interface HJCustomCommandEditorViewController ()
@property (nonatomic, strong) HJCustomCommand *editingCommand;
@property (nonatomic, copy) BOOL (^completion)(HJCustomCommand *command);
@property (nonatomic, strong) UITextField *nameField;
@property (nonatomic, strong) UISegmentedControl *typeControl;
@property (nonatomic, strong) UITextView *commandTextView;
@end

@implementation HJCustomCommandEditorViewController

- (instancetype)initWithCommand:(HJCustomCommand *)command
                      completion:(BOOL (^)(HJCustomCommand *command))completion
{
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        _editingCommand = command;
        _completion = [completion copy];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];
    self.title = self.editingCommand ? @"编辑指令" : @"添加指令";
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"取消"
                                                                                 style:UIBarButtonItemStylePlain
                                                                                target:self
                                                                                action:@selector(cancel)];
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"保存"
                                                                                  style:UIBarButtonItemStyleDone
                                                                                 target:self
                                                                                 action:@selector(save)];

    UILabel *nameLabel = [self labelWithText:@"指令名称"];
    UILabel *typeLabel = [self labelWithText:@"指令类型"];
    UILabel *contentLabel = [self labelWithText:@"指令内容"];
    self.nameField = [[UITextField alloc] initWithFrame:CGRectZero];
    self.nameField.borderStyle = UITextBorderStyleRoundedRect;
    self.nameField.placeholder = @"请输入指令名称";
    self.nameField.text = self.editingCommand.name;

    self.typeControl = [[UISegmentedControl alloc] initWithItems:@[@"HEX", @"ASCII"]];
    self.typeControl.selectedSegmentIndex = self.editingCommand ? self.editingCommand.type : HJCustomCommandTypeHex;

    self.commandTextView = [[UITextView alloc] initWithFrame:CGRectZero];
    self.commandTextView.font = [UIFont systemFontOfSize:16.0];
    self.commandTextView.layer.borderColor = [UIColor colorWithWhite:0.8 alpha:1.0].CGColor;
    self.commandTextView.layer.borderWidth = 1.0;
    self.commandTextView.layer.cornerRadius = 6.0;
    self.commandTextView.text = self.editingCommand.command ?: @"";

    NSArray *views = @[nameLabel, self.nameField, typeLabel, self.typeControl, contentLabel, self.commandTextView];
    for (UIView *view in views) {
        view.translatesAutoresizingMaskIntoConstraints = NO;
        [self.contentView addSubview:view];
    }
    UIView *safeArea = self.contentView;
    [NSLayoutConstraint activateConstraints:@[
        [nameLabel.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor constant:20.0],
        [nameLabel.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor constant:-20.0],
        [nameLabel.topAnchor constraintEqualToAnchor:safeArea.topAnchor constant:20.0],
        [self.nameField.leadingAnchor constraintEqualToAnchor:nameLabel.leadingAnchor],
        [self.nameField.trailingAnchor constraintEqualToAnchor:nameLabel.trailingAnchor],
        [self.nameField.topAnchor constraintEqualToAnchor:nameLabel.bottomAnchor constant:8.0],
        [self.nameField.heightAnchor constraintEqualToConstant:44.0],
        [typeLabel.leadingAnchor constraintEqualToAnchor:nameLabel.leadingAnchor],
        [typeLabel.trailingAnchor constraintEqualToAnchor:nameLabel.trailingAnchor],
        [typeLabel.topAnchor constraintEqualToAnchor:self.nameField.bottomAnchor constant:20.0],
        [self.typeControl.leadingAnchor constraintEqualToAnchor:nameLabel.leadingAnchor],
        [self.typeControl.trailingAnchor constraintEqualToAnchor:nameLabel.trailingAnchor],
        [self.typeControl.topAnchor constraintEqualToAnchor:typeLabel.bottomAnchor constant:8.0],
        [self.typeControl.heightAnchor constraintEqualToConstant:44.0],
        [contentLabel.leadingAnchor constraintEqualToAnchor:nameLabel.leadingAnchor],
        [contentLabel.trailingAnchor constraintEqualToAnchor:nameLabel.trailingAnchor],
        [contentLabel.topAnchor constraintEqualToAnchor:self.typeControl.bottomAnchor constant:20.0],
        [self.commandTextView.leadingAnchor constraintEqualToAnchor:nameLabel.leadingAnchor],
        [self.commandTextView.trailingAnchor constraintEqualToAnchor:nameLabel.trailingAnchor],
        [self.commandTextView.topAnchor constraintEqualToAnchor:contentLabel.bottomAnchor constant:8.0],
        [self.commandTextView.heightAnchor constraintEqualToConstant:160.0],
        [self.commandTextView.bottomAnchor constraintEqualToAnchor:safeArea.bottomAnchor constant:-20.0],
    ]];
}

- (UILabel *)labelWithText:(NSString *)text
{
    UILabel *label = [[UILabel alloc] initWithFrame:CGRectZero];
    label.text = text;
    label.font = [UIFont boldSystemFontOfSize:15.0];
    label.textColor = [UIColor darkTextColor];
    return label;
}

- (void)cancel
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)save
{
    NSString *name = [self.nameField.text stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    NSString *content = [self.commandTextView.text stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (name.length == 0 || content.length == 0) {
        [self showError:@"请输入指令名称和指令内容"];
        return;
    }

    HJCustomCommand *command = self.editingCommand ? [[HJCustomCommand alloc] initWithDictionary:self.editingCommand.dictionaryRepresentation] : [[HJCustomCommand alloc] initWithName:name
                                                                                       command:content
                                                                                          type:self.typeControl.selectedSegmentIndex];
    command.name = name;
    command.command = content;
    command.type = self.typeControl.selectedSegmentIndex;

    NSString *error = nil;
    if (![HJCustomCommandCodec dataForCommand:command appendReturn:NO error:&error]) {
        [self showError:error ?: @"指令格式错误"];
        return;
    }

    if (self.completion && !self.completion(command)) {
        [self showError:@"保存失败，原指令已保留，请稍后重试"];
    }
}

- (void)showError:(NSString *)message
{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"提示"
                                                                       message:message
                                                                preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

@end

@interface HJCustomCommandCell ()
@property (nonatomic, strong) UILabel *nameLabel;
@property (nonatomic, strong) UILabel *typeLabel;
@property (nonatomic, strong) UILabel *commandLabel;
@property (nonatomic, strong) UIButton *editButton;
@property (nonatomic, strong) UIButton *deleteButton;
@end

@implementation HJCustomCommandCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.selectionStyle = UITableViewCellSelectionStyleDefault;
        self.nameLabel = [[UILabel alloc] initWithFrame:CGRectZero];
        self.nameLabel.font = [UIFont boldSystemFontOfSize:16.0];
        self.nameLabel.textColor = [UIColor darkTextColor];
        self.typeLabel = [[UILabel alloc] initWithFrame:CGRectZero];
        self.typeLabel.font = [UIFont systemFontOfSize:12.0];
        self.commandLabel = [[UILabel alloc] initWithFrame:CGRectZero];
        self.commandLabel.font = [UIFont systemFontOfSize:14.0];
        self.commandLabel.textColor = [UIColor grayColor];
        self.commandLabel.numberOfLines = 0;
        self.editButton = [UIButton buttonWithType:UIButtonTypeSystem];
        [self.editButton setTitle:@"编辑" forState:UIControlStateNormal];
        self.deleteButton = [UIButton buttonWithType:UIButtonTypeSystem];
        [self.deleteButton setTitle:@"删除" forState:UIControlStateNormal];
        [self.editButton addTarget:self action:@selector(editButtonTapped) forControlEvents:UIControlEventTouchUpInside];
        [self.deleteButton addTarget:self action:@selector(deleteButtonTapped) forControlEvents:UIControlEventTouchUpInside];

        NSArray *views = @[self.nameLabel, self.typeLabel, self.commandLabel, self.editButton, self.deleteButton];
        for (UIView *view in views) {
            view.translatesAutoresizingMaskIntoConstraints = NO;
            [self.contentView addSubview:view];
        }
        self.nameLabel.numberOfLines = 2;
        [NSLayoutConstraint activateConstraints:@[
            [self.nameLabel.leadingAnchor constraintEqualToAnchor:self.contentView.leadingAnchor constant:12],
            [self.nameLabel.trailingAnchor constraintEqualToAnchor:self.contentView.trailingAnchor constant:-12],
            [self.nameLabel.topAnchor constraintEqualToAnchor:self.contentView.topAnchor constant:12],
            [self.commandLabel.leadingAnchor constraintEqualToAnchor:self.nameLabel.leadingAnchor],
            [self.commandLabel.trailingAnchor constraintEqualToAnchor:self.nameLabel.trailingAnchor],
            [self.commandLabel.topAnchor constraintEqualToAnchor:self.nameLabel.bottomAnchor constant:8],
            [self.typeLabel.leadingAnchor constraintEqualToAnchor:self.nameLabel.leadingAnchor],
            [self.typeLabel.centerYAnchor constraintEqualToAnchor:self.editButton.centerYAnchor],
            [self.editButton.topAnchor constraintEqualToAnchor:self.commandLabel.bottomAnchor constant:4],
            [self.editButton.leadingAnchor constraintGreaterThanOrEqualToAnchor:self.typeLabel.trailingAnchor constant:8],
            [self.editButton.widthAnchor constraintEqualToConstant:44],
            [self.editButton.heightAnchor constraintEqualToConstant:44],
            [self.editButton.bottomAnchor constraintEqualToAnchor:self.contentView.bottomAnchor constant:-4],
            [self.deleteButton.leadingAnchor constraintEqualToAnchor:self.editButton.trailingAnchor constant:8],
            [self.deleteButton.trailingAnchor constraintEqualToAnchor:self.contentView.trailingAnchor constant:-12],
            [self.deleteButton.centerYAnchor constraintEqualToAnchor:self.editButton.centerYAnchor],
            [self.deleteButton.widthAnchor constraintEqualToConstant:44],
            [self.deleteButton.heightAnchor constraintEqualToConstant:44],
        ]];
    }
    return self;
}

- (void)configureWithCommand:(HJCustomCommand *)command
{
    self.nameLabel.text = command.name;
    self.typeLabel.text = command.typeString;
    self.typeLabel.textColor = command.type == HJCustomCommandTypeHex ? [UIColor blueColor] : [UIColor greenColor];
    self.commandLabel.text = command.command;
}

- (void)editButtonTapped
{
    if (self.editHandler) self.editHandler();
}

- (void)deleteButtonTapped
{
    if (self.deleteHandler) self.deleteHandler();
}

@end
