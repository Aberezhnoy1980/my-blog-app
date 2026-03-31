package ya.practicum.blog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ya.practicum.blog.dto.CommentResponseDto;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Returns all comments for a post.
     */
    @GetMapping
    public List<CommentResponseDto> getComments(@PathVariable("postId") long postId) {
        return commentService.getComments(postId);
    }

    /**
     * Returns a specific comment for a post.
     */
    @GetMapping("/{id}")
    public CommentResponseDto getComment(@PathVariable("postId") long postId, @PathVariable("id") long id) {
        return commentService.getComment(postId, id);
    }

    /**
     * Creates a new comment for a post.
     */
    @PostMapping
    public CommentResponseDto createComment(@PathVariable("postId") long postId, @Valid @RequestBody CommentUpsertRequestDto request) {
        return commentService.createComment(postId, request);
    }

    /**
     * Updates comment content.
     */
    @PutMapping("/{id}")
    public CommentResponseDto updateComment(
            @PathVariable("postId") long postId,
            @PathVariable("id") long id,
            @Valid @RequestBody CommentUpsertRequestDto request
    ) {
        return commentService.updateComment(postId, id, request);
    }

    /**
     * Deletes a comment from the post.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable("postId") long postId, @PathVariable("id") long id) {
        commentService.deleteComment(postId, id);
        return ResponseEntity.ok().build();
    }
}
