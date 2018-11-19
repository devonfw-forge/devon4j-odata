package com.devonfw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main entry point of this {@link SpringBootApplication}. Simply run this class to start this app.
 */
@EnableAutoConfiguration
@SpringBootApplication
@ServletComponentScan
@EntityScan(basePackages = {"com.devonfw.sample.dataaccess.api"})
@EnableJpaRepositories( basePackages = {"com.devonfw.sample.dataaccess.impl"})
public class SpringBootApp {

    /**
     * Entry point for spring-boot based app
     *
     * @param args - arguments
     */
    public static void main(String[] args) {

        SpringApplication.run(SpringBootApp.class, args);
    }
}
