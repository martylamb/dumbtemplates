package com.martiansoftware.dumbtemplates;

import com.martiansoftware.dumbtemplates.DumbTemplate.Directive;
import java.util.regex.Matcher;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author <a href="http://martylamb.com">Marty Lamb</a>
 */
public class DumbTemplateTest {
    
    public DumbTemplateTest() {
    }
    
    @Test
    public void testIncludeRegex() {
        Matcher m = Directive.INCLUDE.getMatcher().reset("{#include template}");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        
        m = Directive.INCLUDE.getMatcher().reset("{# include     template     }");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        
        m = Directive.INCLUDE.getMatcher().reset("{#include template if}");
        assertFalse(m.matches());
        
        m = Directive.INCLUDE.getMatcher().reset("{#include template if varname}");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        assertEquals("if", m.group(2));
        assertEquals("varname", m.group(3));
        
        m = Directive.INCLUDE.getMatcher().reset("{#  include template  if   varname   }");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        assertEquals("if", m.group(2));
        assertEquals("varname", m.group(3));
        
        m = Directive.INCLUDE.getMatcher().reset("{#include template if varname value}");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        assertEquals("if", m.group(2));
        assertEquals("varname", m.group(3));
        assertEquals("value", m.group(4));
        
        m = Directive.INCLUDE.getMatcher().reset("{#include template if varname    value with spaces  }");
        assertTrue(m.matches());
        assertEquals("template", m.group(1));
        assertEquals("if", m.group(2));
        assertEquals("varname", m.group(3));
        assertEquals("value with spaces", m.group(4));
    }
    
    
}
