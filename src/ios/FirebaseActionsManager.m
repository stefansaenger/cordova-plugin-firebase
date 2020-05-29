#import <UserNotifications/UserNotifications.h>
@import UserNotifications;

#import "FirebaseActionsManager.h"

@implementation FirebaseActionsManager

+ (void)registerTalkNotificationCategoriesAndActionsForCallRequest {
    // https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/SupportingNotificationsinYourApp.html#//apple_ref/doc/uid/TP40008194-CH4-SW26
    UNNotificationAction *acceptAction = [UNNotificationAction
          actionWithIdentifier:@"ACCEPT_CALL_ACTION"
          title:@"Accept"
          options:UNNotificationActionOptionForeground];

    UNNotificationAction *rejectAction = [UNNotificationAction
          actionWithIdentifier:@"REJECT_CALL_ACTION"
          title:@"Reject"
          options:UNNotificationActionOptionDestructive];

    UNNotificationCategory *videoCallCategory = [UNNotificationCategory
         categoryWithIdentifier:@"VIDEO"
         actions:@[acceptAction, rejectAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];

    UNNotificationCategory *audioCallCategory = [UNNotificationCategory
         categoryWithIdentifier:@"AUDIO"
         actions:@[acceptAction, rejectAction]
         intentIdentifiers:@[]
         options:UNNotificationCategoryOptionCustomDismissAction];

    // Register the notification categories.
    UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
    [center setNotificationCategories:[NSSet setWithObjects:videoCallCategory, audioCallCategory, nil]];
}

+ (BOOL)isVideoAudioCategory:(NSString *)categoryIdentifier {
    return [categoryIdentifier isEqualToString:@"VIDEO"] || [categoryIdentifier isEqualToString:@"AUDIO"];
}

// handle Call actions
// https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/SchedulingandHandlingLocalNotifications.html#//apple_ref/doc/uid/TP40008194-CH5-SW2
//
+ (void)handleCallRequestActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier {

//    aft = "";
//    aps =     {
//        alert =         {
//            body = audio;
//            title = George;
//        };
//        badge = 13;
//        category = AUDIO;
//        sound = default;
//    };
//    body = audio;
//    callSignal = 1;
//    eType = invite;
//    "gcm.message_id" = 1576063852084600;
//    "google.c.a.e" = 1;
//    gt = George;
//    jid = "test__group__calls@conference.vnc.biz";  // jid = "bob@dev2.zimbra-vnc.de";
//    lang = en;
//    mention = "[]";
//    nType = "local_notification";
//    name = George;
//    nsound = nomute;
//    nto = "bob.bobson@vnc.biz";
//    tap = 1;

    NSString *eType = mutableUserInfo[@"eType"];
    if ([eType isEqualToString:@"invite"]) {
        if ([actionIdentifier isEqualToString:@"ACCEPT_CALL_ACTION"] || [actionIdentifier isEqualToString: UNNotificationDefaultActionIdentifier]) {
            [self markCallRequestAsProccessed:mutableUserInfo[@"msgid"]];

        } else if ([actionIdentifier isEqualToString:@"REJECT_CALL_ACTION"]) {
            [self markCallRequestAsProccessed:mutableUserInfo[@"msgid"]];

            BOOL isGroupCall = [mutableUserInfo[@"jid"] rangeOfString:@"@conference"].location != NSNotFound;
            NSString *callType = [mutableUserInfo[@"aps"][@"category"] lowercaseString];
            NSString *callId = mutableUserInfo[@"jid"];
            NSString *callReceiver = mutableUserInfo[@"nto"];
            NSString *confid = isGroupCall ?
                            callId :
            [[[callReceiver stringByReplacingOccurrencesOfString:@"@" withString:@"#"] stringByAppendingString:@","] stringByAppendingString:[callId stringByReplacingOccurrencesOfString:@"@" withString:@"#"]];
//            NSString *target = isGroupCall ? mutableUserInfo[@"jid"] : callId;
            [self rejectCall:callType confid:confid target:callId];
        }
    }
}

+ (BOOL)isCallRejectActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier {
    NSString *eType = mutableUserInfo[@"eType"];
    return [eType isEqualToString:@"invite"] && [actionIdentifier isEqualToString:@"REJECT_CALL_ACTION"];
}

+ (void)markCallRequestAsProccessed:(NSString *)mid {
    NSString *processedCallsIds = [[NSUserDefaults standardUserDefaults] stringForKey:@"processedCallsIds"];
    processedCallsIds = [NSString stringWithFormat:@"%@,%@", processedCallsIds, mid];

    [[NSUserDefaults standardUserDefaults] setObject:processedCallsIds forKey:@"processedCallsIds"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    NSLog(@"[FirebaseActionsManager][markCallRequestAsProccessed] mid: %@, processedCallsIds %@", mid, processedCallsIds);
}

///

+ (void)rejectCall:(NSString *)callType confid:(NSString *)confid target:(NSString *)target {
    NSDictionary *params = @{
        @"messagetext": @"REJECTED_CALL",
        @"reject": callType,
        @"confid": confid,
        @"target": target
    };
    [self postRequestWithSubUrl:@"xmpp-rest" params:params];

    // 1-1
// {"messagetext":"REJECTED_CALL","reject":"video","confid":"bob#dev2.zimbra-vnc.de,ihor.khomenko#vnc.biz","target":"ihor.khomenko@vnc.biz"}

    // group
//    {“messagetext”:“REJECTED_CALL”,“reject”:“video”,“confid”:“grvall@conference.dev2.zimbra-vnc.de”,“target”:“grvall@conference.dev2.zimbra-vnc.de”}
}

+ (void)postRequestWithSubUrl:(NSString *)suburl params:(NSDictionary *)params {
    UIBackgroundTaskIdentifier bgTask = UIBackgroundTaskInvalid;
    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        NSLog(@"[FirebaseActionsManager][postRequestWithSubUrl] beginBackgroundTaskWithExpirationHandler expired");
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
    }];


    NSString *baseUrl = [[NSUserDefaults standardUserDefaults] stringForKey:@"apiUrl"];
    NSString *token = [[NSUserDefaults standardUserDefaults] stringForKey:@"auth-token"];

    NSLog(@"[FirebaseActionsManager][postRequestWithSubUrl] baseUrl: %@, token: %@, params: %@", baseUrl, token, params);

    NSString *targetUrl = [NSString stringWithFormat:@"%@/%@", baseUrl, suburl];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];

    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:params options:0 error:&error];

    [request setHTTPBody:postData];
    [request setHTTPMethod:@"POST"];
    [request setURL:[NSURL URLWithString:targetUrl]];

    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request setValue:token forHTTPHeaderField: @"Authorization"];

    [[[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:
      ^(NSData * _Nullable data,
        NSURLResponse * _Nullable response,
        NSError * _Nullable error) {

            NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;

            NSString *responseStr = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            NSLog(@"[FirebaseActionsManager][postRequestWithSubUrl] response: %@, error %@, status code %ld", responseStr, error, (long)[httpResponse statusCode]);

            // AFTER ALL THE UPDATES, close the task
            if (bgTask != UIBackgroundTaskInvalid) {
               [[UIApplication sharedApplication] endBackgroundTask:bgTask];
            }
    }] resume];
}

@end
