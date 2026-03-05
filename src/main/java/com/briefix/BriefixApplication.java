package com.briefix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Briefix Spring Boot application.
 *
 * <p>This class bootstraps the entire application context by delegating to
 * {@link SpringApplication#run(Class, String[])}. The {@link SpringBootApplication}
 * annotation activates component scanning rooted at the {@code com.briefix} package,
 * auto-configuration, and property source loading from {@code application.yml} /
 * {@code application.properties}.</p>
 *
 * <p><b>Thread safety:</b> This class contains only a static entry-point method and
 * holds no mutable state; it is inherently thread-safe.</p>
 *
 * <p><b>Usage:</b> Execute the {@code main} method directly (e.g., via {@code java -jar})
 * or through an IDE run configuration to start the embedded Tomcat server.</p>
 */
@SpringBootApplication
public class BriefixApplication {

    /**
     * Application entry point.
     *
     * <p>Delegates immediately to {@link SpringApplication#run(Class, String[])} which
     * initialises the Spring {@code ApplicationContext}, starts the embedded servlet
     * container, and begins accepting HTTP traffic.</p>
     *
     * @param args command-line arguments forwarded to the Spring environment; may be
     *             used to override property values via {@code --key=value} syntax
     */
    public static void main(String[] args) {
        SpringApplication.run(BriefixApplication.class, args);
    }
}
