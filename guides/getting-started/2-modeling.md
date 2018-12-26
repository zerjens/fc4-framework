# FC4 Framework » Getting Started » Modeling

Now that you’re [prepped](index.md), the next step is to model the system you
wish to diagram.

By “model” all we mean is to document the subject using [the FC4
DSL](../../reference/dsl.md).

First, create a new file to contain the definition of Spline Reticulator:

```shell
$ touch model/spline-reticulator.yaml
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

While that _is_ a valid system definition, it’s not particularly interesting.
A view with that system as its subject would be quite boring.

So let’s add some more information about our system:

```yaml
system:
  Spline Reticulator:
    description: Reticulates all the splines without breaking a sweat
    # ------ new stuff below this line ------
    containers:
      Flux Capacitor:
        description: Stores up that flux for quick release
        uses:
          Job Queue:
            description: Pulls jobs off the queue
      Job Queue:
        description: Gets those jobs done
      Main Brain:
        description: In your business, running your logic
        uses:
          Job Queue:
            description: Puts jobs on the queue
          Flux Capacitor:
            description: Puts flux in, takes flux out
# ADD THIS LATER
    # tags:
    #   tech: [Avro, Clojure, Kafka]
```
