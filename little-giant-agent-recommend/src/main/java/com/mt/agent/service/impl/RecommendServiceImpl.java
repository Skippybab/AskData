package com.mt.agent.service.impl;

import com.mt.agent.ai.service.AiService;
import com.mt.agent.model.Result;
import com.mt.agent.repository.value.service.IValueQuestionDetailService;
import com.mt.agent.repository.value.service.IValueRecommendRelationService;
import com.mt.agent.repository.value.vo.RecommendQuestionVo;
import com.mt.agent.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 推荐问题服务实现类
 * <p>
 * 该类主要负责根据当前问题或默认条件，为用户推荐相关的问题。
 * 推荐问题会根据用户上下文进行个性化处理，包括替换问题模板中的年份、城市、地区、行业等变量。
 * </p>
 *
 * @author zzq
 * @since 2025-04-07
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final IValueRecommendRelationService valueRecommendRelationService;

    private final IValueQuestionDetailService valueQuestionDetailService;

    private final AiService aiService;


    /**
     * 获取推荐问题列表
     * <p>
     * 根据问题ID获取相关推荐问题，如果问题ID为空，则返回默认推荐问题列表。
     * 所有推荐问题会根据用户上下文进行变量替换处理。
     * </p>
     *
     * @param questionId 问题ID，可以为空
     * @param userId     用户ID，用于获取用户上下文
     * @return 封装了推荐问题列表的结果对象
     * @throws RuntimeException 当推荐问题处理过程中发生异常时抛出
     */
    @Override
    public Result recommendQuestion(String questionId, String userId) {

        try {
            if (questionId == null || questionId.isEmpty()) {
//                // 获取默认推荐问题list - 固定返回指定的三个默认问题
//                LambdaQueryWrapper<ValueQuestionDetail> queryWrapper = new LambdaQueryWrapper<>();
//                queryWrapper.in(ValueQuestionDetail::getQuestionId, List.of("4-1", "4-2", "1-1"));
//                List<ValueQuestionDetail> valueQuestionDetailList = valueQuestionDetailService.list(queryWrapper);
//                List<RecommendQuestionVo> recommendQuestionVoList = new ArrayList<>();
//
//                // 转换问题详情为推荐问题VO对象
//                valueQuestionDetailList.forEach(questionDetail -> recommendQuestionVoList
//                        .add(RecommendQuestionVo.builder().questionId(questionDetail.getQuestionId())
//                                .questionDesc(questionDetail.getQuestionTemplate()).build()));
//                List<RecommendQuestionVo> recommendQuestionVos = buildRecommendQuestion(recommendQuestionVoList, userId, true);
                // todo 构建推荐问题(处理变量替换)
                RecommendQuestionVo recommendQuestionVo1=RecommendQuestionVo.builder().questionId("4-1").questionDesc("数据源支持哪些行业的分析？").build();
                RecommendQuestionVo recommendQuestionVo2=RecommendQuestionVo.builder().questionId("4-1").questionDesc("数据源支持哪些指标类型的分析？").build();

                List<RecommendQuestionVo> recommendQuestionVoList = new ArrayList<>();
                recommendQuestionVoList.add(recommendQuestionVo1);
                recommendQuestionVoList.add(recommendQuestionVo2);

                return Result.success(recommendQuestionVoList);

            } else {
                // 获取问题列表 - 基于问题ID从推荐关系表中获取相关问题
                List<RecommendQuestionVo> recommendQuestionList = valueRecommendRelationService
                        .getRecommendQuestionList(questionId);

                // 拼接推荐问题(处理变量替换)
                List<RecommendQuestionVo> recommendQuestionVos = buildRecommendQuestion(recommendQuestionList, userId, false);

                return Result.success(recommendQuestionVos);
            }
        } catch (Exception e) {
            log.error("推荐问题发生异常：{}", e);
            throw new RuntimeException("推荐问题发生异常", e);
        }
    }

    /**
     * 构建推荐问题
     * <p>
     * 处理问题模板中的变量替换，包括：
     * - {year}: 替换为年份
     * - {city}: 替换为城市
     * - {region}: 替换为地区
     * - {industry}: 替换为行业
     * - {enterprise}: 替换为企业
     * - {index}: 替换为指标(默认为"营收")
     * </p>
     * <p>
     * 变量值优先从用户上下文中获取，如果上下文中不存在，则使用默认值
     * </p>
     *
     * @param recommendQuestionList 待处理的推荐问题列表
     * @param userId                用户ID，用于获取用户上下文
     * @param isNull                是否所有值默认为空
     * @return 处理后的推荐问题列表
     */
    public List<RecommendQuestionVo> buildRecommendQuestion(List<RecommendQuestionVo> recommendQuestionList,
                                                            String userId, boolean isNull) {
        log.info("开始拼接推荐问题：");

        //创建存储信息的变量
        AtomicReference<String> year = new AtomicReference<>();
        AtomicReference<String> city = new AtomicReference<>();
        AtomicReference<String> region = new AtomicReference<>();
        AtomicReference<String> industry = new AtomicReference<>();
        AtomicReference<String> enterprise = new AtomicReference<>();
        AtomicReference<String> index = new AtomicReference<>();

        //如果值默认不为空，则从共识区中获取信息
        if (!isNull) {
            year.set(getContextValueIfExists(userId, "year"));
            city.set(getContextValueIfExists(userId, "city"));
            region.set(getContextValueIfExists(userId, "region"));
            industry.set(getContextValueIfExists(userId, "industry"));
            enterprise.set(getContextValueIfExists(userId, "enterprise"));
            index.set(getContextValueIfExists(userId, "index"));
        }

        //处理问题推荐列表，进行变量替换
        ArrayList<RecommendQuestionVo> resultList = new ArrayList<>();
        recommendQuestionList.stream()
                .filter(recommendQuestionVo -> recommendQuestionVo.getQuestionDesc() != null
                        && !recommendQuestionVo.getQuestionDesc().isEmpty())
                .forEach(recommendQuestionVo -> {
            // 拼接推荐问题
            String questionDesc = recommendQuestionVo.getQuestionDesc();

                    // 处理年份变量替换
                    if (questionDesc.contains("{year}")) {

                        // 如果年份为空，则从数据库中获取
                        if (year.get() == null || year.get().isEmpty() || year.get().equals("null")) {
                            year.set(getYear(userId));
                        }

                        questionDesc = questionDesc.replace("{year}", year + "年");
                    }

                    // 处理城市变量替换
                    if (questionDesc.contains("{city}")) {

                        // 如果城市为空，则从数据库中获取
                        if (city.get() == null || city.get().isEmpty() || city.get().equals("null")) {
                            // 如果年份为空，则从数据库中获取
                            if (year.get() == null || year.get().isEmpty() || year.get().equals("null")) {
                                year.set(getYear(userId));
                            }
                            city.set(getCity(userId, year.get()));
                        }

                        questionDesc = questionDesc.replace("{city}", city.get());
                    }

                    // 处理地区变量替换
                    if (questionDesc.contains("{region}")) {

                        if (region.get() != null && !region.get().isEmpty() && !region.get().equals("null")) {
                            // 上下文存在且有值，使用上下文值
                            questionDesc = questionDesc.replace("{region}", region.get());
                        } else {
                            // 上下文不存在或值不存在，使用空字符串
                            questionDesc = questionDesc.replace("{region}", "");
                        }
                    }

                    // 处理行业变量替换
                    if (questionDesc.contains("{industry}")) {

                        // 如果行业为空，则从数据库中获取
                        if (industry.get() == null || industry.get().isEmpty() || industry.get().equals("null")) {

                            // 如果年份为空，则从数据库中获取
                            if (year.get() == null || year.get().isEmpty() || year.get().equals("null")) {
                                year.set(getYear(userId));
                            }
                            //如果城市为空，则从数据库中获取
                            if (city.get() == null || city.get().isEmpty() || city.get().equals("null")) {
                                city.set(getCity(userId, year.get()));
                            }

                            industry.set(getIndustry(userId, year.get(), city.get(), region.get()));
                        }

                        questionDesc = questionDesc.replace("{industry}", industry.get());
                    }

                    // 处理企业变量替换
                    if (questionDesc.contains("{enterprise}")) {

                        // 如果企业为空，则从数据库中获取
                        if (enterprise.get() == null || enterprise.get().isEmpty() || enterprise.get().equals("null")) {
                            // 如果年份为空，则从数据库中获取
                            if (year.get() == null || year.get().isEmpty() || year.get().equals("null")) {
                                year.set(getYear(userId));
                            }
                            //如果城市为空，则从数据库中获取
                            if (city.get() == null || city.get().isEmpty() || city.get().equals("null")) {
                                city.set(getCity(userId, year.get()));
                            }
                            //如果行业为空，则从数据库中获取
                            if (industry.get() == null || industry.get().isEmpty() || industry.get().equals("null")) {
                                industry.set(getIndustry(userId, year.get(), city.get(), region.get()));
                            }

                            enterprise.set(getEnterprise(userId, year.get(), city.get(), region.get(), industry.get()));
                        }

                        questionDesc = questionDesc.replace("{enterprise}", enterprise.get());
                    }

                    // 处理指标变量替换
                    if (questionDesc.contains("{index}")) {

                        if (index.get() != null && !index.get().isEmpty() && !index.get().equals("null")) {
                            // 上下文存在且有值，使用上下文值
                            questionDesc = questionDesc.replace("{index}", index.get());
                        } else {
                            // 上下文不存在或值不存在，使用默认值"营收"
                            questionDesc = questionDesc.replace("{index}", "营收");
                        }
                    }

                    log.info("拼接推荐问题：{}", questionDesc);
                    recommendQuestionVo.setQuestionDesc(questionDesc);
                    resultList.add(recommendQuestionVo);
                });

        return resultList;
    }

    /**
     * 动态获取年份
     * 通过AI生成SQL查询最新年份数据
     */
    private String getYear(String userId) {
        String queryIntent = "查询年份数据";
        String params = "只查询存储的数据中最新的年份，年份字段使用别名data";

        // 上下文不存在或值不存在，使用数据库最新一年
        List<Map<String, Object>> list = dynamicSqlQuery(userId, queryIntent, params, "");

        if (list.isEmpty() || !list.get(0).containsKey("data")) {
            log.error("[RecommendServiceImpl:getYear] error: 查询不到数据");
            return null;
        }

        return list.get(0).get("data").toString();
    }

    /**
     * 动态获取城市
     * 通过AI生成SQL查询热门城市数据
     */
    private String getCity(String userId, String year) {
        String queryIntent = "查询城市数据";
        String params = "只返回符合条件中年份的一个城市名，城市字段使用别名data";
        String condition = "年份：" + year;

        // 使用动态SQL查询获取推荐城市
        List<Map<String, Object>> list = dynamicSqlQuery(userId, queryIntent, params, condition);

        if (list.isEmpty() || !list.get(0).containsKey("data")) {
            log.error("[RecommendServiceImpl:getCity] error: 查询不到数据");
            return null;
        }

        return list.get(0).get("data").toString();
    }

    /**
     * 动态获取行业
     * 通过AI生成SQL查询行业数据
     */
    private String getIndustry(String userId, String year, String city, String region) {

        String queryIntent = "查询行业数据";
        String params = "只返回符合查询条件的一个行业名称，行业字段使用别名data";

        StringBuilder condition = new StringBuilder();
        condition.append("年份：").append(year).append("，城市：").append(city);
        if (region != null && !region.isEmpty() && !region.equals("null")) {
            condition.append("，区县：").append(region);
        }

        // 使用动态SQL查询获取推荐行业
        List<Map<String, Object>> list = dynamicSqlQuery(userId, queryIntent, params, condition.toString());

        if (list.isEmpty() || !list.get(0).containsKey("data")) {
            log.error("[RecommendServiceImpl:getIndustry] error: 查询不到数据");
            // 查询失败时返回null，由调用方处理
            return null;
        }

        return list.get(0).get("data").toString();
    }

    /**
     * 动态获取企业
     * 通过AI生成SQL查询特定行业的热门企业
     */
    private String getEnterprise(String userId, String year, String city, String region, String industry) {

        String queryIntent = "查询企业数据";
        String params = "只返回符合查询条件的一个企业，企业名称字段使用别名data";

        StringBuilder condition = new StringBuilder();
        condition.append("年份：").append(year).append("，城市：").append(city);
        if (region != null && !region.isEmpty() && !region.equals("null")) {
            condition.append("，区县：").append(region);
        }
        condition.append("，行业：").append(industry);

        // 使用动态SQL查询获取推荐企业
        List<Map<String, Object>> list = dynamicSqlQuery(userId, queryIntent, params, condition.toString());

        if (list.isEmpty() || !list.get(0).containsKey("data")) {
            log.error("[RecommendServiceImpl:getEnterprise] error: 查询不到数据");
            // 查询失败时返回null，由调用方处理
            return null;
        }

        return list.get(0).get("data").toString();
    }

    /**
     * 从用户上下文中获取字段值(如果存在)
     * <p>
     * 仅当上下文存在且值不为null时返回值，否则返回null
     * </p>
     *
     * @param userId    用户ID
     * @param fieldName 字段名称
     * @return 上下文中的值，如果不存在或为null则返回null
     */
    private String getContextValueIfExists(String userId, String fieldName) {
//        Consensus.ValueWithTimestamp<Object> contextField = consensusUtil.getContextField(userId, fieldName);
//        if (contextField != null && contextField.getValue() != null) {
//            return contextField.getValue().toString();
//        }
        return null;
    }

    /**
     * 动态SQL查询方法
     * 使用LLM生成SQL并执行查询
     */
    private List<Map<String, Object>> dynamicSqlQuery(String userId, String queryIntent, String params,
                                                      String condition) {

//        // 获取建表语句
//        String createSQL = consensusUtil.getDataSourceField(userId, "createSQL", String.class);
//
//        String prompt = promptTemplateService.getTemplate("sql_generation").getContent();
//
//        // 替换模板变量
//        prompt = prompt.replace("${query_intent}", queryIntent)
//                .replace("${params}", params)
//                .replace("${condition}", condition)
//                .replace("${schema_info}", createSQL);
//
//        // 调用LLM生成SQL
//        String generatedSql = aiServiceV12.chat(prompt);
//
//        // 验证和安全检查SQL
//        String validatedSql = validateAndSanitizeSql(generatedSql);
//        if (validatedSql == null) {
//            return new ArrayList<>();
//        }
//
//        // 执行SQL查询
//        List<Map<String, Object>> results = SqlRunner.db().selectList(validatedSql);
//        return results;
        return new ArrayList<>();
    }

    /**
     * 验证和安全检查SQL
     *
     * @param sql 生成的SQL
     * @return 验证后的安全SQL
     */
    private String validateAndSanitizeSql(String sql) {

        // 检查是否只包含SELECT语句
        if (!sql.toLowerCase().startsWith("select")) {
            log.error("SQL必须以SELECT开头");
            return null;
        }

        // 检查是否包含危险操作
        String lowerSql = sql.toLowerCase();
        if (lowerSql.contains("drop") || lowerSql.contains("truncate") ||
                lowerSql.contains("delete") || lowerSql.contains("update") ||
                lowerSql.contains("insert") || lowerSql.contains("alter") ||
                lowerSql.contains("create")) {
            log.error("SQL不能包含数据修改或结构修改操作");
            return null;
        }

        return sql;
    }
}
