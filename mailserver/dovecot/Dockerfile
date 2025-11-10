FROM debian:12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        dovecot-imapd \
        dovecot-lmtpd \
        dovecot-sieve \
        dovecot-managesieved \
        rsyslog \
        supervisor \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /var/mail && chown -R dovecot:dovecot /var/mail

EXPOSE 143 993 24
CMD ["/usr/sbin/dovecot", "-F"]
