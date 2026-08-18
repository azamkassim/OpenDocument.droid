#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

MODEL_PATH="${ODR_AI_MODEL:-$HOME/models/qwen2.5-1.5b-instruct-q4_k_m.gguf}"
SERVER_BIN="${ODR_AI_SERVER:-$PREFIX/bin/llama-server}"

if [[ ! -x "$SERVER_BIN" ]]; then
    printf 'llama-server is not installed. Run: pkg install llama-cpp\n' >&2
    exit 1
fi

if [[ ! -f "$MODEL_PATH" ]]; then
    printf 'Model not found: %s\n' "$MODEL_PATH" >&2
    printf 'Set ODR_AI_MODEL to the full path of a GGUF instruct model.\n' >&2
    exit 1
fi

exec "$SERVER_BIN" \
    --host 127.0.0.1 \
    --port 8081 \
    --model "$MODEL_PATH" \
    --ctx-size 4096 \
    --threads 4 \
    --parallel 1
