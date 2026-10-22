package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.boundry.dto.BlogDTO;
import ch.hftm.ds.blog.boundry.dto.NewBlogDTO;
import ch.hftm.ds.blog.boundry.dto.UpdateBlogDTO;
import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("blogs")
@ApplicationScoped
public class BlogResource {

    @Inject
    BlogService blogService;

    @Inject
    UserService userService;

    /**
     * Gibt alle Blogs zurück. Über den optionalen Query-Parameter
     * {@code search} kann nach Blogs gefiltert werden, deren Titel den
     * Suchbegriff enthält (case-insensitive), z. B. {@code /blogs?search=quarkus}.
     */
    @GET
    public List<BlogDTO> getBlogs(@QueryParam("search") String search) {
        return blogService.getBlogs(search).stream().map(BlogDTO::from).toList();
    }

    @GET
    @Path("{id}")
    public Response getBlogById(@PathParam("id") Long id) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return blogNotFound(id);
        }
        return Response.ok(BlogDTO.from(blog)).build();
    }

    /**
     * Legt einen neuen Blog an. Der Autor wird über {@code authorId} im
     * Request-DTO referenziert und muss existieren.
     */
    @POST
    public Response addBlog(@NotNull @Valid NewBlogDTO newBlog) {
        User author = userService.getUserById(newBlog.getAuthorId());
        if (author == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("User with id " + newBlog.getAuthorId() + " not found"))
                    .build();
        }
        Blog blog = newBlog.toEntity(author);
        blogService.addBlog(blog);
        return Response.status(Response.Status.CREATED).entity(BlogDTO.from(blog)).build();
    }

    @PUT
    @Path("{id}")
    public Response updateBlog(@PathParam("id") Long id, @NotNull @Valid UpdateBlogDTO update) {
        Blog updated = blogService.updateBlog(id, update.getTitle(), update.getContent());
        if (updated == null) {
            return blogNotFound(id);
        }
        return Response.ok(BlogDTO.from(updated)).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteBlog(@PathParam("id") Long id) {
        boolean deleted = blogService.deleteBlog(id);
        if (!deleted) {
            return blogNotFound(id);
        }
        return Response.noContent().build();
    }

    private Response blogNotFound(Long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Blog with id " + id + " not found"))
                .build();
    }
}
