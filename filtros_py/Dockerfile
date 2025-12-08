FROM nvidia/cuda:12.4.1-devel-ubuntu22.04

LABEL authors="eduardo"

RUN apt-get update && apt-get install -y \
    python3-pip \
    build-essential \
    libglib2.0-0 \
    libglib2.0-dev \
    libgl1-mesa-glx \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install \
    pycuda \
    flask \
    flask_cors \
    numpy \
    pillow \
    werkzeug \
    opencv-python-headless

COPY . /app
WORKDIR /app

EXPOSE 5000

CMD ["python3", "./app_production.py"]
