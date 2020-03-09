# include .env

# logo:
# 	docker build -t ${SERVER_NAME} . && docker run -p 8124:8124 ${SERVER_NAME}

proxy:
	docker-compose up --force-recreate --detach redis_proxy

redis:
	docker run --rm redis --network=segment_network -p=6379:6379
	docker-compose up --force-recreate --detach redis

stop_redis:
	docker-compose stop redis

rm_redis: stop_redis
	docker-compose rm redis

test:
	docker-compose up
	# docker-compose build --no-cache && docker-compose up