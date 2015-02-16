package com.martiansoftware.dumbtemplates;

import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * A DumbTemplateStore that loads templates from the classpath.  Templates
 * are loaded lazily upon first request.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbLazyClasspathTemplateStore extends DumbTemplateStore {
    
    private final String _path;
    
    /**
     * Creates a new DumbLazyClasspathTemplateStore rooted at the specified
     * path (e.g., "/my/templates").  All templates names are resolved
     * relative to this path.
     * 
     * @param path the root path for templates in the classpath
     */
    public DumbLazyClasspathTemplateStore(String path) {
        this(path, null);
    }
    
    /**
     * Creates a new DumbLazyClasspathTemplateStore rooted at the specified
     * path (e.g., "/my/templates").  All templates names are resolved
     * relative to this path.
     * 
     * @param path the root path for templates in the classpath
     * @param log receives logging events
     */
    public DumbLazyClasspathTemplateStore(String path, DumbLogger log) {
        super(log);
        _path = path.replaceAll("/*$", "");
    }
    
    @Override public DumbTemplate get(String templatePath) {
        DumbTemplate result = _templates.get(templatePath);
        if (result != null) return result;

        String storePath = Util.joinPath(Util.splitAndNormalizePath(new LinkedList<>(), templatePath));
        String fullPath = new StringBuilder(_path).append("/").append(storePath).toString();
        
        try {
            add(storePath, new InputStreamReader(this.getClass().getResourceAsStream(fullPath)));
            return super.get(storePath);
        } catch (Exception e) {
            error("Exception while loading resource " + fullPath);
            exception(e);
        }
        return null;
    }
    
}
