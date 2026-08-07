#import "KRChatByteCacheModule.h"

#import <CommonCrypto/CommonCrypto.h>
#import <OpenKuiklyIOSRender/NSObject+KR.h>

static NSString * const kDefaultChatCacheNamespace = @"kuikly_chat_media";

@interface KRChatByteCacheStore : NSObject

- (instancetype)initWithDirectory:(NSString *)directory;
- (nullable NSData *)dataForKey:(NSString *)key;
- (void)setData:(NSData *)data forKey:(NSString *)key;
- (BOOL)containsKey:(NSString *)key;
- (void)deleteKey:(NSString *)key;
- (void)updateKey:(NSString *)oldKey toKey:(NSString *)newKey error:(NSError **)error;
- (void)clear;

@end

@implementation KRChatByteCacheStore {
    NSString *_directory;
}

- (instancetype)initWithDirectory:(NSString *)directory {
    self = [super init];
    if (self) {
        _directory = [directory copy];
    }
    return self;
}

- (NSString *)pathForKey:(NSString *)key {
    return [_directory stringByAppendingPathComponent:KRChatSHA256Hex(key)];
}

- (void)ensureDirectory {
    [[NSFileManager defaultManager] createDirectoryAtPath:_directory
                              withIntermediateDirectories:YES
                                               attributes:nil
                                                    error:nil];
}

- (nullable NSData *)dataForKey:(NSString *)key {
    return [NSData dataWithContentsOfFile:[self pathForKey:key]];
}

- (void)setData:(NSData *)data forKey:(NSString *)key {
    [self ensureDirectory];
    NSString *path = [self pathForKey:key];
    NSString *temporary = [path stringByAppendingString:@".tmp"];
    [data writeToFile:temporary atomically:YES];
    NSFileManager *manager = [NSFileManager defaultManager];
    if ([manager fileExistsAtPath:path]) {
        [manager removeItemAtPath:path error:nil];
    }
    [manager moveItemAtPath:temporary toPath:path error:nil];
}

- (BOOL)containsKey:(NSString *)key {
    return [[NSFileManager defaultManager] fileExistsAtPath:[self pathForKey:key]];
}

- (void)deleteKey:(NSString *)key {
    NSString *path = [self pathForKey:key];
    if ([[NSFileManager defaultManager] fileExistsAtPath:path]) {
        [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
    }
}

- (void)updateKey:(NSString *)oldKey toKey:(NSString *)newKey error:(NSError **)error {
    [self ensureDirectory];
    NSString *source = [self pathForKey:oldKey];
    if (![[NSFileManager defaultManager] fileExistsAtPath:source]) {
        if (error) {
            *error = [NSError errorWithDomain:@"KRChatByteCache"
                                         code:404
                                     userInfo:@{NSLocalizedDescriptionKey: @"Cache miss"}];
        }
        return;
    }
    NSString *destination = [self pathForKey:newKey];
    [[NSFileManager defaultManager] removeItemAtPath:destination error:nil];
    [[NSFileManager defaultManager] moveItemAtPath:source toPath:destination error:error];
}

- (void)clear {
    NSArray<NSString *> *names = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:_directory error:nil];
    for (NSString *name in names) {
        [[NSFileManager defaultManager] removeItemAtPath:[_directory stringByAppendingPathComponent:name]
                                                   error:nil];
    }
}

