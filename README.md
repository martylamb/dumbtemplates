<a class="mk-toclify" id="table-of-contents"></a>

# Table of Contents
- [DumbTemplates](#dumbtemplates)
    - [Hello, DumbTemplates](#hello-dumbtemplates)
    - [Context](#context)
    - [Directives](#directives)
    - [Truthiness](#truthiness)
    - [Template Resolution](#template-resolution)
    - [Computed Variables](#computed-variables)
    - [Command Line Use](#command-line-use)
    - [Don't](#don-t)
    - [Building](#building)
    - [Using with Maven](#using-with-maven)
        - [Add the repository to your project:](#add-the-repository-to-your-project)
        - [Add the dependency to your project:](#add-the-dependency-to-your-project)
    - [Why the name?](#why-the-name)


<a class="mk-toclify" id="dumbtemplates"></a>
# DumbTemplates

DumbTemplates are an unambitious, and therefore very simple, and arguably dumb, text template processing enging for Java.  Features provided include:

* Variable substitution with or without html escaping or in JSON format
* Conditional inclusion of other templates
* Template inheritance
* Template loading from the filesystem, classpath, or manually-populated data structure

Notably, loops are not supported.

<a class="mk-toclify" id="hello-dumbtemplates"></a>
## Hello, DumbTemplates

```java
DumbTemplateStore ds = new DumbTemplateStore();
ds.add("myTemplate", "Hello, World!");
String result = ds.get("myTemplate").render(null);
```

The first line creates a `DumbTemplateStore`, which is used to hold and lookup templates.  Lookups might be performed by the user (as in the `ds.get()` call in the third line above) or by templates (using the `#include` or `#inside` directives explained below).

The second line defines a new template called "myTemplate" that simply contains the text "Hello, World!".

The third line obtains this new template from the `DumbTemplateStore` and renders it to a `String`.  It passes the `render()` method a `null` because it's not using any variables.

<a class="mk-toclify" id="context"></a>
## Context

Variables can be made accessible to DumbTemplates via a `Map<String, Object>` passed into the `DumbTemplate.render()` method.  This was `null` in line 3 of the above example because we weren't using any variables.

<a class="mk-toclify" id="directives"></a>
## Directives

Templates are just text.  DumbTemplate behavior is controlled using a few simple directives inline with your text:

| Directive | Description |
|-----------|-------------|
| `{= VAR }` | Inserts the value stored in the context with the name `VAR` (calling its `toString()` method if necessary).  VAR should be unquoted and should not contain spaces.  The inserted value will be html-escaped.|
| `{=! VAR}` | Same as above, but without html-escaping the result. |
| `{#include TEMPLATE}` | Inserts the contents of the referenced template.  The template is retrieved from the `DumbTemplateStore`.  The name should not include spaces. See [Template Resolution](#template-resolution), below. |
| `{#include TEMPLATE if VAR}` | Same as above, but only if the value stored in VAR is "[truthy](#truthiness)" (see below). |
| `{#include TEMPLATE unless VAR}` | Same as above, but only if the value stored in VAR is **NOT** "[truthy](#truthiness)" (see below). |  
| `{#include TEMPLATE if VAR VALUE}` | Same as above, but only if the value stored in VAR is equal to VALUE (when compared as a String).  VALUE should not be quoted and should not have leading or trailing whitespace. |
| `{#include TEMPLATE unless VAR}` | Same as above, but only if the value stored in VAR is **NOT** equal to VALUE. |  
| `{#inside TEMPLATE }` | Provides Dumb Template Inheritance &reg;.  The current template is rendered completely, with the result stored in the provided context as "content".  The referenced template is then rendered with the modified context.  It should include somewhere a {= content} or {=! content}.  This can be used, for example, to wrap content in a common html header/footer.  The referenced template can access any variables defined in the contect (e.g., "title" for the html example). Only the first use of this directive inside a template is honored; subsequent uses are ignored. |
| `{$ VAR }` | Inserts the referenced variable as JSON, including a variable declaration.  Example: `var myvar="this is an example";`.  Complex objects may be used.  Serialization is performed by [Gson](https://code.google.com/p/google-gson/).  You can provide the `DumbTemplateStore` with your own `Gson` object if you require specific serialization behavior. |
| `{$}` | Same as above, but inserts the entire context as a series of JSON variable declarations.


<a class="mk-toclify" id="truthiness"></a>
## Truthiness

For conditional `#include` directives, truthiness is determined as follows:

* `null` is not truthy.
* `true` (the boolean or Boolean type) is truthy.  `false` is not.
* Nonzero numeric data types are truthy.  Zero is not truthy.
* Empty `Strings` are not truthy.
* `Strings` that are equal (case-insensitive) to "0", "f", "false", "n", or "no" are not truthy.
* All other objects are truthy.

<a class="mk-toclify" id="template-resolution"></a>
## Template Resolution

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

This technique also works with the simple `DumbTemplateStore` provided that you manually name your `DumbTemplates` properly.

<a class="mk-toclify" id="computed-variables"></a>
## Computed Variables

If you need to compute a variable for insertion rather than store it permanently in the context (for example, if it needs to change between calls within a template), override `get` in the `Map<String, Object>` you provide as a template context.

<a class="mk-toclify" id="command-line-use"></a>
## Command Line Use

There's a ~~Dumb~~ simple command line interface included with the jar.  Its usage is `java -jar JARFILE DIRNAME TEMPLATENAME`.  This initializes a `DumbLazyFileTemplateStore` at DIRNAME, stuffs the environment into a context, and renders TEMPLATENAME to stdout. 

```
$ java -jar target/dumbtemplates-0.1.0-SNAPSHOT-jar-with-dependencies.jar 

Usage: dumbtemplate TEMPLATEDIR TEMPLATENAME

Where TEMPLATEDIR is a directory containing your templates,
and TEMPLATENAME is the relative path within TEMPLATEDIR of the
template you want to render.

$ 
```

<a class="mk-toclify" id="don-t"></a>
## Don't

* use spaces in variable names
* include leading or trailing whitespace in the values of your variables if they are being examined for conditional `#includes`.
* create circular references among your templates (e.g., mutual `#includes`).
* be surprised if you find bugs (but do please let me know about them).

<a class="mk-toclify" id="building"></a>
## Building

`mvn package`

<a class="mk-toclify" id="using-with-maven"></a>
## Using with Maven

<a class="mk-toclify" id="add-the-repository-to-your-project"></a>
### Add the repository to your project:

```xml
<project>
	...
	<repositories>
		<repository>
			<id>martiansoftware</id>
			<url>http://mvn.martiansoftware.com</url>
		</repository>
	</repositories> 
	...
</project>
```

<a class="mk-toclify" id="add-the-dependency-to-your-project"></a>
### Add the dependency to your project:
-----------------------------------

```xml
<dependencies>
	<dependency>
		<groupId>com.martiansoftware</groupId>
		<artifactId>dumbtemplates</artifactId>
		<version>0.1.0-SNAPSHOT</version>
		<scope>compile</scope>
	</dependency>
</dependencies>
```



<a class="mk-toclify" id="why-the-name"></a>
## Why the name?

1. `DumbTemplates` are really pretty unsophisticated (which IMHO is a good thing for some purposes).  There are some seemingly arbitrary but liveable limitations (e.g., "don't use spaces in your variable names").
2. Template parsing uses regular expressions.  While expedient, many would consider this dumb.
3. It has only been minimally tested.  This is dumb.
4. It was originally slapped together very quickly out of frustration with some other libraries.  It seemed dumb to me that I even found them necessary (and maybe they were not necessary, in which case I am dumb.)
5. `DumbTemplates` are actually pretty good.  And [good is dumb.](http://www.imdb.com/title/tt0094012/quotes)




