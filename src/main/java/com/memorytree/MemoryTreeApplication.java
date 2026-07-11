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