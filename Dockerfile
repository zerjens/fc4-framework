FROM clojure:tools-deps-alpine

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"

WORKDIR /code

# Copy deps.edn then invoke `clojure` with a simple form just to download the deps, separately
# from and prior to copying the app code so that we don’t have to re-download deps every time the
# app code changes.
COPY deps.edn ./
RUN clojure -Rrun-tests -e '(println "Our deps are in the house!")'

# Now copy all the app code.
COPY . ./

# The max heap size is set to 1GB because Docker machines frequently have less than 4GB of RAM, and
# JDK 8 defaults to setting the max heap to 1/4 of the total RAM, but such a small heap max leads
# to OOM errors when running the tests (at 512MB; I’ve only tested 512MB and 1GB).
ENTRYPOINT clojure -J-Xmx1g -A:run-tests
