package org.reciclame.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@Profile("aws")
@EnableRedisIndexedHttpSession
public class RedisSessionConfig {
}
