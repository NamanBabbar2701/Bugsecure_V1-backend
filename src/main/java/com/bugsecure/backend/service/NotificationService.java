package com.bugsecure.backend.service;

import com.bugsecure.backend.dto.NotificationDTO;
import com.bugsecure.backend.model.Notification;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private NotificationRepository notificationRepository;

    public void createBugStatusNotification(User recipient, String title, String message, String bugReportId) {
        if (recipient == null) return;
        Notification n = new Notification(recipient, "BUG_STATUS", title, message, bugReportId);
        notificationRepository.save(n);
    }

    public int countUnread(User recipient) {
        if (recipient == null) return 0;
        return notificationRepository.countByRecipientAndRead(recipient, false);
    }

    public Page<NotificationDTO> listNotifications(User recipient, Boolean unreadOnly, int page, int pageSize) {
        if (recipient == null) throw new RuntimeException("Recipient not found");

        int safePage = Math.max(0, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));

        Page<Notification> result;
        PageRequest pageable = PageRequest.of(safePage, safePageSize);
        if (unreadOnly == null) {
            result = notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient, pageable);
        } else if (unreadOnly) {
            result = notificationRepository.findByRecipientAndReadOrderByCreatedAtDesc(recipient, false, pageable);
        } else {
            result = notificationRepository.findByRecipientAndReadOrderByCreatedAtDesc(recipient, true, pageable);
        }

        return result.map(this::toDTO);
    }

    public List<NotificationDTO> listTopNotifications(User recipient, int limit) {
        if (recipient == null) throw new RuntimeException("Recipient not found");
        int safeLimit = Math.min(50, Math.max(1, limit));
        // Repository helper is top10; we'll use it and truncate.
        List<Notification> unread = notificationRepository.findTop10ByRecipientAndReadOrderByCreatedAtDesc(recipient, false);
        return unread.stream()
                .limit(safeLimit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void markAsRead(String notificationId, User recipient) {
        if (notificationId == null) return;
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (recipient == null || n.getRecipient() == null || !n.getRecipient().getId().equals(recipient.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setBugReportId(n.getBugReportId());
        dto.setRead(n.getRead());
        dto.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().format(FORMATTER) : null);
        return dto;
    }
}

