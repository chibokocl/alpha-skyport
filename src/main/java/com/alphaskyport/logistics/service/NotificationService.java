package com.alphaskyport.logistics.service;

import com.alphaskyport.iam.model.User;
import com.alphaskyport.logistics.model.Notification;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional // Ensure notification is saved even if main tx fails? No,
                   // usually we want it bound. But for "queueing", maybe we
                   // want it to be part of the transaction. Let's stick to
                   // default propagation (REQUIRED).
    public void enqueueNotification(User user, Shipment shipment, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setShipment(shipment);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);

        // Generate a simple dedup key for now: TYPE-SHIPMENTID-TIMESTAMP_MINUTE (or
        // just random if we don't have strict dedup requirements yet)
        // Schema says dedup_key is unique. Let's use UUID for now to avoid collisions
        // in this simple implementation.
        // In a real system, this would be deterministic based on the event.
        notification.setDedupKey(type + "-" + shipment.getShipmentId() + "-" + UUID.randomUUID());

        notification.setSendVia(Collections.singletonList("in_app"));
        notification.setStatus("pending");

        notificationRepository.save(notification);
    }
}
