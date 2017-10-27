package persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UtilDao {

	
private DataSource dataSource;

public UtilDao(DataSource dataSource) {
		this.dataSource=dataSource;
	}

public void dropDatabase(){
	
	Connection connection = dataSource.getConnection();
	try {
		String delete = "drop SEQUENCE if EXISTS sequenza_id;"
				+ "drop table if exists iscritto;"
				+ "drop table if exists afferisce;"							
				+ "drop table if exists studente;"
				+ "drop table if exists corso;"
				+ "drop table if exists corsodilaurea;"
				+ "drop table if exists dipartimento;"
				+ "drop table if exists indirizzo;"
				+ "drop table if exists gruppo;"
				;
		PreparedStatement statement = connection.prepareStatement(delete);
		
		statement.executeUpdate();
		System.out.println("Executed drop database");
		
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

public void createDatabase(){
	
	Connection connection = dataSource.getConnection();
	try {
		
		String delete = "create SEQUENCE sequenza_id;"
				+ "create table gruppo (\"id\" bigint primary key, nome varchar(255));"
				+ "create table indirizzo (\"codice\" bigint primary key, nome varchar(255));"
				+ "create table corso (\"codice\" bigint primary key, nome varchar(255));"
				+ "create table dipartimento(\"codice\" bigint primary key,nome varchar(255));"
				+ "create table corsodilaurea(\"codice\" bigint primary key,nome varchar(255),dipartimento_codice bigint REFERENCES dipartimento(\"codice\"));"				
				+ "create table afferisce(\"id\" bigint primary key, corso_codice bigint REFERENCES corso(\"codice\"), corsodilaurea_codice bigint REFERENCES corsodilaurea(\"codice\"));"
				+ "create table studente(matricola CHARACTER(8) primary key,"				
				+ "nome VARCHAR(255),cognome VARCHAR(255),"
				+ "data_nascita DATE, gruppo_id bigint REFERENCES gruppo(\"id\"), indirizzo_codice bigint REFERENCES indirizzo(\"codice\"));"
				+ "create table iscritto(\"id\" bigint primary key, matricola_studente CHARACTER(8) REFERENCES studente(\"matricola\"), corso_codice bigint REFERENCES corso(\"codice\"));";
		
		PreparedStatement statement = connection.prepareStatement(delete);
		
		statement.executeUpdate();
		System.out.println("Executed create database");
		
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


public  void resetDatabase() {
		
		Connection connection = dataSource.getConnection();
		try {
			String delete = "delete FROM studente";
			PreparedStatement statement = connection.prepareStatement(delete);
			
			statement.executeUpdate();

			delete = "delete FROM gruppo";
			statement = connection.prepareStatement(delete);
			
			delete = "delete FROM corso";
			statement = connection.prepareStatement(delete);
			
			statement.executeUpdate();
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
