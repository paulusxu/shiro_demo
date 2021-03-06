package com.example.shiro.config;

import com.example.shiro.core.shiro.MyRealm;
import com.example.shiro.core.shiro.MySessionManager;
import com.example.shiro.core.shiro.RedisCacheManager;
import com.example.shiro.core.shiro.RedisSessionDAO;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/28.
 */
@Configuration
public class ShiroConfiguration {
    @Value("${shiro.redis.sessionLive}")
    private long sessionLive;
    @Value("${shiro.redis.sessionPrefix}")
    private String sessionPrefix;
    @Value("${shiro.redis.cacheLive}")
    private long cacheLive;
    @Value("${shiro.redis.cachePrefix}")
    private String cachePrefix;

    /**
     * 自定义shiro cache管理
     *
     * @return
     */
    @Bean(name = "redisCacheManager")
    public RedisCacheManager redisCacheManager(@Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        //cache过期时间及前缀
        redisCacheManager.setCacheLive(cacheLive);
        redisCacheManager.setCacheKeyPrefix(cachePrefix);
        redisCacheManager.setRedisTemplate(redisTemplate);
        return redisCacheManager;
    }

    /**
     * 凭证匹配器（密码加密）
     *
     * @return
     */
    @Bean(name = "hashedCredentialsMatcher")
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
        //加密算法:这里使用MD5算法;
        hashedCredentialsMatcher.setHashAlgorithmName("md5");
        //加密的次数
        hashedCredentialsMatcher.setHashIterations(2);
        return hashedCredentialsMatcher;
    }

    /**
     * Session ID生成管理器
     *
     * @return
     */
    @Bean(name = "sessionIdGenerator")
    public JavaUuidSessionIdGenerator sessionIdGenerator() {
        JavaUuidSessionIdGenerator sessionIdGenerator = new JavaUuidSessionIdGenerator();
        return sessionIdGenerator;
    }

    /**
     * 自定义shiro session
     *
     * @return
     */
    @Bean(name = "redisSessionDAO")
    public RedisSessionDAO redisSessionDAO(JavaUuidSessionIdGenerator sessionIdGenerator, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setSessionIdGenerator(sessionIdGenerator);
        //session过期时间及前缀
        redisSessionDAO.setSessionLive(sessionLive);
        redisSessionDAO.setSessionKeyPrefix(sessionPrefix);
        redisSessionDAO.setRedisTemplate(redisTemplate);
        return redisSessionDAO;
    }

    /**
     * 自定义sessionManager
     *
     * @return
     */
    @Bean(name = "sessionManager")
    public SessionManager sessionManager(RedisSessionDAO redisSessionDAO) {
        MySessionManager mySessionManager = new MySessionManager();
        mySessionManager.setSessionDAO(redisSessionDAO);
        return mySessionManager;
    }

    @Bean(name = "myRealm")
    public MyRealm myRealm() {
        MyRealm myShiroRealm = new MyRealm();
//        myShiroRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        myShiroRealm.setCachingEnabled(true);
//        myShiroRealm.setCacheManager(redisCacheManager());
        return myShiroRealm;
    }

    @Bean(name = "securityManager")
    public SecurityManager securityManager(SessionManager sessionManager, RedisCacheManager redisCacheManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(myRealm());
        // 自定义session管理 使用redis
        securityManager.setSessionManager(sessionManager);
        // 自定义缓存实现 使用redis
        securityManager.setCacheManager(redisCacheManager);
        return securityManager;
    }


    @Bean(name = "shirFilter")
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        //注意过滤器配置顺序 不能颠倒
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap();
        //退出
        filterChainDefinitionMap.put("/logout", "logout");
        //匿名访问 跳转页面
        filterChainDefinitionMap.put("/userLogin", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/captcha", "anon");
        //拦截所有请求
        filterChainDefinitionMap.put("/**", "authc");
        //未认证 跳转未认证页面
        shiroFilterFactoryBean.setLoginUrl("/unAuthen");
        //未授权 跳转未权限页面
        shiroFilterFactoryBean.setUnauthorizedUrl("/unAuthor");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    /**
     * 加这个本类@Values取不到值...cao
     *
     * @return
     */
//    @Bean(name = "lifecycleBeanPostProcessor")
//    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
//        return new LifecycleBeanPostProcessor();
//    }
    @Bean(name = "advisorAutoProxyCreator")
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);
        return advisorAutoProxyCreator;
    }

    @Bean(name = "authorizationAttributeSourceAdvisor")
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }
}
