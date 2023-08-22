# Android 端常见串口问题以及解决办法

```markdown
Q:无法枚举所有可用节点
A:一般app是通过读取/proc/tty/drivers这个文件的内容来获取,java代码会报java.io.FileNotFoundException: /proc/tty/drivers: open failed: EACCES (Permission denied)
F:解决办法,修改系统给/proc/tty/drivers文件444权限，app也可以通过new File("/dev/ttyHS0").exists判断文件节点是否存在


Q:无法枚举/dev/ttyUSB*节点
A:部分设备（高通cpu）需要将USB模式切换成OTG，模式以后才会出现/dev/ttyUSB*的节点
F:解决办法,在系统中找可以切换到OTG模式的app或者在设置中找OTG切换开关。


Q:串口打开异常
A:java层报java.io.IOException打开失败，一般是由于avc权限等问题导致，报错在IOException上方会有avc错误:type=1400 audit(0.0:1566): avc: denied { read write } for name="ttyHS0" dev="tmpfs" ino=15406 scontext=u:r:system_app:s0 tcontext=u:object_r:hci_attach_dev:s0 tclass=chr_file permissive=0
F:解决办法一、修改系统增加节点的读写权限；解决办法二、APP申明为系统app，并且用对应的系统签名。


Q:串口收发数据量过大导致app卡死甚至系统重启
A:可考虑替换串口so或者将多余的so库移除，仅保留【armeabi】文件夹内的so。


Q:串口工具收发乱码
A:乱码可以检查收发端波特率是否一致，也可以通过短接tx和rx自发自收排查是否还会存在乱码问题。

```