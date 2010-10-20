package util.polling;

public class PoolingFreshing extends Pooling {

	private int refresh;

	public PoolingFreshing(
			String connectionUrl,
			int connections,
			String threadPrefix,
			int refresh
	) {
		super(connectionUrl, connections, threadPrefix);
		this.refresh=refresh;
	}

	@Override
	protected void close(PoolingConnection conn) {
		if (++conn.used < this.refresh) {
			super.close(conn);
		} else {
			synchronized (alive) {
				alive.remove(conn);
			}
			conn.kill();
			CreateConnectionThread.start(this);
		}
	}

}
