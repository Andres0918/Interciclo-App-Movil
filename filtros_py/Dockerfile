FROM nvidia/cuda:12.4.1-devel-ubuntu22.04

LABEL authors="eduardo"

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        python3 python3-dev python3-pip python3-venv \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install \
    pycuda \
    flask \
    flask_cors \
    numpy \
    pillow \
    werkzeug

COPY . /app
WORKDIR /app

EXPOSE 5000

CMD ["python3", "./app_production.py"]
