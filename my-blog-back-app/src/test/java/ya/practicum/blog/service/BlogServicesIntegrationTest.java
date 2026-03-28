package ya.practicum.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import ya.practicum.blog.config.DatabaseConfig;
import ya.practicum.blog.config.ValidationConfig;
import ya.practicum.blog.dto.CommentResponseDto;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.dto.PostListResponseDto;
import ya.practicum.blog.dto.PostResponseDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;
import ya.practicum.blog.exception.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {DatabaseConfig.class, ValidationConfig.class, BlogServicesIntegrationTest.ServiceTestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class BlogServicesIntegrationTest {
    @Configuration
    @ComponentScan(basePackages = {
            "ya.practicum.blog.repository",
            "ya.practicum.blog.service"
    })
    static class ServiceTestConfig {
    }

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    void shouldCreateAndGetPost() {
        PostResponseDto created = postService.createPost(
                new PostUpsertRequestDto(null, "First post", "Hello markdown", List.of("java", "spring"))
        );

        PostResponseDto fetched = postService.getPost(created.id());
        assertEquals("First post", fetched.title());
        assertEquals("Hello markdown", fetched.text());
        assertEquals(List.of("java", "spring"), fetched.tags());
    }

    @Test
    void shouldTruncateTextOnFeedResponse() {
        String longText = "a".repeat(160);
        postService.createPost(new PostUpsertRequestDto(null, "Title", longText, List.of("tag")));

        PostListResponseDto feed = postService.getPosts("", 1, 10);
        assertEquals(1, feed.posts().size());
        assertEquals(131, feed.posts().getFirst().text().length());
        assertTrue(feed.posts().getFirst().text().endsWith("..."));
    }

    @Test
    void shouldRejectInvalidPostPayload() {
        assertThrows(BadRequestException.class, () -> postService.createPost(
                new PostUpsertRequestDto(null, " ", "text", List.of("tag"))
        ));

        assertThrows(BadRequestException.class, () -> postService.createPost(
                new PostUpsertRequestDto(null, "Title", "text", List.of())
        ));
    }

    @Test
    void shouldUpdateCommentWithBrokenFrontendPathFallback() {
        PostResponseDto firstPost = postService.createPost(
                new PostUpsertRequestDto(null, "Post 1", "Body 1", List.of("tag"))
        );
        postService.createPost(new PostUpsertRequestDto(null, "Post 2", "Body 2", List.of("tag")));

        CommentResponseDto createdComment = commentService.createComment(
                firstPost.id(),
                new CommentUpsertRequestDto(null, firstPost.id(), "Comment body")
        );

        long brokenPathPostId = createdComment.id();
        CommentResponseDto updated = commentService.updateComment(
                brokenPathPostId,
                createdComment.id(),
                new CommentUpsertRequestDto(createdComment.id(), firstPost.id(), "Updated body")
        );

        assertEquals("Updated body", updated.text());
        assertEquals(firstPost.id(), updated.postId());
    }
}
