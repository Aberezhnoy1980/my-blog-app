package ya.practicum.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.web.WebAppConfiguration;
import ya.practicum.blog.config.AppConfig;
import ya.practicum.blog.config.DatabaseConfig;
import ya.practicum.blog.config.WebMvcConfig;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = {AppConfig.class, WebMvcConfig.class, DatabaseConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class BlogControllersIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    void shouldCreateAndGetPostViaBothEndpoints() throws Exception {
        PostUpsertRequestDto createRequest = new PostUpsertRequestDto(
                null,
                "Controller post",
                "Controller text",
                List.of("java", "spring")
        );

        MvcResult createPostResult = mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Controller post"))
                .andReturn();
        long postId = objectMapper.readTree(createPostResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId));

        mockMvc.perform(post("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId));
    }

    @Test
    void shouldReturnBadRequestForInvalidPostPayload() throws Exception {
        PostUpsertRequestDto invalidRequest = new PostUpsertRequestDto(
                null,
                " ",
                "body",
                List.of("tag")
        );

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    @Test
    void shouldHandleCommentCrud() throws Exception {
        PostUpsertRequestDto createPost = new PostUpsertRequestDto(null, "P1", "Body", List.of("tag"));
        MvcResult createPostResult = mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createPost)))
                .andExpect(status().isOk())
                .andReturn();
        long postId = objectMapper.readTree(createPostResult.getResponse().getContentAsString()).get("id").asLong();

        CommentUpsertRequestDto createComment = new CommentUpsertRequestDto(null, postId, "Comment body");
        MvcResult createCommentResult = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andReturn();
        long commentId = objectMapper.readTree(createCommentResult.getResponse().getContentAsString()).get("id").asLong();

        CommentUpsertRequestDto updateComment = new CommentUpsertRequestDto(commentId, postId, "Updated");
        mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated"));

        mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundForMissingPost() throws Exception {
        mockMvc.perform(get("/api/posts/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"));
    }
}
