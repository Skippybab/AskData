package com.mt.agent.test;

import com.mt.agent.buffer.util.BufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class RedisTest {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BufferUtil bufferUtil;

    @Test
    public void test1() {
        String key = "coze:buffer:4:pythonCode";
        log.info("测试获取Redis键: {}", key);

        // 方法1：使用默认编解码器
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            Object value = bucket.get();
            log.info("使用默认编解码器获取的值: {}", value);
            log.info("值的类型: {}", value != null ? value.getClass().getName() : "null");
        } catch (Exception e) {
            log.error("使用默认编解码器失败: {}", e.getMessage());
        }

        // 方法2：使用String编解码器
        try {
            RBucket<String> stringBucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
            String stringValue = stringBucket.get();
            log.info("使用String编解码器获取的值: {}", stringValue);
        } catch (Exception e) {
            log.error("使用String编解码器失败: {}", e.getMessage());
        }

        // 方法3：使用JSON编解码器
        try {
            RBucket<Object> jsonBucket = redissonClient.getBucket(key, new JsonJacksonCodec());
            Object jsonValue = jsonBucket.get();
            log.info("使用JSON编解码器获取的值: {}", jsonValue);
            log.info("JSON值的类型: {}", jsonValue != null ? jsonValue.getClass().getName() : "null");
        } catch (Exception e) {
            log.error("使用JSON编解码器失败: {}", e.getMessage());
        }

        // 方法4：检查键是否存在
        try {
            boolean exists = redissonClient.getBucket(key, StringCodec.INSTANCE).isExists();
            log.info("键是否存在: {}", exists);
        } catch (Exception e) {
            log.error("检查键存在性失败: {}", e.getMessage());
        }
    }

    @Test
    public void testSimpleStringOperation() {
        String testKey = "test:string:key";
        String testValue = "Hello Redis!";

        try {
            // 存储字符串
            RBucket<String> bucket = redissonClient.getBucket(testKey);
            bucket.set(testValue);
            log.info("成功存储字符串: {} -> {}", testKey, testValue);

            // 读取字符串
            String retrieved = bucket.get();
            log.info("成功读取字符串: {} -> {}", testKey, retrieved);

            // 清理测试数据
            bucket.delete();
            log.info("测试数据已清理");

        } catch (Exception e) {
            log.error("字符串操作测试失败: {}", e.getMessage(), e);
        }
    }

    @Test
    public void testJsonObjectOperation() {
        String pythonCode = """
                # 第一步：查询近三年净利润总额及增长率
                sql = sql_gen_single_ind("查询近三年净利润总额及增长率，查询字段包含年份、净利润总额、增长率")
                # 第二步：执行SQL获取净利润数据
                result = execute_sql(sql)
                # 第三步：用信息块展示最新净利润总额及增长率
                latest_growth_rate = ext_double_list_from_dataList(result, "增长率")[-1]
                latest_profit_total = ext_double_list_from_dataList(result, "净利润总额")[-1]
                vis_textblock("净利润核心指标", latest_profit_total)
                vis_textblock("净利润核心指标", latest_growth_rate)
                # 第四步：用单柱状图展示净利润年度趋势
                years = extStrListFromDataList(result, "年份")
                profits = extDoubleListFromDataList(result, "净利润总额")
                vis_single_bar("净利润增长趋势", years, profits)
                """;

        bufferUtil.savePythonCode("4", pythonCode);
    }
}
