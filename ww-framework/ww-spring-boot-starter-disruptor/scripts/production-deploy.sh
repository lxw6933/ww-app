#!/bin/bash

# Disruptor生产环境部署脚本
# 包含JVM调优、监控配置、健康检查等

set -e

# 配置参数
APP_NAME=${APP_NAME:-"ww-disruptor-app"}
JAVA_OPTS=${JAVA_OPTS:-""}
HEAP_SIZE=${HEAP_SIZE:-"4g"}
GC_TYPE=${GC_TYPE:-"G1GC"}
THREAD_COUNT=${THREAD_COUNT:-"8"}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Disruptor生产环境部署脚本 ===${NC}"

# 检查Java版本
check_java_version() {
    echo "检查Java版本..."
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 11 ]; then
        echo -e "${RED}错误: 需要Java 11或更高版本${NC}"
        exit 1
    fi
    echo -e "${GREEN}Java版本检查通过: $(java -version 2>&1 | head -n 1)${NC}"
}

# 检查系统资源
check_system_resources() {
    echo "检查系统资源..."
    
    # 检查内存
    total_mem=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    if [ "$total_mem" -lt 8192 ]; then
        echo -e "${YELLOW}警告: 系统内存不足8GB，建议增加内存${NC}"
    fi
    
    # 检查CPU核心数
    cpu_cores=$(nproc)
    echo "CPU核心数: $cpu_cores"
    
    # 检查磁盘空间
    disk_space=$(df -h . | awk 'NR==2{print $4}')
    echo "可用磁盘空间: $disk_space"
}

# 生成JVM参数
generate_jvm_opts() {
    echo "生成JVM参数..."
    
    # 基础参数
    JVM_OPTS="-server"
    JVM_OPTS="$JVM_OPTS -Xms${HEAP_SIZE} -Xmx${HEAP_SIZE}"
    
    # GC配置
    case $GC_TYPE in
        "G1GC")
            JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
            JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
            JVM_OPTS="$JVM_OPTS -XX:G1HeapRegionSize=16m"
            JVM_OPTS="$JVM_OPTS -XX:G1NewSizePercent=30"
            JVM_OPTS="$JVM_OPTS -XX:G1MaxNewSizePercent=40"
            JVM_OPTS="$JVM_OPTS -XX:G1MixedGCCountTarget=8"
            JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=45"
            ;;
        "ZGC")
            JVM_OPTS="$JVM_OPTS -XX:+UnlockExperimentalVMOptions"
            JVM_OPTS="$JVM_OPTS -XX:+UseZGC"
            ;;
        "ParallelGC")
            JVM_OPTS="$JVM_OPTS -XX:+UseParallelGC"
            JVM_OPTS="$JVM_OPTS -XX:ParallelGCThreads=$THREAD_COUNT"
            ;;
    esac
    
    # 性能优化参数
    JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"
    JVM_OPTS="$JVM_OPTS -XX:+UseCompressedClassPointers"
    JVM_OPTS="$JVM_OPTS -XX:-UseBiasedLocking"
    JVM_OPTS="$JVM_OPTS -XX:+AlwaysPreTouch"
    JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"
    
    # 线程配置
    JVM_OPTS="$JVM_OPTS -Xss256k"
    JVM_OPTS="$JVM_OPTS -XX:CICompilerCount=4"
    
    # 监控参数
    JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
    JVM_OPTS="$JVM_OPTS -XX:+PrintGCTimeStamps"
    JVM_OPTS="$JVM_OPTS -XX:+PrintGCApplicationStoppedTime"
    JVM_OPTS="$JVM_OPTS -Xloggc:gc.log"
    JVM_OPTS="$JVM_OPTS -XX:+UseGCLogFileRotation"
    JVM_OPTS="$JVM_OPTS -XX:NumberOfGCLogFiles=5"
    JVM_OPTS="$JVM_OPTS -XX:GCLogFileSize=10M"
    
    # 故障转储
    JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
    JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=./heapdump.hprof"
    
    # 自定义参数
    if [ -n "$JAVA_OPTS" ]; then
        JVM_OPTS="$JVM_OPTS $JAVA_OPTS"
    fi
    
    echo "JVM参数: $JVM_OPTS"
}

