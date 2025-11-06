# Features

* No Singleton
* Thread Safe
* Leak detection
* Daemon thread for new connections and closing connections.
* Timer for killing all connections. Recreates automatically if used again.
* Another pooling named "PoolingFreshing" for setting how many times the same connection will be used.
* 'DriverManager.getConnection' method can be overridden.
* getConnection could cause wait/stuck situations. Will create another connection exceeding the total connections. (Force New
  on Stuck setting)
* ClosePool will try to close leaked connections.

# Know issues

* For JTDS the "isValid" method does not work. So overwrite it.
* Some databases are automatically "Auto commit". So, you can disable it manually.

### See Settings on class

[See settings on constructor](src/main/java/com/github/frkr/jpooling/Pooling.java)

# Why?

- Why was this project made? In a very old project, it was used because there was a bug in the cluster where from time to time the connection with jTDS was no longer valid.

- To protect the copyright solution (as well as all the other things I put in the git).

- For laziness to put something else in the project inspired this class here: https://gist.github.com/frkr/d1d4707d094bb61ac3abfa06600b29f1

> Finally, in hypothetical scenarios where performance is needed, this application has already been tested in critical scenarios.

# Best features

> The main reason I keep looking at this project is the use of Proxy Class, Autocloseable and how to use Stacktrace to your advantage to detect problems (used to know where the leak is)
