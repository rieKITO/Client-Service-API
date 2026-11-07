package ru.liga.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clearClients() {
        jdbc.update("DELETE FROM client_account");
    }

    @Test
    void createsActiveClientAndPersistsAllFields() throws Exception {
        JsonNode created = create("{\"fullName\":\"  Иванов Иван Иванович  \",\"gender\":\"M\"}");
        long id = created.get("id").asLong();

        assertThat(created.get("fullName").asText()).isEqualTo("Иванов Иван Иванович");
        assertThat(created.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(created.get("createdAt").asText()).isEqualTo(created.get("updatedAt").asText());
        assertThat(Instant.parse(created.get("createdAt").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(jdbc.queryForObject("SELECT full_name FROM client_account WHERE id = ?", String.class, id))
                .isEqualTo("Иванов Иван Иванович");

        mvc.perform(get("/api/clients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("M"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void allowsOmittedGender() throws Exception {
        JsonNode client = create("{\"fullName\":\"Петров Пётр\"}");
        assertThat(client.get("gender").isNull()).isTrue();
    }

    @Test
    void returnsEmptyList() throws Exception {
        mvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listsAllClientsInIdOrder() throws Exception {
        JsonNode first = create("{\"fullName\":\"Первый\",\"gender\":\"M\"}");
        JsonNode second = create("{\"fullName\":\"Вторая\",\"gender\":\"F\"}");

        mvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.get("id").asLong()))
                .andExpect(jsonPath("$[1].id").value(second.get("id").asLong()));
    }

    @Test
    void updatesOnlyStatusAndPreservesCreationTime() throws Exception {
        long id = create("{\"fullName\":\"Иванов Иван\",\"gender\":\"M\"}").get("id").asLong();

        jdbc.update("UPDATE client_account SET modify_dttm = '2000-01-01T00:00:00Z' WHERE id = ?", id);
        JsonNode before = read(mvc.perform(get("/api/clients/{id}", id)).andExpect(status().isOk()));
        JsonNode updated = read(
                mvc.perform(
                        put("/api/clients/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}")
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.fullName").value("Иванов Иван"))
                        .andExpect(jsonPath("$.gender").value("M"))
                        .andExpect(jsonPath("$.status").value("INACTIVE"))
        );

        assertThat(updated.get("createdAt")).isEqualTo(before.get("createdAt"));

        Instant persisted = jdbc.queryForObject(
                "SELECT modify_dttm FROM client_account WHERE id = ?",
                (row, index) -> row.getTimestamp(1).toInstant(),
                id
        );

        assertThat(persisted).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
        assertThat(Instant.parse(updated.get("updatedAt").asText()).truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
                .isEqualTo(persisted.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void updatesAllMutableFields() throws Exception {
        long id = create("{\"fullName\":\"Первый\",\"gender\":\"M\"}").get("id").asLong();
        mvc.perform(
                put("/api/clients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  Петрова Анна  \",\"gender\":\"F\",\"status\":\"INACTIVE\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Петрова Анна"))
                .andExpect(jsonPath("$.gender").value("F"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        assertThat(jdbc.queryForObject("SELECT status FROM client_account WHERE id = ?", String.class, id))
                .isEqualTo("INACTIVE");
    }

    @Test
    void emptyAndRepeatedUpdatesDoNotChangeTimestamp() throws Exception {
        JsonNode created = create("{\"fullName\":\"Иванов Иван\",\"gender\":\"M\"}");
        long id = created.get("id").asLong();

        for (String body : new String[]{"{}", "{\"status\":\"ACTIVE\"}"}) {
            mvc.perform(put("/api/clients/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedAt").value(created.get("updatedAt").asText()));
        }
    }

    @Test
    void physicallyDeletesClientAndRepeatedDeleteReturns404() throws Exception {
        long id = create("{\"fullName\":\"Иванов Иван\"}").get("id").asLong();

        mvc.perform(delete("/api/clients/{id}", id))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/clients/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/clients")).andExpect(content().json("[]"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM client_account WHERE id = ?", Long.class, id))
                .isZero();

        mvc.perform(delete("/api/clients/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void missingClientReturns404ForReadUpdateAndDelete() throws Exception {
        mvc.perform(get("/api/clients/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/clients/9223372036854775807").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/clients/9223372036854775807")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"fullName\":null}", "{\"fullName\":\"\"}", "{\"fullName\":\"   \"}",
            "{\"fullName\":123}", "{\"fullName\":true}", "{\"fullName\":{}}",
            "{\"fullName\":\"Иван\",\"gender\":\"X\"}", "{\"fullName\":\"Иван\",\"gender\":0}",
            "{\"fullName\":\"Иван\",\"status\":\"INACTIVE\"}",
            "{\"fullName\":\"Иван\",\"id\":42}", "{\"fullNmae\":\"Иван\"}", "{", "null", "[]"
    })
    void rejectsInvalidCreateRequest(String body) throws Exception {
        mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM client_account", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"fullName\":null}", "{\"gender\":null}", "{\"status\":null}",
            "{\"fullName\":\"\"}", "{\"fullName\":\"   \"}", "{\"fullName\":123}",
            "{\"gender\":\"X\"}", "{\"status\":\"DELETED\"}", "{\"status\":1}",
            "{\"id\":42}", "{\"createdAt\":\"2000-01-01T00:00:00Z\"}"
    })
    void rejectsInvalidUpdateWithoutChangingClient(String body) throws Exception {
        JsonNode created = create("{\"fullName\":\"Иванов Иван\",\"gender\":\"M\"}");
        long id = created.get("id").asLong();
        mvc.perform(put("/api/clients/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/clients/{id}", id)).andExpect(content().json(created.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "1.5"})
    void rejectsInvalidId(String id) throws Exception {
        mvc.perform(get("/api/clients/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void exposesOpenApiAndSwaggerUi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/clients'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/clients'].get").exists())
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].delete.responses['204']").exists());
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    private JsonNode create(String body) throws Exception {
        ResultActions result = mvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        JsonNode client = read(result);
        result.andExpect(header().string("Location", endsWith("/api/clients/" + client.get("id").asLong())));

        return client;
    }

    private JsonNode read(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }
    
}
