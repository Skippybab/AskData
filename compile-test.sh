#!/bin/bash

echo "清理并编译mt-agent-workflow-api项目..."
echo "=================================="

# 清理旧的编译文件
echo "1. 清理旧的编译文件..."
cd mt-agent-workflow-api-core
mvn clean
cd ..

# 编译核心模块
echo "2. 编译核心模块..."
cd mt-agent-workflow-api-core
mvn compile -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 核心模块编译成功！"
else
    echo "❌ 核心模块编译失败！"
    exit 1
fi

echo "=================================="
echo "编译完成！"