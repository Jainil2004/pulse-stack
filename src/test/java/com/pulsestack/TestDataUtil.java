package com.pulsestack;

import com.pulsestack.model.Role;
import com.pulsestack.model.User;

public final class TestDataUtil {
    public TestDataUtil() {

    }

    public static User createNormalUser1() {
        return User.builder()
//                .id(1L)
                .username("user1")
                .password("user1")
                .role(Role.USER)
                .build();
    }

    public static User createNormalUser2() {
        return User.builder()
//                .id(2L)
                .username("user2")
                .password("user2")
                .role(Role.USER)
                .build();
    }

    public static User createNormalUser3() {
        return User.builder()
//                .id(3L)
                .username("user3")
                .password("user3")
                .role(Role.USER)
                .build();
    }

    public static User createAdminUser1() {
        return User.builder()
//                .id(4L)
                .username("adminUser1")
                .password("adminUser1")
                .role(Role.ADMIN)
                .build();
    }
}
