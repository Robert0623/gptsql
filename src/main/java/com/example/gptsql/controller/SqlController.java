package com.example.gptsql.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@RestController
public class SqlController {

    @Value("classpath:/movie.sql")
    private Resource ddlResource;

    @Value("classpath:/sql-prompt-template.st")
    private Resource sqlPromptTemplateResource;

    private final ChatClient aiClient;
    private final JdbcTemplate jdbcTemplate;

    public SqlController(ChatClient.Builder aiClient, JdbcTemplate jdbcTemplate) {
        this.aiClient = aiClient.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/sql")
    public SqlResponse sql(@RequestParam("question") String question) throws IOException {
        String schema = ddlResource.getContentAsString(Charset.defaultCharset()); // UTF-8
        // LLM이 자동으로 SELECT SQL 생성
        String query = aiClient.prompt()
                .user(userSpec -> userSpec
                        .text(sqlPromptTemplateResource)
                        .param("question", question)
                        .param("ddl", schema)
                )
                .call()
                .content();

        if (query.toLowerCase().startsWith("select")) {
            return new SqlResponse(query, jdbcTemplate.queryForList(query));
        }
        return new SqlResponse(query, List.of()); // null
    }

}
