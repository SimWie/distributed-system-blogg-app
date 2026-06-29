package ch.hftm.ds.blog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.CommentService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.Comment;
import ch.hftm.ds.blog.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class BlogServiceTest {
    @Inject
    BlogService blogService;

    @Inject
    UserService userService;

    @Inject
    CommentService commentService;

    @Test
    void listingAndAddingBlogs() {
        // Arrange
        User author = new User("testuser");
        userService.addUser(author);
        Blog blog = new Blog("Testing Blog", "This is my testing blog", author);
        int blogsBefore = blogService.getBlogs().size();

        // Act
        blogService.addBlog(blog);
        List<Blog> blogs = blogService.getBlogs();

        // Assert
        assertEquals(blogsBefore + 1, blogs.size());
    }

    @Test
    void addingUserAndRetrievingIt() {
        // Arrange
        User user = new User("alice");
        int usersBefore = userService.getUsers().size();

        // Act
        userService.addUser(user);
        List<User> users = userService.getUsers();

        // Assert
        assertEquals(usersBefore + 1, users.size());
        assertEquals("alice", users.get(users.size() - 1).getUsername());
    }

    @Test
    void addingCommentToBlog() {
        // Arrange
        User author = new User("commenter");
        userService.addUser(author);
        Blog blog = new Blog("Blog with comments", "Some content", author);
        blogService.addBlog(blog);

        Comment comment = new Comment("Great post!", blog, author);
        int commentsBefore = commentService.getCommentsByBlogId(blog.getId()).size();

        // Act
        commentService.addComment(comment);
        List<Comment> comments = commentService.getCommentsByBlogId(blog.getId());

        // Assert
        assertEquals(commentsBefore + 1, comments.size());
        assertEquals("commenter", comments.get(0).getAuthor().getUsername());
        assertNotNull(comments.get(0).getBlog());
    }
}
