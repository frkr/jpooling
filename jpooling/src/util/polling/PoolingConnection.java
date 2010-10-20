package util.polling;

import java.sql.Connection;

import util.sql.ConnectionWrapper;

/**
 * Classe utilitária para controlar Pool de conexoes
 * @author Davi Mesquita
 * @version 1.0
 */
public class PoolingConnection extends ConnectionWrapper {

	private Pooling pool;
	protected int used=0;
	protected PoolingConnection(Connection conn, Pooling pool) {
		super(conn);
		this.pool=pool;
	}

	@Override
	public void close() {
		Pooling po=getPool();
		if (po==null) {
			try {
				super.close();
			} catch (Exception e) {
			}
		} else {
			po.close(this);
		}
	}

	protected synchronized Pooling getPool() {
		return pool;
	}

	protected synchronized void setPool(Pooling pool) {
		this.pool = pool;
	}

	protected void kill() {
		setPool(null);
		try {
			super.close();
		} catch (Exception e) {
		}
	}

}
