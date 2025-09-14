package org.example.userserver.global.dataloader;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SqlDummyDataLoader implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Loading dummy SQL data from data-dummy.sql...");
        try {
            ClassPathResource resource = new ClassPathResource("data-dummy.sql");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String sqlScript = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                    // Split the script into individual statements (assuming statements are separated by ';')
                    List<String> statements = Arrays.asList(sqlScript.split(";"));
                    for (String statement : statements) {
                        if (!statement.trim().isEmpty()) {
                            jdbcTemplate.execute(statement.trim());
                        }
                    }
                    System.out.println("Dummy SQL data loaded successfully.");
                }
            } else {
                System.out.println("data-dummy.sql not found on classpath. Skipping SQL dummy data loading.");
            }
        } catch (Exception e) {
            System.err.println("Error loading dummy SQL data: " + e.getMessage());
            throw e;
        }
    }
}
