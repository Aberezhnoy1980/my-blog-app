package ya.practicum.blog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ya.practicum.blog.BlogConstraints;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BlogControllersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
        // H2: DELETE does not reset identity sequences; keep IDs deterministic for integration tests.
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE comments ALTER COLUMN id RESTART WITH 1");
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
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        var idNode = objectMapper.readTree(createPostResult.getResponse().getContentAsString()).get("id");
        assertNotNull(idNode, "Create post response must contain id");
        long postId = idNode.asLong();
        assertTrue(postId > 0, "Post id must be positive");

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId));

        mockMvc.perform(post("/api/posts/" + postId))
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

    @Test
    void shouldReturnEmptyFeedForBlankSearchAndUnknownHashtag() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0))
                .andExpect(jsonPath("$.lastPage").value(1));

        mockMvc.perform(get("/api/posts")
                        .param("search", "#nonexistent")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void shouldAcceptLargePageSizeWhenFewPosts() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new PostUpsertRequestDto(null, "Solo", "body", List.of("solo")))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void shouldReturnBadRequestForNullPostText() throws Exception {
        PostUpsertRequestDto invalid = new PostUpsertRequestDto(null, "Title", null, List.of("tag"));
        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("text is required"));
    }

    @Test
    void shouldReturnBadRequestForOversizedTitle() throws Exception {
        String tooLong = "x".repeat(BlogConstraints.MAX_TITLE_LENGTH + 1);
        PostUpsertRequestDto invalid = new PostUpsertRequestDto(null, tooLong, "body", List.of("tag"));
        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is too long"));
    }

    @Test
    void shouldReturnBadRequestForNonPositivePaging() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageNumber and pageSize must be positive"));
    }
}
