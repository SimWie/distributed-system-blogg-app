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
        Log.debugf("Returning %d blogs", blogs.size());
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
        Log.debugf("Returning %d blogs matching search '%s'", blogs.size(), search);
        return blogs;
    }

    public Blog getBlogById(Long id) {
        return blogRepository.findById(id);
    }

    @Transactional
    public void addBlog(Blog blog) {
        blogRepository.persist(blog);
        Log.debugf("Persisted blog %d", blog.getId());
    }

    /**
     * Aktualisiert Titel und Inhalt eines bestehenden Blogs.
     *
     * @return den aktualisierten Blog, oder {@code null}, falls kein Blog
     *         mit der übergebenen id existiert.
     */
    @Transactional
    public Blog updateBlog(Long id, String title, String content) {
        Blog existing = blogRepository.findById(id);
        if (existing == null) return null;
        existing.setTitle(title);
        existing.setContent(content);
        Log.debugf("Updated blog %d", id);
        return existing;
    }

    @Transactional
    public boolean deleteBlog(Long id) {
        boolean deleted = blogRepository.deleteById(id);
        Log.debugf("Delete blog %d: %s", id, deleted ? "deleted" : "not found");
        return deleted;
    }
}
