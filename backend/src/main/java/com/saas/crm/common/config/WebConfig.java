package com.saas.crm.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 静态资源映射：
 * 1. /uploads/** → 磁盘上传目录，供前端直接访问图片/附件；
 * 2. /** → 前端构建产物（桌面/单包部署时由 app.web.static-dir 指定外置目录，
 *    否则回退 classpath:/static/），未命中的前端路由统一回退 index.html（SPA）。
 *    /api 与 /uploads 前缀绝不回退，避免接口 404 被吞成 HTML。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /** 前端构建产物目录（桌面端由主进程注入；为空时仅用 classpath:/static/） */
    @Value("${app.web.static-dir:}")
    private String staticDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);

        registry.addResourceHandler("/**")
                .addResourceLocations(frontendLocations())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    private String[] frontendLocations() {
        if (StringUtils.hasText(staticDir)) {
            return new String[]{
                    Paths.get(staticDir).toAbsolutePath().toUri().toString(),
                    "classpath:/static/"};
        }
        return new String[]{"classpath:/static/"};
    }

    private class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = location.createRelative(resourcePath);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            // API 与上传路径不回退，避免接口 404 被吞成 HTML
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("uploads/")) {
                return null;
            }
            // SPA 路由回退：优先当前 location 下的 index.html，兜底 classpath
            Resource index = location.createRelative("index.html");
            if (index.exists() && index.isReadable()) {
                return index;
            }
            Resource classpathIndex = new ClassPathResource("/static/index.html");
            return classpathIndex.exists() ? classpathIndex : null;
        }
    }
}
