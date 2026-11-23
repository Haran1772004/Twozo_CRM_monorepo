package com.tlc.crm.hospital.api.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentManagerTest {

    @Test
    void detectsOverlappingAppointments() {
        assertTrue(AppointmentManager.intervalsOverlap(1_000L, 3_000L, 2_000L, 4_000L));
        assertTrue(AppointmentManager.intervalsOverlap(2_000L, 5_000L, 1_000L, 6_000L));
    }

    @Test
    void allowsAppointmentsThatMeetAtTheBoundary() {
        assertFalse(AppointmentManager.intervalsOverlap(1_000L, 2_000L, 2_000L, 3_000L));
        assertFalse(AppointmentManager.intervalsOverlap(2_000L, 3_000L, 1_000L, 2_000L));
    }
}
