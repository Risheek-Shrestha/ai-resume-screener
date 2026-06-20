FROM python:3.11-slim

WORKDIR /app

# CPU-only torch: the default PyPI wheel pulls CUDA binaries that bloat
# the image by several GB and aren't needed for inference on a CPU host.
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Bake the embedding model into the image at build time, so the container
# doesn't need internet access (or a slow first request) at startup.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

COPY main.py .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]