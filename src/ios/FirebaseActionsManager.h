@interface FirebaseActionsManager

+ (void)registerTalkNotificationCategoriesAndActionsForCallRequest;
+ (BOOL)isVideoAudioCategory:(NSString *)categoryIdentifier;
+ (void)handleCallRequestActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier;
+ (BOOL)isCallRejectActions:(NSDictionary *)mutableUserInfo actionIdentifier:(NSString *)actionIdentifier;

@end
