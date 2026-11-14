package com.notifyhub;

import com.notifyhub.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class NotifyhubApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // The whole app starts against real MySQL, RabbitMQ and Redis: Flyway runs V1 to V8,
        // Hibernate validates the schema, and all beans wire up. No dependency on your machine's setup.
    }
}