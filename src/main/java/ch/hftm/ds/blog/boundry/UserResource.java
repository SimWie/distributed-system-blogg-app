package ch.hftm.ds.blog.boundry;

import java.util.List;

import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("users")
@ApplicationScoped
public class UserResource {

    @Inject
    UserService userService;

    @GET
    public List<User> getUsers() {
        return userService.getUsers();
    }

    @GET
    @Path("{id}")
    public Response getUserById(@PathParam("id") Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(user).build();
    }

    @POST
    public Response addUser(User user) {
        userService.addUser(user);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("{id}")
    public Response updateUser(@PathParam("id") Long id, User user) {
        boolean updated = userService.updateUser(id, user);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteUser(@PathParam("id") Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
