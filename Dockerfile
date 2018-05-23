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

# The max heap size is set to 2GB because I’ve seen OOM errors at 1GB and below. (JDK 8 defaults to
# setting the max heap to ¼ of the total RAM, and containers frequently have <= 4GB RAM.)
ENTRYPOINT clojure -J-Xmx2g -A:run-tests
