go install github.com/nats-io/natscli/nats@latest
export PATH="$PATH:$HOME/go/bin"
nats --version
nats --server "nats://localhost:4222" --user jetstream_app --password jetstream_app123 rtt

# Tạo stream + publish + check
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" \
  stream add ORDERS --subjects "orders.*" --storage file
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" \
  publish orders.created "hello-1"
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" \
  stream info ORDERS

# Đọc message kiểu JetStream
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" \
  consumer add ORDERS C1 --ack explicit --deliver all
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" \
  consumer next ORDERS C1

# Xoá stream nếu test
nats --server "nats://jetstream_app:jetstream_app123@localhost:4222" stream rm ORDERS
