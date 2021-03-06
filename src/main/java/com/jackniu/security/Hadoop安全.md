## Hadoop 安全管理
由于Hadoop集群都部署在有防火墙保护的局域网中且只允许公司内部人员访问，因此为Hadoop添加安全机制的动机并不是
传统的安全概念那样为了防御外部黑客的攻击，而是为了更好的让多用户在共享Hadoop集群环境下安全高效的使用集群资源。

    Hadoop对安全方面的需求
    1 引入授权机制，只有经授权的用户才可以访问Hadoop
    2 任何用户只能访问那些有权限访问的文件或目录
    3 任何用户只能修改或杀死自己的作业
    4 服务与服务之间引入权限认证机制，防止未经授权的服务接入Hadoop集群
    5 新引入的安全机制应对用户透明，且哟过户可放弃使用该机制以保证向后兼容。
    
    性能需求： 引入安全机制后带来的开销应在可接受范围内
    
### 基础知识
Hadoop RPC中采用了SASL(Simple Authentication and Security Layer,简单认证和安全层)。

11.2.1 安全认证机制

    1 SASL
    SASL是一种拓展C/S模式验证能力的认证机制。核心思想是把用户认证和安全传输从应用程序中隔离出来。比如SMTP
    > Anonymous: 无须认证。
    > Plain      最简单的认证机制，采用明文密码传输
    > Digest-md5: 基于MD5，可以提供数据的安全传输层，这是方便性和安全性集合的最好的一种方式，也是SASL默认采用的方式
    客户端和服务器共享同一个密钥，而且该密钥不从网络传输，验证过程是从服务器端先提出质询开始，客户端使用此质询
    与密钥计算出一个应答。不同的质询，不可能计算出相同的应答。
    > GSSAPI:通用安全服务应用接口
    2 JAAS
    JAAS主要由认证和授权两大部分组成，认证就是简单的对一个实体的身份进行判断;而授权则是想实体授予对数据资源和信息
    访问权限的决策过程。
    JAAS通过插件的方式工作，这使得Java应用程序独立于底层的认证技术，应用程序可以使用新的或经过修改的认证技术
    而不需要修改应用程序本身。应用程序通过实例化一个登录上下文对象来开始认证过程，这个对象根据配置决定采用哪个登录
    模块，而登录模块决定了认证技术和登录方式，一个比较典型的饿就是输入用户名和口令，其他登录方式还有读入并核实声音或指纹样本
    
    JAAS的核心类可以分为公共类、认证类和授权类三部分。其中公共类包括Subject、Principal、Credential三个类:
    认证类包括LoginContext,LoginModule,CallbackHandler,Callback四个类；授权类包括Policy，AuthPermission和PrivateCrtxentialPermission三个类
    Hadoop中仅用到了公共类和授权类，没有认证类
    
    