paxos的Java实现

# paxos细节思考
1.为什么proroser要取编号最大的value（如果存在），为什么较小的不行？

可能有一些value只被少部分acceptor接收，后面有一些更大编号的value prepare的时候没经过这些少部分acceptor，所以选了新的值。
要大多数acceptor接收了这个值才能确定被选择了。

2.为什么需要提交已经确定了值的提案？

proposer需要确定当前的instance确定了哪个值。因为可能proposer以为之前ballot没提交成功（prepareresponse丢失或超时），实际上大多数Acceptor已经accept，这个值已经被选择了,如果在重试过程中发现已经确定值了不重复提交，而是开启下一个instance，就会造成这个value在下一个instance重复提交

还有可能value只被少部分acceptor接收，实际上没有成功提交，所以需要重复提交一次。如果不重复提交，在下一个instance中提交值，就会可能造成上一个instance没有一个值被大多数acceptor选择

# 共同结构
接受数据包后分发。

把接受的消息放在一个队列里，然后轮循线程不停的处理

# proposer
prepare和accept过程需要过一段时间检查是否执行成功，如果不成功重试。

如果使用timer来延时执行，就会造成多线程竞争，需要加锁。

只采用一个线程，避免了加锁消耗。

使用优先队列维护下一次检查的时间点，队列poll来保证在时间节点前返回。



# 网络
tcp由于是流协议，需要拆包分包，通过在消息前加上长度来区分每个数据包。

## 接收
异步处理连接建立和读取消息。

每个连接记录当前读消息长度还是消息内容。每次能读多少读多少，然后做记录，不会while循环等待一个消息包全部读取。

## 发送
记录每个地址对应的连接。

把要发送的数据包缓存在一个队列里，然后主线程不停读取发送

# 学习模仿
https://github.com/luohaha/MyPaxos

https://github.com/Tencent/phxpaxos
