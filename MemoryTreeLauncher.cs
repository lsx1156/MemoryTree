using System;
using System.Diagnostics;
using System.IO;

namespace MemoryTreeLauncher
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                string exeDir = AppDomain.CurrentDomain.BaseDirectory;
                Console.WriteLine("EXE目录: " + exeDir);
                
                string jarPath = Path.Combine(exeDir, "memorytree-2.1.0.jar");
                Console.WriteLine("JAR路径: " + jarPath);
                Console.WriteLine("JAR存在: " + File.Exists(jarPath));
                
                string javaPath = FindJava21();
                Console.WriteLine("Java路径: " + javaPath);
                Console.WriteLine("Java存在: " + File.Exists(javaPath));
                
                if (!File.Exists(javaPath))
                {
                    Console.WriteLine("ERROR: JDK 21未找到");
                    Console.WriteLine("已检查路径:");
                    Console.WriteLine("  C:\\Program Files\\Microsoft\\jdk-21.0.9.10-hotspot\\bin\\java.exe");
                    Console.WriteLine("  C:\\Program Files\\Java\\jdk-21\\bin\\java.exe");
                    Console.WriteLine("请安装JDK 21并将其路径添加到上述位置之一");
                    Console.WriteLine("下载地址: https://learn.microsoft.com/zh-cn/java/openjdk/download");
                    Console.WriteLine("按任意键退出...");
                    Console.ReadKey();
                    return;
                }
                
                if (!File.Exists(jarPath))
                {
                    Console.WriteLine("ERROR: JAR文件未找到");
                    Console.WriteLine("按任意键退出...");
                    Console.ReadKey();
                    return;
                }
                
                Console.WriteLine("启动MemoryTree...");
                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaPath;
                psi.Arguments = "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -jar \"" + jarPath + "\"";
                psi.WorkingDirectory = exeDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = false;
                
                Process process = Process.Start(psi);
                if (process != null)
                {
                    process.WaitForExit();
                    Console.WriteLine("MemoryTree已停止，退出码: " + process.ExitCode);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("错误: " + ex.ToString());
                Console.WriteLine("按任意键退出...");
                Console.ReadKey();
            }
        }
        
        static string FindJava21()
        {
            string[] paths = {
                @"C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe",
                @"C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe",
                @"C:\Program Files\Java\jdk-21\bin\java.exe",
                @"C:\Program Files\Java\jdk21\bin\java.exe",
                @"C:\Program Files\Microsoft\jdk-21\bin\java.exe"
            };
            
            foreach (string path in paths)
            {
                if (File.Exists(path))
                    return path;
            }
            
            return "";
        }
    }
}
