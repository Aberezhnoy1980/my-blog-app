package ya.practicum.blog.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
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
    private final Validator beanValidator;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, Validator beanValidator) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.beanValidator = beanValidator;
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
        for (ConstraintViolation<CommentUpsertRequestDto> v : beanValidator.validate(request)) {
            throw new BadRequestException(v.getMessage());
        }
        if (!idAllowed && request.id() != null) {
            throw new BadRequestException("id must be empty for create");
        }
        if (!idAllowed && request.postId() != null && request.postId() != postId) {
            throw new BadRequestException("Path postId and body postId must be equal");
        }
    }

    private void ensurePostExists(long postId) {
        postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
    }

    private CommentResponseDto toResponse(Comment comment) {
        return new CommentResponseDto(comment.id(), comment.text(), comment.postId());
    }
}
