# Command to get redis up and running (rm deletes the container after)
docker run --rm --network=segment_network -p=6379:6379 --name=redis redis


# Commands to get the redis_proxy up and running
# Build the image
docker image build -t redis_proxy RedisProxy/
# -f RedisProxy/Dockerfile

# Run the container in interactive mode
docker run -it --network=segment_network -it -p=8080:8080 --name=redis_proxy redis_proxy

# Run the container in bash (so you can ping redis)
docker run -it --network=segment_network --entrypoint /bin/bash redis_proxy -s




# Changed .docker/config.json to fix error in eclipse runs
was: "credsStore" : "desktop",
changed to:  "credsStore" : "osxkeychain",