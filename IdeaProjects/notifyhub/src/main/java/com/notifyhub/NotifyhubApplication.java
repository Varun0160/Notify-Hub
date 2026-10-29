package com.notifyhub;

import com.notifyhub.tenant.Tenant;
import com.notifyhub.tenant.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NotifyhubApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyhubApplication.class, args);
    }
//    @Bean
//    CommandLineRunner printBeans(ApplicationContext ctx) {
//        return args -> {
//            System.out.println("Total beans: " + ctx.getBeanDefinitionCount());
//            System.out.println("Has PingService? " + ctx.containsBean("pingService"));
//        };
//    }



//    @Bean
//    CommandLineRunner smokeTest(TenantRepository repo) {
//        return args -> {
//            if (!repo.existsByName("demo-app")) {
//                Tenant saved = repo.save(new Tenant("demo-app"));
//                System.out.println("Saved tenant id=" + saved.getId()
//                        + " publicId=" + saved.getPublicId()
//                        + " createdAt=" + saved.getCreatedAt());
//            }
//        };
//  }
}
