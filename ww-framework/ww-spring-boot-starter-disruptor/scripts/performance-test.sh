#!/bin/bash

# Disruptor性能测试脚本
# 用于生产环境性能验证和调优

set -e

# 配置参数
TEST_DURATION=${TEST_DURATION:-60}  # 测试持续时间（秒）
EVENT_RATE=${EVENT_RATE:-10000}      # 每秒事件数
BATCH_SIZE=${BATCH_SIZE:-100}        # 批处理大小
THREAD_COUNT=${THREAD_COUNT:-4}      # 线程数

# JVM参数
JVM_OPTS="-Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 测试应用配置
APP_CONFIG="
ww:
  disruptor:
    enabled: true
    ring-buffer-size: 65536
    consumer-threads: 8
    producer-threads: 4
    batch-size: ${BATCH_SIZE}
    batch-timeout-ms: 100
    wait-strategy: YIELDING
    persistence:
      enabled: false
    metrics:
      enabled: true
      detailed: true
"

echo "=== Disruptor性能测试 ==="
echo "测试持续时间: ${TEST_DURATION}秒"
echo "事件速率: ${EVENT_RATE} events/sec"
echo "批处理大小: ${BATCH_SIZE}"
echo "线程数: ${THREAD_COUNT}"
echo ""

# 创建测试配置
cat > application-test.yml << EOF
${APP_CONFIG}
EOF

# 启动应用
echo "启动测试应用..."
java ${JVM_OPTS} -jar target/ww-app-*.jar \
  --spring.profiles.active=test \
  --spring.config.location=classpath:/application-test.yml &

APP_PID=$!
echo "应用PID: ${APP_PID}"

# 等待应用启动
echo "等待应用启动..."
sleep 30

# 检查应用状态
if ! kill -0 ${APP_PID} 2>/dev/null; then
    echo "应用启动失败"
    exit 1
fi

echo "应用启动成功，开始性能测试..."

# 运行性能测试
python3 << EOF
import requests
import time
import threading
import json
from concurrent.futures import ThreadPoolExecutor
import statistics

# 测试配置
DURATION = ${TEST_DURATION}
EVENT_RATE = ${EVENT_RATE}
THREAD_COUNT = ${THREAD_COUNT}
BATCH_SIZE = ${BATCH_SIZE}

# 统计信息
published_events = 0
failed_requests = 0
response_times = []

def publish_events():
    global published_events, failed_requests, response_times
    
    events_per_second = EVENT_RATE // THREAD_COUNT
    events_per_batch = min(BATCH_SIZE, events_per_second)
    
    start_time = time.time()
    end_time = start_time + DURATION
    
    while time.time() < end_time:
        batch_start = time.time()
        
        # 准备批量事件
        events = []
        for i in range(events_per_batch):
            event = {
                "id": f"perf-event-{int(time.time() * 1000)}-{i}",
                "data": f"Performance test data {i}",
                "timestamp": int(time.time() * 1000)
            }
            events.append(event)
        
        # 发送请求
        try:
            response_start = time.time()
            response = requests.post(
                "http://localhost:8080/api/disruptor/publish/batch",
                json=events,
                timeout=5
            )
            response_end = time.time()
            
            if response.status_code == 200:
                published_events += len(events)
                response_times.append((response_end - response_start) * 1000)
            else:
                failed_requests += 1
                print(f"Request failed with status: {response.status_code}")
                
        except Exception as e:
            failed_requests += 1
            print(f"Request error: {e}")
        
        # 控制发送速率
        batch_duration = time.time() - batch_start
        target_duration = events_per_batch / events_per_second
        if batch_duration < target_duration:
            time.sleep(target_duration - batch_duration)

def get_metrics():
    try:
        response = requests.get("http://localhost:8080/actuator/disruptor", timeout=5)
        if response.status_code == 200:
            return response.json()
    except:
        pass
    return {}

# 启动测试线程
print(f"启动 {THREAD_COUNT} 个测试线程...")
with ThreadPoolExecutor(max_workers=THREAD_COUNT) as executor:
    futures = [executor.submit(publish_events) for _ in range(THREAD_COUNT)]
    
    # 监控指标
    start_time = time.time()
    while time.time() - start_time < DURATION:
        metrics = get_metrics()
        if metrics:
            print(f"队列利用率: {metrics.get('queueUtilization', 0):.2f}%")
            print(f"待处理事件: {metrics.get('pendingEventCount', 0)}")
        time.sleep(5)
    
    # 等待所有线程完成
    for future in futures:
        future.result()

# 获取最终指标
final_metrics = get_metrics()

# 计算统计信息
actual_duration = DURATION
throughput = published_events / actual_duration
avg_response_time = statistics.mean(response_times) if response_times else 0
p95_response_time = statistics.quantiles(response_times, n=20)[18] if len(response_times) > 20 else 0
p99_response_time = statistics.quantiles(response_times, n=100)[98] if len(response_times) > 100 else 0

print("\\n=== 性能测试结果 ===")
print(f"测试持续时间: {actual_duration:.2f}秒")
print(f"发布事件总数: {published_events}")
print(f"失败请求数: {failed_requests}")
print(f"平均吞吐量: {throughput:.2f} events/sec")
print(f"平均响应时间: {avg_response_time:.2f}ms")
print(f"P95响应时间: {p95_response_time:.2f}ms")
print(f"P99响应时间: {p99_response_time:.2f}ms")

if final_metrics:
    print(f"最终队列利用率: {final_metrics.get('queueUtilization', 0):.2f}%")
    print(f"最终待处理事件: {final_metrics.get('pendingEventCount', 0)}")

# 性能评估
if throughput >= EVENT_RATE * 0.9:
    print("\\n✅ 性能测试通过")
else:
    print("\\n❌ 性能测试未达到预期")
    exit(1)
EOF

# 停止应用
echo "停止测试应用..."
kill ${APP_PID} 2>/dev/null || true
wait ${APP_PID} 2>/dev/null || true

# 清理
rm -f application-test.yml

echo "性能测试完成"