static NSString *KRChatSHA256Hex(NSString *value) {
    NSData *data = [value dataUsingEncoding:NSUTF8StringEncoding];
    uint8_t digest[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(data.bytes, (CC_LONG)data.length, digest);
    NSMutableString *output = [NSMutableString stringWithCapacity:CC_SHA256_DIGEST_LENGTH * 2];
    for (int index = 0; index < CC_SHA256_DIGEST_LENGTH; index++) {
        [output appendFormat:@"%02x", digest[index]];
    }
    return output;
}

static NSString *KRChatNormalizeNamespace(NSString *namespaceValue) {
    if (namespaceValue.length == 0) {
        return kDefaultChatCacheNamespace;
    }
    NSMutableString *normalized = [NSMutableString string];
    NSCharacterSet *allowed = [NSCharacterSet characterSetWithCharactersInString:@"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_."]; 
    for (NSUInteger index = 0; index < namespaceValue.length; index++) {
        unichar character = [namespaceValue characterAtIndex:index];
        if ([allowed characterIsMember:character]) {
            [normalized appendFormat:@"%C", character];
        } else {
            [normalized appendString:@"_"];
        }
    }
    NSString *trimmed = [normalized stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"."]];
    return trimmed.length > 0 ? trimmed : kDefaultChatCacheNamespace;
}

static NSString *KRChatCacheRootDirectory(void) {
    NSArray<NSString *> *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *root = paths.firstObject;
    return root.length > 0 ? root : NSTemporaryDirectory();
}

static NSArray *KRChatArrayParam(id param) {
    if ([param isKindOfClass:[NSArray class]]) {
        return param;
    }
    if ([param isKindOfClass:[NSDictionary class]]) {
        id value = ((NSDictionary *)param)[KR_PARAM_KEY];
        if ([value isKindOfClass:[NSArray class]]) {
            return value;
        }
    }
    return nil;
}

static NSDictionary *KRChatJsonParam(id param) {
    if ([param isKindOfClass:[NSDictionary class]]) {
        id value = ((NSDictionary *)param)[KR_PARAM_KEY];
        if ([value isKindOfClass:[NSDictionary class]]) {
            return value;
        }
        if ([value isKindOfClass:[NSString class]]) {
            NSData *data = [(NSString *)value dataUsingEncoding:NSUTF8StringEncoding];
            if (!data) {
                return nil;
            }
            id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
            return [json isKindOfClass:[NSDictionary class]] ? json : nil;
        }
        return nil;
    }
    if ([param isKindOfClass:[NSString class]]) {
        NSData *data = [(NSString *)param dataUsingEncoding:NSUTF8StringEncoding];
        if (!data) {
            return nil;
        }
        id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
        return [json isKindOfClass:[NSDictionary class]] ? json : nil;
    }
    return nil;
}

@implementation KRChatByteCacheModule {
    KRChatByteCacheStore *_store;
}

@synthesize hr_rootView;

TDF_EXPORT_MODULE(KRChatByteCacheModule)

- (void)configure:(id)args {
    NSDictionary *params = KRChatJsonParam(args);
    NSString *namespaceValue = params[@"namespace"];
    NSString *directory = [KRChatCacheRootDirectory() stringByAppendingPathComponent:KRChatNormalizeNamespace(namespaceValue)];
    _store = [[KRChatByteCacheStore alloc] initWithDirectory:directory];
}

- (NSData *)get:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *key = params.firstObject;
    return [_store dataForKey:key];
}

- (void)set:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *key = params.count > 0 ? params[0] : nil;
    NSData *value = params.count > 1 ? params[1] : nil;
    [_store setData:value forKey:key];
}

- (NSNumber *)contains:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *key = params.firstObject;
    return @([_store containsKey:key]);
}

- (void)delete:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *key = params.firstObject;
    [_store deleteKey:key];
}

- (void)updateKey:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *oldKey = params.count > 0 ? params[0] : nil;
    NSString *newKey = params.count > 1 ? params[1] : nil;
    NSError *error = nil;
    [_store updateKey:oldKey toKey:newKey error:&error];
    if (error) {
        @throw [NSException exceptionWithName:@"ChatCacheMissException"
                                       reason:error.localizedDescription
                                     userInfo:nil];
    }
}

- (void)clear:(id)args {
    [_store clear];
}

- (NSData *)readLocalFile:(id)args {
    NSArray *params = KRChatArrayParam(args);
    NSString *source = params.firstObject;
    NSString *path = [source stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    return [NSData dataWithContentsOfFile:path];
}

@end
