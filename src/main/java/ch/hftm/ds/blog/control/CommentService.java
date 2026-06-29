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
        Log.info("Returning " + comments.size() + " comments for blog " + blogId);
        return comments;
    }

    public Comment getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    @Transactional
    public void addComment(Comment comment) {
        Log.info("Adding comment by " + comment.getAuthor().getUsername() + " on blog " + comment.getBlog().getId());
        commentRepository.persist(comment);
    }

    @Transactional
    public boolean deleteComment(Long id) {
        Log.info("Deleting comment " + id);
        return commentRepository.deleteById(id);
    }
}
