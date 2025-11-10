FROM debian:12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        postfix \
        postfix-pcre \
        opendkim \
        opendkim-tools \
        rsyslog \
        supervisor \
        libsasl2-modules \
        libsasl2-modules-db \
    && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 25 587
CMD ["/entrypoint.sh"]
