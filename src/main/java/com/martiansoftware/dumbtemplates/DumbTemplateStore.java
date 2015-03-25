package com.martiansoftware.dumbtemplates;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Holds a collection of DumbTemplates indexed by arbitrary template names.  The names
 * are used to lookup templates, either directly by the user or by the
 * <code>include</code> and <code>inside</code> template directives.
 * 
 * DumbTemplates can be manually added, or the DumbLazyFileTemplateStore or
 * DumbLazyClasspathTemplateStore can be used instead of this to automatically
 * initialize themselves from the filesystem or classpath, respectively.
 * 
 * A Gson object can be provided to control JSON serialization in templates.
 * If one is not provided, a default Gson object will be created automatically.
 *
 * An optional DumbLogger can be provided to the constructor to alert
 * the user when things might be going wrong.  Otherwise template processing will
 * plow ahead. A default DumbLogger can be specified via <code>new DumbLogger(){}</code>.
 *  
 * <b>Note:</b> Don't use spaces in your template names/paths or in the names
 * of variables you provide for rendering.  DumbTemplate is too dumb to parse them.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbTemplateStore {

    protected final DumbLogger _log;
    protected final Map<String, DumbTemplate> _templates = new java.util.HashMap<>();
    protected volatile Gson _gson; // used for jsonizing vars in {$ [VAR]} directive
    
    /**
     * Create a new, empty DumbTemplateStore that will silently ignore any exceptions
     * during template processing.
     */
    public DumbTemplateStore() { this(null); }
    
    /**
     * Create a new, empty DumbTemplateStore that will hand any exceptions
     * encountered during template processing over to the specified DumbLogger.
     * 
     * @param log recipient of log notifications
     */
    public DumbTemplateStore(DumbLogger log) { _log = log; }
        
    /**
     * Creates a DumbTemplate and adds it to this DumbTemplateStore with the given name.
     * 
     * @param templateName The name to use to access this DumbTemplate
     * @param templateDef The template definition
     * @return this DumbTemplateStore
     */
    public DumbTemplateStore add(String templateName, String templateDef) {
        if (templateName.startsWith("/")) warning("template name '" + templateName + "' starts with a slash.  You probably don't want this.");
        _templates.put(templateName, new DumbTemplate(templateName, this, templateDef));
        return this;
    }
    
    /**
     * Creates a DumbTemplate and adds it to this DumbTemplateStore with the given name.
     * 
     * @param templateName The name to use to access this DumbTemplate
     * @param templateDef A Reader providing the template definition
     * @return this DumbTemplateStore
     * @throws IOException 
     */
    public DumbTemplateStore add(String templateName, Reader templateDef) throws IOException {        
        StringBuilder s = new StringBuilder();
        int n; char[] cbuf = new char[4096];
        while ((n = templateDef.read(cbuf)) != -1) s.append(cbuf, 0, n);            
        return add(templateName, s.toString());
    }    

    /**
     * Returns the DumbTemplate with the specified name, or null if no such template exists.
     * 
     * @param templateName the name of the desired DumbTemplate
     * @return 
     */
    public DumbTemplate get(String templateName) {
        DumbTemplate result = _templates.get(templateName);
        if (result == null) error("Template not found: " + templateName);
        return result;
    }

    Gson getGson() {
        if (_gson == null) _gson = new Gson();
        return _gson;
    }
    
    public void setGson(Gson gson) { _gson = gson; }
    
    void exception(Exception e) { if (_log != null) _log.log(e); }    
    void error(String msg) { if (_log != null) _log.log("Error: " + msg); }
    void warning(String msg) { if (_log != null) _log.log("Warning: " + msg); }
    void log(String msg) { if (_log != null) _log.log(msg); }
    
}
