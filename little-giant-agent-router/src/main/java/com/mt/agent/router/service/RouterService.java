package com.mt.agent.router.service;

import com.mt.agent.reporter.SubEventReporter;

public interface RouterService {

    /**
     * 路由匹配
     *
     * @param userId
     * @param reporter
     */
    void routeMatching(String userId, SubEventReporter reporter);

}
