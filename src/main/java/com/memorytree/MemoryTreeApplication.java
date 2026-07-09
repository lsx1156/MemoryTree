package com.memorytree;

import com.memorytree.gui.MemoryTreeFxApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MemoryTreeApplication {
    public static void main(String[] args) {
        boolean useGui = true;
        for (String arg : args) {
            if ("--no-gui".equalsIgnoreCase(arg) || "-ng".equalsIgnoreCase(arg)) {
                useGui = false;
                break;
            }
        }
        
        if (useGui) {
            MemoryTreeFxApplication.main(args);
        } else {
            SpringApplication.run(MemoryTreeApplication.class, args);
        }
    }
}