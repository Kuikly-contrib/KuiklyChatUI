#import "KRChatAttachmentPickerModule.h"

#import <PhotosUI/PhotosUI.h>
#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>
#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/NSObject+KR.h>

static const CGFloat kKRChatThumbnailSize = 256.0;

static UIViewController *KRChatTopViewController(UIViewController *controller) {
    UIViewController *current = controller;
    while (current != nil) {
        if (current.presentedViewController != nil) {
            current = current.presentedViewController;
        } else if ([current isKindOfClass:UINavigationController.class]) {
            current = ((UINavigationController *)current).visibleViewController;
        } else if ([current isKindOfClass:UITabBarController.class]) {
            current = ((UITabBarController *)current).selectedViewController;
        } else {
            break;
        }
    }
    return current;
}

static NSString *KRChatMimeType(NSString *typeIdentifier, NSString *fileName) {
    UTType *type = typeIdentifier.length > 0 ? [UTType typeWithIdentifier:typeIdentifier] : nil;
    if (type.preferredMIMEType.length == 0 && fileName.pathExtension.length > 0) {
        type = [UTType typeWithFilenameExtension:fileName.pathExtension];
    }
    return type.preferredMIMEType ?: @"application/octet-stream";
}

static NSString *KRChatThumbnailBase64(NSData *data, NSString *mimeType) {
    if (![mimeType hasPrefix:@"image/"]) {
        return @"";
    }
    UIImage *image = [UIImage imageWithData:data];
    if (image == nil || image.size.width <= 0 || image.size.height <= 0) {
        return @"";
    }
    CGFloat scale = MIN(1.0, kKRChatThumbnailSize / MAX(image.size.width, image.size.height));
    CGSize size = CGSizeMake(MAX(1.0, image.size.width * scale),
                             MAX(1.0, image.size.height * scale));
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size];
    UIImage *thumbnail = [renderer imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    }];
    NSData *encoded = UIImageJPEGRepresentation(thumbnail, 0.78);
    return [encoded base64EncodedStringWithOptions:0] ?: @"";
}

static NSDictionary *KRChatAttachment(NSData *data,
                                      NSString *fileName,
                                      NSString *mimeType) {
    BOOL isImage = [mimeType hasPrefix:@"image/"];
    return @{
        @"id": [NSUUID UUID].UUIDString,
        @"kind": isImage ? @"IMAGE" : ([mimeType hasPrefix:@"audio/"] ? @"AUDIO" : @"FILE"),
        @"name": fileName.length > 0 ? fileName : (isImage ? @"image" : @"file"),
        @"mimeType": mimeType,
        @"url": @"",
        @"bytesBase64": [data base64EncodedStringWithOptions:0] ?: @"",
        @"thumbnailBase64": KRChatThumbnailBase64(data, mimeType),
        @"sizeBytes": @(data.length),
    };
}

@interface KRChatAttachmentPickerModule ()
<PHPickerViewControllerDelegate,
UIImagePickerControllerDelegate,
UINavigationControllerDelegate,
UIDocumentPickerDelegate>

@property (nonatomic, copy, nullable) KuiklyRenderCallback pendingCallback;
@property (nonatomic, weak, nullable) UIViewController *presentedController;

@end

@implementation KRChatAttachmentPickerModule

@synthesize hr_rootView;

TDF_EXPORT_MODULE(KRChatAttachmentPickerModule)

- (NSString *)supportedSources:(id)args {
    NSMutableArray<NSString *> *sources = [NSMutableArray arrayWithObjects:
                                           @"PHOTO_LIBRARY", @"FILE", nil];
    if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        [sources addObject:@"CAMERA"];
    }
    return [sources componentsJoinedByString:@","];
}

