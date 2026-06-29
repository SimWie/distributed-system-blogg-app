package ch.hftm.ds.blog;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.CommentService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.Comment;
import ch.hftm.ds.blog.entity.User;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupData {

    @Inject
    UserService userService;

    @Inject
    BlogService blogService;

    @Inject
    CommentService commentService;

    void onStart(@Observes StartupEvent event) {
        if (!userService.getUsers().isEmpty()) {
            Log.info("Startup data already present, skipping.");
            return;
        }

        Log.info("Loading startup data...");

        User alice = new User("alice");
        User bob = new User("bob");
        userService.addUser(alice);
        userService.addUser(bob);

        Blog blog1 = new Blog("Willkommen im Blog", "Das ist mein erster Blogeintrag. Schön, dass du vorbeischaust!", alice);
        Blog blog2 = new Blog("Quarkus ist grossartig", "Quarkus macht Java-Entwicklung schnell und einfach – besonders mit Panache.", alice);
        Blog blog3 = new Blog("Verteilte Systeme", "In diesem Post erkläre ich die Grundlagen von verteilten Systemen.", bob);
        blogService.addBlog(blog1);
        blogService.addBlog(blog2);
        blogService.addBlog(blog3);

        commentService.addComment(new Comment("Super Post, danke!", blog1, bob));
        commentService.addComment(new Comment("Ich stimme zu, Quarkus ist wirklich toll.", blog2, bob));
        commentService.addComment(new Comment("Sehr interessant!", blog3, alice));
    }
}
