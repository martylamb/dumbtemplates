package com.martiansoftware.dumbtemplates;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * A DumbTemplateStore that loads templates from the filesystem.  Templates
 * are loaded lazily upon first request.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbLazyFileTemplateStore extends DumbTemplateStore {
    
    private final File _dir;
    
    /**
     * Creates a new DumbLazyFileTemplateStore rooted at the specified
     * directory (e.g., "/home/me/templates").  All templates names are resolved
     * relative to this path.
     * 
     * @param dir the root directory for templates in this store
     */    
    public DumbLazyFileTemplateStore(File dir) {
        this(dir, null);
    }
    
    /**
     * Creates a new DumbLazyFileTemplateStore rooted at the specified
     * directory (e.g., "/home/me/templates").  All templates names are resolved
     * relative to this path.
     * 
     * @param dir the root directory for templates in this store
     * @param log receives logging events
     */    
    public DumbLazyFileTemplateStore(File dir, DumbLogger log) {
        super(log);
        _dir = dir;
    }
    
    @Override public DumbTemplate get(String templatePath) {
        System.out.println("Requested: " + templatePath);
        DumbTemplate result = _templates.get(templatePath);
        if (result != null) return result;

        File f = _dir;
        for (String part : Util.splitAndNormalizePath(new LinkedList<>(), templatePath)) {
            f = new File(f, part);
        }
        
        try {
            add(templatePath, new FileReader(f));
            return super.get(templatePath);
        } catch (IOException e) {
            exception(e);
        }
        return null;
    }
    
}
