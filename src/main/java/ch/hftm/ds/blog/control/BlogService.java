package ch.hftm.ds.blog.control;

import java.util.List;

import ch.hftm.ds.blog.entity.Blog;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BlogService {
    @Inject
    BlogRepository blogRepository;

    public List<Blog> getBlogs() {
        var blogs = blogRepository.listAll();
        Log.info("Returning " + blogs.size() + " blogs");
        return blogs;
    }

    /**
     * Gibt alle Blogs zurück, deren Titel den übergebenen Suchbegriff
     * enthält (Gross-/Kleinschreibung wird ignoriert). Ist der Suchbegriff
     * leer oder null, wird das Verhalten von {@link #getBlogs()} verwendet.
     */
    public List<Blog> getBlogs(String search) {
        if (search == null || search.isBlank()) {
            return getBlogs();
        }
        var blogs = blogRepository.list("lower(title) like ?1", "%" + search.toLowerCase() + "%");
        Log.info("Returning " + blogs.size() + " blogs matching search '" + search + "'");
        return blogs;
    }

    public Blog getBlogById(Long id) {
        return blogRepository.findById(id);
    }

    @Transactional
    public void addBlog(Blog blog) {
        Log.info("Adding blog " + blog.getTitle());
        blogRepository.persist(blog);
    }

    /**
     * Aktualisiert Titel und Inhalt eines bestehenden Blogs.
     *
     * @return den aktualisierten Blog, oder {@code null}, falls kein Blog
     *         mit der übergebenen id existiert.
     */
    @Transactional
    public Blog updateBlog(Long id, Blog updated) {
        Blog existing = blogRepository.findById(id);
        if (existing == null) return null;
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        Log.info("Updating blog " + id);
        return existing;
    }

    @Transactional
    public boolean deleteBlog(Long id) {
        Log.info("Deleting blog " + id);
        return blogRepository.deleteById(id);
    }
}
