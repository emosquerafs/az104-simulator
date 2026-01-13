package co.singularit.az104simulator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Locale;

@Component
@Slf4j
public class BootDiagnosticsRunner implements ApplicationRunner {

    private final Environment environment;
    private final MessageSource messageSource;

    public BootDiagnosticsRunner(Environment environment, MessageSource messageSource) {
        this.environment = environment;
        this.messageSource = messageSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("===============================================================");
        log.info("             BOOT DIAGNOSTICS - MARKER DETECTED               ");
        log.info("===============================================================");

        log.info("Default Charset: {}", Charset.defaultCharset());
        log.info("file.encoding: {}", System.getProperty("file.encoding"));

        String[] profiles = environment.getActiveProfiles();
        log.info("Active Profiles: {}", profiles.length > 0 ? String.join(", ", profiles) : "default");

        log.info("server.servlet.encoding.charset: {}",
            environment.getProperty("server.servlet.encoding.charset", "NOT SET"));
        log.info("server.servlet.encoding.force: {}",
            environment.getProperty("server.servlet.encoding.force", "NOT SET"));

        log.info("spring.messages.encoding: {}",
            environment.getProperty("spring.messages.encoding", "NOT SET"));

        try {
            String testMsg = messageSource.getMessage("domain.COMPUTE", null, Locale.forLanguageTag("es"));
            log.info("TEST MESSAGE (domain.COMPUTE, es): '{}'", testMsg);
            log.info("Contains accent? {}", testMsg.contains("\u00F3"));
        } catch (Exception e) {
            log.error("ERROR retrieving test message: {}", e.getMessage());
        }

        log.info("===============================================================");
    }
}

