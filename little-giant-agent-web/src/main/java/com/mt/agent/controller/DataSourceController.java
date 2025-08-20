package com.mt.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.model.Result;
import com.mt.agent.repository.data.entity.DataSourceConfig;
import com.mt.agent.repository.data.service.IDataSourceConfigService;
import com.mt.agent.service.DatasourceService;
import com.mt.agent.utils.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据源控制器
 *
 * @author wsx
 * @date 2025/3/27 9:09
 */
@RestController
@RequestMapping("/datasource")
@Slf4j
@RequiredArgsConstructor
public class DataSourceController {

    private final IDataSourceConfigService dataSourceConfigService;

    private final DatasourceService datasourceService;

    /**
     * 查询数据源配置列表
     *
     * @param request HTTP请求
     * @return 数据源配置列表
     */
    @PostMapping("/dataSourceList")
    public Result dataSourceList(HttpServletRequest request) {
        try {
            log.info("[DataSourceController:dataSourceList]query data source config list");
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);

            // 创建查询条件
            LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
            // userId相同或者userId为空
            wrapper.eq(DataSourceConfig::getUserId, userId)
                    .or()
                    .isNull(DataSourceConfig::getUserId);


            // 查询数据
            List<DataSourceConfig> dataSourceConfigList = dataSourceConfigService.list(wrapper);

            return Result.success(dataSourceConfigList);
        } catch (Exception e) {
            log.error("[DataSourceController:dataSourceList]false to query data source config list", e);
            return Result.error();
        }
    }

    /**
     * 上传数据源
     *
     * @param file        文件
     * @param description 数据源描述
     * @param request     http请求
     * @return 返回信息
     */
    @PostMapping("/uploadDataSource")
    public Result uploadDataSource(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "datasourceName") String datasourceName,
                                   @RequestParam("description") String description,
                                   @RequestParam("keywords") String keywords,
                             HttpServletRequest request) {
        try {

            log.info("[DataSourceController:uploadDataSource]upload file of data source.");
            // 获取登录用户信息
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);

            //检查是否有上传任务

            // 服务层处理数据源。正常流程：保存文件、读取文件、提取数据、入库、删除文件，异常返回

            return datasourceService.uploadDataSource(file, userId, description, datasourceName, keywords);

        } catch (Exception e) {

            log.error("[DataSourceController:uploadDataSource]failed to upload file of data source: ", e);
            return Result.error("数据源加载失败，请检查文件内容" );

        }
    }


    // 根据用户id获取数据源上传信息
    @GetMapping("/getDataSourceUploadInfo")
    public Result getDataSourceUploadInfo(HttpServletRequest request) {
        try {
            log.info("[DataSourceController:getDataSourceUploadInfo]get data source upload info.");
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);
            return Result.success(datasourceService.getDataSourceUploadInfo(userId));
        } catch (Exception e) {
            log.error("[DataSourceController:getDataSourceUploadInfo]failed to get data sourceupload info: ", e);
        }
        return null;
    }


    /**
     * 上传数据源文件
     *
     * @param file        上传的文件
     * @param description 文件描述
     * @param keywords    文件关键词
     * @param request     HTTP请求
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "datasourceName", required = false) String datasourceName,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "keywords", required = false) String keywords,
                             HttpServletRequest request) {
        try {
            log.info("[DataSourceController:uploadFile]start to upload file: {}", file.getOriginalFilename());

            datasourceService.uploadFile(file);

            log.info(datasourceName);
            log.info(description);
            log.info(keywords);
            return Result.success("上传文件成功");

        } catch (Exception e) {
            log.error("[DataSourceController:uploadFile]failed to upload file", e);
            return Result.error("文件上传失败,请检查文件格式是否正确" );
        }
    }

    // 选择数据源
    @PostMapping("/selectDataSource")
    public Result selectDataSource(@RequestParam("datasourceId") String datasourceId,
                                   HttpServletRequest request) {

        try {
            log.info("[DataSourceController:selectDataSource]select data source: {}", datasourceId);
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);
            return datasourceService.selectDataSource(datasourceId, userId);

        }catch (Exception e){
            log.error("[DataSourceController:selectDataSource]failed to select data source: ", e);
            return Result.error("数据源不存在");
        }
    }


    @GetMapping("/checkDataSource")
    public Result checkDataSource(HttpServletRequest request){
        try {
            log.info("[DataSourceController:checkDataSource]check data source");
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);
            return datasourceService.checkDataSourceAndGetId(userId);
        }catch (Exception e){
            log.error("[DataSourceController:checkDataSource]failed to check data source: ", e);
            return Result.success(false);
        }

    }

}
