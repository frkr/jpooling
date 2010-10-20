package util.polling;

class CreateConnectionThread implements Runnable {

	private Pooling pool;

	private CreateConnectionThread(Pooling pool) {
		this.pool=pool;
	}

	public static Thread start(Pooling pool) {
		Thread th = new Thread(new CreateConnectionThread(pool));
		th.setName("Pool-"+pool.threadString+"-Conn-" + th.getId());
		th.setDaemon(true);
		th.start();
		return th;
	}

	public void run() {
		pool.open();
	}

}
