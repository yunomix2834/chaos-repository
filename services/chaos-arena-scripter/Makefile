.PHONY: proto run

proto:
	python -m grpc_tools.protoc \
		-I proto \
		--python_out=generated \
		--grpc_python_out=generated \
		../../proto/arena.proto

run:
	python -m chaos_scripter.main