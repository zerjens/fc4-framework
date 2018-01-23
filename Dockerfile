FROM openjdk:8-alpine

# Should this be "maintainer"?
LABEL author="Avi Flax <avi.flax@fundingcircle.com>"

WORKDIR /tmp
RUN echo "@testing http://nl.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories
RUN apk add --update bash curl rlwrap@testing && rm -rf /var/cache/apk/*
RUN curl -O https://download.clojure.org/install/linux-install-1.9.0.315.sh
RUN chmod +x linux-install-1.9.0.315.sh
RUN ./linux-install-1.9.0.315.sh

WORKDIR /code
COPY . ./

# Trigger clj to download Clojure itself and all deps from deps.edn
RUN echo '(println "Clojure is in the house!")' | clj -

# The sleep is a workaround found here: https://github.com/sflyr/docker-sqlplus/pull/2
ENTRYPOINT sleep 1; clj