- (void)pick:(NSDictionary *)args {
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSDictionary *params = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *source = params[@"source"];
    if (callback == nil) {
        return;
    }
    if (self.pendingCallback != nil) {
        callback(@{ @"attachments": @[], @"error": @"Another attachment picker is active" });
        return;
    }
    UIViewController *presenter = [self presentingViewController];
    if (presenter == nil) {
        callback(@{ @"attachments": @[], @"error": @"Unable to find a presenting view controller" });
        return;
    }
    self.pendingCallback = callback;

    if ([source isEqualToString:@"PHOTO_LIBRARY"]) {
        [self presentPhotoPickerFrom:presenter];
    } else if ([source isEqualToString:@"CAMERA"]) {
        [self presentCameraFrom:presenter];
    } else if ([source isEqualToString:@"FILE"]) {
        [self presentDocumentPickerFrom:presenter];
    } else {
        [self finishWithAttachments:@[] error:@"Unsupported attachment source"];
    }
}

- (void)cancel:(id)args {
    UIViewController *controller = self.presentedController;
    if (controller != nil) {
        [controller dismissViewControllerAnimated:YES completion:nil];
    }
    [self finishWithAttachments:@[] error:nil];
}

- (UIViewController *)presentingViewController {
    UIView *rootView = [self.hr_rootView isKindOfClass:UIView.class]
        ? (UIView *)self.hr_rootView
        : nil;
    UIViewController *rootController = rootView.window.rootViewController;
    if (rootController != nil) {
        return KRChatTopViewController(rootController);
    }
    for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
        if (scene.activationState != UISceneActivationStateForegroundActive ||
            ![scene isKindOfClass:UIWindowScene.class]) {
            continue;
        }
        for (UIWindow *window in ((UIWindowScene *)scene).windows) {
            if (window.isKeyWindow && window.rootViewController != nil) {
                return KRChatTopViewController(window.rootViewController);
            }
        }
    }
    return nil;
}

- (void)presentPhotoPickerFrom:(UIViewController *)presenter {
    PHPickerConfiguration *configuration = [[PHPickerConfiguration alloc] init];
    configuration.filter = PHPickerFilter.imagesFilter;
    configuration.selectionLimit = 0;
    PHPickerViewController *picker =
        [[PHPickerViewController alloc] initWithConfiguration:configuration];
    picker.delegate = self;
    self.presentedController = picker;
    [presenter presentViewController:picker animated:YES completion:nil];
}

- (void)presentCameraFrom:(UIViewController *)presenter {
    if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        [self finishWithAttachments:@[] error:@"Camera is unavailable"];
        return;
    }
    UIImagePickerController *picker = [UIImagePickerController new];
    picker.sourceType = UIImagePickerControllerSourceTypeCamera;
    picker.mediaTypes = @[(NSString *)UTTypeImage.identifier];
    picker.delegate = self;
    self.presentedController = picker;
    [presenter presentViewController:picker animated:YES completion:nil];
}

- (void)presentDocumentPickerFrom:(UIViewController *)presenter {
    UIDocumentPickerViewController *picker =
        [[UIDocumentPickerViewController alloc] initForOpeningContentTypes:@[UTTypeItem]
                                                                   asCopy:YES];
    picker.allowsMultipleSelection = YES;
    picker.delegate = self;
    self.presentedController = picker;
    [presenter presentViewController:picker animated:YES completion:nil];
}

