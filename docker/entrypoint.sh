#!/bin/sh
set -eu

config=/usr/local/tomcat/libreclinica.config/datainfo.properties

set_property() {
    key=$1
    value=$2
    escaped_value=$(printf '%s' "$value" | sed 's/[&|\\]/\\&/g')

    if grep -q "^${key}=" "$config"; then
        sed -i "s|^${key}=.*|${key}=${escaped_value}|" "$config"
    else
        printf '\n%s=%s\n' "$key" "$value" >> "$config"
    fi
}

# LibreClinica reads this properties file at startup rather than Docker
# environment variables. Keep defaults compatible with the bundled config.
set_property dbType "${dbType:-postgres}"
set_property dbUser "${dbUser:-postgres}"
set_property dbPass "${dbPass:-password}"
set_property db "${dbName:-${db:-libreclinica}}"
set_property dbPort "${dbPort:-5432}"
set_property dbHost "${dbHost:-db}"
set_property filePath "${filePath:-/usr/local/tomcat/libreclinica.data/}"
set_property userAccountNotification "${userAccountNotification:-email}"
set_property adminEmail "${adminEmail:-admin@example.com}"
set_property sysURL "${sysURL:-http://localhost:8080/libreclinica/MainMenu}"

if [ -n "${supportURL:-}" ]; then
    set_property supportURL "$supportURL"
fi

exec "$@"
