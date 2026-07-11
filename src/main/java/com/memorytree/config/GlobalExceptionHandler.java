/*
 * Copyright 2026 lsx1156
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.memorytree.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalExceptionHandler {

    public String handle(Throwable e) {
        if (e == null) {
            return "未知错误";
        }

        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

        if (e instanceof RuntimeException && message.contains("Ollama")) {
            log.error("Ollama service error: {}", message, e);
            return "Ollama 服务异常: " + message + "\n请确认 Ollama 服务已启动 (ollama serve) 且模型已下载。";
        }

        if (message.contains("Connection refused") || message.contains("connect")) {
            log.error("Connection error: {}", message, e);
            return "无法连接到 Ollama 服务，请确认服务已启动。";
        }

        if (message.contains("timeout") || e instanceof java.net.SocketTimeoutException) {
            log.error("Timeout error: {}", message, e);
            return "请求超时，请检查网络或模型加载状态。";
        }

        log.error("Unhandled exception: {}", message, e);
        return "系统异常: " + message;
    }

    public void handleAndLog(Throwable e) {
        handle(e);
    }
}
