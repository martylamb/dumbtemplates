package com.martiansoftware.dumbtemplates;

import java.io.File;
import java.util.Map;

/**
 * Simple tool to process templates from the command line.  Two arguments are
 * required:
 * 
 * <ol>
 * <li>The top-level directory containing your templates (and anything they
 *     include, etc.)</li>
 * <li>The name of the template to process, specified as a path relative to
 *     the directory specified above.  Use forward slashes for subdirectories.</li>
 * </ol>
 * 
 * Environment variables are automatically placed in the context prior to
 * evaluating the template.
 * 
 * Output is written to stdout.
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class Main {
 
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("\nUsage: dumbtemplate TEMPLATEDIR TEMPLATENAME\n");
            System.err.println("Where TEMPLATEDIR is a directory containing your templates,");
            System.err.println("and TEMPLATENAME is the relative path within TEMPLATEDIR of the");
            System.err.println("template you want to render.\n");
            System.exit(1);
        }

        DumbLazyFileTemplateStore s = new DumbLazyFileTemplateStore(new File(args[0]), new DumbLogger(){});
        Map<String, Object> ctx = new java.util.HashMap<>();
        System.getenv().forEach((k,v) -> ctx.put(k, v));        
        System.out.println(s.get(args[1]).render(ctx));
    }
}
