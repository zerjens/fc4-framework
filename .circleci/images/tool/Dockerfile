FROM node:11.1.0-stretch

LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"
LABEL description="Node 11, Clojure 1.9, and Chromium 70 on Debian Stretch"

ENV JRE_VERSION=8u181-b13-2~deb9u1
ENV CLOJURE_VERSION=1.9.0.394
ENV CHROMIUM_VERSION=70.0.3538.67-1~deb9u1

WORKDIR /tmp

# Install dependencies of the Renderer and Clojure
# We’re including rlwrap just because it’s handy in case we need to run a REPL
# in a container, for debugging.
RUN apt-get update -yq
RUN apt-get install -yq chromium=$CHROMIUM_VERSION openjdk-8-jre=$JRE_VERSION rlwrap

# Install Clojure
RUN wget -q https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
      && chmod +x linux-install-$CLOJURE_VERSION.sh \
      && ./linux-install-$CLOJURE_VERSION.sh
