package util.polling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitária para controlar Pool de conexoes
 * @author Davi Mesquita
 * @version 1.0
 */
public class Pooling {

	/**
	 * Pulso do Pooling para ser morto
	 * Default = 300000L = 5 Minutos
	 * 120000L = 2 minutos
	 */
	public long PULSEDEAD=120000L;

	protected String connectionUrl;
	protected boolean dead=false;
	protected List<PoolingConnection> pool=new ArrayList<PoolingConnection>();
	protected List<PoolingConnection> alive=new ArrayList<PoolingConnection>();
	protected int connections;
	protected String threadString="Pool";
	private Thread th=null;

	/**
	 * Abre 5 conexoes
	 * @param connectionUrl URL de conexao ao Banco
	 */
	public Pooling(String connectionUrl) {
		this(connectionUrl,5);
	}
	/**
	 * @param connectionUrl URL de conexao ao Banco
	 * @param connections Numero maximo de conexoes do pooling.
	 */
	public Pooling(String connectionUrl, int connections) {
		this(connectionUrl,connections,"Pool");
	}
	/**
	 * @param connectionUrl URL de conexao ao Banco
	 * @param connections Numero maximo de conexoes do pooling.
	 * @param thread_prefix Prefixo da thread
	 */
	public Pooling(String connectionUrl, int connections,String thread_prefix) {
		this.connections=connections;
		this.connectionUrl=connectionUrl;
		this.threadString=thread_prefix;
		iniciarThread(this.threadString);
	}
	/**
	 * NÃO ESQUECER DE FECHAR A CONEXAO<br>
	 * Retornar sempre. Nao existe timeout.
	 * @return {@link Connection}
	 */
	public PoolingConnection getConnection() {
		return getConnection(0);
	}
	private synchronized void iniciarThread(String prefix) {
		if (th==null || !th.isAlive()) {
			th = ThreadPooling.start(this, prefix);
		}
	}
	/**
	 * NÃO ESQUECER DE FECHAR A CONEXAO
	 * @param timeout Milis quanto tempo esperar ate retornar nulo
	 * @return {@link Connection}
	 */
	public PoolingConnection getConnection(long timeout) {
		if (dead) {
			dead=false;
			iniciarThread(this.threadString);
		}
		long tempo = System.currentTimeMillis() + timeout;
		while (true) {
			PoolingConnection cn=null;
			synchronized (pool) {
				try {
					cn = pool.get(0);
					pool.remove(0);
				} catch (Exception e) {
				}
			}
			if (cn != null) {
				try {
					if (isValid(cn)) {
						synchronized (alive) {
							alive.add(cn);
							return cn;
						}
					} else
						throw new SQLException("isValid");
				} catch (Exception e) {
					CreateConnectionThread.start(this);
				}
			}
			try {
				Thread.sleep(100); // XXX Timeout
			} catch (Exception e) {
			}
			if (timeout!=0 && System.currentTimeMillis() > tempo)
				return null;
		}
	}
	protected void close(PoolingConnection conn) {
		synchronized (alive) {
			alive.remove(conn);
		}
		if (dead || pool.size()+alive.size()+1 > connections) {
			conn.kill();
		} else {
			synchronized (pool) {
				pool.remove(conn);
				pool.add(conn);
			}
		}
	}
	protected boolean open() {
		if (!dead && pool.size()+alive.size() < connections) {
			Connection conn=null;
			try {
				conn = DriverManager.getConnection(connectionUrl);
				Thread.yield();
				if (isValid(conn)) {
					synchronized (pool) {
						pool.add(new PoolingConnection(conn,this));
					}
				} else
					throw new SQLException("isValid");
			} catch (Exception e) {
				e.printStackTrace();
				try {
					conn.close();
				} catch (Exception e2) {
				}
				CreateConnectionThread.start(this);
			}
			return true;
		}
		return false;
	}

	/**
	 * in jtds does not work
	 * @param conn Connection
	 * @return boolean
	 * @throws SQLException isValid may throw errors
	 */
	protected boolean isValid(Connection conn) throws SQLException {
		return conn.isValid( 15 ); // XXX Timeout
	}

	/**
	 * @return pool size
	 */
	public int getStandby() {
		return pool.size();
	}

	/**
	 * @return active size
	 */
	public int getActive() {
		return alive.size();
	}

	/**
	 * Fechar pooling
	 */
	public void closePool() {
		dead=true;
		List<PoolingConnection> ln = null;
		synchronized (pool) {
			ln = new ArrayList<PoolingConnection>( pool );
			pool.removeAll(ln);
		}
		for (PoolingConnection o: ln) {
			o.setPool(null);
		}
		for (PoolingConnection o: ln) {
			o.kill();
		}
	}

}
