package com.cosmic.servercms;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class GameDatabaseConfig {
    @Bean
    @Primary
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "gameDataSource")
    DriverManagerDataSource gameDataSource(@Value("${cosmic.game-database.url}") String url,
                                           @Value("${cosmic.game-database.username}") String username,
                                           @Value("${cosmic.game-database.password}") String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "gameJdbc")
    JdbcTemplate gameJdbc(@Qualifier("gameDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
