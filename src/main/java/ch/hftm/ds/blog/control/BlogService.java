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

    public Blog getBlogById(Long id) {
        return blogRepository.findById(id);
    }

    @Transactional
    public void addBlog(Blog blog) {
        Log.info("Adding blog " + blog.getTitle());
        blogRepository.persist(blog);
    }

    @Transactional
    public boolean updateBlog(Long id, Blog updated) {
        Blog existing = blogRepository.findById(id);
        if (existing == null) return false;
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        Log.info("Updating blog " + id);
        return true;
    }

    @Transactional
    public boolean deleteBlog(Long id) {
        Log.info("Deleting blog " + id);
        return blogRepository.deleteById(id);
    }
}
