FROM debian:12-slim

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        kafkacat \
        redis-tools \
        jq \
        ca-certificates \
        python3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY consume.sh .

RUN chmod +x consume.sh

CMD ["./consume.sh"]
