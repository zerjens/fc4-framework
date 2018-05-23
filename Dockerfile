FROM openjdk:8-alpine

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"

WORKDIR /tmp
RUN echo "@testing http://nl.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories
RUN apk add --update --no-cache bash curl
RUN curl -O https://download.clojure.org/install/linux-install-1.9.0.326.sh
RUN chmod +x linux-install-1.9.0.326.sh
RUN ./linux-install-1.9.0.326.sh

# Use the `clojure` script to evaluate a simple form so as to to download Clojure itself so we
# don’t have to re-download it and make a new image whenever the deps or the app code change.
RUN clojure -e '(println "Clojure is in the house!")'

WORKDIR /code

# Copy deps.edn, then trigger clj to download the deps, separately from and prior to copying the
# app code so that we don’t have to re-download deps every time the app code changes.
COPY deps.edn ./
RUN clojure -Rrun-tests -e '(println "Our deps are in the house!")'

# Now copy all the app code.
COPY . ./

# The max heap size is set to 1GB because Docker machines frequently have less than 4GB of RAM, and
# JDK 8 defaults to setting the max heap to 1/4 of the total RAM, but such a small heap max leads
# to OOM errors when running the tests (at 512MB; I’ve only tested 512MB and 1GB).
ENTRYPOINT clojure -J-Xmx1g -A:run-tests
