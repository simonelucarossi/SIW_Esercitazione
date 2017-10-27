package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Corso;
import model.CorsoDiLaurea;
import model.Studente;
import persistence.dao.CorsoDao;
import persistence.dao.CorsoDiLaureaDao;


public class CorsoDiLaureaDaoJDBC implements CorsoDiLaureaDao {
	private DataSource dataSource;

	public CorsoDiLaureaDaoJDBC(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void save(CorsoDiLaurea corsoDiLaurea) {
		if ( (corsoDiLaurea.getCorsi() == null) 
				|| corsoDiLaurea.getCorsi().isEmpty()){
			throw new PersistenceException("Corso di laurea non memorizzato: un corso di laurea deve avere almeno un corso");
		}
		Connection connection = this.dataSource.getConnection();
		try {
			Long id = IdBroker.getId(connection);
			corsoDiLaurea.setCodice(id); 
			String insert = "insert into corsodilaurea(codice, nome, dipartimento_codice) values (?,?,?)";
			PreparedStatement statement = connection.prepareStatement(insert);
			statement.setLong(1, corsoDiLaurea.getCodice());
			statement.setString(2, corsoDiLaurea.getNome());
			statement.setLong(3, corsoDiLaurea.getDipartimento().getCodice());

			//connection.setAutoCommit(false);
			//serve in caso gli studenti non siano stati salvati. Il DAO studente apre e chiude una transazione nuova.
			//connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);			
			statement.executeUpdate();
			
			this.updateCorsi(corsoDiLaurea, connection);
			//connection.commit();
		} catch (SQLException e) {
			if (connection != null) {
				try {
					connection.rollback();
				} catch(SQLException excep) {
					throw new PersistenceException(e.getMessage());
				}
			} 
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}
	}
	
	private void updateCorsi(CorsoDiLaurea corsodilaurea, Connection connection) throws SQLException {
		CorsoDao corsoDao = new CorsoDaoJDBC(dataSource);
		for (Corso corso : corsodilaurea.getCorsi()) {
			if (corsoDao.findByPrimaryKey(corso.getCodice()) == null){
				corsoDao.save(corso);
			}
			String afferisce = "select id from afferisce where corso_codice=? AND corsodilaurea_codice=?";
			PreparedStatement statementAfferisce = connection.prepareStatement(afferisce);
			statementAfferisce.setLong(1, corso.getCodice());
			statementAfferisce.setLong(2, corsodilaurea.getCodice());
			ResultSet result = statementAfferisce.executeQuery();
			if(result.next()){
				String update = "update afferisce SET corsodilaurea_codice = ? WHERE id = ?";
				PreparedStatement statement = connection.prepareStatement(update);
				statement.setLong(1, corsodilaurea.getCodice());
				statement.setLong(2, result.getLong("id"));
				statement.executeUpdate();
			}else{			
				String iscrivi = "insert into afferisce(id, corso_codice, corsodilaurea_codice) values (?,?,?)";
				PreparedStatement statementIscrivi = connection.prepareStatement(iscrivi);
				Long id = IdBroker.getId(connection);
				statementIscrivi.setLong(1, id);
				statementIscrivi.setLong(2, corso.getCodice());
				statementIscrivi.setLong(3, corsodilaurea.getCodice());
				statementIscrivi.executeUpdate();
			}
		}
	}
	
	private void removeForeignKeyFromCorso(CorsoDiLaurea corsodilaurea, Connection connection) throws SQLException {
		for (Corso corso : corsodilaurea.getCorsi()) {
			String update = "update afferisce SET corsodilaurea_codice = NULL WHERE corso_codice = ?";
			PreparedStatement statement = connection.prepareStatement(update);
			statement.setLong(1, corso.getCodice());
			statement.executeUpdate();
		}	
	}
	
	/* 
	 * versione con Join
	 */
	public CorsoDiLaurea findByPrimaryKeyJoin(Long id) {
		Connection connection = this.dataSource.getConnection();
		CorsoDiLaurea corsoDiLaurea = null;
		try {
			PreparedStatement statement;
			String query = "select cl.codice as cl_codice, cl.nome as cl_nome, c.codice as c_codice, c.nome as c_nome, "
					+ "s.matricola as s_matricola, s.nome as s_nome, "
					+ "s.cognome as s_cognome, s.data_nascita as s_data_nascita, s.indirizzo_codice as s_indirizzo_codice "
					+ "from corsodilaurea cl, afferisce a, corso c, studente s, iscritto i "
					+ "where cl.codice = ?"
					+ "			AND cl.codice = a.corsodilaurea_codice "
					+ "			AND a.corso_codice = c.codice "
					+ "			AND i.corso_codice = c.codice "
					+ "			AND i.matricola_studente = s.matricola"
					;
			statement = connection.prepareStatement(query);
			statement.setLong(1, id);
			ResultSet result = statement.executeQuery();
			boolean primaRiga = true;
			while (result.next()) {
				if (primaRiga) {
					corsoDiLaurea = new CorsoDiLaurea();
					corsoDiLaurea.setCodice(result.getLong("cl_codice"));				
					corsoDiLaurea.setNome(result.getString("cl_nome"));
					primaRiga = false;
				}
				if(result.getString("c_codice")!=null){
					CorsoDaoJDBC corsoDao = new CorsoDaoJDBC(dataSource);
					Corso corso;
					corso = corsoDao.findByPrimaryKeyJoin(result.getLong("c_codice"));
////					corso.setCodice(result.getLong("c_codice"));
////					corso.setNome(result.getString("c_nome"));
//					
//					if (result.getString("s_matricola") != null){
//						StudenteDaoJDBC studenteDao = new StudenteDaoJDBC(dataSource);
//						corso.addStudente(studenteDao.findByPrimaryKey(result.getString("s_matricola")));
//					}
					
					corsoDiLaurea.addCorso(corso);
					
					
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage());
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}	
		return corsoDiLaurea;
	}

	/* 
	 * versione con Lazy Load
	 */
	public CorsoDiLaurea findByPrimaryKey(Long id) {
		Connection connection = this.dataSource.getConnection();
		CorsoDiLaurea corsoDiLaurea = null;
		try {
			PreparedStatement statement;
			String query = "select * from corsodilaurea where codice = ?";
			statement = connection.prepareStatement(query);
			statement.setLong(1, id);
			ResultSet result = statement.executeQuery();
			if (result.next()) {
				corsoDiLaurea = new CorsoDiLaurea();
				corsoDiLaurea.setCodice(result.getLong("codice"));				
				corsoDiLaurea.setNome(result.getString("nome"));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage());
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}	
		return corsoDiLaurea;
	}

	public List<CorsoDiLaurea> findAll() {
		Connection connection = this.dataSource.getConnection();
		List<CorsoDiLaurea> corsidilaurea = new ArrayList<>();
		try {			
			CorsoDiLaurea corsoDiLaurea;
			PreparedStatement statement;
			String query = "select * from corsodilaurea";
			statement = connection.prepareStatement(query);
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				corsoDiLaurea = findByPrimaryKeyJoin(result.getLong("codice"));
				corsidilaurea.add(corsoDiLaurea);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage());
		}	 finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}
		return corsidilaurea;
	}

