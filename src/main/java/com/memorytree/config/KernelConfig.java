package com.memorytree.config;

import com.memorytree.kernel.OllamaTrunkKernel;
import com.memorytree.kernel.TrunkKernel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class KernelConfig {

    @Bean
    @Primary
    public TrunkKernel trunkKernel(OllamaTrunkKernel ollamaTrunkKernel) {
        return ollamaTrunkKernel;
    }
}