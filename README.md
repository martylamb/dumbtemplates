# DumbTemplates

DumbTemplates are an unambitious, and therefore very simple, and arguably dumb, text template processing enging for Java.  Features provided include:

  * Variable substitution with or without html escaping or in JSON format
  * Conditional inclusion of other templates
  * Template inheritance
  * Template loading from the filesystem, classpath, or manually-populated data structure

Notably, loops are not supported.

## Hello, DumbTemplates

```java
DumbTemplateStore ds = new DumbTemplateStore();
ds.add("myTemplate", "Hello, World!");
String result = ds.get("myTemplate").render(null);
```

The first line creates a `DumbTemplateStore`, which is used to hold and lookup templates.  Lookups might be performed by the user (as in the `ds.get()` call in the third line above) or by templates (using the `#include` or `#inside` directives explained below).

The second line defines a new template called "myTemplate" that simply contains the text "Hello, World!".

The third line obtains this new template from the `DumbTemplateStore` and renders it to a `String`.  It passes the `render()` method a `null` because it's not using any variables.

## Context

Variables can be made accessible to DumbTemplates via a `Map<String, Object>` passed into the `DumbTemplate.render()` method.  This was `null` in line 3 of the above example because we weren't using any variables.

## Directives

Templates are just text.  DumbTemplate behavior is controlled using a few simple directives inline with your text:

| Directive | Description |
|-----------|-------------|
| `{= VAR }` | Inserts the value stored in the context with the name `VAR` (calling its `toString()` method if necessary).  VAR should be unquoted and should not contain spaces.  The inserted value will be html-escaped.|
| `{=! VAR}` | Same as above, but without html-escaping the result. |
| `{#include TEMPLATE}` | Inserts the contents of the referenced template.  The template is retrieved from the `DumbTemplateStore`.  The name should not include spaces. See [Template Resolution](#templateResolution), below. |
| `{#include TEMPLATE if VAR}` | Same as above, but only if the value stored in VAR is "[truthy](#truthy)" (see below). |
| `{#include TEMPLATE unless VAR}` | Same as above, but only if the value stored in VAR is **NOT** "[truthy](#truthy)" (see below). |  
| `{#include TEMPLATE if VAR VALUE}` | Same as above, but only if the value stored in VAR is equal to VALUE (when compared as a String).  VALUE should not be quoted and should not have leading or trailing whitespace. |
| `{#include TEMPLATE unless VAR}` | Same as above, but only if the value stored in VAR is **NOT** equal to VALUE. |  
| `{#inside TEMPLATE }` | Provides Dumb Template Inheritance &reg;.  The current template is rendered completely, with the result stored in the provided context as "content".  The referenced template is then rendered with the modified context.  It should include somewhere a {= content} or {=! content}.  This can be used, for example, to wrap content in a common html header/footer.  The referenced template can access any variables defined in the contect (e.g., "title" for the html example). Only the first use of this directive inside a template is honored; subsequent uses are ignored. |
| `{$ VAR }` | Inserts the referenced variable as JSON, including a variable declaration.  Example: `var myvar="this is an example";`.  Complex objects may be used.  Serialization is performed by [Gson](https://code.google.com/p/google-gson/).  You can provide the `DumbTemplateStore` with your own `Gson` object if you require specific serialization behavior. |
| `{$}` | Same as above, but inserts the entire context as a series of JSON variable declarations.


## <a name="truthiness"></a>Truthiness

For conditional `#include` directives, truthiness is determined as follows:

  * `null` is not truthy.
  * `true` (the boolean or Boolean type) is truthy.  `false` is not.
  * Nonzero numeric data types are truthy.  Zero is not truthy.
  * Empty `String`s are not truthy.
  * `String`s that are equal (case-insensitive) to "0", "f", "false", "n", or "no" are not truthy.
  * All other objects are truthy.

## <a name="templateResolution"></a>Template Resolution

It can be useful to organize your templates into a hierarchy, especially if you are using a `DumbLazyFileTemplateStore` or `DumbLazyClasspathTemplateStore` instead of the simpler `DumbTemplateStore`.  Template names are treated as hierarchical, using `/` as a path delimiter.  `#include` and `#inside` directives resolve their template references as relative to the template currently being processed.  Templates can be referenced absolutely (relative to the root of their store) with a leading `/`.

For example, suppose you have the following directory/file structure under `/home/me/templates`:

```
+-- a.txt
+-- b.txt
+-- dir1
  +-- dir2
    +-- deep.txt
    +-- dir3
      +-- deeper.txt
```        

...and you then create a `DumbLazyFileTemplateStore`:

```java
DumbTemplateStore ds = new DumbLazyFileTemplateStore(new File("/home/me/templates"));
```
You can now reference anything in that directory structure as a template, using names like "a.txt" and "dir1/dir2/dir3/deeper.txt".

If the "deeper.txt" file, needs to include the contents of "deep.txt", it can do so via an absolute path (e.g., `{#include /dir1/dir2/deep.txt}`), or via a relative path (e.g., `{#include ../deep.txt}`).

Care has been taken to prevent template resolution from escaping from the root of the `DumbLazyFileTemplateStore` or `DumbLazyClasspathTemplateStore`.

This technique also works with the simple `DumbTemplateStore` provided that you manually name your `DumbTemplate`s properly.

## Computed Variables

If you need to compute a variable for insertion rather than store it permanently in the context (for example, if it needs to change between calls within a template), override `get` in the `Map<String, Object>` you provide as a template context.

## Command Line Use

There's a ~~Dumb~~ simple command line interface included with the jar.  Its usage is `java -jar JARFILE DIRNAME TEMPLATENAME`.  This initializes a `DumbLazyFileTemplateStore` at DIRNAME, stuffs the environment into a context, and renders TEMPLATENAME to stdout. 

## Don't

  * use spaces in variable names
  * include leading or trailing whitespace in the values of your variables if they are being examined for conditional `#include`s.
  * create circular references among your templates (e.g., mutual `#include`s).
  * be surprised if you find bugs (but do please let me know about them).
  
## Building

`mvn package`

## Why the name?

  1. `DumbTemplate`s are really pretty unsophisticated (which IMHO is a good thing for some purposes).  There are some seemingly arbitrary but liveable limitations (e.g., "don't use spaces in your variable names").
  2. Template parsing uses regular expressions.  While expedient, many would consider this dumb.
  3. It has only been minimally tested.  This is dumb.
  4. It was originally slapped together very quickly out of frustration with some other libraries.  It seemed dumb to me that I even found them necessary (and maybe they were not necessary, in which case I am dumb.)
  5. `DumbTemplate`s are actually pretty good.  And [good is dumb.](http://www.imdb.com/title/tt0094012/quotes)



