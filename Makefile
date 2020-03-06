include .env

logo:
	docker build -t ${SERVER_NAME} . && docker run -p 8124:8124 ${SERVER_NAME}

redis:
	docker-compose up --force-recreate --detach ${REDIS_NAME}

stop_redis:
	docker-compose stop ${REDIS_NAME}

rm_redis: stop_redis
	docker-compose rm ${REDIS_NAME}


test:
	docker-compose build --no-cache && docker-compose up