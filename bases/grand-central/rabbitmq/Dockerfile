FROM rabbitmq:4-management

ARG DELAYED_PLUGIN_VERSION=4.2.0

# rabbitmq images don't include curl by default
RUN set -eux; \
  apt-get update; \
  apt-get install -y --no-install-recommends curl ca-certificates; \
  rm -rf /var/lib/apt/lists/*

RUN set -eux; \
  EZ="rabbitmq_delayed_message_exchange-${DELAYED_PLUGIN_VERSION}.ez"; \
  URL="https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v${DELAYED_PLUGIN_VERSION}/${EZ}"; \
  curl -fsSL -o "/opt/rabbitmq/plugins/${EZ}" "${URL}"; \
  rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange