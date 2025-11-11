# docker build --no-cache -t spark-summarizer:latest .
FROM apache/spark:3.5.0

USER root

COPY requirements.txt /app/
COPY summarizer.py /app/

WORKDIR /app

RUN pip install --no-cache-dir -r requirements.txt
