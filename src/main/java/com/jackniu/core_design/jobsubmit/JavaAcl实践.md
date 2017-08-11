## 使用动态Proxy和JavaAcl进行用户访问控制机制实现
用户访问控制(Access Control)机制总是围绕粗粒度和细粒度两个方面来讨论：

粗粒度控制: 可以规定访问整个对象或对象群的某个层，而细粒度控制则总是在方法或属性上进行。

运行一个文件为只读是属于粗粒度控制，而允许这个文件某行有写操作则属于细粒度控制

一个好的用户控制机制当然既允许粗粒度也允许细粒度控制，在Java 中通过使用Proxy来实现，由于需要对每个类都要进行细粒度控制，所以
必然对每个类都要做一个Proxy类，这样增加了许多Proxy类，增加了系统复杂性。

使用动态代理可以很好的解决这个问题，再集合java.security.acl 机制，可以灵活的实现粗粒度和细粒度的双重控制。

    当一个用户login后，就要在内存中为其建立相应的授权访问机制，使用java.security.acl可以很方便的建立这样一个安全系统。

    首先任何一个对应都应该有个基本属性: 拥有者，或者拥有者所属权组(window中每个目录安全由4部分构成: 对象的创建者，对象所属的组，自由存取控制和系统存取控制)
    
    1 Java Acl 第一步是建立一个主体Principal，其中SecurityOwner是主体的拥有者
    private static final Principal_securityOwner = new PrincipalImpl("SecurityOwner");
    
    2 当用户login进来时，带有两个基本数据: 访问密码和要访问的对象ApplicationName。首先验证用户名和密码，然后从数据库中取出权限数据，建立
    Permission,这里使用Feature继承了Permission，在Future中定义了有关权限的细节数据。
        
        // 取出用户和被访问对象之间的权限关系,这种权限关系可能不只一个，也就是说，用户
        //可能对被访问对象拥有读 写 删等多个权限，将其打包在Hasbtable中。
        Hashtable features = loadFeaturesForUser(sApplicationName, sUserID);
    
    3. 创建一个用户对象
        User user = new UserImpl(sUserID, new Hashtable() );
    4. 为这个对象创建一个活动的acl entity
        addAclentity(user,feature)
        
        // 为这个用户创建一个新的Acl entry
        AclEntry newAclEntry = new AclEntryImpl( user);
        
        //遍历Hashtable features，将其中多种权限加入:
        ....
        feature = (Feature) hFeatures.get(keyName);
        newAclEntry.addPermission( feature );
        ....
        
        最后也要加入主体拥有者SecurityOwner
        
        这样一个安全体系就已经建立完成。
        
        当你在系统中要检验某个用户使用拥有某个权限，如读的权利时，只要
        acl.checkPermission(user, feature )就可以,acl是ACL的一个实例，这样权限检查就交给
        java.security.acl.ACL 去处理了。
        
        有了ACL机制后，我们就可以在我们系统中使用动态Proxy模式来对具体对象或方法进行控制，
        比如，我们有一个Report类，有些用户可以读，有些用户可以写(哪些用户可以读 哪些用户可以写，已经在上面ACL里部署完成)。
        
        
        
    