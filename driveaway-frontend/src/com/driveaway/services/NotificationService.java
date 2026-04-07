package com.driveaway.services;

import com.driveaway.utils.HttpUtil;

public class NotificationService {

    private static final String BASE_URL = "http://localhost:8080";

    public String getNotificationsForUser(String userId) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/notifications/user/" + userId);
    }

    public String getUnreadNotifications(String userId) {
        return HttpUtil.sendGet(BASE_URL + "/api/v1/notifications/user/" + userId + "/unread");
    }

    public String markAsRead(String notificationId) {
        return HttpUtil.sendPost(BASE_URL + "/api/v1/notifications/" + notificationId + "/read", "{}");
    }
}
