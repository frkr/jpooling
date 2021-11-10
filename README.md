# Features

* No SingleTon
* Thread Safe
* Leak detection
* Deamon thread for new connections
* Timer for killing all connection. Recreates automatically if used again.
* Another pooling named "PoolingFreshing" for setting how much times will use the same connection.
* 'DriverManager.getConnection' method could be overrided.
* getConnection could draw several wait/stuck. Will create another connection exceding the total connections. (Force New
  on Stuck setting)
* ClosePool will try to close leaked connections.

# Know issues

* For JTDS does not work the "isValid" method. So overwrite it.
* Some databases are automatically "Auto commit". So, you can disable it manually.

### See Settings on class

[See settings on constructor](src/main/java/com/github/frkr/jpooling/Pooling.java)
