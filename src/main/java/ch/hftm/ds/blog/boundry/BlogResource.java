package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.entity.Blog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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

    /**
     * Gibt alle Blogs zurück. Über den optionalen Query-Parameter
     * {@code search} kann nach Blogs gefiltert werden, deren Titel den
     * Suchbegriff enthält (case-insensitive), z. B. {@code /blogs?search=quarkus}.
     */
    @GET
    public List<Blog> getBlogs(@QueryParam("search") String search) {
        return blogService.getBlogs(search);
    }

    @GET
    @Path("{id}")
    public Response getBlogById(@PathParam("id") Long id) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Blog with id " + id + " not found"))
                    .build();
        }
        return Response.ok(blog).build();
    }

    @POST
    public Response addBlog(@Valid Blog blog) {
        blogService.addBlog(blog);
        return Response.status(Response.Status.CREATED).entity(blog).build();
    }

    @PUT
    @Path("{id}")
    public Response updateBlog(@PathParam("id") Long id, @Valid Blog blog) {
        Blog updated = blogService.updateBlog(id, blog);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Blog with id " + id + " not found"))
                    .build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteBlog(@PathParam("id") Long id) {
        boolean deleted = blogService.deleteBlog(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Blog with id " + id + " not found"))
                    .build();
        }
        return Response.noContent().build();
    }
}
