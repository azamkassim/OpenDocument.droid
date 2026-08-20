# Offline document AI

The Android app sends document text only to an OpenAI-compatible server on
`127.0.0.1:8081`. Nothing is sent to an internet host.

## Termux

1. Install `llama-cpp` in Termux.
2. Put a small instruct GGUF model at
   `~/models/qwen2.5-1.5b-instruct-q4_k_m.gguf`, or set `ODR_AI_MODEL` to another
   model path.
3. Copy `start-ai.sh` to Termux and make it executable.
4. Run it before using the AI button in OpenDocument Reader.

The script binds to loopback only. Other devices on the network cannot call the model.
