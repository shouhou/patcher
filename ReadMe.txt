使用说明：
1、请将git提交的文件列表复制到src/main/resources/patch_files.txt文件中
2、修改patchFiles,patchDir,baseDir相应的为自身文件系统的文件 其中baseDir为项目根目录
3、执行SmartPatcher.java

项目目标：
v2.0  加入自动读取git日志信息，加入自动调用shell提交
v2.1  加入自适应匹配规则


更新日志：
v2.2
1、加入重启服务器
2、加入上传进度条
3、加入日志记录,查看日志
4、加入系统命令,加入命令帮助字    patch fqbus -log  建立规则

v2.0
1、增加了自动读取git提交信息，在application.properties中配置日志读取的范围
2、增加了服务器自动上传功能，需配置服务器项目目录

v1.1
在1.0版本基本上，扩展了不含WebRoot目录项目更新 适用于fqbus dahsing_jjk类项目

v1.0  
针对newstar项目完成了自动生成更新补丁功能


PS：如有任何bug，请联系我，邮箱：shouhou91@foxmail.com