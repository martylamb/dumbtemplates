package com.martiansoftware.dumbtemplates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Map;

/**
 * A DumbTemplateStore that loads templates from the filesystem.  Templates
 * are loaded lazily upon first request.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbLazyFileTemplateStore extends DumbTemplateStore {
    
    private final File _dir;
    private final Map<String, FileTemplateEntry> _fileTemplates = new java.util.HashMap<>();
    
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
    
    @Override public synchronized DumbTemplate get(String templatePath) {
        DumbTemplate override = _templates.get(templatePath);
        if (override != null) return override; // override files via parent class's add() methods

        FileTemplateEntry result = _fileTemplates.get(templatePath);
        if (result != null) return result.get();

        File f = _dir;
        for (String part : Util.splitAndNormalizePath(new LinkedList<>(), templatePath)) {
            f = new File(f, part);
        }
        
        result = new FileTemplateEntry(templatePath, f);
        _fileTemplates.put(templatePath, result);
        return result.get();
    }
    
    private class FileTemplateEntry {
        private long _lastModified;
        private final File _f;
        private DumbTemplate _template;
        private final String _templatePath;
        
        private FileTemplateEntry(String templatePath, File f) {
            _templatePath = templatePath;
            _f = f;
            _lastModified = Long.MIN_VALUE;
        }
        
        public DumbTemplate get() {
            if (_f.canRead()) {
                long mod = _f.lastModified();
                if (_template == null || _f.lastModified() != _lastModified) {
                    try {
                        log("Loading template " + _templatePath + " from " + _f.getAbsolutePath());
                        String s = new String(Files.readAllBytes(_f.toPath()));
                        _template = new DumbTemplate(_templatePath, DumbLazyFileTemplateStore.this, s);
                        _lastModified = mod;
                    } catch (IOException e) {
                        exception(e);
                        return null;
                    }
                }
            } else {
                error("Template not found in filesystem: " + _f.getAbsolutePath());
            }
            return _template;
        }
    }
}