- (void)picker:(PHPickerViewController *)picker
didFinishPicking:(NSArray<PHPickerResult *> *)results {
    [picker dismissViewControllerAnimated:YES completion:nil];
    self.presentedController = nil;
    if (results.count == 0) {
        [self finishWithAttachments:@[] error:nil];
        return;
    }

    dispatch_group_t group = dispatch_group_create();
    NSMutableArray *attachments = [NSMutableArray arrayWithCapacity:results.count];
    for (NSUInteger index = 0; index < results.count; index++) {
        [attachments addObject:NSNull.null];
    }
    [results enumerateObjectsUsingBlock:^(PHPickerResult *result, NSUInteger index, BOOL *stop) {
        NSItemProvider *provider = result.itemProvider;
        NSString *typeIdentifier = [self imageTypeIdentifierForProvider:provider];
        if (typeIdentifier.length == 0) {
            return;
        }
        dispatch_group_enter(group);
        [provider loadDataRepresentationForTypeIdentifier:typeIdentifier
                                        completionHandler:^(NSData *data, NSError *error) {
            if (data.length > 0) {
                NSString *mimeType = KRChatMimeType(typeIdentifier, provider.suggestedName);
                NSString *name = provider.suggestedName;
                if (name.pathExtension.length == 0) {
                    NSString *extension = [UTType typeWithIdentifier:typeIdentifier]
                        .preferredFilenameExtension;
                    NSString *baseName = name.length > 0 ? name : @"image";
                    name = [baseName stringByAppendingPathExtension:extension ?: @"jpg"];
                }
                @synchronized (attachments) {
                    attachments[index] = KRChatAttachment(data, name, mimeType);
                }
            }
            dispatch_group_leave(group);
        }];
    }];
    dispatch_group_notify(group, dispatch_get_main_queue(), ^{
        NSMutableArray<NSDictionary *> *loaded = [NSMutableArray array];
        for (id attachment in attachments) {
            if ([attachment isKindOfClass:NSDictionary.class]) {
                [loaded addObject:attachment];
            }
        }
        NSString *error = loaded.count == 0 ? @"Unable to read selected photos" : nil;
        [self finishWithAttachments:loaded error:error];
    });
}

- (NSString *)imageTypeIdentifierForProvider:(NSItemProvider *)provider {
    for (NSString *identifier in provider.registeredTypeIdentifiers) {
        UTType *type = [UTType typeWithIdentifier:identifier];
        if ([type conformsToType:UTTypeImage]) {
            return identifier;
        }
    }
    return nil;
}

- (void)imagePickerController:(UIImagePickerController *)picker
didFinishPickingMediaWithInfo:(NSDictionary<UIImagePickerControllerInfoKey, id> *)info {
    [picker dismissViewControllerAnimated:YES completion:nil];
    self.presentedController = nil;
    UIImage *image = info[UIImagePickerControllerOriginalImage];
    NSData *data = image != nil ? UIImageJPEGRepresentation(image, 0.88) : nil;
    if (data.length == 0) {
        [self finishWithAttachments:@[] error:@"Camera did not return an image"];
        return;
    }
    [self finishWithAttachments:@[KRChatAttachment(data, @"camera.jpg", @"image/jpeg")]
                          error:nil];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [picker dismissViewControllerAnimated:YES completion:nil];
    self.presentedController = nil;
    [self finishWithAttachments:@[] error:nil];
}

- (void)documentPicker:(UIDocumentPickerViewController *)controller
didPickDocumentsAtURLs:(NSArray<NSURL *> *)urls {
    self.presentedController = nil;
    NSMutableArray<NSDictionary *> *attachments = [NSMutableArray array];
    for (NSURL *url in urls) {
        BOOL scoped = [url startAccessingSecurityScopedResource];
        NSData *data = [NSData dataWithContentsOfURL:url
                                            options:NSDataReadingMappedIfSafe
                                              error:nil];
        if (scoped) {
            [url stopAccessingSecurityScopedResource];
        }
        if (data.length == 0) {
            continue;
        }
        NSString *name = url.lastPathComponent ?: @"file";
        NSString *mimeType = KRChatMimeType(nil, name);
        [attachments addObject:KRChatAttachment(data, name, mimeType)];
    }
    NSString *error = urls.count > 0 && attachments.count == 0
        ? @"Unable to read selected files"
        : nil;
    [self finishWithAttachments:attachments error:error];
}

- (void)documentPickerWasCancelled:(UIDocumentPickerViewController *)controller {
    self.presentedController = nil;
    [self finishWithAttachments:@[] error:nil];
}

- (void)finishWithAttachments:(NSArray<NSDictionary *> *)attachments
                        error:(NSString *)error {
    KuiklyRenderCallback callback = self.pendingCallback;
    self.pendingCallback = nil;
    self.presentedController = nil;
    if (callback != nil) {
        callback(@{
            @"attachments": attachments ?: @[],
            @"error": error ?: @"",
        });
    }
}

@end
