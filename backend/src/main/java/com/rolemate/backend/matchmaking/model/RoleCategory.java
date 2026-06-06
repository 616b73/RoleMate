package com.rolemate.backend.matchmaking.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Predefined role categories supported by the RoleMate matchmaking system.
 * Users are matched only within the same role category.
 */
public enum RoleCategory {

    BACKEND_ENGINEERING("Backend Engineering"),
    FRONTEND_ENGINEERING("Frontend Engineering"),
    FULL_STACK_DEVELOPMENT("Full Stack Development"),
    DATA_SCIENCE("Data Science"),
    DEVOPS("DevOps"),
    UI_UX_DESIGN("UI/UX Design");

    private final String displayName;

    /** Lookup map keyed by lower-cased display name for flexible input matching. */
    private static final Map<String, RoleCategory> DISPLAY_NAME_LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            rc -> rc.displayName.toLowerCase(),
                            Function.identity()
                    ));

    RoleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a user-provided role string to a {@link RoleCategory}.
     * Matching is case-insensitive and trimmed.
     *
     * @param input the raw role string from the client
     * @return the matching {@link RoleCategory}, or {@code null} if no match is found
     */
    public static RoleCategory fromDisplayName(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return DISPLAY_NAME_LOOKUP.get(input.trim().toLowerCase());
    }
}
