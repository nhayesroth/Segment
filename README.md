# Command to get redis up and running (rm deletes the container after)
docker run --rm --network=segment_network -p=6379:6379 --name=redis redis
docker run --rm --network=segment_network -p=6379:6379 --name=redis --detach redis


# Commands to get the redis_proxy up and running
# Build the image
docker image build -t redis_proxy RedisProxy/
docker image build --no-cache -t redis_proxy . -f RedisProxy/Dockerfile
# -f RedisProxy/Dockerfile

# Run the container in interactive mode
docker run -it --network=segment_network -it -p=8080:8080 --name=redis_proxy redis_proxy
docker run -it --network=segment_network -p=8080:8080 -p=8124:8124 --name=redis_proxy --detach redis_proxy
docker-compose run -it --network=segment_network -p=8080:8080 -p=8124:8124 --name=redis_proxy --detach redis_proxy

# Run the container in bash (so you can ping redis)
docker run -it --network=segment_network --entrypoint /bin/bash redis_proxy -s


docker run -it --rm -v /var/run/docker.sock:/var/run/docker.sock --network=segment_network  -p=8500:8500 --name=integration_tests integration_tests



# Changed .docker/config.json to fix error in eclipse runs
was: "credsStore" : "desktop",
changed to:  "credsStore" : "osxkeychain",