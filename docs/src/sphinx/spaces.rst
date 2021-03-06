Spaces
======

Introduction
------------

Spaces are a powerful dbuild feature that allows you to describe the flow of dependencies
across projects, and to group artifacts in independent sets within the same dbuild configuration
file.

They are quite easy to use. In principle, each project just contains an optional field
"space", that you can use to specify which logical space the project belongs to. If the field
is not specified, the space "default" is used.

For example, let's assume that we want to compile two projects whose dependencies should not
be related to one another. We can just write:

.. code-block:: javascript

  {
    space: one
    name: a
    uri: "git://a..."
  },{
    space: two
    name: b
    uri: "git://b..."
  }

In this example, the dependencies of project "a" will be rewritten using only the other
projects that are in the same space "one"; similarly for project "b". Interestingly,
the two projects could also generate the same, or overlapping sets of artifacts: since
they are in two separate spaces, they will not interfere with one another.

From and to
-----------

When used as in the above example, independent spaces are a simple way to group together
unrelated groups of projects. However, projects can use spaces in a much more flexible
way, by specifying different source and target spaces for their artifacts. For example:

.. code-block:: javascript

  {
    space: first
    name: a
    uri: "git://a..."
  },{
    space.from: first
    space.to: second
    name: b
    uri: "git://b..."
  },{
    space.from: first
    space.to: third
    name: c
    uri: "git://c..."
  }

In this example, the artifacts that are found in the first space will be used to rewrite
the dependencies of project "a"; its resulting artifacts will also be made available to
the same first space. However, the projects "b" and "c" are publishing their artifacts
instead to two different spaces: their artifacts are independent, but they can both use
the artifacts compiled by "a".

To make a concrete example, if the first space contains a given Scala compiler and other
libraries, including "a", then the projects "b" and "c" could be two different commits of
the same project. Both will then be tested using the same compiler, and the same
ecosystem of libraries contained in the first spece.

Building in stages
------------------

An ideal application of spaces is building projects in stages, in order to bootstrap a
build, for instance. In that case, you can set up a chain of spaces in which you have
``{from:a,to:b}``, ``{from:b,to:c}``, ``{from:c,to:d}``, and so on until your
artifacts are stable.

You should be careful while handling dependencies, however, as you should make sure
that each space at each stage contains all of the dependencies required by the
following one, in order to avoid dependencies conflicts (see below for more).

Disabling dependencies rewriting
--------------------------------

If you specify as a source space the empty string "",
dbuild will not change anything in the dependencies of your project,
leaving everything to its normal defaults. You should of course be wary about
possible dependencies conflict that may arise as a result, as explained below.

If the target "to" space is "", similarly, the set of published artifacts will
become invisible to other projects; this can be useful for testing.


Copying across spaces
---------------------

One additional application of spaces is republishing just-compiled artifacts
using different version numbers, or cross-versioning suffixes, without
having to do the round trip to a Maven repository. For example, using the Ivy
build system, one can simply use:

.. code-block:: javascript

  {
    space.from: xxx
    space.to: yyy
    name: a
    system: ivy
    uri: "ivy:org#name;version"
    set-version: "1.9.31-RC5"
  }

The artifact generated by some project in space xxx will simply be grabbed, renamed,
and made available to space yyy. It can also be deployed to Maven/Artifactory/Nexus
just by referring to project "a" by name.

Multiple "to"s
--------------

It is also possible to specify multiple destination spaces in one step. For example:

.. code-block:: javascript

  {
    name: a
    uri: "git://a..."
    space.from: xxx
    space.to: [ xxx, yyy ]
  }

This mechanism can be used to arrange spaces in a tree with minimal effort. The
section below contains important details about using multiple spaces.

Organizing spaces
-----------------

One important point to remember is that spaces are unrelated. Because of that,
you should ensure that each project will have access to all of the artifacts it
may need.

If "cross-version: standard" is used, in particular, the missing artifacts may
be grabbed from external repositories without control, potentially leading
to dependency conflicts.

Let's make an example:

.. code-block:: javascript

  {
    space: xxx
    name: a
    uri: "..."
  },{
    space.from: xxx
    space.to: yyy
    name: b
    uri: "..."
  },{
    space: yyy
    name: c
    uri: "..."
  }

In this example, we have the artifacts of a, b that are visible in space xxx,
while only b and c are visible in yyy.

When rewriting the dependencies of project c, only the artifacts of b will be
used. If b depends on a, project c will be unable to see the artifacts of a.

That may cause dependencies conflicts: the pom file of b will contain a reference
to a, while c may happen to pull in an unrelated version of the same artifacts,
which may not be compatible.

In order to avoid this sort of problems, all spaces should also contain all of the
artifacts of the spaces that logically precede them: a first space contains
some artifacts, then a second contains all of the above plus some others,
a third again all of them plus a few more, and so on.

That can be easily done by publishing to all of the needed children spaces, by using
the multiple "to"s feature. In the case above, we would have:

.. code-block:: javascript
  
  {
    space.from: xxx
    space.to: [xxx, yyy, zzz]
    name: a
    uri: "..."
  },{
    space.from: xxx
    space.to: [xxx, yyy, zzz]
    name: b
    uri: "..."
  },{
    space: yyy
    name: c
    uri: "..."
  },{
    space: zzz
    name: d
    uri: "..."
  }

Here we have that both c and d have access to the artifacts of a and b, as well
as their own.

Specifying "from" and "to" in each project may be a bit tedious however, which is
why there is a handy shorthand syntax available.

A simpler syntax
----------------

We know that projects are described in general using the syntax:

