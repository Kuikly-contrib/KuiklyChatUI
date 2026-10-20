#import <Foundation/Foundation.h>

NSString *KRSsePercentEncode(NSString *string, BOOL spaceAsPlus) {
    static NSCharacterSet *allowed = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        allowed = [NSCharacterSet characterSetWithCharactersInString:
                   @"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789*-._"];
    });
    NSString *encoded = [string stringByAddingPercentEncodingWithAllowedCharacters:allowed] ?: @"";
    return spaceAsPlus ? [encoded stringByReplacingOccurrencesOfString:@"%20" withString:@"+"] : encoded;
}

static NSString *KRSseFormValue(id value) {
    if (!value || value == [NSNull null]) {
        return @"";
    }
    if ([value isKindOfClass:[NSString class]]) {
        return value;
    }
    if ([value isKindOfClass:[NSNumber class]] && CFGetTypeID((__bridge CFTypeRef)value) == CFBooleanGetTypeID()) {
        return [value boolValue] ? @"true" : @"false";
    }
    if ([value isKindOfClass:[NSDictionary class]] || [value isKindOfClass:[NSArray class]]) {
        NSData *json = [NSJSONSerialization dataWithJSONObject:value options:0 error:nil];
        return json ? [[NSString alloc] initWithData:json encoding:NSUTF8StringEncoding] : @"";
    }
    return [value description];
}

NSString *KRSseFormURLEncodedString(NSDictionary *params) {
    NSMutableArray<NSString *> *parts = [NSMutableArray arrayWithCapacity:params.count];
    for (id key in params) {
        NSString *name = KRSsePercentEncode([key description], YES);
        NSString *value = KRSsePercentEncode(KRSseFormValue(params[key]), YES);
        [parts addObject:[NSString stringWithFormat:@"%@=%@", name, value]];
    }
    return [parts componentsJoinedByString:@"&"];
}

NSURL *KRSseRequestURL(id urlString, NSString *method, id params) {
    if (![urlString isKindOfClass:[NSString class]] || [(NSString *)urlString length] == 0) {
        return nil;
    }
    NSURLComponents *components = [NSURLComponents componentsWithString:urlString];
    NSString *scheme = components.scheme.lowercaseString;
    if (!components || !([scheme isEqualToString:@"http"] || [scheme isEqualToString:@"https"]) ||
        components.host.length == 0) {
        return nil;
    }
    if ([[method uppercaseString] isEqualToString:@"GET"] && [params isKindOfClass:[NSDictionary class]] &&
        [(NSDictionary *)params count] > 0) {
        NSMutableArray<NSString *> *parts = [NSMutableArray array];
        if (components.percentEncodedQuery.length > 0) {
            [parts addObject:components.percentEncodedQuery];
        }
        for (id key in (NSDictionary *)params) {
            NSString *name = KRSsePercentEncode([key description], NO);
            NSString *value = KRSsePercentEncode(KRSseFormValue(params[key]), NO);
            [parts addObject:[NSString stringWithFormat:@"%@=%@", name, value]];
        }
        components.percentEncodedQuery = [parts componentsJoinedByString:@"&"];
    }
    return components.URL;
}

/// Keeps the original JSON object text for a top-level key so POST bodies keep key order.
NSString *KRSseExtractJsonObject(NSString *key, NSString *jsonStr) {
    if (![jsonStr isKindOfClass:[NSString class]] || !jsonStr.length || !key.length) return nil;
    NSString *keyToken = [NSString stringWithFormat:@"\"%@\"", key];
    NSInteger keyTokenLen = (NSInteger)keyToken.length;
    NSInteger len = (NSInteger)jsonStr.length;
    NSInteger pos = 0;
    NSInteger depth = 0;
    BOOL inString = NO;
    BOOL escaped = NO;
    while (pos < len) {
        unichar c = [jsonStr characterAtIndex:pos];
        if (escaped) {
            escaped = NO;
            pos++;
            continue;
        }
        if (c == '\\' && inString) {
            escaped = YES;
            pos++;
            continue;
        }
        if (c == '"') {
            if (!inString && depth == 1 && pos + keyTokenLen <= len &&
                [[jsonStr substringWithRange:NSMakeRange(pos, keyTokenLen)] isEqualToString:keyToken]) {
                NSInteger colonPos = pos + keyTokenLen;
                while (colonPos < len) {
                    unichar wc = [jsonStr characterAtIndex:colonPos];
                    if (wc != ' ' && wc != '\t' && wc != '\n' && wc != '\r') break;
                    colonPos++;
                }
                if (colonPos >= len || [jsonStr characterAtIndex:colonPos] != ':') {
                    inString = !inString;
                    pos++;
                    continue;
                }
                NSInteger valueStart = colonPos + 1;
                while (valueStart < len) {
                    unichar wc = [jsonStr characterAtIndex:valueStart];
                    if (wc != ' ' && wc != '\t' && wc != '\n' && wc != '\r') break;
                    valueStart++;
                }
                if (valueStart >= len) return nil;
                unichar firstChar = [jsonStr characterAtIndex:valueStart];
                if (firstChar != '{' && firstChar != '[') return nil;
                NSInteger vDepth = 0;
                BOOL vInString = NO;
                BOOL vEscaped = NO;
                for (NSInteger vPos = valueStart; vPos < len; vPos++) {
                    unichar vc = [jsonStr characterAtIndex:vPos];
                    if (vEscaped) {
                        vEscaped = NO;
                    } else if (vc == '\\' && vInString) {
                        vEscaped = YES;
                    } else if (vc == '"') {
                        vInString = !vInString;
                    } else if (!vInString && (vc == '{' || vc == '[')) {
                        vDepth++;
                    } else if (!vInString && (vc == '}' || vc == ']')) {
                        vDepth--;
                        if (vDepth == 0) {
                            return [jsonStr substringWithRange:NSMakeRange(valueStart, vPos - valueStart + 1)];
                        }
                    }
                }
                return nil;
            }
            inString = !inString;
            pos++;
            continue;
        }
        if (!inString) {
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
        }
        pos++;
    }
    return nil;
}