# 生成应用配置
generate_app_config() {
    echo "生成应用配置..."
    
    cat > application-production.yml << EOF
# Disruptor生产环境配置
ww:
  disruptor:
    enabled: true
    ring-buffer-size: 65536
    consumer-threads: ${THREAD_COUNT}
    producer-threads: $((THREAD_COUNT / 2))
    batch-size: 500
    batch-timeout-ms: 100
    wait-strategy: YIELDING
    persistence:
      enabled: true
      data-dir: /data/disruptor
      segment-size: 134217728  # 128MB
      segment-retention-hours: 48
      flush-interval-ms: 1000
      sync-flush: false
    metrics:
      enabled: true
      prefix: ww.disruptor
      detailed: true

# Spring Boot配置
spring:
  application:
    name: ${APP_NAME}
  profiles:
    active: production

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,disruptor
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# 日志配置
logging:
  level:
    com.ww.app.disruptor: INFO
    root: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: ./logs/${APP_NAME}.log
    max-size: 100MB
    max-history: 30
EOF

    echo -e "${GREEN}应用配置已生成: application-production.yml${NC}"
}

# 创建启动脚本
create_startup_script() {
    echo "创建启动脚本..."
    
    cat > start-disruptor.sh << EOF
#!/bin/bash

# Disruptor应用启动脚本
APP_NAME="${APP_NAME}"
JAR_FILE="target/ww-app-*.jar"
PID_FILE="disruptor.pid"
LOG_FILE="logs/disruptor.log"

# 创建必要目录
mkdir -p logs
mkdir -p /data/disruptor

# 启动应用
echo "启动Disruptor应用..."
nohup java $JVM_OPTS -jar \$JAR_FILE \\
  --spring.profiles.active=production \\
  --spring.config.location=classpath:/application-production.yml \\
  > \$LOG_FILE 2>&1 &

echo \$! > \$PID_FILE
echo "应用已启动，PID: \$(cat \$PID_FILE)"
echo "日志文件: \$LOG_FILE"
EOF

    chmod +x start-disruptor.sh
    echo -e "${GREEN}启动脚本已创建: start-disruptor.sh${NC}"
}

# 创建停止脚本
create_stop_script() {
    echo "创建停止脚本..."
    
    cat > stop-disruptor.sh << EOF
#!/bin/bash

# Disruptor应用停止脚本
PID_FILE="disruptor.pid"

if [ -f "\$PID_FILE" ]; then
    PID=\$(cat \$PID_FILE)
    echo "停止应用，PID: \$PID"
    
    # 优雅停止
    kill -TERM \$PID
    
    # 等待停止
    for i in {1..30}; do
        if ! kill -0 \$PID 2>/dev/null; then
            echo "应用已停止"
            rm -f \$PID_FILE
            exit 0
        fi
        sleep 1
    done
    
    # 强制停止
    echo "强制停止应用..."
    kill -KILL \$PID 2>/dev/null || true
    rm -f \$PID_FILE
    echo "应用已强制停止"
else
    echo "PID文件不存在，应用可能未运行"
fi
EOF

    chmod +x stop-disruptor.sh
    echo -e "${GREEN}停止脚本已创建: stop-disruptor.sh${NC}"
}

