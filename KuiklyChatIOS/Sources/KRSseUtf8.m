#import <Foundation/Foundation.h>

/// Validates UTF-8 following the WHATWG decoder. Copied from KuiklyUI `KRNetworkModule.m`.
/// Returns how many input bytes were consumed. When flush is NO, a trailing incomplete
/// sequence of at most 3 bytes is left unconsumed.
NSUInteger KRSseAppendSanitizedUTF8(const uint8_t *bytes, NSUInteger length, BOOL flush, NSMutableData *output) {
    static const uint8_t kReplacement[] = {0xEF, 0xBF, 0xBD};
    NSUInteger i = 0;
    while (i < length) {
        NSUInteger start = i;
        uint8_t lead = bytes[i];
        if (lead <= 0x7F) {
            [output appendBytes:&bytes[i] length:1];
            i++;
            continue;
        }
        NSUInteger needed = 0;
        uint8_t lower = 0x80;
        uint8_t upper = 0xBF;
        if (lead >= 0xC2 && lead <= 0xDF) {
            needed = 1;
        } else if (lead >= 0xE0 && lead <= 0xEF) {
            if (lead == 0xE0) lower = 0xA0;
            if (lead == 0xED) upper = 0x9F;
            needed = 2;
        } else if (lead >= 0xF0 && lead <= 0xF4) {
            if (lead == 0xF0) lower = 0x90;
            if (lead == 0xF4) upper = 0x8F;
            needed = 3;
        } else {
            [output appendBytes:kReplacement length:sizeof(kReplacement)];
            i++;
            continue;
        }
        NSUInteger j = i + 1;
        BOOL valid = YES;
        for (NSUInteger seen = 0; seen < needed; seen++, j++) {
            if (j >= length) {
                if (!flush) {
                    return start;
                }
                [output appendBytes:kReplacement length:sizeof(kReplacement)];
                return length;
            }
            uint8_t continuation = bytes[j];
            if (continuation < lower || continuation > upper) {
                valid = NO;
                break;
            }
            lower = 0x80;
            upper = 0xBF;
        }
        if (valid) {
            [output appendBytes:&bytes[start] length:j - start];
        } else {
            [output appendBytes:kReplacement length:sizeof(kReplacement)];
        }
        i = j;
    }
    return length;
}
