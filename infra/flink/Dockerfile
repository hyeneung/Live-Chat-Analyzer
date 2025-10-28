# # docker build --no-cache -t flink-sentiment-env:latest .
# 1. Use an official Flink image as a base
FROM flink:1.17.1-scala_2.12-java11

# 2. Switch to the root user to install system packages
USER root

# 3. Install Python, pip, and build essentials
RUN sed -i 's/archive.ubuntu.com/mirror.kakao.com/g' /etc/apt/sources.list && \
    apt-get update -y && \
    apt-get install -y python3 python3-pip && \
    # python -> python3 symboolic link
    ln -s /usr/bin/python3 /usr/bin/python && \ 
    apt-get clean

# 4. Set the working directory
WORKDIR /app

# 5. Copy only the requirements file to leverage Docker cache
COPY requirements.txt .

# 6. Install all Python dependencies
# This layer will be cached. It only re-runs if requirements.txt changes.
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy custom jars from the local 'lib' directory to Flink's lib directory
COPY ./lib /opt/flink/lib/

# 7. Switch back to the default flink user
USER flink