package com.notifyhub.support;

import com.notifyhub.auth.UserRepository;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.NotificationAttemptRepository;
import com.notifyhub.notification.NotificationRepository;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.recipient.RecipientRepository;
import com.notifyhub.security.ApiKeyGenerator;
import com.notifyhub.template.Template;
import com.notifyhub.template.TemplateRepository;
import com.notifyhub.tenant.ApiKey;
import com.notifyhub.tenant.ApiKeyRepository;
import com.notifyhub.tenant.Tenant;
import com.notifyhub.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // Singleton containers: started once per test run, shared by every test class.
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("notifyhub").withUsername("notifyhub").withPassword("notifyhub");
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        Startables.deepStart(MYSQL, RABBIT, REDIS).join();   // start all three in parallel
    }

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);

        r.add("spring.rabbitmq.host", RABBIT::getHost);
        r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        r.add("spring.rabbitmq.username", () -> "guest");
        r.add("spring.rabbitmq.password", () -> "guest");

        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Keep limits out of the way (the limiter has its own focused test)
        r.add("notifyhub.ratelimit.tenant.capacity", () -> "10000");
        r.add("notifyhub.ratelimit.recipient.capacity", () -> "1000");
        // The test drives the sweeper itself, so the background one should stay quiet
        r.add("notifyhub.sweeper.interval-ms", () -> "3600000");
    }

    @Value("${local.server.port}")
    protected int port;

    @Value("${notifyhub.webhooks.secret}")
    protected String webhookSecret;

    @Autowired protected TenantRepository tenantRepository;
    @Autowired protected ApiKeyRepository apiKeyRepository;
    @Autowired protected RecipientRepository recipientRepository;
    @Autowired protected TemplateRepository templateRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected NotificationAttemptRepository attemptRepository;
    @Autowired protected UserRepository userRepository;

    protected TestApi api() {
        return new TestApi("http://localhost:" + port);
    }

    /** A brand-new tenant with a working API key and an EMAIL template called "welcome". */
    protected Tenancy newTenant() {
        Tenant tenant = tenantRepository.save(new Tenant("t-" + UUID.randomUUID()));
        ApiKeyGenerator.GeneratedKey key = ApiKeyGenerator.generate();
        apiKeyRepository.save(new ApiKey(tenant, "test", key.prefix(), key.hash()));
        templateRepository.save(new Template(tenant, "welcome", Channel.EMAIL, 1,
                "Welcome {{name}}", "<p>Hi {{name}}</p>"));
        return new Tenancy(tenant, key.rawKey());
    }

    protected Recipient newRecipient(Tenant tenant) {
        Recipient recipient = new Recipient(tenant, "u-" + UUID.randomUUID().toString().substring(0, 8));
        recipient.setEmail("user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        return recipientRepository.save(recipient);
    }

    protected String sendBody(String recipientPublicId) {
        return "{\"recipientId\":\"" + recipientPublicId + "\",\"channel\":\"EMAIL\","
                + "\"template\":\"welcome\",\"variables\":{\"name\":\"Ravi\"}}";
    }

    public record Tenancy(Tenant tenant, String apiKey) {
    }
}