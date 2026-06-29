package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.entity.Blog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("blogs")
@ApplicationScoped
public class BlogResource {

    @Inject
    BlogService blogService;

    @GET
    public List<Blog> getBlogs() {
        return blogService.getBlogs();
    }

    @GET
    @Path("{id}")
    public Response getBlogById(@PathParam("id") Long id) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(blog).build();
    }

    @POST
    public Response addBlog(Blog blog) {
        blogService.addBlog(blog);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("{id}")
    public Response updateBlog(@PathParam("id") Long id, Blog blog) {
        boolean updated = blogService.updateBlog(id, blog);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteBlog(@PathParam("id") Long id) {
        boolean deleted = blogService.deleteBlog(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