.. code-block:: javascript

  build.projects: [{...},{...}]

This is supplemented by a list of defaults, which are directly specified as fields
of ``build``:

.. code-block:: javascript

  build: {
    sbt-version: "..."
    extraction-version: "..."
    ...
    projects: [{...},{...}]
  }

Each project can provide their own value for "extraction-version", "sbt-version", etc,
but if they do not, the value in ``build`` offers a handy default.

The same syntax can also applied to spaces. For example:

.. code-block:: javascript

  build: {
    space: one
    projects: [{
      name: a
      ...
    },{
      name: b
      ...
    }]
  }

Unless a project defines its own space specification, the default (in this case "one")
will be used. If none is present the standard "default" space is used.

In order to specify more easily lists of projects in unrelated spaces, the following
syntax is also available:

.. code-block:: javascript

  build: [{
    space.from: one
    projects: [{
      name: a
      uri: "..."
    },{
      name: b
      uri: "..."
    }]
  },{
    space: two
    projects: [{
      name: c
      uri: "..."
    },{
      name: d
      uri: "..."
    }]
  }]

If you need to publish a set of projects to multiple spaces, therefore, the syntax can be
simply:

.. code-block:: javascript
  
  build: [{
    space.from: xxx
    space.to: [xxx, yyy, zzz]
    projects: [{
      name: a
      uri: "..."
    },{
      name: b
      uri: "..."
    ...
    }]
  },{
    space: yyy
    projects: [...]
  ...

.. note::
  If you have a space specification in a specific project, it will completely override the general
  one in the "build" section. Therefore, you will need to specify in the project *both* the
  ``space.from`` and the ``space.to`` values: if you only specify one of the two, the other
  will not be inherited from the general build space specification.
  
Hierarchical spaces
--------------------

OK, we lied: spaces are not *all* independent: they can actually be arranged in a hierarchical
structure. The discussion above was however crucial at introducing the reasons why they are
useful, and the inherent dangers of using incomplete sets of dependencies.

In order to use nested spaces, just name them with a dot-separated name. Projects in
those spaces will be able to see all the artifacts of the projects that published in
all enclosing spaces.

For instance, if you have ``space.from: "aaa.bbb"``, you will see all the dependencies
published to ``aaa.bbb``, as well as those published to ``aaa``.

The example above can now be simply rewritten as:

.. code-block:: javascript

  {
    space: xxx
    name: a
    uri: "..."
  },{
    space: xxx
    name: b
    uri: "..."
  },{
    space: xxx.yyy
    name: c
    uri: "..."
  },{
    space: xxx.zzz
    name: d
    uri: "..."
  }

But wait, there's more!

Variables expansion
-------------------

Variables expansion works great in conjunction with spaces, in order to reduce
verbosity and duplication. For example, let's assume that we have a given project
that needs to be built against multiple versions of Scala, each of which lives in
a different space. We can easily define the common structure of the project once,
then define the three occurrences. For example:

.. code-block:: javascript

  vars.p: {
    uri: "git://github.com/user/repo#branch"
    sbt-version: "0.13.0"
    extra.projects: "core"
    ...more stuff...
  }
  
  build.projects: [ 
   ${vars.p} {
    space: one
    name: t1
   }
   ${vars.p} {
    space: two
    name: t2
   }
   ${vars.p} {
    space: three
    name: t3
   }

|

.. note::
  Spaces work as artifact repositories of sorts. Each project has a single "from",
  and only the dependent artifacts that are reachable within that particular space, also transitively,
  will be made available to the project, but not those in other spaces. The fact that cross-space transitive
  dependencies are not included is by design: it would be impossible to support bootstrap cycles
  otherwise.

Republishing artifacts
----------------------

A further application of spaces is the opportunity to republish existing artifacts in a
different space, changing in the process the cross-versioning suffix and the version number.
That can be accomplished by using the Aether build system, for example. Consider the following
configuration:

.. code-block:: javascript

  build.projects: [
    {
      name: lib, system: aether, set-version: "2.10.18"
      uri: "aether:org.scala-lang#scala-library;2.10.2"
    }
    {
      name: "gpg-republish"
      space.to: source
      system: aether
      uri: "aether:com.jsuereth#gpg-library_2.10;0.8.3"
      cross-version: full
      set-version-suffix: "test"
    }
  ]

In this example, an external artifact will be republished locally as ``com.jsuereth#gpg-library_2.10.18;0.8.3-test``.
It is similarly possible to republish artifacts compiled by some other project. That requires a bit of attention, however.
First, the version string specified in the uri must match that of an existing already published artifact. That is
necessary since, during the initial extraction stage, dbuild needs to look somewhere in order to discover the
project dependencies, and the source project has not been built yet at that point. Second, the project needs to be
"injected" a dependency on itself. For example, let us consider the following project:

.. code-block:: javascript

  {
    name: republishtest
    space.from: source
    space.to: dest
    deps.inject: "com.jsuereth#gpg-library"
    system: aether
    uri: "aether:com.jsuereth#gpg-library_2.10;0.8.3"
    cross-version: disabled
    set-version-suffix: "fix"
  }

If we generated the ``gpg-library`` artifacts in the ``source`` space, either by downloading them or by generating
them from source, they will now also be republished to the ``dest`` space with different cross-version suffix and
version number. That may be useful in order to generate multiple copies of the same artifacts, with different
version suffixes for example, in different spaces without the need to recompile them. In order to make sure that
the projects are being republished correctly, you can check for the list of dependencies in the "Dependency
Information" section of the log file.


*Next:* :doc:`plugins`.

