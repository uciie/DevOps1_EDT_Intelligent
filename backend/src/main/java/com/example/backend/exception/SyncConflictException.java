package com.example.backend.exception;

import com.example.backend.dto.SyncConflictDTO;

/**
 * Exception levée lorsque des conflits de créneaux sont détectés lors de la synchronisation.
 */
public class SyncConflictException extends Exception {
    
    private final SyncConflictDTO conflictDetails;

    public SyncConflictException(String message, SyncConflictDTO conflictDetails) {
        super(message);
        this.conflictDetails = conflictDetails;
    }

    public SyncConflictDTO getConflictDetails() {
        return conflictDetails;
    }
}