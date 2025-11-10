#!/bin/bash
set -euo pipefail

postconf -e "myhostname = ${MAIL_HOSTNAME}"
postconf -e "mydomain = ${MAIL_DOMAIN}"
postconf -e "myorigin = /etc/mailname"
postconf -e "mydestination = ${MAIL_HOSTNAME}, localhost.${MAIL_DOMAIN}, localhost"
postconf -e "mynetworks = 0.0.0.0/0"
postconf -e "relayhost = "
postconf -e "smtp_tls_security_level = may"
postconf -e "smtpd_tls_security_level = may"
postconf -e "smtp_tls_loglevel = 1"
postconf -e "smtpd_tls_loglevel = 1"
postconf -e "smtpd_sasl_auth_enable = yes"
postconf -e "smtpd_sasl_type = dovecot"
postconf -e "smtpd_sasl_path = private/auth"
postconf -e "smtpd_sasl_local_domain ="
postconf -e "smtpd_sasl_security_options = noanonymous"
postconf -e "smtpd_recipient_restrictions = permit_sasl_authenticated, permit_mynetworks, reject_unauth_destination"
postconf -e "virtual_transport = lmtp:inet:dovecot:24"
postconf -e "virtual_mailbox_domains = ${MAIL_DOMAIN}"
postconf -e "virtual_mailbox_maps = hash:/etc/postfix/virtual_mailbox"
postconf -e "virtual_alias_maps = hash:/etc/postfix/virtual_alias"
postconf -e "milter_protocol = 6"
postconf -e "milter_default_action = accept"
postconf -e "smtpd_milters = inet:rspamd:11332"
postconf -e "non_smtpd_milters = inet:rspamd:11332"
postconf -e "inet_interfaces = all"
postconf -e "inet_protocols = ipv4"

cat <<MAP >/etc/postfix/virtual_mailbox
${MAIL_USER}@${MAIL_DOMAIN} ${MAIL_DOMAIN}/${MAIL_USER}/
MAP
postmap /etc/postfix/virtual_mailbox

cat <<ALIASES >/etc/postfix/virtual_alias
postmaster@${MAIL_DOMAIN} ${POSTMASTER}
ALIASES
postmap /etc/postfix/virtual_alias

echo "${MAIL_DOMAIN}" > /etc/mailname

cat <<DKIM >/etc/opendkim.conf
Syslog                  yes
UMask                   002
Mode                    sv
Socket                  inet:8891@0.0.0.0
PidFile                 /var/run/opendkim/opendkim.pid
UserID                  opendkim:opendkim
KeyTable                /etc/opendkim/key.table
SigningTable            refile:/etc/opendkim/signing.table
ExternalIgnoreList      refile:/etc/opendkim/trusted.hosts
InternalHosts           refile:/etc/opendkim/trusted.hosts
Canonicalization        relaxed/simple
OversignHeaders         From
DKIM

mkdir -p /etc/opendkim
cat <<KEYTABLE >/etc/opendkim/key.table
${DKIM_SELECTOR}._domainkey.${MAIL_DOMAIN} ${MAIL_DOMAIN}:${DKIM_SELECTOR}:/etc/opendkim/keys/mail.private
KEYTABLE

cat <<SIGNING >/etc/opendkim/signing.table
*@${MAIL_DOMAIN} ${DKIM_SELECTOR}._domainkey.${MAIL_DOMAIN}
SIGNING

cat <<TRUSTED >/etc/opendkim/trusted.hosts
127.0.0.1
localhost
${MAIL_HOSTNAME}
${MAIL_DOMAIN}
TRUSTED

chown -R opendkim:opendkim /etc/opendkim
chmod 600 /etc/opendkim/keys/mail.private

service rsyslog start
service opendkim start
service postfix start

tail -f /var/log/mail.log
