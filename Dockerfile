# 基础镜像使用jdk1.8
FROM java:8
# 作者
MAINTAINER ww
# 创建应用目录
WORKDIR /app
# 将jar包添加到容器中并更名
COPY my.jar /app/app.jar
# run
ENTRYPOINT ["java $JAVA_OPTS -jar /app/app.jar"]
CMD echo "server success start finsh..."
# 暴露jar服务端口
EXPOSE 19001