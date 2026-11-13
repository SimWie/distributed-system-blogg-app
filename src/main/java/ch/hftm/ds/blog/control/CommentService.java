package ch.hftm.ds.blog.control;

import java.util.List;

import ch.hftm.ds.blog.entity.Comment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CommentService {
    @Inject
    CommentRepository commentRepository;

    public List<Comment> getCommentsByBlogId(Long blogId) {
        var comments = commentRepository.list("blog.id", blogId);
        Log.debugf("Returning %d comments for blog %d", comments.size(), blogId);
        return comments;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    @Transactional
    public void addComment(Comment comment) {
        commentRepository.persist(comment);
        Log.debugf("Persisted comment %d on blog %d", comment.getId(), comment.getBlog().getId());
    }

    @Transactional
    public boolean deleteComment(Long id) {
        boolean deleted = commentRepository.deleteById(id);
        Log.debugf("Delete comment %d: %s", id, deleted ? "deleted" : "not found");
        return deleted;
    }
}
