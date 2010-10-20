package util.polling;

class ThreadPooling implements Runnable {

	private Pooling pool;
	private ThreadPooling(Pooling pool) {
		this.pool=pool;
	}

	public static Thread start(Pooling pool, String prefix) {
		Thread th = new Thread(new ThreadPooling(pool));
		th.setName(prefix+"-" + th.getId());
		th.setDaemon(true);
		th.start();
		return th;
	}

	public void run() {
		while (!pool.dead) {
			criarConexoes();
			try {
				Thread.sleep(pool.PULSEDEAD);
			} catch (Exception e) {
			}
			if (pool.alive.size()==0) {
				MatarPooling();
			}
		}
	}

	private void MatarPooling() {
		pool.closePool();
	}

	private void criarConexoes() {
		while (pool.open()) {
			;
		}
	}

}
