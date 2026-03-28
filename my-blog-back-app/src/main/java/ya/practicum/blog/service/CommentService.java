package ya.practicum.blog.service;

import org.springframework.stereotype.Service;
import ya.practicum.blog.BlogConstraints;
import org.springframework.transaction.annotation.Transactional;
import ya.practicum.blog.dto.CommentResponseDto;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.exception.BadRequestException;
import ya.practicum.blog.exception.NotFoundException;
import ya.practicum.blog.model.Comment;
import ya.practicum.blog.repository.CommentRepository;
import ya.practicum.blog.repository.PostRepository;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    /**
     * Returns all comments for a given post.
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(long postId) {
        ensurePostExists(postId);
        return commentRepository.findByPostId(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns one comment by post and comment ids.
     */
    @Transactional(readOnly = true)
    public CommentResponseDto getComment(long postId, long commentId) {
        ensurePostExists(postId);
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        return toResponse(comment);
    }

    /**
     * Creates a new comment.
     */
    @Transactional
    public CommentResponseDto createComment(long postId, CommentUpsertRequestDto request) {
        ensurePostExists(postId);
        validateRequest(postId, request, false);
        Comment created = commentRepository.create(postId, request.text().trim());
        return toResponse(created);
    }

    /**
     * Updates comment content and keeps compatibility with known frontend URI bug.
     */
    @Transactional
    public CommentResponseDto updateComment(long postId, long commentId, CommentUpsertRequestDto request) {
        validateRequest(postId, request, true);

        Comment target = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseGet(() -> commentRepository.findById(commentId).orElseThrow(
                        () -> new NotFoundException("Comment not found")
                ));

        Comment updated = commentRepository.updateById(target.id(), request.text().trim());
        return toResponse(updated);
    }

    /**
     * Deletes a comment by ids.
     */
    @Transactional
    public void deleteComment(long postId, long commentId) {
        ensurePostExists(postId);
        if (commentRepository.deleteByIdAndPostId(commentId, postId) == 0) {
            throw new NotFoundException("Comment not found");
        }
    }

    private void validateRequest(long postId, CommentUpsertRequestDto request, boolean idAllowed) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (!idAllowed && request.id() != null) {
            throw new BadRequestException("id must be empty for create");
        }
        if (!idAllowed && request.postId() != null && request.postId() != postId) {
            throw new BadRequestException("Path postId and body postId must be equal");
        }
        if (request.text() == null || request.text().isBlank()) {
            throw new BadRequestException("text is required");
        }
        if (request.text().trim().length() > BlogConstraints.MAX_COMMENT_LENGTH) {
            throw new BadRequestException("text is too long");
        }
    }

    private void ensurePostExists(long postId) {
        postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
    }

    private CommentResponseDto toResponse(Comment comment) {
        return new CommentResponseDto(comment.id(), comment.text(), comment.postId());
    }
}
