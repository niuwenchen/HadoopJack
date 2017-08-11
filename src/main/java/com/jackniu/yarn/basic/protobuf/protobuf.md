## Protobuf

    1) 定义消息格式文件，通常以proto作为扩展名
    2) 使用Google提供的Protocol Buffers编译器生成特定语言(c++,Java,Python 三类语言)的代码文件
    3) 使用Python Buffers库提供的API来编写应用。
    
    package tutorial;               //自定义的命名空间
    option java_package="com.jackniu.tutorial";     // 生成文件的包名
    option java_outer_classname="PersonProtos";     //类名
    
    message Person{                     //待描述的结构化数据
        required string name=1;
        required int32  id=2;
        optional string email =3;
    
        message PhoneNumber{
            required string number =1;
            optional int32 type=2;
        }
        repeated PhoneNumber phone =4;
    }
    
    protoc  --java_out  outdir  infile
    测试
    
    // 在发送端创建消息，也就是client端或者本地端要发送的对象
    Person person = Person.newBuilder()
    				.setName("Jack")
    				.setEmail("540051856@qq.com")
    				.setId(1111)
    				.addPhone(Person.PhoneNumber.newBuilder().setNumber("123").setType(0))
    				.addPhone(Person.PhoneNumber.newBuilder()
    						.setNumber("345").setType(2)).build();
    // 在发送端发送，这里是发送到文本文件，但是也可以发送通过socket发送到远端
    FileOutputStream output = new FileOutputStream("example.txt");
    			person.writeTo(output);
    			output.close();
    
    
    .proto Type	Notes	C++ Type	Java Type	Python Type[2]	Go Type
    double		double	double	float	*float64
    float		float	float	float	*float32
    int32	Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead.	int32	int	int	*int32
    int64	Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead.	int64	long	int/long[3]	*int64
    uint32	Uses variable-length encoding.	uint32	int[1]	int/long[3]	*uint32
    uint64	Uses variable-length encoding.	uint64	long[1]	int/long[3]	*uint64
    sint32	Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s.	int32	int	int	*int32
    sint64	Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s.	int64	long	int/long[3]	*int64
    fixed32	Always four bytes. More efficient than uint32 if values are often greater than 228.	uint32	int[1]	int	*uint32
    fixed64	Always eight bytes. More efficient than uint64 if values are often greater than 256.	uint64	long[1]	int/long[3]	*uint64
    sfixed32	Always four bytes.	int32	int	int	*int32
    sfixed64	Always eight bytes.	int64	long	int/long[3]	*int64
    bool		bool	boolean	bool	*bool
    string	A string must always contain UTF-8 encoded or 7-bit ASCII text.	string	String	str/unicode[4]	*string
    bytes	May contain any arbitrary sequence of bytes.	string	ByteString	str	[]byte
    
    
    默认的数据： optional int32 result_per_page = 3 [default = 10];
    枚举类型
    enum Corpus {
        UNIVERSAL = 0;
        WEB = 1;
        IMAGES = 2;
        LOCAL = 3;
        NEWS = 4;
        PRODUCTS = 5;
        VIDEO = 6;
      }
      optional Corpus corpus = 4 [default = UNIVERSAL];
      
      
    
    序列化和反序列化
    Parsing and Serialization
    
    Finally, each protocol buffer class has methods for writing and reading messages of your chosen type using the protocol buffer binary format. 
    These include:
    
        byte[] toByteArray();: serializes the message and returns a byte array containing its raw bytes.
        static Person parseFrom(byte[] data);: parses a message from the given byte array.
        void writeTo(OutputStream output);: serializes the message and writes it to an OutputStream.
        static Person parseFrom(InputStream input);: reads and parses a message from an InputStream.
    
    为什么不用XML
    message Test1 {
      required int32 a = 1;
    }
    In an application, you create a Test1 message and set a to 150. You then serialize the message to an output stream. If you were able to examine the encoded message, you'd see three bytes:
    
    08 96 01
    So far, so small and numeric – but what does it mean? Read on...
    
    ProtocolBuffer
        方便引入新字段，中间服务器可以忽略这些字段，直接传递过去而无需理解所有的字段
        格式可以自描述，并且可以在多种语言中使用  C++, Java等，用户仍然需要手写解析代码
        简单的RPC请求。
        RPC服务器接口可以作为.photo文件来描述，通过ProtocolBuffer的编译(stub)类供用户实现服务器接口
        
        
    