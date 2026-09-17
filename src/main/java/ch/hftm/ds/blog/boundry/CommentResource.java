package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.CommentService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.Comment;
import ch.hftm.ds.blog.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("blogs/{blogId}/comments")
@ApplicationScoped
public class CommentResource {

    @Inject
    CommentService commentService;

    @Inject
    BlogService blogService;

    @Inject
    UserService userService;

    @GET
    public Response getComments(@PathParam("blogId") Long blogId) {
        Blog blog = blogService.getBlogById(blogId);
        if (blog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Blog with id " + blogId + " not found"))
                    .build();
        }
        List<Comment> comments = commentService.getCommentsByBlogId(blogId);
        return Response.ok(comments).build();
    }

    /**
     * Legt einen neuen Kommentar an. Der Autor wird über den
     * Query-Parameter {@code userId} bestimmt, z. B.
     * {@code POST /blogs/1/comments?userId=2}.
     */
    @POST
    public Response addComment(@PathParam("blogId") Long blogId, @QueryParam("userId") Long userId,
            @Valid Comment comment) {
        Blog blog = blogService.getBlogById(blogId);
        if (blog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Blog with id " + blogId + " not found"))
                    .build();
        }
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Query parameter 'userId' is required"))
                    .build();
        }
        User author = userService.getUserById(userId);
        if (author == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("User with id " + userId + " not found"))
                    .build();
        }
        comment.setBlog(blog);
        comment.setAuthor(author);
        commentService.addComment(comment);
        return Response.status(Response.Status.CREATED).entity(comment).build();
    }

    @DELETE
    @Path("{commentId}")
    public Response deleteComment(@PathParam("blogId") Long blogId, @PathParam("commentId") Long commentId) {
        boolean deleted = commentService.deleteComment(commentId);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Comment with id " + commentId + " not found"))
                    .build();
        }
        return Response.noContent().build();
    }
}
