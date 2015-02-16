package com.martiansoftware.dumbtemplates;

import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Helper methods for DumbTemplates
 * 
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
class Util {
    
    // this method is based upon Android's Html.withinStyle() method, published
    // under the Apache 2.0 license, retrieved on June 1, 2014 from
    // http://grepcode.com/file_/repository.grepcode.com/java/ext/com.google.android/android/4.4.2_r1/android/text/Html.java/?v=source
    // License available at http://www.apache.org/licenses/LICENSE-2.0
    static String escape(Object o) {
        if (o == null) return "";
        String text = o.toString();
        StringBuilder out = new StringBuilder();
        int end = text.length();
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                if (c < 0xDC00 && i + 1 < end) {
                    char d = text.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                        out.append("&#").append(codepoint).append(";");
                    }
                }
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
    
    /**
     * Indicates is an object is "truthy" and thus should support a conditional
     * include if no value is specified.  Truthiness is similar to javascript
     * truthiness.
     */
    static boolean isTruthy(Object o) {
        if (o == null) return false;
        
        if (Boolean.TRUE.equals(o)) return true;
        if (Boolean.FALSE.equals(o)) return false;
        
        if (o instanceof Byte && ((Byte) o == (byte) 0)) return false;
        if (o instanceof Double && ((Double) o == (double) 0)) return false;
        if (o instanceof Float && ((Float) o == (float) 0)) return false;
        if (o instanceof Integer && ((Integer) o == 0)) return false;
        if (o instanceof Long && ((Long) o == (long) 0)) return false;
        if (o instanceof Short && ((Short) o == (short) 0)) return false;
        
        if (o instanceof String) {
            String s = (String) o;
            if (s.length() == 0) return false;
            if ("0".equals(s)) return false;
            if ("f".equalsIgnoreCase(s)) return false;
            if ("false".equalsIgnoreCase(s)) return false;
            if ("n".equalsIgnoreCase(s)) return false;
            if ("no".equalsIgnoreCase(s)) return false;
        }
        return true;
    }

    /**
     * Splits of up /-separated path, drops "." elements, consumes and processes ".."
     * elements (with excessive ".."s serving as no-ops) and stores result in a list
     * of the resulting path elements
     * 
     * @param result the list to store the result in - can be seeded with an initial path
     * @param path the path to split and normalize into the result
     */
    static LinkedList<String> splitAndNormalizePath(LinkedList<String> result, String path) {
        for (String part : path.split("/")) {
            if (part == null || part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if(!result.isEmpty()) result.removeLast();
            } else {
                result.add(part);
            }
        }
        return result;
    }
    
    /**
     * Joins a list of path elements into a /-separated string
     */
    static String joinPath(LinkedList<String> path) {
        return path.stream().collect(Collectors.joining("/"));
    }
    
    /**
     * Resolves a path as relative to the asker, with constraints preventing
     * resolution from exiting the path root.
     */
    static String resolvePath(String asker, String path) {
        // if no slashes then there's nothing to resolve
        if (!asker.contains("/") && !path.contains("/")) return path;
        
        // if path starts with a slash then it's absolute and just needs to be normalized
        if (path.startsWith("/")) return joinPath(splitAndNormalizePath(new LinkedList<>(), path));
        
        // if path does not start with a slash then it's relative to asker
        LinkedList<String> base = splitAndNormalizePath(new LinkedList<>(), asker);
        if (!base.isEmpty()) base.removeLast(); // drop template name so starting point is asker's parent dir
        return joinPath(splitAndNormalizePath(base, path));        
    }
}
