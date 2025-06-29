#!/bin/bash

[ ! "$(docker network ls | grep sdnet )" ] && \
	docker network create --driver=bridge --subnet=172.20.0.0/16 sdnet


if [  $# -le 1 ] 
then 
		echo "usage: $0 -image <img> [ -test <num> ] [ -log OFF|ALL|FINE ] [ -sleep <seconds> ]"
		exit 1
fi 

#execute the client with the given command line parameters
docker pull smduarte/sd2425testerbase
docker pull smduarte/sd2425-tester-tp2
docker pull smduarte/sd2324-kafka:latest
docker run --rm --name=tester --network=sdnet -it -v /var/run/docker.sock:/var/run/docker.sock smduarte/sd2425-tester-tp2:latest $*

