package com.ww.app.gateway.auth;

import com.ww.app.gateway.properties.AppGatewayProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class WhiteListMatcher {
    private final PathMatcher matcher = new AntPathMatcher();
    private final AppGatewayProperties appGatewayProperties;

    private volatile int whiteListHash = 0;
    private volatile List<String> exactWhiteList = Collections.emptyList();
    private volatile List<String> prefixWhiteList = Collections.emptyList();
    private volatile List<String> patternWhiteList = Collections.emptyList();

    public WhiteListMatcher(AppGatewayProperties appGatewayProperties) {
        this.appGatewayProperties = appGatewayProperties;
    }

    public boolean isWhiteListed(String urlPath) {
        List<String> whiteList = appGatewayProperties.getWhiteUriList();
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        refreshWhiteListCacheIfNeeded(whiteList);
        if (exactWhiteList.contains(urlPath)) {
            return true;
        }
        for (String prefix : prefixWhiteList) {
            if (urlPath.startsWith(prefix)) {
                return true;
            }
        }
        for (String pattern : patternWhiteList) {
            if (matcher.match(pattern, urlPath)) {
                return true;
            }
        }
        return false;
    }

    private void refreshWhiteListCacheIfNeeded(List<String> whiteList) {
        int currentHash = whiteList.hashCode();
        if (currentHash == whiteListHash) {
            return;
        }
        // Split patterns to speed up matching in hot paths.
        List<String> exact = new ArrayList<>();
        List<String> prefix = new ArrayList<>();
        List<String> pattern = new ArrayList<>();
        for (String path : whiteList) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (!path.contains("*") && !path.contains("?") && !path.contains("{")) {
                exact.add(path);
                continue;
            }
            if (path.endsWith("/**") && !path.substring(0, path.length() - 3).contains("*")
                    && !path.contains("?") && !path.contains("{")) {
                prefix.add(path.substring(0, path.length() - 3));
                continue;
            }
            pattern.add(path);
        }
        exactWhiteList = Collections.unmodifiableList(exact);
        prefixWhiteList = Collections.unmodifiableList(prefix);
        patternWhiteList = Collections.unmodifiableList(pattern);
        whiteListHash = currentHash;
    }
}
