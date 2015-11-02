package com.martiansoftware.dumbtemplates;

import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple text template.
 * 
 * Several directives are supported as described in accompanying documentation.
 * Upon creation, the template is split around these directives and converted
 * to a list of individual render steps.  Template processing accepts a
 * Map<String, Object> holding variables that may be used.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbTemplate {
    
    private final List<Renderer> _renderers;      // when run in order, this list produces the template output
    private final DumbTemplateStore _store;       // used to lookup includes and insides
    private final String _inside;                 // name of the template this is inside of (or null if none)
    private final String _name;                   // name of this template
    
    /**
     * Creates a new DumbTemplate.  You should probably be calling DumbTemplateStore.add()
     * instead of this.
     * 
     * @param templateName the name (possibly hierarchical, delimited by "/") for this template
     * @param store the DumbTemplateStore to use for #include and #inside directives
     * @param templateDefinition the actual template text (including directives)
     */
    public DumbTemplate(String templateName, DumbTemplateStore store, String templateDefinition) {
        _name = templateName;
        _store = (store == null ? new DumbTemplateStore() : store);
        Map<Integer, MatchResult> matches = new java.util.TreeMap<>();
        MatchResult inside = null;
        
        // first find all matches of each type and sort by start index
        for (Directive t : Directive.values()) {
            Matcher m = t.getMatcher();
            m.reset(templateDefinition);
            while (m.find()) {
                MatchResult mr = new MatchResult(t, m);
                if (t == Directive.INSIDE && inside == null) inside = mr;
                matches.put(m.start(), mr);
            }
        }
        
        // then assemble a list of lambdas to output each part of the template
        List<Renderer> r = new java.util.LinkedList<>();        
        int upTo = 0;
        for(MatchResult mr : matches.values()) {
            if (mr.start > upTo ) r.add(rawRenderer(templateDefinition, upTo, mr.start));
            r.add(getRenderer(mr));
            upTo = mr.end;
        }
        if (upTo < templateDefinition.length()) r.add(rawRenderer(templateDefinition, upTo, templateDefinition.length()));
        r.add((ctx, out) -> out.flush());
        
        _renderers = Collections.unmodifiableList(new java.util.ArrayList<>(r));
        _inside = (inside == null ? null : Util.resolvePath(_name, inside.group[Directive.INSIDE_TEMPLATE]));
    }

    /**
     * Renders this DumbTemplate directly to the specified PrintWriter.  Does not flush.  Ew.
     * 
     * @param ctx a Map containing variables that can be referenced via this DumbTemplate's directives
     * @param out the PrintWriter that should receive the rendered output
     */
    public void render(Map<String, Object> ctx, PrintWriter out) { render(ctx, out, true); }
    
    /**
     * Renders this DumbTemplate to a String.
     * 
     * @param ctx a Map containing variables that can be referenced via this DumbTemplate's directives
     * @return the rendered output
     */
    public String render(Map<String, Object> ctx) { return render(ctx, true); }
    
    /**
     * Renders this DumbTemplate to a String with no context at all
     * @return the rendered output
     */
    public String render() { return render(null); }
    
    public String getName() { return _name; }
   
    private void render(Map<String, Object> ctx, PrintWriter out, boolean allowInside) {
        Map<String, Object> nctx = ((ctx == null) ? Collections.EMPTY_MAP : ctx);
        if (allowInside && _inside != null) {
            DumbTemplate d = _store.get(_inside);
            if (d != null) {
                nctx.put("content", render(nctx, false));
                d.render(nctx, out);
                return;
            }
        }
        _renderers.forEach((r) -> r.render(nctx, out));
    }
    
    private String render(Map<String, Object> ctx, boolean allowInside) {
        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s);
        render(ctx, p, allowInside);
        return s.toString();
    }
    
    // just renders a (raw) portion of a template.  this is for the template
    // parts in between dumplate directives.
    private Renderer rawRenderer(String s, int start, int end) {
        return (ctx, out) ->  out.print(s.substring(start, end));
    }
    
    final Renderer getRenderer(MatchResult mr) {
        switch(mr.t) {
            case INCLUDE:
                String resolvedTemplate = Util.resolvePath(_name, mr.group[Directive.INCLUDE_TEMPLATE]);
                return (ctx, out) -> {
                    boolean shouldInclude = true;
                    if (mr.group[Directive.INCLUDE_IFUNLESS] != null) { // is this more than just a dumb include?
                        boolean invertResult = "unless".equals(mr.group[Directive.INCLUDE_IFUNLESS]);
                        String var = mr.group[Directive.INCLUDE_CONDVAR];
                        String val = mr.group[Directive.INCLUDE_CONDVALUE];
                        if (val == null) {
                            shouldInclude = Util.isTruthy(ctx.get(var));
                        } else {
                            Object o = ctx.get(var); 
                            String v = (o == null) ? "" : o.toString();
                            shouldInclude = val.equals(v);
                        }
                        if (invertResult) shouldInclude = !shouldInclude;
                    }
                    if (shouldInclude) {
                        DumbTemplate d = _store.get(resolvedTemplate);
                        if (d == null) {
                            _store.warning("template '" + _name + "', cannot find template '" + resolvedTemplate + "'");
                        } else {
                            d.render(ctx, out);
                        }
                    }
                };
            case VAR:
                return (ctx, out) -> {
                    if (ctx == null) return;
                    Object o = ctx.get(mr.group[Directive.VAR_NAME]);
                    if (o == null) {
                        _store.warning("variable not defined: " + mr.group[Directive.VAR_NAME]);
                    } else {
                        out.format("%s", "!".equals(mr.group[Directive.VAR_ESCAPEHINT]) ? o : Util.escape(o));
                    }
                };
            case JSON:
                String var = (mr.group.length > Directive.JSON_NAME) ? mr.group[Directive.JSON_NAME] : null;
                return (ctx, out) -> {
                    if (ctx == null) return;
                    Gson gson = _store.getGson();
                    if (var == null) {
                        gson.toJson(ctx, out);
                    } else {
                        Object o = ctx.get(var);
                        if (o == null) _store.warning("variable not defined: " + mr.group[Directive.VAR_NAME]);
                        gson.toJson(o, out);
                    }
                };
            case INSIDE:
                return (ctc, out) -> {};
        }
        return null;
    }
      
    private interface Renderer { void render(Map<String, Object> ctx, PrintWriter out); }

    private class MatchResult {
        public final Directive t;
        public final int start, end;
        public final String[] group;

        public MatchResult(Directive t, Matcher m) {
            this.t = t;
            this.start = m.start();
            this.end = m.end();
            this.group = new String[m.groupCount() + 1];
            for (int i = 0; i <= m.groupCount(); ++i) group[i] = m.group(i);
        }
        
    }    
        
    private static final String SYMBOLGROUP = "([^}\\s]+)";
    enum Directive {
        // {# include TEMPLATE }
        // {# include TEMPLATE if VAR }
        // {# include TEMPLATE unless VAR }
        // {# include TEMPLATE if VAR VALUE }
        // {# include TEMPLATE unless VAR VALUE}
        INCLUDE("\\{#\\s*include\\s+"
                + SYMBOLGROUP           // group 1 = template to include
                + "(?:\\s+"             // non-capture for entire if/unless clause
                  + "(if|unless)\\s+"   // group 2 = if|unless
                  + SYMBOLGROUP         // group 3 = varname
                  + "(?:\\s+"           // non-capture for optional value clause
                    + "([^}]+?)"        // group 4 = optional variable value
                  + ")?"
                + ")?"
                + "\\s*\\}"),

        // {# inside TEMPLATE }
        INSIDE("\\{#\\s*inside\\s+" + SYMBOLGROUP + "\\s*\\}"),   // group 1 = template to infect
        // {= var }
        // {=! var }
        VAR("\\{=(!)?\\s*" + SYMBOLGROUP + "\\s*\\}"),            // group 1 = optional '!', group 2 = var(s)
        // {$ vars }
        // {$}
        JSON("\\{\\$\\s*" + SYMBOLGROUP + "?\\s*\\}");             // group 1 = var (optional)

        // capture group indexes
        public static final int INCLUDE_TEMPLATE = 1;
        public static final int INCLUDE_IFUNLESS = 2;
        public static final int INCLUDE_CONDVAR = 3;
        public static final int INCLUDE_CONDVALUE = 4;
        public static final int INSIDE_TEMPLATE = 1;
        public static final int VAR_ESCAPEHINT = 1;
        public static final int VAR_NAME = 2;
        public static final int JSON_NAME = 1;
        private final Matcher m;
        private Directive(String pattern) { m = Pattern.compile(pattern).matcher(""); }
        public Matcher getMatcher() { return m; }        
    }
    
}
