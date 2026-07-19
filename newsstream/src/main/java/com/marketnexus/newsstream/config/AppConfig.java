package com.marketnexus.newsstream.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NewsStreamProperties.class)
public class AppConfig {}
