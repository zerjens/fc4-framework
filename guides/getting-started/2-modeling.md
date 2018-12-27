# FC4 Framework » Getting Started » Modeling

Now that you’re [prepped](index.md), the next step is to model the system you
wish to diagram.

By “model” all we mean is to document the subject using [the FC4
DSL](../../reference/dsl.md).

## Start fc4-tool

If you haven’t already done so, start [fc4-tool](../../methodology/toolset.md)
running in the background:

```shell
~/dev/docs/fc4 $ fc4 edit
```

<aside>

* The tool will check that its working directory is an FC4
  [corpus](../../concepts.md#corpus) and will warn if it isn’t
* If you haven’t placed the tool’s files in a directory that’s included in your
  shell’s PATH environment variable, then you’ll need to specify the full path
  to the `fc4` executable

</aside>

## Create the File

Create a new file to contain the definition of Spline Reticulator:

```shell
~/dev/docs/fc4 $ touch model/spline-reticulator.yaml
```

Then open that file in your editor of choice, and paste in this YAML:

```yaml
system:
  Spline Reticulator:
    description: Reticulates all the splines without breaking a sweat
```

It may not look like much, but that’s a valid minimal definition of a system
in the FC4 DSL. Now that we’ve defined it, it can be included in views (which
yield diagrams).

## Containers

While the above _is_ a valid system definition, it’s not particularly interesting.
A view with that system as its subject would be quite boring.

So let’s add some more information about our system:

```yaml
system:
  Spline Reticulator:
    description: Reticulates all the splines without breaking a sweat
    ### ------ new stuff below this line ------
    containers:
      Flux Capacitor:
        description: Stores up that flux for quick release
      Job Queue:
        description: Gets those jobs done
      Main Brain:
        description: In your business, running your logic
```

### Connections

As you can see, we’ve added three [containers](../../reference/concepts.md#container)
to the definition. A view of this system would now be slightly more interesting.
But there are still no connections defined, so our view would yield a diagram
showing a bunch of floating boxes with no lines connecting them. Let’s fix that.

```yaml
system:
  Spline Reticulator:
    description: Reticulates all the splines without breaking a sweat
    containers:
      Flux Capacitor:
        description: Stores up that flux for quick release
        ### this property is new:
        uses:
          Job Queue:
            description: Pulls jobs off the queue
      Job Queue:
        description: Gets those jobs done
      Main Brain:
        description: In your business, running your logic
        ### this property is new:
        uses:
          Job Queue:
            description: Puts jobs on the queue
          Flux Capacitor:
            description: Puts flux in, takes flux out
# ADD THIS LATER
    # tags:
    #   tech: [Avro, Clojure, Kafka]
```

Now we’re getting somewhere. We’ve specified that the _Flux Capacitor_ container
uses the _Job Queue_, and the _Main Brain_ container uses both the _Job Queue_
and the _Flux Capacitor_.

## Context

At this point, we’ve covered all the internals of our subject system. So it’s
time to shift our focus to its [context](../../concepts.md#context). That means
specifying the [users](../../concepts.md#user) and systems with which our
subject interacts.

### Users

All systems have users, in the sense that there are always people who derive
value from the features of a system — otherwise, the system would have no reason
to exist. Usually, users interact directly with a system, but sometimes the
interaction is indirect.

### Other Systems

There’s one last major aspect of our subject that we should add: connections to
other systems.

<aside>

* While not universal, it’s extremely common for a system to interact with other
  systems
* These other systems might be application-level systems providing APIs or
  similar features to the subject, or they might be lower-level plumbing, such
  as DNS.

</aside>
