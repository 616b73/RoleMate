package com.rolemate.backend.matchmaking.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link RoleCategory} enum and its display-name lookup logic.
 */
class RoleCategoryTest {

    @Test
    void fromDisplayName_exactMatch_returnsCategory() {
        assertEquals(RoleCategory.BACKEND_ENGINEERING, RoleCategory.fromDisplayName("Backend Engineering"));
        assertEquals(RoleCategory.FRONTEND_ENGINEERING, RoleCategory.fromDisplayName("Frontend Engineering"));
        assertEquals(RoleCategory.FULL_STACK_DEVELOPMENT, RoleCategory.fromDisplayName("Full Stack Development"));
        assertEquals(RoleCategory.DATA_SCIENCE, RoleCategory.fromDisplayName("Data Science"));
        assertEquals(RoleCategory.DEVOPS, RoleCategory.fromDisplayName("DevOps"));
        assertEquals(RoleCategory.UI_UX_DESIGN, RoleCategory.fromDisplayName("UI/UX Design"));
    }

    @Test
    void fromDisplayName_caseInsensitive_returnsCategory() {
        assertEquals(RoleCategory.BACKEND_ENGINEERING, RoleCategory.fromDisplayName("backend engineering"));
        assertEquals(RoleCategory.BACKEND_ENGINEERING, RoleCategory.fromDisplayName("BACKEND ENGINEERING"));
        assertEquals(RoleCategory.DEVOPS, RoleCategory.fromDisplayName("devops"));
        assertEquals(RoleCategory.UI_UX_DESIGN, RoleCategory.fromDisplayName("ui/ux design"));
    }

    @Test
    void fromDisplayName_withWhitespace_returnsCategory() {
        assertEquals(RoleCategory.DATA_SCIENCE, RoleCategory.fromDisplayName("  Data Science  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "Machine Learning", "Product Management", ""})
    void fromDisplayName_unknownRole_returnsNull(String input) {
        assertNull(RoleCategory.fromDisplayName(input));
    }

    @Test
    void fromDisplayName_null_returnsNull() {
        assertNull(RoleCategory.fromDisplayName(null));
    }

    @Test
    void getDisplayName_returnsHumanReadable() {
        assertEquals("Backend Engineering", RoleCategory.BACKEND_ENGINEERING.getDisplayName());
        assertEquals("UI/UX Design", RoleCategory.UI_UX_DESIGN.getDisplayName());
    }

    @Test
    void allValues_haveNonBlankDisplayNames() {
        for (RoleCategory role : RoleCategory.values()) {
            assertNotNull(role.getDisplayName());
            assertFalse(role.getDisplayName().isBlank());
        }
    }
}
