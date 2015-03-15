# Features

  * No SingleTon
  * Creates a deamon thread for create a new connection.
  * Timer for killing all connection. Recreates automatically if used again.
  * Another pooling named "PoolingFreshing" for setting how much times will use the same connection.
  * Thread Safe

# Know issues

  * Do not forget to close an connection. Otherwise you will have a lost connection for ever.
  * For JTDS does not work the "isValid" method. So overwrite it.
  * Method "isValid" has 15 seconds of timeout.
  * Some databases are automatically "Auto commit". So, you can disable it manually.
