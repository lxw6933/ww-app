# 基础镜像使用jdk1.8
FROM openjdk:8-jdk-alpine
# 作者
LABEL maintainer="ww"
LABEL version="1.0.0"
LABEL description="秒杀服务"
# 创建应用目录
WORKDIR /app
# 将jar包添加到容器中并更名
COPY mall-seckill.jar /app/mall-seckill.jar
# run
ENTRYPOINT ["java $JAVA_OPTS -jar /app/mall-seckill.jar"]
CMD echo "server success start finsh..."
# 暴露jar服务端口
EXPOSE 17001