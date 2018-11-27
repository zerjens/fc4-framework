FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.28

LABEL description="Example that shows how to install fc4-tool and render a diagram."
LABEL maintainer="Avi Flax <avi.flax@fundingcircle.com>"

# Install the dependencies of fc4-tool
RUN apt-get -yq update \
      && apt-get -yq install --no-install-recommends chromium-browser

# Install fc4-tool
RUN curl -L -s \
      https://github.com/FundingCircle/fc4-framework/releases/download/master_2018-11-27_678/fc4-linux.tar.gz \
      | tar xzv

COPY *.yaml ./

RUN export PATH=$PWD/fc4:$PATH && fc4 render *.yaml