	public void update(CorsoDiLaurea corsodilaurea) {
		Connection connection = this.dataSource.getConnection();
		try {
			String update = "update corso SET nome = ? WHERE codice = ?";
			PreparedStatement statement = connection.prepareStatement(update);
			statement.setString(1, corsodilaurea.getNome());
			statement.setLong(2, corsodilaurea.getCodice());

			//connection.setAutoCommit(false);
			//connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);			
			statement.executeUpdate();
			this.updateCorsi(corsodilaurea, connection); // se abbiamo deciso di propagare gli update seguendo il riferimento
			//connection.commit();
		} catch (SQLException e) {
			if (connection != null) {
				try {
					connection.rollback();
				} catch(SQLException excep) {
					throw new PersistenceException(e.getMessage());
				}
			} 
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}
	}

	public void delete(CorsoDiLaurea corsoDiLaurea) {
		Connection connection = this.dataSource.getConnection();
		try {
			String delete = "delete FROM corsodilaurea WHERE codice = ? ";
			PreparedStatement statement = connection.prepareStatement(delete);
			statement.setLong(1, corsoDiLaurea.getCodice());

			/* 
			 * rimuoviamo gli studenti dal gruppo (ma non dal database) 
			 * potevano esserci soluzioni diverse (ad esempio rimuovere tutti gli studenti dal database
			 * (ma in questo caso non avrebbe senso)
			 * La scelta dipende dalla semantica delle operazioni di dominio
			 * 
			 * */
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);			
			this.removeForeignKeyFromCorso(corsoDiLaurea, connection);     			
			/* 
			 * ora rimuoviamo il gruppo
			 * 
			 * */
			statement.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage());
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage());
			}
		}
	}


}
