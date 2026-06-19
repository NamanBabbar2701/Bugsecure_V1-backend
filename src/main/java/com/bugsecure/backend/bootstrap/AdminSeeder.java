package com.bugsecure.backend.bootstrap;

import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        System.out.println("Connected DB: " + mongoTemplate.getDb().getName());
        System.out.println("Total users: " + userRepository.count());

        System.out.println("🔥 Seeding admins if missing...");

        createAdminIfMissing("goutamp0242@gmail.com", "Goutam@123", "Goutam");
        createAdminIfMissing("namanbabbar37@gmail.com", "Naman@123", "Naman");
        createAdminIfMissing("bugsecure12admin@gmail.com", "BugSecure12Admin", "BugSecureAdmin");

        System.out.println("✅ Admin seeding completed");
    }

    private void createAdminIfMissing(String email, String rawPassword, String username) {
        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            System.out.println("⚡ Admin already exists: " + email);
            return;
        }

        User admin = new User();
        admin.setEmail(email.trim());
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole("ADMIN");

        // Preserve existing schema defaults/values (required)
        admin.setBalance(0.0);
        admin.setContractAccepted(false);
        admin.setCompanyAgreementAccepted(false);

        userRepository.save(admin);

        System.out.println("✅ Admin created: " + email);
    }
}