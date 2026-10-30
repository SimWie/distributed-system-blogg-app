package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.boundry.dto.BlogDTO;
import ch.hftm.ds.blog.boundry.dto.NewBlogDTO;
import ch.hftm.ds.blog.boundry.dto.UpdateBlogDTO;
import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.User;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
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

/**
 * Lesen ist öffentlich, Schreiben braucht die Rolle {@code user} oder
 * {@code admin} (siehe Berechtigungskonzept im README).
 */
@Path("blogs")
@ApplicationScoped
@RolesAllowed({ "user", "admin" })
public class BlogResource {

    @Inject
    BlogService blogService;

    @Inject
    UserService userService;

    @Inject
    SecurityIdentity identity;

    /**
     * Gibt alle Blogs zurück. Über den optionalen Query-Parameter
     * {@code search} kann nach Blogs gefiltert werden, deren Titel den
     * Suchbegriff enthält (case-insensitive), z. B. {@code /blogs?search=quarkus}.
     */
    @GET
    @PermitAll
    public List<BlogDTO> getBlogs(@QueryParam("search") String search) {
        return blogService.getBlogs(search).stream().map(BlogDTO::from).toList();
    }

    @GET
    @PermitAll
    @Path("{id}")
    public Response getBlogById(@PathParam("id") Long id) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return blogNotFound(id);
        }
        return Response.ok(BlogDTO.from(blog)).build();
    }

    /**
     * Legt einen neuen Blog an. Autor ist immer der angemeldete Benutzer
     * (Benutzername aus dem Access-Token). Existiert er lokal noch nicht,
     * wird er angelegt.
     */
    @POST
    public Response addBlog(@NotNull @Valid NewBlogDTO newBlog) {
        User author = userService.getOrCreateUser(identity.getPrincipal().getName());
        Blog blog = newBlog.toEntity(author);
        blogService.addBlog(blog);
        return Response.status(Response.Status.CREATED).entity(BlogDTO.from(blog)).build();
    }

    /**
     * Aktualisiert einen Blog. Nur der Autor oder ein Admin darf das.
     */
    @PUT
    @Path("{id}")
    public Response updateBlog(@PathParam("id") Long id, @NotNull @Valid UpdateBlogDTO update) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return blogNotFound(id);
        }
        if (!isAuthorOrAdmin(blog)) {
            return notAuthor(id);
        }
        Blog updated = blogService.updateBlog(id, update.getTitle(), update.getContent());
        if (updated == null) {
            return blogNotFound(id);
        }
        return Response.ok(BlogDTO.from(updated)).build();
    }

    /**
     * Löscht einen Blog. Nur der Autor oder ein Admin darf das.
     */
    @DELETE
    @Path("{id}")
    public Response deleteBlog(@PathParam("id") Long id) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null) {
            return blogNotFound(id);
        }
        if (!isAuthorOrAdmin(blog)) {
            return notAuthor(id);
        }
        boolean deleted = blogService.deleteBlog(id);
        if (!deleted) {
            return blogNotFound(id);
        }
        return Response.noContent().build();
    }

    private boolean isAuthorOrAdmin(Blog blog) {
        if (identity.hasRole("admin")) {
            return true;
        }
        return blog.getAuthor() != null
                && blog.getAuthor().getUsername().equals(identity.getPrincipal().getName());
    }

    private Response notAuthor(Long id) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Only the author or an admin may modify blog with id " + id))
                .build();
    }

    private Response blogNotFound(Long id) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Blog with id " + id + " not found"))
                .build();
    }
}
