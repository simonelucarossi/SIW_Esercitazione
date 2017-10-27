package persistence.dao;

import java.util.List;

import model.CorsoDiLaurea;

public interface CorsoDiLaureaDao {
	public void save(CorsoDiLaurea corsoDiLaurea);  // Create
	public CorsoDiLaurea findByPrimaryKey(Long codice);     // Retrieve
	public List<CorsoDiLaurea> findAll();       
	public void update(CorsoDiLaurea corsoDiLaurea); //Update
	public void delete(CorsoDiLaurea corsoDiLaurea); //Delete	
}
