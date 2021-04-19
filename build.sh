#!/bin/bash

if [ `docker images | grep ${GIT_COMMIT} | wc -l` eq 1 ] then
	exit 0
fi

docker build -t chdm053/demo:${GIT_COMMIT} .
docker push chdm053/demo:${GIT_COMMIT}
docker rmi -f $(docker images chdm053/demo -f "dangling=true" -q)
exit 0