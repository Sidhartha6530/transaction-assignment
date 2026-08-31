package com.example.transactionstarter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class TransactionStarterApplication {
    private static boolean launchedFromMain;

    public static void main(String[] args) {
        launchedFromMain = true;
        SpringApplication.run(TransactionStarterApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openApplicationInBrowser() {
        if (!launchedFromMain || !Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
        } catch (Exception ignored) {
        }
    }
}
