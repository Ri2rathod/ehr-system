package com.example.ehrsystem.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOGGER");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String MASK = "***MASKED***";

    private final Map<String, String> userContext = new ConcurrentHashMap<>();

    public void setUserContext(String userId, String username) {
        userContext.put("userId", userId);
        userContext.put("username", username);
    }

    public void clearUserContext() {
        userContext.clear();
    }

    private String hashReference(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "REF_UNAVAILABLE";
        }
    }

    public void logLoginSuccess(String username, String ipAddress) {
        auditLog.info("EVENT=LOGIN_SUCCESS | USER={} | IP={} | TIMESTAMP={}",
                username, ipAddress, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logLoginFailure(String username, String ipAddress, String reason) {
        auditLog.warn("EVENT=LOGIN_FAILURE | USER={} | IP={} | REASON={} | TIMESTAMP={}",
                username, ipAddress, reason, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logLogout(String username) {
        auditLog.info("EVENT=LOGOUT | USER={} | TIMESTAMP={}",
                username, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logPasswordReset(String username, String resetBy) {
        auditLog.info("EVENT=PASSWORD_RESET | USER={} | RESET_BY={} | TIMESTAMP={}",
                username, resetBy, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logRoleAssigned(String username, String role, String assignedBy) {
        auditLog.info("EVENT=ROLE_ASSIGNED | USER={} | ROLE={} | ASSIGNED_BY={} | TIMESTAMP={}",
                username, role, assignedBy, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logRoleRevoked(String username, String role, String revokedBy) {
        auditLog.info("EVENT=ROLE_REVOKED | USER={} | ROLE={} | REVOKED_BY={} | TIMESTAMP={}",
                username, role, revokedBy, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logPermissionChanged(String username, String permission, String action, String changedBy) {
        auditLog.info("EVENT=PERMISSION_{} | USER={} | PERMISSION={} | CHANGED_BY={} | TIMESTAMP={}",
                action, username, permission, changedBy, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logPatientRecordViewed(String username, Long patientId, String viewReason) {
        StringBuilder sb = new StringBuilder("EVENT=PATIENT_RECORD_VIEWED");
        sb.append(" | USER=").append(username != null ? username : "SYSTEM");
        sb.append(" | PATIENT_REF=").append(hashReference(patientId != null ? patientId.toString() : "unknown"));
        sb.append(" | VIEW_REASON=").append(viewReason);
        sb.append(" | TIMESTAMP=").append(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        auditLog.info(sb.toString());
    }

    public void logPrescriptionSigned(String username, Long prescriptionId, Long patientId) {
        auditLog.info("EVENT=PRESCRIPTION_SIGNED | USER={} | PRESCRIPTION_ID={} | PATIENT_REF={} | TIMESTAMP={}",
                username, prescriptionId, hashReference(patientId != null ? patientId.toString() : "unknown"),
                LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logEncounterFinalized(String username, Long encounterId, Long patientId) {
        auditLog.info("EVENT=ENCOUNTER_FINALIZED | USER={} | ENCOUNTER_ID={} | PATIENT_REF={} | TIMESTAMP={}",
                username, encounterId, hashReference(patientId != null ? patientId.toString() : "unknown"),
                LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public void logCustomEvent(String eventType, Map<String, Object> details) {
        StringBuilder sb = new StringBuilder("EVENT=").append(eventType);
        if (userContext.containsKey("userId")) {
            sb.append(" | USER_ID=").append(userContext.get("userId"));
        }
        if (userContext.containsKey("username")) {
            sb.append(" | USERNAME=").append(userContext.get("username"));
        }
        details.forEach((key, value) -> {
            String safeValue = isPhiField(key) ? MASK : String.valueOf(value);
            sb.append(" | ").append(key).append("=").append(safeValue);
        });
        sb.append(" | TIMESTAMP=").append(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        auditLog.info(sb.toString());
    }

    private boolean isPhiField(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("mrn") ||
               lower.contains("patientid") ||
               lower.contains("phone") ||
               lower.contains("email") ||
               lower.contains("insurance") ||
               lower.contains("allergies") ||
               lower.contains("diagnosis") ||
               lower.contains("prescription") ||
               lower.contains("firstname") ||
               lower.contains("lastname") ||
               lower.contains("address") ||
               lower.contains("dob") ||
               lower.contains("bloodgroup") ||
               lower.contains("ssn");
    }
}