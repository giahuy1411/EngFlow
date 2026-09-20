package com.datn.engflow.service;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class StreakEmptyActivityTest {
    @Test
    void emptyGameDoesNotStartStudyStreak() {
        var study = mock(StudyActivityService.class);
        new StreakService(study).checkin(42L, 0, 0);
        verifyNoInteractions(study);
    }
}