# 创建健康检查脚本
create_health_check_script() {
    echo "创建健康检查脚本..."
    
    cat > health-check.sh << EOF
#!/bin/bash

# Disruptor应用健康检查脚本
HEALTH_URL="http://localhost:8080/actuator/health"
DISRUPTOR_URL="http://localhost:8080/actuator/disruptor"

check_health() {
    echo "检查应用健康状态..."
    
    # 检查基本健康状态
    if curl -s -f "\$HEALTH_URL" > /dev/null; then
        echo -e "\${GREEN}✅ 应用健康检查通过\${NC}"
    else
        echo -e "\${RED}❌ 应用健康检查失败\${NC}"
        return 1
    fi
    
    # 检查Disruptor状态
    if curl -s -f "\$DISRUPTOR_URL" > /dev/null; then
        echo -e "\${GREEN}✅ Disruptor状态检查通过\${NC}"
        
        # 获取详细状态
        STATUS=\$(curl -s "\$DISRUPTOR_URL" | jq -r '.running // false')
        if [ "\$STATUS" = "true" ]; then
            echo -e "\${GREEN}✅ Disruptor服务运行正常\${NC}"
        else
            echo -e "\${YELLOW}⚠️  Disruptor服务未运行\${NC}"
        fi
    else
        echo -e "\${RED}❌ Disruptor状态检查失败\${NC}"
        return 1
    fi
}

check_health
EOF

    chmod +x health-check.sh
    echo -e "${GREEN}健康检查脚本已创建: health-check.sh${NC}"
}

# 创建监控脚本
create_monitoring_script() {
    echo "创建监控脚本..."
    
    cat > monitor.sh << EOF
#!/bin/bash

# Disruptor应用监控脚本
DISRUPTOR_URL="http://localhost:8080/actuator/disruptor"
METRICS_URL="http://localhost:8080/actuator/metrics"

monitor_metrics() {
    echo "=== Disruptor监控指标 ==="
    echo "时间: \$(date)"
    echo ""
    
    # 获取Disruptor状态
    if curl -s -f "\$DISRUPTOR_URL" > /dev/null; then
        echo "Disruptor状态:"
        curl -s "\$DISRUPTOR_URL" | jq '.'
        echo ""
    fi
    
    # 获取关键指标
    echo "关键指标:"
    
    # 队列利用率
    QUEUE_UTIL=\$(curl -s "\$METRICS_URL/ww.disruptor.queue.utilization" | jq -r '.measurements[0].value // 0')
    echo "队列利用率: \${QUEUE_UTIL}%"
    
    # 待处理事件数
    PENDING_EVENTS=\$(curl -s "\$METRICS_URL/ww.disruptor.events.pending" | jq -r '.measurements[0].value // 0')
    echo "待处理事件数: \$PENDING_EVENTS"
    
    # 发布事件数
    PUBLISHED_EVENTS=\$(curl -s "\$METRICS_URL/ww.disruptor.events.published" | jq -r '.measurements[0].value // 0')
    echo "发布事件数: \$PUBLISHED_EVENTS"
    
    # 处理事件数
    PROCESSED_EVENTS=\$(curl -s "\$METRICS_URL/ww.disruptor.events.processed" | jq -r '.measurements[0].value // 0')
    echo "处理事件数: \$PROCESSED_EVENTS"
    
    echo ""
}

# 持续监控
if [ "\$1" = "continuous" ]; then
    echo "开始持续监控（按Ctrl+C停止）..."
    while true; do
        monitor_metrics
        sleep 10
    done
else
    monitor_metrics
fi
EOF

    chmod +x monitor.sh
    echo -e "${GREEN}监控脚本已创建: monitor.sh${NC}"
}

# 主函数
main() {
    check_java_version
    check_system_resources
    generate_jvm_opts
    generate_app_config
    create_startup_script
    create_stop_script
    create_health_check_script
    create_monitoring_script
    
    echo ""
    echo -e "${GREEN}=== 部署准备完成 ===${NC}"
    echo "配置文件: application-production.yml"
    echo "启动脚本: start-disruptor.sh"
    echo "停止脚本: stop-disruptor.sh"
    echo "健康检查: health-check.sh"
    echo "监控脚本: monitor.sh"
    echo ""
    echo "使用方法:"
    echo "1. 启动应用: ./start-disruptor.sh"
    echo "2. 检查状态: ./health-check.sh"
    echo "3. 监控指标: ./monitor.sh"
    echo "4. 停止应用: ./stop-disruptor.sh"
}

# 执行主函数
main "$@"
